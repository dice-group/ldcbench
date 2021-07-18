package org.dice_research.ldcbench.benchmark;

import static org.dice_research.ldcbench.Constants.DATAGEN_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.EMPTY_SERVER_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.EVALMODULE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.VOS_PASSWORD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.benchmark.cloud.AbstractNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.CkanNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.DereferencingHttpNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.HttpDumpNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.JsonLdNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.NodeManager;
import org.dice_research.ldcbench.benchmark.cloud.RDFaNodeManager;
import org.dice_research.ldcbench.benchmark.cloud.SparqlNodeManager;
import org.dice_research.ldcbench.benchmark.node.NodeSizeDeterminer;
import org.dice_research.ldcbench.benchmark.node.NodeSizeDeterminerFactory;
import org.dice_research.ldcbench.data.NodeMetadata;
import org.dice_research.ldcbench.generate.SeedGenerator;
import org.dice_research.ldcbench.generate.SequentialSeedGenerator;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.rdf.SimpleTripleCreator;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.EnvVariables;
import org.hobbit.utils.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;

public class BenchmarkController extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkController.class);

    public static final String DEFAULT_EVAL_STORAGE_IMAGE = AbstractBenchmarkController.DEFAULT_EVAL_STORAGE_IMAGE;

    private Set<Future<String>> dataGenContainers = new HashSet<>();
    private List<Future<String>> nodeContainers = new ArrayList<>();

    private Class<?>[] possibleNodeManagerClasses = {
        DereferencingHttpNodeManager.class,
        CkanNodeManager.class,
        SparqlNodeManager.class,
        HttpDumpNodeManager.class,
        RDFaNodeManager.class,
        JsonLdNodeManager.class,
    };

    private boolean sdk;
    private boolean dockerized;
    private int nodesAmount;

    private String sparqlContainer;
    private String sparqlUrl;
    private String sparqlUrlAuth;
    private String[] sparqlCredentials;
    private ArrayList<Integer> seedNodes;
    private ArrayList<String> seedURIs;

    private ArrayList<NodeManager> nodeManagers = new ArrayList<>();

    private ArrayList<Semaphore> nodeStarted = new ArrayList<>();
    private Semaphore nodesInitSemaphore = new Semaphore(0);
    private Semaphore nodesReadySemaphore = new Semaphore(0);
    private Semaphore nodesResultSemaphore = null;
    private Semaphore nodeGraphMutex = new Semaphore(0);
    private Semaphore evaluationStorageReady = new Semaphore(0);

    protected Channel dataGeneratorsChannel;

    protected String dataGeneratorsExchange;
    protected DataSender systemDataSender;
    protected DataSender systemTaskSender;
    protected NodeMetadata[] nodeMetadata = null;
    protected Map<String, NodeMetadata> nodeContainerMap = new HashMap<>();
    private Graph nodeGraph;

    protected List<String> dotlangLines = Collections.synchronizedList(new ArrayList<>());

    private String getNodeURI(int nodeIndex) {
        return experimentUri + "_Node_" + nodeIndex;
    }

    private String getRandomNameForRabbitMQ() {
        return java.util.UUID.randomUUID().toString();
    }

    private void createDataGenerator(NodeManager nodeManager, String[] envVariables) {
        String variables[] = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 1)
                : new String[1];
        variables[variables.length - 1] = Constants.GENERATOR_ID_KEY + "=" + (dataGenContainers.size() + 1);
        Future<String> container = createContainerAsync(nodeManager.getDataGeneratorImageName(), Constants.CONTAINER_TYPE_BENCHMARK,
                variables);
        dataGenContainers.add(container);
    }

    private void waitForDataGenToBeCreated(Set<Future<String>> containers)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(containers.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);

        try {
            Set<String> containerIds = new HashSet<>();
            LOGGER.info("Waiting for {} Data Generators to be created.", containers.size());

            for (Future<String> container : containers) {
                completionService.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        String containerId = container.get();
                        if (containerId != null) {
                            containerIds.add(containerId);
                            // add to the set in AbstractBenchmarkController
                            dataGenContainerIds.add(containerId);
                            LOGGER.debug("Data generator started: {}", containerId);
                        } else {
                            String errorMsg = "Couldn't create generator component. Aborting.";
                            LOGGER.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        return null;
                    }
                });
            }

            for (int i = 0; i < containers.size(); i++) {
                completionService.take().get();
            }
        } finally {
            executor.shutdown();
        }
    }

    private void waitForNodesToBeCreated(List<Future<String>> containers) throws Exception {
        LOGGER.info("Waiting for {} nodes to be created.", containers.size());
        for (int i = 0; i < containers.size(); i++) {
            if (nodeMetadata[i] == null) {
                String containerId = containers.get(i).get();
                if (containerId != null) {
                    nodeMetadata[i] = new NodeMetadata();
                    nodeMetadata[i].setContainer(containerId);
                    String defaultUriTemplate = "http://default.invalid/";
                    nodeMetadata[i].setResourceUriTemplate(defaultUriTemplate);
                    nodeMetadata[i].setAccessUriTemplate(defaultUriTemplate);
                    nodeContainerMap.put(containerId, nodeMetadata[i]);
                    nodeStarted.get(i).acquire();
                    sendToCmdQueue(ApiConstants.NODE_ACK_SIGNAL, RabbitMQUtils.writeLong(i));
                } else {
                    String errorMsg = "Couldn't create generator component. Aborting.";
                    LOGGER.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            }
        }
    }

    @Override
    public void init() throws Exception {
        super.init();

        sdk = EnvVariables.getBoolean(ApiConstants.ENV_SDK_KEY, false, LOGGER);
        dockerized = EnvVariables.getBoolean(ApiConstants.ENV_DOCKERIZED_KEY, true, LOGGER);

        // Start SPARQL endpoint
        createSparqlEndpoint();

        createEmptyServer();

        // You might want to load parameters from the benchmarks parameter model
        long seed = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.seed).getLong();
        nodesAmount = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.numberOfNodes).getInt();
        long averageCrawlDelay = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageCrawlDelay).getLong();
        double averageDisallowedRatio = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageDisallowedRatio).getDouble();
        int averageNodeGraphDegree = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageNodeGraphDegree)
                .getInt();
        int averageRdfGraphDegree = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageRdfGraphDegree)
                .getInt();
        double httpDumpNodeCompressedRatio = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.httpDumpNodeCompressedRatio).getDouble();

        /*
         * Determine the number of components which will make use of a random number
         * generator: Benchmark Controller, all Data Generators, all Nodes, Evaluation
         * module
         */
        int componentCount = (2 * nodesAmount) + 3;
        int componentId = 0;

        // Create a seed generator for the benchmark controller. Do not send these seeds
        // to other components!
        SeedGenerator localSeedGenerator = new SequentialSeedGenerator(seed, componentId, componentCount);
        ++componentId;

        NodeSizeDeterminer nodeSizeDeterminer = NodeSizeDeterminerFactory.create(benchmarkParamModel,
                localSeedGenerator.getNextSeed());
        Random random = new Random(localSeedGenerator.getNextSeed());

        // Create the other components
        dataGeneratorsChannel = cmdQueueFactory.getConnection().createChannel();
        String queueName = dataGeneratorsChannel.queueDeclare().getQueue();
        String dataGeneratorsExchange = getRandomNameForRabbitMQ();
        dataGeneratorsChannel.exchangeDeclare(dataGeneratorsExchange, "fanout", false, true, null);
        dataGeneratorsChannel.queueBind(queueName, dataGeneratorsExchange, "");

        Consumer consumer = new GraphConsumer(dataGeneratorsChannel) {
            @Override
            public void handleNodeGraph(int senderId, Graph g) {
                LOGGER.info("Got the node graph #{}", senderId);
                if (senderId == 0) {
                    int numberOfNodes = g.getNumberOfNodes();
                    for (int node = 0; node < numberOfNodes; node++) {
                        for (int target : g.outgoingEdgeTargets(node)) {
                            String targetUrl = getSeedForNode(target); // FIXME: keep the same as in DataGenerator
                            dotlangLines.add(String.format("%d -> %d [tooltip=\"%s\"]", node, target, targetUrl));
                        }
                    }
                    nodeGraph = g;
                    nodeGraphMutex.release();
                }
            }
        };
        dataGeneratorsChannel.basicConsume(queueName, true, consumer);

        // Exchange for broadcasting metadata to nodes
        String benchmarkExchange = getRandomNameForRabbitMQ();
        dataGeneratorsChannel.exchangeDeclare(benchmarkExchange, "fanout", false, true, null);

        // Create queues for sending data to nodes
        String[] dataQueues = new String[nodesAmount];
        for (int i = 0; i < nodesAmount; i++) {
            dataQueues[i] = dataGeneratorsChannel.queueDeclare(getRandomNameForRabbitMQ(), false, false, true, null)
                    .getQueue();
        }

        String[] envVariables;
        LOGGER.debug("Starting all cloud nodes...");
        ArrayList<Float> nodeWeights = new ArrayList<>();
        float totalNodeWeight = 0;
        ArrayList<Class<?>> nodeManagerClasses = new ArrayList<>();
        for (int i = 0; i < possibleNodeManagerClasses.length; i++) {
            Property param = (Property) possibleNodeManagerClasses[i].getDeclaredMethod("getBenchmarkParameter")
                    .invoke(null);
            Literal literal = RdfHelper.getLiteral(benchmarkParamModel, null, param);
            float value = literal != null ? literal.getFloat() : 1;
            if (value != 0) {
                totalNodeWeight += value;
                nodeWeights.add(value);
                nodeManagerClasses.add(possibleNodeManagerClasses[i]);
            }
        }
        // create at least one node per any included node type
        int[] typecounts = new int[nodeManagerClasses.size()];
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            if (nodeWeights.get(i) != 0) {
                LOGGER.info("adding required node of type {}", i);
                typecounts[i] += 1;
            }
        }
        // create other nodes according to the weights provided
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            nodeWeights.set(i, nodeWeights.get(i) / totalNodeWeight);
        }
        for (int i = IntStream.of(typecounts).sum(); i < nodesAmount; i++) {
            float sample = random.nextFloat();
            float current = 0;
            for (int j = 0; j < nodeManagerClasses.size(); j++) {
                current += nodeWeights.get(j);
                if (sample <= current || j == nodeManagerClasses.size() - 1) {
                    typecounts[j] += 1;
                    break;
                }
            }
        }

        ArrayList<Integer> nodetypes = new ArrayList<>();
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            for (int j = 0; j < typecounts[i]; j++) {
                nodetypes.add(i);
                nodeManagers.add((AbstractNodeManager) nodeManagerClasses.get(i).newInstance());
            }
        }

        nodeMetadata = new NodeMetadata[nodesAmount];
        int batchSize = dockerized ? 20 : 1;
        for (int batch = 0; batch < (float) nodesAmount / batchSize; batch++) {
            for (int i = batch * batchSize; i < (batch + 1) * batchSize && i < nodesAmount; i++) {
                LOGGER.info("Creating node {}...", i);
                nodeStarted.add(new Semaphore(0));

                envVariables = ArrayUtils.addAll(new String[] {
                            Constants.BENCHMARK_PARAMETERS_MODEL_KEY + "=" + RabbitMQUtils.writeModel2String(benchmarkParamModel),
                            ApiConstants.ENV_DOCKERIZED_KEY + "=" + dockerized,
                            ApiConstants.ENV_SEED_KEY + "=" + seed,
                            ApiConstants.ENV_NODE_ID_KEY + "=" + i,
                            ApiConstants.ENV_NODE_URI_KEY + "=" + getNodeURI(i),
                            ApiConstants.ENV_BENCHMARK_EXCHANGE_KEY + "=" + benchmarkExchange,
                            ApiConstants.ENV_DATA_QUEUE_KEY + "=" + dataQueues[i],
                            ApiConstants.ENV_CRAWL_DELAY_KEY + "=" + averageCrawlDelay,
                            ApiConstants.ENV_DISALLOWED_RATIO_KEY + "=" + averageDisallowedRatio,
                            ApiConstants.ENV_HTTP_PORT_KEY + "=" + (dockerized ? 80 : 30000 + i),
                            ApiConstants.ENV_COMPONENT_COUNT_KEY + "=" + componentCount,
                            ApiConstants.ENV_COMPONENT_ID_KEY + "=" + componentId++,
                            ApiConstants.ENV_COMPRESSED_RATIO_KEY + "=" + httpDumpNodeCompressedRatio,
                        },
                        nodeManagers.get(i).getNodeEnvironment());

                nodeContainers.add(createContainerAsync(nodeManagers.get(i).getNodeImageName(),
                        Constants.CONTAINER_TYPE_BENCHMARK, envVariables));

                // FIXME: HOBBIT SDK workaround (setting environment for "containers")
                if (sdk) {
                    Thread.sleep(2000);
                }
            }

            waitForNodesToBeCreated(nodeContainers);
        }

        String evalDataQueueName = getRandomNameForRabbitMQ();
        LOGGER.debug("Creating evaluation module...");
        createEvaluationModule(EVALMODULE_IMAGE_NAME,
                new String[] { ApiConstants.ENV_BENCHMARK_EXCHANGE_KEY + "=" + benchmarkExchange,
                        ApiConstants.ENV_EVAL_DATA_QUEUE_KEY + "=" + evalDataQueueName,
                        ApiConstants.ENV_SPARQL_ENDPOINT_KEY + "=" + sparqlUrl,
                        ApiConstants.ENV_COMPONENT_COUNT_KEY + "=" + componentCount,
                        ApiConstants.ENV_COMPONENT_ID_KEY + "=" + componentId++,  });

        LOGGER.debug("Waiting for all cloud nodes and evaluation module to initialize...");
        nodesInitSemaphore.acquire(nodesAmount + 1);
        nodesInitSemaphore = null;

        LOGGER.debug("Broadcasting metadata to cloud nodes...");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        new ObjectOutputStream(buf).writeObject(nodeMetadata);
        dataGeneratorsChannel.basicPublish(benchmarkExchange, "", null, buf.toByteArray());

        LOGGER.debug("Creating data generators...");

        // Node graph generator
        LOGGER.info("Creating node graph generator...");
        LOGGER.info("Type connectivity matrix: {}", Arrays.deepToString(getTypeConnectivity(nodeManagerClasses)));
        envVariables = new String[] { DataGenerator.ENV_TYPE_KEY + "=" + DataGenerator.Types.NODE_GRAPH_GENERATOR,
                ApiConstants.ENV_SEED_KEY + "=" + seed,
                DataGenerator.ENV_NUMBER_OF_NODES_KEY + "=" + nodesAmount,
                DataGenerator.ENV_AVERAGE_DEGREE_KEY + "=" + averageNodeGraphDegree,
                DataGenerator.ENV_DATAGENERATOR_EXCHANGE_KEY + "=" + dataGeneratorsExchange,
                // DataGenerator.ENV_NODETYPES_KEY + "=" +
                // nodetypes.stream().map(String::valueOf).collect(Collectors.joining(",")),
                DataGenerator.ENV_NODETYPES_KEY + "="
                        + Stream.of(ArrayUtils.toObject(typecounts)).map(String::valueOf)
                                .collect(Collectors.joining(",")),
                DataGenerator.ENV_ISHUB_KEY + "="
                        + Stream.of(ArrayUtils.toObject(getIsHub(nodeManagerClasses))).map(String::valueOf)
                                .collect(Collectors.joining(",")),
                DataGenerator.ENV_TYPECONNECTIVITY_KEY + "="
                        + Arrays.stream(getTypeConnectivity(nodeManagerClasses)).map(Arrays::stream)
                                .map(s -> s.mapToObj(String::valueOf).collect(Collectors.joining(",")))
                                .collect(Collectors.joining(";")),
                ApiConstants.ENV_COMPONENT_COUNT_KEY + "=" + componentCount,
                ApiConstants.ENV_COMPONENT_ID_KEY + "=" + componentId++, };
        createDataGenerators(DATAGEN_IMAGE_NAME, 1, envVariables);
        // FIXME: HOBBIT SDK workaround (setting environment for "containers")
        if (sdk) {
            Thread.sleep(2000);
        }

        String accessUris[] = Arrays.stream(nodeMetadata).map(m -> m.getAccessUriTemplate()).toArray(String[]::new);
        String serializedAccessUris = Arrays.toString(accessUris);
        String resourceUris[] = Arrays.stream(nodeMetadata).map(m -> m.getResourceUriTemplate()).toArray(String[]::new);
        String serializedResourceUris = Arrays.toString(resourceUris);

        // RDF graph generators
        for (int batch = 0; batch < (float) nodesAmount / batchSize; batch++) {
            for (int i = batch * batchSize; i < (batch + 1) * batchSize && i < nodesAmount; i++) {
                LOGGER.info("Creating RDF graph generator {}...", i);
                envVariables = ArrayUtils.addAll(new String[] { DataGenerator.ENV_NUMBER_OF_NODES_KEY + "=" + 0, // HOBBIT
                                                                                                                 // SDK
                                                                                                                 // workaround
                        Constants.GENERATOR_COUNT_KEY + "=" + nodesAmount,
                        DataGenerator.ENV_TYPE_KEY + "=" +
                        DataGenerator.Types.RDF_GRAPH_GENERATOR,
                        ApiConstants.ENV_SEED_KEY + "=" + seed,
                        DataGenerator.ENV_DATA_QUEUE_KEY + "=" + dataQueues[i],
                        ApiConstants.ENV_EVAL_DATA_QUEUE_KEY + "=" + evalDataQueueName,
                        DataGenerator.ENV_DATAGENERATOR_EXCHANGE_KEY + "=" + dataGeneratorsExchange,
                        ApiConstants.ENV_COMPONENT_COUNT_KEY + "=" + componentCount,
                        ApiConstants.ENV_COMPONENT_ID_KEY + "=" + componentId++,
                        DataGenerator.ENV_ACCESS_URI_TEMPLATES_KEY + "=" + serializedAccessUris,
                        DataGenerator.ENV_RESOURCE_URI_TEMPLATES_KEY + "=" + serializedResourceUris,},
                        nodeManagers.get(i).getDataGeneratorEnvironment(averageRdfGraphDegree,
                                nodeSizeDeterminer.getNodeSize()));
                createDataGenerator(nodeManagers.get(i), envVariables);
                // FIXME: HOBBIT SDK workaround (setting environment for "containers")
                if (sdk) {
                    Thread.sleep(2000);
                }
            }
            waitForDataGenToBeCreated(dataGenContainers);
        }

        LOGGER.debug("Creating queues for sending data and tasks to the system...");
        systemDataSender = DataSenderImpl.builder().queue(getFactoryForOutgoingDataQueues(),
                generateSessionQueueName(Constants.DATA_GEN_2_SYSTEM_QUEUE_NAME)).build();
        systemTaskSender = DataSenderImpl.builder().queue(getFactoryForOutgoingDataQueues(),
                generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME)).build();

        LOGGER.debug("Waiting for components to initialize...");
        waitForComponentsToInitialize();
    }

    protected void createSparqlEndpoint() {
        String defaultPort = "8890";
        String exposedPort = "8889";
        String sparqlHostname = createContainer(ApiConstants.SPARQL_IMAGE, Constants.CONTAINER_TYPE_BENCHMARK,
                new String[] { "DBA_PASSWORD=" + VOS_PASSWORD, "HOBBIT_SDK_CONTAINER_NAME=benchmark-sparql",
                        "HOBBIT_SDK_PUBLISH_PORTS=" + exposedPort + ":" + defaultPort });
        if (sparqlHostname == null) {
            throw new IllegalStateException("Couldn't create SPARQL endpoint. Aborting.");
        }
        sparqlContainer = sparqlHostname;
        sparqlHostname += ":" + defaultPort;
        if (!dockerized) {
            sparqlHostname = "localhost:" + exposedPort;
        }
        sparqlUrl = "http://" + sparqlHostname + "/sparql";
        sparqlUrlAuth = sparqlUrl + "-auth";
        sparqlCredentials = new String[] { "dba", VOS_PASSWORD };

        new Thread() {
            public void run() {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("dba", VOS_PASSWORD));
                HttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

                boolean success = false;
                do {
                    try {
                        UpdateProcessor up = UpdateExecutionFactory.createRemoteForm(
                            UpdateFactory.create("DELETE {GRAPH ?g {?s ?p ?o}} WHERE {GRAPH ?g {?s ?p ?o}}"),
                            sparqlUrlAuth,
                            httpClient
                        );
                        up.execute();
                        success = true;
                    } catch (Exception e) {
                        if (e.getCause() instanceof SocketException) {
                            LOGGER.info("Cannot connect to the evaluation storage {} to clean it up, will try again...", sparqlUrl);
                        } else {
                            LOGGER.info("Cannot clean up the evaluation storage {}, will try again...", sparqlUrl, e);
                        }
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                        }
                    }
                } while (!success);
                LOGGER.info("Cleaned up the evaluation storage.");
                evaluationStorageReady.release();
            }
        }.start();
    }

    protected void createEmptyServer() {
        LOGGER.info("Creating empty-server");
        createContainer(EMPTY_SERVER_IMAGE_NAME, Constants.CONTAINER_TYPE_BENCHMARK, null,
                new String[] {
                    "creativecommons.org",
                    "danbri.org",
                    "digitalbazaar.com",
                    "example.com",
                    "example.net",
                    "example.orb",
                    "example.org",
                    "freetime.example.org",
                    "github.org",
                    "greggkellogg.net",
                    "internet",
                    "kellogg",
                    "openspring.net",
                    "purl.org",
                    "rdfa.info",
                    "rdf.data",
                    "rdfs.org",
                    "schema.org",
                    "vocab.org",
                    "web.resource.org",
                    "www.amazon.com",
                    "www.blogger.com",
                    "www.cwi.nl",
                    "www.daml.org",
                    "www.example.com",
                    "www.example.org",
                    "www.ivan",
                    "www.ivan-herman.net",
                    "www.leobard.net",
                    "www.milowski.com",
                    "www.openlinksw.com",
                    "www-sop.inria.fr",
                    "www.w2.org",
                    "www.w3.org",
                    "xmlns.com",
                    "xmlns.openid.net",
                });
    }

    protected String getSeedForNode(int node) {
        SimpleTripleCreator tripleCreator = new SimpleTripleCreator(node,
                Stream.of(nodeMetadata).map(nm -> nm.getResourceUriTemplate()).toArray(String[]::new),
                Stream.of(nodeMetadata).map(nm -> nm.getAccessUriTemplate()).toArray(String[]::new));
        // FIXME use one of entrance nodes in graph instead of 0
        // FIXME better signal that we just want an externally accessible URI instead of
        // -2
        return tripleCreator.createNode(0, -1, -2, false).toString();
    }

    protected void addNodeToSeed(ArrayList<String> seedURIs, int node) {
        seedNodes.add(node);
        seedURIs.add(getSeedForNode(node));
    }

    protected void getSeedURIs(Graph g) {
        seedNodes = new ArrayList<>();
        seedURIs = new ArrayList<>();

        Literal allNodesInSeed = RdfHelper.getLiteral(benchmarkParamModel, null, benchmarkParamModel.getProperty(LDCBench.getURI() + "allNodesInSeed"));
        if (allNodesInSeed != null && allNodesInSeed.getBoolean()) {
            // For testing purposes, include all nodes in seed.
            for (int node = 0; node < g.getNumberOfNodes(); node++) {
                addNodeToSeed(seedURIs, node);
            }
        } else {
            int[] entranceNodes = g.getEntranceNodes();
            for (int node : entranceNodes) {
                addNodeToSeed(seedURIs, node);
            }
        }
        if (seedURIs.size() == 0) {
            throw new IllegalStateException();
        }
    }

    protected int[][] getTypeConnectivity(ArrayList<Class<?>> nodeManagerClasses)
            throws IllegalAccessException, InstantiationException {
        // Build node type -> connectivity matrix for node graph generator.
        int[][] typeconnectivity = new int[nodeManagerClasses.size()][nodeManagerClasses.size()];
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            AbstractNodeManager manager = (AbstractNodeManager) nodeManagerClasses.get(i).newInstance();
            for (int j = 0; j < nodeManagerClasses.size(); j++) {
                typeconnectivity[j][i] = manager.weightOfLinkFrom(nodeManagerClasses.get(j));
            }
        }
        return typeconnectivity;
    }

    protected boolean[] getIsHub(ArrayList<Class<?>> nodeManagerClasses)
            throws IllegalAccessException, InstantiationException {
        boolean[] ishub = new boolean[nodeManagerClasses.size()];
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            AbstractNodeManager manager = (AbstractNodeManager) nodeManagerClasses.get(i).newInstance();
            ishub[i] = manager.canBeHub();
        }
        return ishub;
    }

    @Override
    protected void executeBenchmark() throws Exception {
        LOGGER.trace("BenchmarkController.executeBenchmark()");
        // Send the start signal to enable our data generators to get through their run
        // method (and terminate)
        sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

        LOGGER.debug("Waiting for the node graph...");
        nodeGraphMutex.acquire();

        getSeedURIs(nodeGraph);
        LOGGER.info("Seed URIs: {}", seedURIs);

        waitForDataGenToFinish();

        LOGGER.debug("Waiting for nodes to be ready...");
        nodesReadySemaphore.acquire(nodesAmount);
        nodesReadySemaphore = null;

        long startTime = System.currentTimeMillis();

        LOGGER.debug("Waiting for the evaluation storage to be ready...");
        evaluationStorageReady.acquire();
        LOGGER.debug("Evaluation storage is ready.");

        LOGGER.debug("Sending data to the system...");
        systemDataSender.sendData(RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(sparqlUrlAuth),
                RabbitMQUtils.writeString(sparqlCredentials[0]), RabbitMQUtils.writeString(sparqlCredentials[1]),
                RabbitMQUtils.writeString(String.join("\n", seedURIs)), }));
        systemTaskSender.close();

        sendToCmdQueue(ApiConstants.CRAWLING_STARTED_SIGNAL, RabbitMQUtils.writeLong(startTime));

        LOGGER.debug("Waiting for the system to finish...");
        waitForSystemToFinish();

        // Create a model so we can store information about nodes.
        resultModel = ModelFactory.createDefaultModel();
        nodesResultSemaphore = new Semaphore(0);
        sendToCmdQueue(ApiConstants.CRAWLING_FINISHED_SIGNAL, RabbitMQUtils.writeLong(new Date().getTime()));

        nodesResultSemaphore.acquire(nodesAmount);
        nodesResultSemaphore = null;
        waitForEvalComponentsToFinish();

        // the evaluation module should have sent an RDF model containing the
        // results. We should add the configuration of the benchmark to this
        // model.
        // this.resultModel.add(...);

        dotlangLines.add("{rank=min; S [label=seed, style=dotted]}");
        dotlangLines.add(String.format("{rank=same; %s}",
                seedNodes.stream().map(String::valueOf).collect(Collectors.joining(" "))));
        seedNodes.stream().map(i -> String.format("S -> %d [tooltip=\"%s\", style=dotted]", i, getSeedForNode(i)))
                .forEach(dotlangLines::add);

        final int amountOfColors = 9;
        // https://www.graphviz.org/doc/info/colors.html
        final String colorScheme = "rdylgn" + amountOfColors;
        long delaySupportingNodes = 0;
        Double minDelayFulfillment = null;
        Double maxDelayFulfillment = null;
        double delayFulfillmentSum = 0;
        long disallowedTotal = 0;
        long disallowedRequested = 0;
        for (int i = 0; i < nodesAmount; i++) {
            Resource nodeResource = resultModel.createResource(getNodeURI(i));
            double recall = Double
                    .parseDouble(RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.microRecall));

            Double crawlDelayFulfillment = null;
            Double minCrawlDelay = null;
            Double maxCrawlDelay = null;
            String microAverageCrawlDelayFulfillmentString = RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.microAverageCrawlDelayFulfillment);
            if (microAverageCrawlDelayFulfillmentString != null) {
                crawlDelayFulfillment = Double.parseDouble(microAverageCrawlDelayFulfillmentString);
                try {
                    minCrawlDelay = Double.parseDouble(RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.minCrawlDelay));
                } catch (NullPointerException e) {
                }
                try {
                    maxCrawlDelay = Double.parseDouble(RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.maxCrawlDelay));
                } catch (NullPointerException e) {
                }
                delayFulfillmentSum += crawlDelayFulfillment;
                if (minDelayFulfillment == null || minDelayFulfillment > crawlDelayFulfillment) {
                    minDelayFulfillment = crawlDelayFulfillment;
                }
                if (maxDelayFulfillment == null || maxDelayFulfillment < crawlDelayFulfillment) {
                    maxDelayFulfillment = crawlDelayFulfillment;
                }
                delaySupportingNodes++;
            }

            long numberOfDisallowed = 0;
            double ratioReqDisallowed = 0;
            String numberOfDisallowedString = RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.numberOfDisallowedResources);
            if (numberOfDisallowedString != null) {
                numberOfDisallowed = Long.parseLong(numberOfDisallowedString);
                disallowedTotal += numberOfDisallowed;
                String ratioReqDisallowedString = RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.ratioOfRequestedDisallowedResources);
                if (ratioReqDisallowedString != null) {
                    ratioReqDisallowed = Double.parseDouble(ratioReqDisallowedString);
                    disallowedRequested += numberOfDisallowed * ratioReqDisallowed;
                }
            }

            String tooltip = String.format("%d: %s\n"
                    + "Ratio of requested disallowed: %.2f\nTotal disallowed: %d\n"
                    + "Crawl delay fulfillment: %.2f\nMin crawl delay: %.2f\nMax crawl delay: %.2f",
                    i, nodeMetadata[i].getContainer(),
                    ratioReqDisallowed, numberOfDisallowed,
                    crawlDelayFulfillment, minCrawlDelay, maxCrawlDelay);
            String fillColor = Double.isNaN(recall) ? ""
                    : "/" + colorScheme + "/" + String.valueOf((int) Math.floor(recall * 8) + 1);
            dotlangLines.add(String.format("%d [label=\"%s\n%s%s\", tooltip=\"%s\", fillcolor=\"%s\", style=filled]", i,
                    nodeManagers.get(i).getLabel(), Double.isNaN(recall) ? "&empty;" : String.format("%.2f", recall),
                    ratioReqDisallowed > 0 ? "!" : "",
                    tooltip, fillColor));
            resultModel.removeAll(nodeResource, null, null);
        }

        dotlangLines.add(0, "digraph {");
        dotlangLines.add("}");
        String dotlang = String.join("\n", dotlangLines);

        Resource experimentResource = resultModel.getResource(experimentUri);
        if (delaySupportingNodes != 0) {
            resultModel.addLiteral(experimentResource, LDCBench.macroAverageCrawlDelayFulfillment, delayFulfillmentSum / delaySupportingNodes);
        }
        if (minDelayFulfillment != null) {
            resultModel.addLiteral(experimentResource, LDCBench.minAverageCrawlDelayFulfillment, (double)minDelayFulfillment);
        }
        if (maxDelayFulfillment != null) {
            resultModel.addLiteral(experimentResource, LDCBench.maxAverageCrawlDelayFulfillment, (double)maxDelayFulfillment);
        }
        resultModel.addLiteral(experimentResource, LDCBench.numberOfDisallowedResources, disallowedTotal);
        if (disallowedTotal != 0) {
            resultModel.addLiteral(experimentResource, LDCBench.ratioOfRequestedDisallowedResources, ((double)disallowedRequested) / disallowedTotal);
        }
        resultModel.add(resultModel.createLiteralStatement(experimentResource, LDCBench.graphVisualization, dotlang));

        // Send the resultModul to the platform controller and terminate
        sendResultModel(resultModel);
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        switch (command) {
        case ApiConstants.NODE_URI_TEMPLATE: {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            int node = Integer.parseInt(RabbitMQUtils.readString(buffer));
            nodeMetadata[node].setResourceUriTemplate(RabbitMQUtils.readString(buffer));
            nodeMetadata[node].setAccessUriTemplate(RabbitMQUtils.readString(buffer));
            LOGGER.debug("Resource URI template {} for node {}.", nodeMetadata[node].getResourceUriTemplate(), node);
            LOGGER.debug("Access URI template {} for node {}.", nodeMetadata[node].getAccessUriTemplate(), node);
            break;
        }
        case ApiConstants.NODE_START_SIGNAL: {
            nodeStarted.get((int) RabbitMQUtils.readLong(data)).release();
            break;
        }
        case ApiConstants.NODE_INIT_SIGNAL: {
            if (nodesInitSemaphore != null) {
                LOGGER.debug("Received NODE_INIT_SIGNAL");
                nodesInitSemaphore.release();
            } else {
                throw new IllegalStateException("Received unexpected NODE_INIT_SIGNAL");
            }
            break;
        }
        case ApiConstants.NODE_READY_SIGNAL: {
            if (nodesReadySemaphore != null) {
                LOGGER.debug("Received NODE_READY_SIGNAL");
                nodesReadySemaphore.release();
            } else {
                throw new IllegalStateException("Received unexpected NODE_READY_SIGNAL");
            }
            break;
        }
        case ApiConstants.NODE_RESULTS_SIGNAL: {
            if (nodesResultSemaphore != null) {
                LOGGER.debug("Received NODE_RESULTS_SIGNAL");
                try {
                    resultModelMutex.acquire();
                    resultModel.add(RabbitMQUtils.readModel(data));
                    resultModelMutex.release();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                    throw new IllegalStateException(e);
                }

                nodesResultSemaphore.release();
            } else {
                throw new IllegalStateException("Received unexpected NODE_RESULTS_SIGNAL");
            }
            break;
        }
        case Commands.DOCKER_CONTAINER_TERMINATED: {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String containerName = RabbitMQUtils.readString(buffer);
            int exitCode = buffer.get();
            if (nodeContainerMap.containsKey(containerName)) {
                LOGGER.error("Node container {} terminated with exitCode={}.", containerName, exitCode);
                nodeContainerMap.get(containerName).setTerminated(true);
                // FIXME: only do this when container terminates unexpectedly
                containerCrashed(containerName);
            }
            if (dataGenContainerIds.contains(containerName)) {
                LOGGER.error("Data generator container {} terminated with exitCode={}.", containerName, exitCode);
                // FIXME: only do this when container terminates unexpectedly
                if (exitCode != 0) {
                    containerCrashed(containerName);
                }
            }
            if (containerName.equals(sparqlContainer)) {
                LOGGER.error("Sparql container {} terminated with exitCode={}.", containerName, exitCode);
                containerCrashed(containerName);
            }
        }
        }
        super.receiveCommand(command, data);
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("BenchmarkController.close()");
        // Free the resources you requested here
        IOUtils.closeQuietly(systemDataSender);
        IOUtils.closeQuietly(systemTaskSender);
        if (nodeMetadata != null) {
            for (int i = 0; i < nodeMetadata.length; ++i) {
                if (!nodeMetadata[i].isTerminated()) {
                    stopContainer(nodeMetadata[i].getContainer());
                }
            }
        }
        // Always close the super class after yours!
        super.close();
    }
}
