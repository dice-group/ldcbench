package org.dice_research.ldcbench.benchmark;

import java.util.stream.Collectors;
import static org.dice_research.ldcbench.Constants.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.benchmark.cloud.*;
import org.dice_research.ldcbench.data.NodeMetadata;
import org.dice_research.ldcbench.generate.SeedGenerator;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.rdf.SimpleTripleCreator;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;

public class BenchmarkController extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkController.class);

    private Set<Future<String>> dataGenContainers = new HashSet<>();
    private List<Future<String>> nodeContainers = new ArrayList<>();

    private Class<?>[] possibleNodeManagerClasses = { DereferencingHttpNodeManager.class, CkanNodeManager.class,
            SparqlNodeManager.class, };

    private boolean sdk;
    private boolean dockerized;
    private int nodesAmount;

    private String sparqlUrl;
    private String sparqlUrlAuth;
    private String[] sparqlCredentials;
    private ArrayList<Integer> seedNodes;
    private ArrayList<String> seedURIs;

    private ArrayList<AbstractNodeManager> nodeManagers = new ArrayList<>();

    private Semaphore nodesInitSemaphore = new Semaphore(0);
    private Semaphore nodesReadySemaphore = new Semaphore(0);
    private Semaphore nodeGraphMutex = new Semaphore(0);

    protected Channel dataGeneratorsChannel;

    protected String dataGeneratorsExchange;
    protected DataSender systemDataSender;
    protected DataSender systemTaskSender;
    protected NodeMetadata[] nodeMetadata = null;
    protected Map<String, NodeMetadata> nodeContainerMap = new HashMap<>();
    private Graph nodeGraph;

    protected List<String> dotlangLines = Collections.synchronizedList(new ArrayList<>());

    private String getRandomNameForRabbitMQ() {
        return java.util.UUID.randomUUID().toString();
    }

    private void createDataGenerator(String generatorImageName, String[] envVariables) {
        String variables[] = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 1)
                : new String[1];

        variables[variables.length - 1] = Constants.GENERATOR_ID_KEY + "=" + (dataGenContainers.size() + 1);
        Future<String> container = createContainerAsync(generatorImageName, Constants.CONTAINER_TYPE_BENCHMARK,
                variables);
        dataGenContainers.add(container);
    }

    private void waitForDataGenToBeCreated() throws InterruptedException, ExecutionException {
        LOGGER.info("Waiting for {} Data Generators to be created.", dataGenContainers.size());
        for (Future<String> container : dataGenContainers) {
            String containerId = container.get();
            if (containerId != null) {
                dataGenContainerIds.add(containerId);
            } else {
                String errorMsg = "Couldn't create generator component. Aborting.";
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }
    }

    private void waitForNodesToBeCreated() throws InterruptedException, ExecutionException {
        LOGGER.info("Waiting for {} nodes to be created.", nodeContainers.size());
        for (int i = 0; i < nodeContainers.size(); i++) {
            String containerId = nodeContainers.get(i).get();
            if (containerId != null) {
                nodeMetadata[i] = new NodeMetadata();
                nodeMetadata[i].setContainer(containerId);
                nodeMetadata[i].setResourceUriTemplate("http://" + containerId + "/%s-%s/%s-%s");
                nodeMetadata[i].setAccessUriTemplate("http://" + containerId + "/%s-%s/%s-%s");
                nodeContainerMap.put(containerId, nodeMetadata[i]);
            } else {
                String errorMsg = "Couldn't create generator component. Aborting.";
                LOGGER.error(errorMsg);
                throw new IllegalStateException(errorMsg);
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
        int seed = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.seed).getInt();
        nodesAmount = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.numberOfNodes).getInt();
        int triplesPerNode = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.triplesPerNode).getInt();
        long averageNodeDelay = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageNodeDelay).getLong();
        int averageNodeGraphDegree = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageNodeGraphDegree)
                .getInt();
        int averageRdfGraphDegree = RdfHelper.getLiteral(benchmarkParamModel, null, LDCBench.averageRdfGraphDegree)
                .getInt();

        Random random = new Random(seed);

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
                            dotlangLines.add(String.format("%d -> %d", node, target));
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
        float nodeWeight[] = new float[possibleNodeManagerClasses.length];
        float totalNodeWeight = 0;
        ArrayList<Class<?>> nodeManagerClasses = new ArrayList<>();
        for (int i = 0; i < possibleNodeManagerClasses.length; i++) {
            Property param = (Property) possibleNodeManagerClasses[i].getDeclaredMethod("getBenchmarkParameter").invoke(null);
            Literal value = RdfHelper.getLiteral(benchmarkParamModel, null, param);
            nodeWeight[i] = value != null ? value.getFloat() : 1;
            if (nodeWeight[i] != 0) {
                totalNodeWeight += nodeWeight[i];
                nodeManagerClasses.add(possibleNodeManagerClasses[i]);
            }
        }
        // create at least one node per any included node type
        int[] typecounts = new int[nodeManagerClasses.size()];
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            if (nodeWeight[i] != 0) {
                typecounts[i] += 1;
            }
        }
        // create other nodes according to the weights provided
        for (int i = 0; i < nodeManagerClasses.size(); i++) {
            nodeWeight[i] /= totalNodeWeight;
        }
        for (int i = IntStream.of(typecounts).sum(); i < nodesAmount; i++) {
            float sample = random.nextFloat();
            float current = 0;
            for (int j = 0; j < nodeManagerClasses.size(); j++) {
                current += nodeWeight[j];
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
        for (int i = 0; i < nodesAmount; i++) {
            envVariables = new String[] { ApiConstants.ENV_DOCKERIZED_KEY + "=" + dockerized,
                    ApiConstants.ENV_NODE_ID_KEY + "=" + i,
                    ApiConstants.ENV_BENCHMARK_EXCHANGE_KEY + "=" + benchmarkExchange,
                    ApiConstants.ENV_DATA_QUEUE_KEY + "=" + dataQueues[i],
                    ApiConstants.ENV_NODE_DELAY_KEY + "=" + averageNodeDelay,
                    ApiConstants.ENV_HTTP_PORT_KEY + "=" + (dockerized ? 80 : 12345), };

            nodeContainers.add(createContainerAsync(nodeManagers.get(i).getImageName(),
                    Constants.CONTAINER_TYPE_BENCHMARK, envVariables));

            // FIXME: HOBBIT SDK workaround (setting environment for "containers")
            if (sdk) {
                Thread.sleep(2000);
            }
        }

        waitForNodesToBeCreated();

        String evalDataQueueName = getRandomNameForRabbitMQ();
        LOGGER.debug("Creating evaluation module...");
        createEvaluationModule(EVALMODULE_IMAGE_NAME,
                new String[] { ApiConstants.ENV_BENCHMARK_EXCHANGE_KEY + "=" + benchmarkExchange,
                        ApiConstants.ENV_EVAL_DATA_QUEUE_KEY + "=" + evalDataQueueName,
                        ApiConstants.ENV_SPARQL_ENDPOINT_KEY + "=" + sparqlUrl });

        LOGGER.debug("Waiting for all cloud nodes and evaluation module to initialize...");
        nodesInitSemaphore.acquire(nodesAmount + 1);
        nodesInitSemaphore = null;

        LOGGER.debug("Broadcasting metadata to cloud nodes...");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        new ObjectOutputStream(buf).writeObject(nodeMetadata);
        dataGeneratorsChannel.basicPublish(benchmarkExchange, "", null, buf.toByteArray());

        LOGGER.debug("Creating data generators...");

        SeedGenerator seedGenerator = new SeedGenerator(seed);

        // Node graph generator
        LOGGER.info("Creating node graph generator...");
        envVariables = new String[] {
                DataGenerator.ENV_TYPE_KEY + "=" + DataGenerator.Types.NODE_GRAPH_GENERATOR,
                DataGenerator.ENV_SEED_KEY + "=" + seedGenerator.applyAsInt(0),
                DataGenerator.ENV_NUMBER_OF_NODES_KEY + "=" + nodesAmount,
                DataGenerator.ENV_AVERAGE_DEGREE_KEY + "=" + averageNodeGraphDegree,
                DataGenerator.ENV_DATAGENERATOR_EXCHANGE_KEY + "=" + dataGeneratorsExchange,
                //DataGenerator.ENV_NODETYPES_KEY + "=" + nodetypes.stream().map(String::valueOf).collect(Collectors.joining(",")),
                DataGenerator.ENV_NODETYPES_KEY + "=" + Stream.of(ArrayUtils.toObject(typecounts)).map(String::valueOf).collect(Collectors.joining(",")),
                DataGenerator.ENV_ISHUB_KEY + "=" + Stream.of(ArrayUtils.toObject(getIsHub(nodeManagerClasses))).map(String::valueOf).collect(Collectors.joining(",")),
                DataGenerator.ENV_TYPECONNECTIVITY_KEY + "=" + Arrays.stream(getTypeConnectivity(nodeManagerClasses)).map(Arrays::stream).map(s -> s.mapToObj(String::valueOf).collect(Collectors.joining(","))).collect(Collectors.joining(";")),
        };
        createDataGenerators(DATAGEN_IMAGE_NAME, 1, envVariables);
        // FIXME: HOBBIT SDK workaround (setting environment for "containers")
        if (sdk) {
            Thread.sleep(2000);
        }

        // RDF graph generators
        for (int i = 0; i < nodesAmount; i++) {
            LOGGER.info("Requesting creation of {}/{} RDF graph generator...", i + 1, nodesAmount);
            envVariables = ArrayUtils.addAll(new String[] { DataGenerator.ENV_NUMBER_OF_NODES_KEY + "=" + 0, // HOBBIT
                                                                                                             // SDK
                                                                                                             // workaround
                    Constants.GENERATOR_COUNT_KEY + "=" + nodesAmount,
                    DataGenerator.ENV_TYPE_KEY + "=" + DataGenerator.Types.RDF_GRAPH_GENERATOR,
                    DataGenerator.ENV_SEED_KEY + "=" + seedGenerator.applyAsInt(1 + i),
                    DataGenerator.ENV_DATA_QUEUE_KEY + "=" + dataQueues[i],
                    ApiConstants.ENV_EVAL_DATA_QUEUE_KEY + "=" + evalDataQueueName,
                    DataGenerator.ENV_DATAGENERATOR_EXCHANGE_KEY + "=" + dataGeneratorsExchange, },
                    nodeManagers.get(i).getDataGeneratorEnvironment(averageRdfGraphDegree, triplesPerNode));
            createDataGenerator(DATAGEN_IMAGE_NAME, envVariables);
            // FIXME: HOBBIT SDK workaround (setting environment for "containers")
            if (sdk) {
                Thread.sleep(2000);
            }
        }

        waitForDataGenToBeCreated();

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
        String sparqlHostname = createContainer("openlink/virtuoso-opensource-7", Constants.CONTAINER_TYPE_BENCHMARK,
                new String[] { "DBA_PASSWORD=" + VOS_PASSWORD, "HOBBIT_SDK_CONTAINER_NAME=benchmark-sparql",
                        "HOBBIT_SDK_PUBLISH_PORTS=" + exposedPort + ":" + defaultPort });
        if (sparqlHostname == null) {
            throw new IllegalStateException("Couldn't create SPARQL endpoint. Aborting.");
        }
        sparqlHostname += ":" + defaultPort;
        if (!dockerized) {
            sparqlHostname = "localhost:" + exposedPort;
        }
        sparqlUrl = "http://" + sparqlHostname + "/sparql";
        sparqlUrlAuth = sparqlUrl + "-auth";
        sparqlCredentials = new String[] { "dba", VOS_PASSWORD };
    }

    protected void createEmptyServer() {
        LOGGER.info("Creating empty-server");
        createContainer("git.project-hobbit.eu:4567/ldcbench/ldcbench/empty-server", Constants.CONTAINER_TYPE_BENCHMARK,
                null, new String[] { "purl.org", "www.openlinksw.com", "www.w3.org", "www.w2.org", });
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

        int[] entranceNodes = g.getEntranceNodes();
        for (int i = 0; i < entranceNodes.length; i++) {
            addNodeToSeed(seedURIs, entranceNodes[i]);
        }
        if (seedURIs.size() == 0) {
            throw new IllegalStateException();
        }
    }

    protected int[][] getTypeConnectivity(ArrayList<Class<?>> nodeManagerClasses) throws IllegalAccessException, InstantiationException {
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

    protected boolean[] getIsHub(ArrayList<Class<?>> nodeManagerClasses) throws IllegalAccessException, InstantiationException {
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

        LOGGER.debug("Waiting for the data generators to finish...");
        waitForDataGenToFinish();

        LOGGER.debug("Waiting for nodes to be ready...");
        nodesReadySemaphore.acquire(nodesAmount);
        nodesReadySemaphore = null;

        long startTime = System.currentTimeMillis();

        LOGGER.debug("Sending data to the system...");
        systemDataSender.sendData(RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(sparqlUrlAuth),
                RabbitMQUtils.writeString(sparqlCredentials[0]), RabbitMQUtils.writeString(sparqlCredentials[1]),
                RabbitMQUtils.writeString(String.join("\n", seedURIs)), }));
        systemTaskSender.close();

        sendToCmdQueue(ApiConstants.CRAWLING_STARTED_SIGNAL, RabbitMQUtils.writeLong(startTime));

        LOGGER.debug("Waiting for the system to finish...");
        waitForSystemToFinish();

        sendToCmdQueue(ApiConstants.CRAWLING_FINISHED_SIGNAL, RabbitMQUtils.writeLong(new Date().getTime()));

        waitForEvalComponentsToFinish();

        // the evaluation module should have sent an RDF model containing the
        // results. We should add the configuration of the benchmark to this
        // model.
        // this.resultModel.add(...);

        dotlangLines.add("{rank=min; S [label=seed, style=dotted]}");
        dotlangLines.add(String.format("{rank=same; %s}", seedNodes.stream().map(String::valueOf).collect(Collectors.joining(" "))));
        seedNodes.stream().map(i -> String.format("S -> %d [style=dotted]", i)).forEach(dotlangLines::add);

        final int amountOfColors = 9;
        // https://www.graphviz.org/doc/info/colors.html
        final String colorScheme = "rdylgn" + amountOfColors;
        for (int i = 0; i < nodesAmount; i++) {
            Resource nodeResource = resultModel.createResource(experimentUri + "_Node_" + i);
            double recall = Double.parseDouble(RdfHelper.getStringValue(resultModel, nodeResource, LDCBench.recall));
            String fillColor = Double.isNaN(recall) ? "" : "/" + colorScheme + "/" + String.valueOf((int) Math.floor(recall * 8) + 1);
            dotlangLines.add(String.format("%d [label=<%s<BR/>%s>, fillcolor=\"%s\", style=filled]", i, nodeManagers.get(i).getLabel(), Double.isNaN(recall) ? "&empty;" : String.format("%.2f", recall), fillColor));
            resultModel.removeAll(nodeResource, null, null);
        }

        dotlangLines.add(0, "digraph {");
        dotlangLines.add("}");
        String dotlang = String.join("\n", dotlangLines);

        Resource experimentResource = resultModel.getResource(experimentUri);
        resultModel.add(resultModel.createLiteralStatement(experimentResource, LDCBench.graphVisualization, dotlang));

        Path dotfile = Files.createTempFile("cloud", ".dot");
        Files.write(dotfile, dotlangLines, Charset.defaultCharset());

        // Send the resultModul to the platform controller and terminate
        LOGGER.debug("Sending result model: {}", RabbitMQUtils.writeModel2String(resultModel));
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
        case Commands.DOCKER_CONTAINER_TERMINATED: {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String containerName = RabbitMQUtils.readString(buffer);
            int exitCode = buffer.get();
            // FIXME Handle the crash of a container during the crawling phase
            if (nodeContainerMap.containsKey(containerName)) {
                nodeContainerMap.get(containerName).setTerminated(true);
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
