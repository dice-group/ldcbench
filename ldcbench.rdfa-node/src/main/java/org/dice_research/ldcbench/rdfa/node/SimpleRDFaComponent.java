package org.dice_research.ldcbench.rdfa.node;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.nodes.http.simple.CrawleableResourceContainer;
import org.dice_research.ldcbench.nodes.http.simple.FileBasedResource;
import org.dice_research.ldcbench.nodes.http.simple.SimpleHttpServerComponent;
import org.dice_research.ldcbench.nodes.rabbit.DataHandler;
import org.dice_research.ldcbench.rdfa.gen.RDFaDataGenerator;
import org.dice_research.ldcbench.utils.CloseableHelper;
import org.dice_research.ldcbench.utils.tar.FileHandler;
import org.dice_research.ldcbench.utils.tar.SimpleWritingFileHandler;
import org.dice_research.ldcbench.utils.tar.TarFileReader;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.hobbit.utils.EnvVariables;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRDFaComponent extends SimpleHttpServerComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRDFaComponent.class);

    protected DataHandler dataHandler;

    @Override
    public void initBeforeDataGeneration() throws Exception {
        port = EnvVariables.getInt(ApiConstants.ENV_HTTP_PORT_KEY, SimpleHttpServerComponent.DEFAULT_PORT, LOGGER);
        compressedRatio = Double.parseDouble(EnvVariables.getString(ApiConstants.ENV_COMPRESSED_RATIO_KEY, LOGGER));
        disallowedRatio = Double.parseDouble(EnvVariables.getString(ApiConstants.ENV_DISALLOWED_RATIO_KEY, LOGGER));
        crawlDelay = EnvVariables.getInt(ApiConstants.ENV_CRAWL_DELAY_KEY, LOGGER);

        String hostname = InetAddress.getLocalHost().getHostName();
        LOGGER.info("Hostname: {}", hostname);
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append("http://");
        if (dockerized) {
            uriBuilder.append(hostname);
        } else {
            uriBuilder.append("localhost");
        }
        if (port != 80) {
            uriBuilder.append(':').append(port);
        }
        uriBuilder.append(RDFaDataGenerator.ACCESS_URI_TEMPLATE_PATTERN);

        accessUriTemplate = uriBuilder.toString();
        resourceUriTemplate = accessUriTemplate;
    }

    /**
     * A method that creates a thread for receiving the data of from the data
     * generator.
     * 
     * @return
     * @throws IOException
     */
    protected Thread createReceiverThread() throws IOException {
        String queueName = EnvVariables.getString(ApiConstants.ENV_DATA_QUEUE_KEY);
        fileReceiver = SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);
        dataHandler = new DataHandler(fileReceiver);
        return new Thread(dataHandler);
    }

    /**
     * Method that triggers the receiver thread to terminate, waits for it to finish
     * its work and checks the received data.
     * 
     * @param receiverThread
     * @throws InterruptedException
     */
    protected void joinReceiverThread(Thread receiverThread) throws InterruptedException {
        fileReceiver.terminate();
        receiverThread.join();

        if (dataHandler.encounteredError()) {
            throw new IllegalStateException("Encountered an error while receiving graphs.");
        }
        String files[] = dataHandler.getReceivedFiles();
        if ((files == null) || (files.length == 0)) {
            throw new IllegalStateException("Didn't receive a single graph.");
        }
        if (nodeMetadata == null) {
            throw new IllegalStateException("Didn't receive the URI templates from the benchmark controller.");
        }
    }

    @Override
    public void initAfterDataGeneration() throws Exception {
        // Extract received tar file
        File contentDir = new File("content");
        contentDir.mkdir();
        Set<String> files = extractTarFiles(contentDir);

        // Create URL to File mapping
        Map<String, File> mapping = createMapping(contentDir, files);

        // Create container using the file mapping
        Container container = new CrawleableResourceContainer(new FileBasedResource(mapping, "text/html"));
        // Start server
        
        server = new ContainerServer(container);
        connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(port);
        connection.connect(address);
    }

    protected Map<String, File> createMapping(File contentDir, Set<String> files) throws MalformedURLException {
        Map<String, File> mapping = new HashMap<>();
        String prefix = contentDir.getAbsolutePath() + File.separator;
        for (String f : files) {
            if (RDFaDataGenerator.ENTRANCE_HTML_FILE_NAME.equals(f)) {
                mapping.put(new URL(String.format(accessUriTemplate, "dataset", "0", "resource", "0")).getPath(), new File(prefix, f));
            } else {
                mapping.put(new URL(f).getPath(), new File(prefix, f));
            }
        }
        return mapping;
    }

    protected Container createContainer() throws Exception {
//        Graph[] graphsArray = graphs.toArray(new Graph[graphs.size()]);
//        ArrayList<CrawleableResource> resources = new ArrayList<>();
//        CrawleableResource resource = null;
//        String[] resourceUriTemplates = Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate())
//                .toArray(String[]::new);
//        String[] accessUriTemplates = Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate())
//                .toArray(String[]::new);
//        if (dumpFileNode) {
//            resource = DumpFileResource.create(cloudNodeId.get(),
//                    Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate()).toArray(String[]::new),
//                    Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate()).toArray(String[]::new),
//                    graphs.toArray(new Graph[graphs.size()]),
//                    r -> r.getPath().toString().equals(dumpFilePath),
//                    dumpFileLang, dumpFileCompression);
//        } else {
//            SimpleTripleCreator tripleCreator = new SimpleTripleCreator(cloudNodeId.get(), resourceUriTemplates,
//                    accessUriTemplates);
//            HashSet<String> disallowedPaths = new HashSet<>();
//            Random random = new Random(seedGenerator.getNextSeed());
//            for (int g = 0; g < graphs.size(); g++) {
//                GraphBuilder gb = new GrphBasedGraph(graphs.get(g));
//                int nodes = gb.getNumberOfNodes();
//                int disallowedAmount = Math.max((int)(nodes * disallowedRatio / (1 - disallowedRatio)), disallowedRatio == 0 ? 0 : 1);
//                LOGGER.debug("Adding {} disallowed resources...", disallowedAmount);
//                for (int i = 0; i < disallowedAmount; i++) {
//                    int linkingNode = random.nextInt(nodes);
//                    // Make sure it's an internal node (belonging to this graph).
//                    // Otherwise, the link will not be available for crawling.
//                    while (gb.getGraphId(linkingNode) != Graph.INTERNAL_NODE_GRAPH_ID) {
//                        linkingNode = (linkingNode + 1) % nodes;
//                    }
//                    int disallowedNode = gb.addNode();
//                    gb.addEdge(linkingNode, disallowedNode, 0);
//                    String path = new URL(tripleCreator.createNode(disallowedNode, -1, -1, false).toString()).getPath();
//                    disallowedPaths.add(path);
//                    LOGGER.debug("Added a disallowed resource {}.", path);
//                }
//                graphsArray[g] = gb.build();
//            }
//            resources.add(new RobotsResource(disallowedPaths, crawlDelay));
//            disallowedResource = new DisallowedResource(disallowedPaths);
//            resources.add(disallowedResource);
//
//            // Create list of available content types
//            Set<String> contentTypes = new HashSet<String>();
//            for (Lang lang : RDFLanguages.getRegisteredLanguages()) {
//                if (!RDFLanguages.RDFNULL.equals(lang)) {
//                    contentTypes.add(lang.getContentType().getContentType());
//                    contentTypes.addAll(lang.getAltContentTypes());
//                }
//            }
//            // Create the container based on the information that has been received
//            graphBasedResource = new GraphBasedResource(cloudNodeId.get(), resourceUriTemplates, accessUriTemplates,
//                    graphsArray,
//                    (r -> r.getTarget().contains(UriHelper.DATASET_KEY_WORD)
//                            && r.getTarget().contains(UriHelper.RESOURCE_NODE_TYPE)),
//                    contentTypes.toArray(new String[contentTypes.size()]));
//            resource = graphBasedResource;
//        }
//        Objects.requireNonNull(resource, "Couldn't create crawleable resource. Exiting.");
//        resources.add(resource);
//        return new CrawleableResourceContainer(resources.toArray(new CrawleableResource[resources.size()]));
        return null;
    }

    @Override
    public void close() throws IOException {
        CloseableHelper.closeQuietly(connection);
        try {
            if (server != null) {
                server.stop();
            }
        } catch (IOException e) {
            LOGGER.error("Exception while closing server. It will be ignored.", e);
        }
        super.close();
    }

    protected Set<String> extractTarFiles(File contentDir) throws IOException {
        TarFileReader reader = new TarFileReader();
        FileHandler handler = new SimpleWritingFileHandler(contentDir.getAbsolutePath());
        Set<String> files = new HashSet<>();
        for (String file : dataHandler.getReceivedFiles()) {
            files.addAll(reader.read(new File(file), handler, true));
        }
        return files;
    }

}
