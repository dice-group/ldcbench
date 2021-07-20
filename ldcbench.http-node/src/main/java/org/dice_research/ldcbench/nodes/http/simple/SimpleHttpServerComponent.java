package org.dice_research.ldcbench.nodes.http.simple;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.dice_research.ldcbench.nodes.components.NodeComponent;
import org.dice_research.ldcbench.nodes.http.simple.dump.DumpFileBuilder;
import org.dice_research.ldcbench.nodes.http.simple.dump.DumpFileResource;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.Archiver;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.CompressionStreamFactory;
import org.dice_research.ldcbench.nodes.utils.LangUtils;
import org.dice_research.ldcbench.rdf.SimpleTripleCreator;
import org.dice_research.ldcbench.rdf.UriHelper;
import org.dice_research.ldcbench.utils.CloseableHelper;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.core.components.Component;
import org.hobbit.utils.EnvVariables;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toools.collections.Collections;

public class SimpleHttpServerComponent extends NodeComponent implements Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpServerComponent.class);

    protected static final int DEFAULT_PORT = 80;

    protected int port;
//    protected String pathTemplate;
    protected Container container;
    protected Server server;
    protected Connection connection;
    protected boolean dumpFileNode;
    protected int crawlDelay;
    protected double compressedRatio;
    protected double disallowedRatio;
    protected GraphBasedResource graphBasedResource = null;
    protected DisallowedResource disallowedResource = null;
    protected String dumpFilePath = null;
    protected Lang dumpFileLang = null;
    protected CompressionStreamFactory dumpFileCompression = null;
    protected Archiver dumpfileArchiver = null;

    @Override
    public void initBeforeDataGeneration() throws Exception {
        port = EnvVariables.getInt(ApiConstants.ENV_HTTP_PORT_KEY, DEFAULT_PORT, LOGGER);
        compressedRatio = Double.parseDouble(EnvVariables.getString(ApiConstants.ENV_COMPRESSED_RATIO_KEY, LOGGER));
        disallowedRatio = Double.parseDouble(EnvVariables.getString(ApiConstants.ENV_DISALLOWED_RATIO_KEY, LOGGER));
        crawlDelay = EnvVariables.getInt(ApiConstants.ENV_CRAWL_DELAY_KEY, LOGGER);

        String hostname = InetAddress.getLocalHost().getHostName();
        LOGGER.info("Hostname: {}", hostname);
        String authority = (dockerized ? hostname : "localhost") + (port == 80 ? "" : ":" + port);

        // check whether this node contains dump files
        dumpFileNode = EnvVariables.getBoolean("LDCBENCH_USE_DUMP_FILE", false);
        String pathTemplate;
        if (dumpFileNode) {
            LOGGER.debug("Init as HTTP dump file node.");
            Random random = new Random(seedGenerator.getNextSeed());

            // FIXME: it helps to avoid constant result after the first nextInt(4)
            random.nextInt();

            dumpFileLang = LangUtils.getRandomLang(benchmarkParamModel, random);
            LOGGER.debug("Language: {}", dumpFileLang);
            LOGGER.debug("File extensions: {}", dumpFileLang.getFileExtensions());

            if (random.nextDouble() < compressedRatio) {
                if (random.nextDouble() < (DumpFileBuilder.COMPRESSIONS.size()/(DumpFileBuilder.COMPRESSIONS.size()
                                                    + DumpFileResource.ARCHIVERS.size()))) {
                    dumpFileCompression = Collections.pickRandomObject(DumpFileBuilder.COMPRESSIONS, random);
                }
                else {
                    dumpfileArchiver = Collections.pickRandomObject(DumpFileResource.ARCHIVERS, random);
                }
            }

            // Create path including the dump file name
            StringBuilder builder = new StringBuilder("/dumpFile");
            builder.append(".");
            builder.append(dumpFileLang.getFileExtensions().get(0));
            if (dumpFileCompression != null) {
                builder.append(dumpFileCompression.getFileNameExtension());
            } else if (dumpfileArchiver != null) {
                builder.append(dumpfileArchiver.getFileNameExtension());
            }
            dumpFilePath = builder.toString();
            LOGGER.debug("Path: {}", dumpFilePath);
            builder.append("#%s-%s-%s-%s");
            pathTemplate = builder.toString();
        } else {
            LOGGER.debug("Init as dereferencing HTTP node.");
            pathTemplate = "/%s-%s/%s-%s";
        }
        accessUriTemplate = "http://" + authority + pathTemplate;
        resourceUriTemplate = accessUriTemplate;
    }

    @Override
    public void initAfterDataGeneration() throws Exception {
        // Create the container based on the information that has been received
        container = createContainer();
        graphs = null;
        // Start server
        server = new ContainerServer(container);
        connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(port);
        connection.connect(address);
    }

    @Override
    public void addResults(Model model, Resource root) {
        if (graphBasedResource != null) {
            if (crawlDelay != 0) {
                Double averageDelay = graphBasedResource.getAverageDelay();
                if (averageDelay != null) {
                    model.addLiteral(root, LDCBench.microAverageCrawlDelayFulfillment,
                            averageDelay / (crawlDelay * 1000));
                }
            }
            Long minDelay = graphBasedResource.getMinDelay();
            if (minDelay != null) {
                model.addLiteral(root, LDCBench.minCrawlDelay, ((double) minDelay) / 1000);
            }
            Long maxDelay = graphBasedResource.getMaxDelay();
            if (maxDelay != null) {
                model.addLiteral(root, LDCBench.maxCrawlDelay, ((double) maxDelay) / 1000);
            }
        }
        if (disallowedResource != null) {
            int total = disallowedResource.getTotalAmount();
            model.addLiteral(root, LDCBench.numberOfDisallowedResources, total);
            if (total != 0) {
                model.addLiteral(root, LDCBench.ratioOfRequestedDisallowedResources,
                        ((double) disallowedResource.getRequestedAmount()) / ((double) total));
            }
        }
    }

    protected Container createContainer() throws Exception {
        Graph[] graphsArray = graphs.toArray(new Graph[graphs.size()]);
        ArrayList<CrawleableResource> resources = new ArrayList<>();
        CrawleableResource resource = null;
        String[] resourceUriTemplates = Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate())
                .toArray(String[]::new);
        String[] accessUriTemplates = Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate())
                .toArray(String[]::new);
        if (dumpFileNode) {
            resource = DumpFileResource.create(cloudNodeId.get(),
                    Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate()).toArray(String[]::new),
                    Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate()).toArray(String[]::new),
                    graphs.toArray(new Graph[graphs.size()]),
                    r -> r.getPath().toString().equals(dumpFilePath),
                    dumpFileLang, dumpFileCompression, dumpfileArchiver);
        } else {
            SimpleTripleCreator tripleCreator = new SimpleTripleCreator(cloudNodeId.get(), resourceUriTemplates,
                    accessUriTemplates);
            HashSet<String> disallowedPaths = new HashSet<>();
            Random random = new Random(seedGenerator.getNextSeed());
            for (int g = 0; g < graphs.size(); g++) {
                GraphBuilder gb = new GrphBasedGraph(graphs.get(g));
                int nodes = gb.getNumberOfNodes();
                int disallowedAmount = Math.max((int) (nodes * disallowedRatio / (1 - disallowedRatio)),
                        disallowedRatio == 0 ? 0 : 1);
                LOGGER.debug("Adding {} disallowed resources...", disallowedAmount);
                for (int i = 0; i < disallowedAmount; i++) {
                    int linkingNode = random.nextInt(nodes);
                    // Make sure it's an internal node (belonging to this graph).
                    // Otherwise, the link will not be available for crawling.
                    while (gb.getGraphId(linkingNode) != Graph.INTERNAL_NODE_GRAPH_ID) {
                        linkingNode = (linkingNode + 1) % nodes;
                    }
                    int disallowedNode = gb.addNode();
                    gb.addEdge(linkingNode, disallowedNode, 0);
                    String path = new URL(tripleCreator.createNode(disallowedNode, -1, -1, false).toString()).getPath();
                    disallowedPaths.add(path);
                    LOGGER.debug("Added a disallowed resource {}.", path);
                }
                graphsArray[g] = gb.build();
            }
            resources.add(new RobotsResource(disallowedPaths, crawlDelay));
            disallowedResource = new DisallowedResource(disallowedPaths);
            resources.add(disallowedResource);

            // Create list of available content types
            final Set<Lang> unsupportedContentTypes = new HashSet<>(Arrays.asList(Lang.RDFNULL, Lang.CSV, Lang.TSV));
            Set<String> contentTypes = RDFLanguages.getRegisteredLanguages().stream()
                    .filter(lang -> !unsupportedContentTypes.contains(lang))
                    .flatMap(lang -> Stream.concat(Stream.of(lang.getContentType().getContentType()),
                            lang.getAltContentTypes().stream()))
                    .filter(s -> !s.contains("sparql")).collect(Collectors.toSet());
            // Create the container based on the information that has been received
            graphBasedResource = new GraphBasedResource(cloudNodeId.get(), resourceUriTemplates, accessUriTemplates,
                    graphsArray,
                    (r -> r.getTarget().contains(UriHelper.DATASET_KEY_WORD)
                            && r.getTarget().contains(UriHelper.RESOURCE_NODE_TYPE)),
                    contentTypes.toArray(new String[contentTypes.size()]));
            resource = graphBasedResource;
        }
        Objects.requireNonNull(resource, "Couldn't create crawleable resource. Exiting.");
        resources.add(resource);
        return new CrawleableResourceContainer(resources.toArray(new CrawleableResource[resources.size()]));
    }

    protected Model readModel(String modelFile, String modelLang) {
        Model model = ModelFactory.createDefaultModel();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(modelFile);
            model.read(fin, "", modelLang);
        } catch (Exception e) {
            LOGGER.error("Couldn't read model file. Returning null.", e);
            return null;
        } finally {
            CloseableHelper.closeQuietly(fin);
        }
        return model;
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
}
