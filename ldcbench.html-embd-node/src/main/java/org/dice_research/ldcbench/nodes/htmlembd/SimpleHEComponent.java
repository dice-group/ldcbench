package org.dice_research.ldcbench.nodes.htmlembd;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.htmlembd.singlefile.SingleFileResource;
import org.dice_research.ldcbench.nodes.http.simple.CrawleableResource;
import org.dice_research.ldcbench.nodes.http.simple.CrawleableResourceContainer;
import org.dice_research.ldcbench.nodes.http.simple.FileBasedResource;
import org.dice_research.ldcbench.nodes.http.simple.SimpleHttpServerComponent;
import org.dice_research.ldcbench.nodes.rabbit.DataHandler;
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

public class SimpleHEComponent extends SimpleHttpServerComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHEComponent.class);

    public static final String ACCESS_URI_TEMPLATE_PATTERN = "/%s-%s/%s-%s";
    public static final String ENTRANCE_HTML_FILE_NAME = "entrance.html";
    public static final String ENTRANCE_TTL_FILE_NAME = "entrance.ttl";

    public static final String CONTENT_EXTRACTION_DIR = "content/";
    public static final String SINGLE_FILE_NAME = "singleFile.html";

    protected DataHandler dataHandler;

    private boolean singleFileNode;
    private String  singleFilePath;

    @Override
    public void initBeforeDataGeneration() throws Exception {
        port = EnvVariables.getInt(ApiConstants.ENV_HTTP_PORT_KEY, SimpleHttpServerComponent.DEFAULT_PORT, LOGGER);
        //crawlDelay = EnvVariables.getInt(ApiConstants.ENV_CRAWL_DELAY_KEY, LOGGER);

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

        singleFileNode = EnvVariables.getBoolean("LDCBENCH_USE_SINGLE_FILE", false);

        String pathTemplate;
        if(singleFileNode) {
            // Create path including the single file name
            StringBuilder builder = new StringBuilder(CONTENT_EXTRACTION_DIR);
            builder.append(SINGLE_FILE_NAME);
            singleFilePath = builder.toString();
            LOGGER.debug("Path: {}", singleFilePath);
            builder.append("#%s-%s-%s-%s");
            pathTemplate = builder.toString();
        } else {
            pathTemplate = ACCESS_URI_TEMPLATE_PATTERN;
        }

        uriBuilder.append(pathTemplate);
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
        File contentDir = new File(CONTENT_EXTRACTION_DIR);
        contentDir.mkdir();
        Set<String> files = extractTarFiles(contentDir);

        if(singleFileNode) {
            container = createSingleFileContainer();
        } else {

            // Create URL to File mapping
            Map<String, File> mapping = createMapping(contentDir, files);

            // Create container using the file mapping
            container = new CrawleableResourceContainer(new FileBasedResource(mapping, "text/html"));
        }

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
            if (ENTRANCE_HTML_FILE_NAME.equals(f)) {
                mapping.put(new URL(String.format(accessUriTemplate, "dataset", "0", "resource", "0")).getPath(), new File(prefix, f));
            } else {
                mapping.put(new URL(f).getPath(), new File(prefix, f));
            }
        }
        return mapping;
    }

    protected Container createSingleFileContainer() throws Exception {
       ArrayList<CrawleableResource> resources = new ArrayList<>();
       CrawleableResource resource = null;

       if (dumpFileNode) {
           resource = new SingleFileResource(r -> r.getPath().toString().equals(singleFilePath), singleFilePath);
       }
       Objects.requireNonNull(resource, "Couldn't create crawleable resource. Exiting.");
       resources.add(resource);
       return new CrawleableResourceContainer(resources.toArray(new CrawleableResource[resources.size()]));
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(connection);
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

        String[] receivedFiles = dataHandler.getReceivedFiles();
        if(singleFileNode) {
            if(receivedFiles.length != 1) {
                LOGGER.error("The amount of data received by the singleFileNode is incorrect!");
            } else {
                files.addAll(reader.read(new File(receivedFiles[0]), handler, true));
            }
        } else {
            for (String file : dataHandler.getReceivedFiles()) {
                files.addAll(reader.read(new File(file), handler, true));
            }
        }
        return files;
    }

}
