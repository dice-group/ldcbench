package org.dice_research.ldcbench;

import static org.dice_research.ldcbench.Constants.BENCHMARK_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.CKANNODE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.DATAGEN_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.EVALMODULE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.HTTPNODE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.SPARQLNODE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.HENODE_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.JSONLDDATAGEN_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.RDFADATAGEN_IMAGE_NAME;
import static org.dice_research.ldcbench.Constants.SYSTEM_IMAGE_NAME;
import static org.hobbit.core.Constants.BENCHMARK_PARAMETERS_MODEL_KEY;
import static org.hobbit.core.Constants.HOBBIT_EXPERIMENT_URI_KEY;
import static org.hobbit.core.Constants.HOBBIT_SESSION_ID_KEY;
import static org.hobbit.core.Constants.RABBIT_MQ_HOST_NAME_KEY;
import static org.hobbit.core.Constants.SYSTEM_PARAMETERS_MODEL_KEY;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.rdf.model.Model;
import org.dice_research.ldcbench.benchmark.BenchmarkController;
import org.dice_research.ldcbench.benchmark.DataGenerator;
import org.dice_research.ldcbench.benchmark.EvalModule;
import org.dice_research.ldcbench.builders.LDCBenchNodeBuilder;
import org.dice_research.ldcbench.builders.CkanNodeBuilder;
import org.dice_research.ldcbench.builders.ExampleDockersBuilder;
import org.dice_research.ldcbench.builders.SimpleHttpNodeBuilder;
import org.dice_research.ldcbench.builders.SparqlNodeBuilder;
import org.dice_research.ldcbench.nodes.ckan.simple.SimpleCkanComponent;
import org.dice_research.ldcbench.nodes.http.simple.SimpleHttpServerComponent;
import org.dice_research.ldcbench.nodes.sparql.simple.SimpleSparqlComponent;
import org.dice_research.ldcbench.nodes.htmlembd.SimpleHEComponent;
// import org.dice_research.ldcbench.rdfa.node.SimpleRDFaComponent;
import org.dice_research.ldcbench.rdfa.gen.RDFaDataGenerator;
import org.dice_research.ldcbench.jsonld.gen.JsonLDDataGenerator;
import org.dice_research.ldcbench.system.SystemAdapter;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.core.components.Component;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.sdk.docker.AbstractDockerizer;
import org.hobbit.sdk.docker.RabbitMqDockerizer;
import org.hobbit.sdk.docker.builders.hobbit.BenchmarkDockerBuilder;
import org.hobbit.sdk.docker.builders.hobbit.DataGenDockerBuilder;
import org.hobbit.sdk.docker.builders.hobbit.EvalModuleDockerBuilder;
import org.hobbit.sdk.docker.builders.hobbit.SystemAdapterDockerBuilder;
import org.hobbit.sdk.utils.CommandQueueListener;
import org.hobbit.sdk.utils.ComponentsExecutor;
import org.hobbit.sdk.utils.ModelsHandler;
import org.hobbit.sdk.utils.commandreactions.CommandReactionsBuilder;
import org.hobbit.utils.rdf.RdfHelper;
import org.junit.Assert;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavel Smirnov
 */

public class BenchmarkTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTestBase.class);

    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private AbstractDockerizer rabbitMqDockerizer;
    private ComponentsExecutor componentsExecutor;
    private CommandQueueListener commandQueueListener;

    BenchmarkDockerBuilder benchmarkBuilder;
    DataGenDockerBuilder dataGeneratorBuilder;
    SystemAdapterDockerBuilder systemAdapterBuilder;
    EvalModuleDockerBuilder evalModuleBuilder;
    SimpleHttpNodeBuilder httpNodeBuilder;
    CkanNodeBuilder ckanNodeBuilder;
    SparqlNodeBuilder sparqlNodeBuilder;
    LDCBenchNodeBuilder rdfaNodeBuilder;
    LDCBenchNodeBuilder rdfaGenBuilder;
    LDCBenchNodeBuilder jsonLdNodeBuilder;
    LDCBenchNodeBuilder jsonLdGenBuilder;

    public void init(Boolean useCachedImage) throws Exception {

        benchmarkBuilder = new BenchmarkDockerBuilder(new ExampleDockersBuilder(BenchmarkController.class, BENCHMARK_IMAGE_NAME).useCachedImage(useCachedImage));
        dataGeneratorBuilder = new DataGenDockerBuilder(new ExampleDockersBuilder(DataGenerator.class, DATAGEN_IMAGE_NAME).useCachedImage(useCachedImage).addFileOrFolder("data"));

        systemAdapterBuilder = new SystemAdapterDockerBuilder(new ExampleDockersBuilder(SystemAdapter.class, SYSTEM_IMAGE_NAME).useCachedImage(useCachedImage));
        evalModuleBuilder = new EvalModuleDockerBuilder(new ExampleDockersBuilder(EvalModule.class, EVALMODULE_IMAGE_NAME).useCachedImage(useCachedImage));

        // FIXME do not build node images as part of this project
        httpNodeBuilder = new SimpleHttpNodeBuilder(new ExampleDockersBuilder(SimpleHttpServerComponent.class, HTTPNODE_IMAGE_NAME).useCachedImage(useCachedImage));
        ckanNodeBuilder = new CkanNodeBuilder(new ExampleDockersBuilder(SimpleCkanComponent.class, CKANNODE_IMAGE_NAME).useCachedImage(useCachedImage));
        sparqlNodeBuilder = new SparqlNodeBuilder(new ExampleDockersBuilder(SimpleSparqlComponent.class, SPARQLNODE_IMAGE_NAME).useCachedImage(useCachedImage));
        rdfaNodeBuilder = new LDCBenchNodeBuilder(new ExampleDockersBuilder(SimpleHEComponent.class, HENODE_IMAGE_NAME).useCachedImage(useCachedImage)) {
            @Override public String getName() { return HENODE_IMAGE_NAME; }
        };
        rdfaGenBuilder = new LDCBenchNodeBuilder(new ExampleDockersBuilder(RDFaDataGenerator.class, RDFADATAGEN_IMAGE_NAME).useCachedImage(useCachedImage)) {
            @Override public String getName() { return RDFADATAGEN_IMAGE_NAME; }
        };
        jsonLdNodeBuilder = new LDCBenchNodeBuilder(new ExampleDockersBuilder(SimpleHEComponent.class, HENODE_IMAGE_NAME).useCachedImage(useCachedImage)) {
            @Override public String getName() { return HENODE_IMAGE_NAME; }
        };
        jsonLdGenBuilder = new LDCBenchNodeBuilder(new ExampleDockersBuilder(JsonLDDataGenerator.class, JSONLDDATAGEN_IMAGE_NAME).useCachedImage(useCachedImage)) {
            @Override public String getName() { return JSONLDDATAGEN_IMAGE_NAME; }
        };

//        benchmarkBuilder = new BenchmarkDockerBuilder(new PullBasedDockersBuilder(BENCHMARK_IMAGE_NAME));
//        dataGeneratorBuilder = new DataGenDockerBuilder(new PullBasedDockersBuilder(DATAGEN_IMAGE_NAME));
//        evalModuleBuilder = new EvalModuleDockerBuilder(new PullBasedDockersBuilder(EVALMODULE_IMAGE_NAME));

    }

    protected void executeBenchmark(Boolean dockerized) throws Exception {
        Model benchmarkParameters = createBenchmarkParameters();
        LOGGER.info("Benchmark parameters:\n{}", prettyModelString(benchmarkParameters));

        Model systemParameters = createSystemParameters();
        LOGGER.info("System parameters:\n{}", prettyModelString(systemParameters));

        String[] benchmarkParamsStr = new String[]{
            HOBBIT_EXPERIMENT_URI_KEY+"="+org.hobbit.vocab.HobbitExperiments.New.getURI(),
            BENCHMARK_PARAMETERS_MODEL_KEY+"="+RabbitMQUtils.writeModel2String(ModelsHandler.createMergedParametersModel(benchmarkParameters, ModelsHandler.readModelFromFile("../benchmark.ttl"))),
            RABBIT_MQ_HOST_NAME_KEY+"="+(dockerized ? "rabbit" : "localhost"),
        };
        String [] systemParamsStr = new String[]{
            SYSTEM_PARAMETERS_MODEL_KEY+"="+RabbitMQUtils.writeModel2String(ModelsHandler.createMergedParametersModel(systemParameters, ModelsHandler.readModelFromFile("../system.ttl"))),
            RABBIT_MQ_HOST_NAME_KEY+"="+(dockerized ? "rabbit" : "localhost"),
        };

        Boolean useCachedImages = true;
        init(useCachedImages);

        rabbitMqDockerizer = RabbitMqDockerizer.builder().useCachedContainer().build();

        environmentVariables.set(ApiConstants.ENV_SDK_KEY, "true");
        environmentVariables.set(ApiConstants.ENV_DOCKERIZED_KEY, dockerized.toString());
        environmentVariables.set(RABBIT_MQ_HOST_NAME_KEY, "localhost"); // rabbit hostname for things running on the host directly
        environmentVariables.set(HOBBIT_SESSION_ID_KEY, "session_"+String.valueOf(new Date().getTime()));


        Component benchmarkController = new BenchmarkController();
        Component dataGen = new DataGenerator();
        Component systemAdapter = new SystemAdapter();
        Component evalModule = new EvalModule();
        Component httpNode = new SimpleHttpServerComponent();
        Component ckanNode = new SimpleCkanComponent();
        Component sparqlNode = new SimpleSparqlComponent();
        Component rdfaNode = new SimpleHEComponent();//new SimpleRDFaComponent();
        Component rdfaGen = new RDFaDataGenerator();
        Component jsonldNode = new SimpleHEComponent();
        Component jsonldGen = new JsonLDDataGenerator();

        if(dockerized) {

            benchmarkController = benchmarkBuilder.build();
            dataGen = dataGeneratorBuilder.build();
            evalModule = evalModuleBuilder.build();
            systemAdapter = systemAdapterBuilder.build();
            httpNode = httpNodeBuilder.build();
            ckanNode = ckanNodeBuilder.build();
            sparqlNode = sparqlNodeBuilder.build();
            rdfaNode = rdfaNodeBuilder.build();
            rdfaGen = rdfaGenBuilder.build();
            jsonldNode = jsonLdNodeBuilder.build();
            jsonldGen = jsonLdGenBuilder.build();
        }

        commandQueueListener = new CommandQueueListener();
        componentsExecutor = new ComponentsExecutor();

        rabbitMqDockerizer.run();


        //comment the .systemAdapter(systemAdapter) line below to use the code for running from python
        CommandReactionsBuilder commandReactionsBuilder = new CommandReactionsBuilder(componentsExecutor, commandQueueListener)
                        .benchmarkController(benchmarkController).benchmarkControllerImageName(BENCHMARK_IMAGE_NAME)
                        .dataGenerator(dataGen).dataGeneratorImageName(dataGeneratorBuilder.getImageName())
                        .evalModule(evalModule).evalModuleImageName(evalModuleBuilder.getImageName())
                        .systemAdapter(systemAdapter).systemAdapterImageName(SYSTEM_IMAGE_NAME)
                        .customContainerImage(httpNode, HTTPNODE_IMAGE_NAME)
                        .customContainerImage(ckanNode, CKANNODE_IMAGE_NAME)
                        .customContainerImage(sparqlNode, SPARQLNODE_IMAGE_NAME)
                        .customContainerImage(rdfaNode, HENODE_IMAGE_NAME)
                        .customContainerImage(rdfaGen, RDFADATAGEN_IMAGE_NAME)
                        .customContainerImage(jsonldNode, HENODE_IMAGE_NAME)
                        .customContainerImage(jsonldGen, JSONLDDATAGEN_IMAGE_NAME)
                        //.customContainerImage(systemAdapter, DUMMY_SYSTEM_IMAGE_NAME)
                ;

        commandQueueListener.setCommandReactions(
                commandReactionsBuilder.containerCommandsReaction(), //comment this if you want to run containers on a platform instance (if the platform is running)
                commandReactionsBuilder.benchmarkSignalsReaction()
        );

        componentsExecutor.submit(commandQueueListener);
        commandQueueListener.waitForInitialisation();

        // Start components without sending command to queue. Components will be executed by SDK, not the running platform (if it is running)
        String benchmarkContainerId = "benchmark";
        String systemContainerId = "system";

//        componentsExecutor.submit(benchmarkController, benchmarkContainerId, new String[]{ HOBBIT_EXPERIMENT_URI_KEY+"="+EXPERIMENT_URI,  BENCHMARK_PARAMETERS_MODEL_KEY+"="+ createBenchmarkParameters() });
//        componentsExecutor.submit(systemAdapter, systemContainerId, new String[]{ SYSTEM_PARAMETERS_MODEL_KEY+"="+ createSystemParameters() });

        //Alternative. Start components via command queue (will be executed by the platform (if running))
        benchmarkContainerId = commandQueueListener.createContainer(benchmarkBuilder.getImageName(), "benchmark", benchmarkParamsStr);
        systemContainerId = commandQueueListener.createContainer(systemAdapterBuilder.getImageName(), "system" , systemParamsStr);

        environmentVariables.set("BENCHMARK_CONTAINER_ID", benchmarkContainerId);
        environmentVariables.set("SYSTEM_CONTAINER_ID", systemContainerId);

        commandQueueListener.waitForTermination();

        // FIXME: This allows the terminating components to successfully send things to RabbitMQ
        Thread.sleep(10000);

        rabbitMqDockerizer.stop();

        if (componentsExecutor.anyExceptions()) {
            LOGGER.error("Some components didn't execute cleanly");
            for (Throwable e : componentsExecutor.getExceptions()) {
                LOGGER.error("Component didn't execute cleanly", e);
            }
            Assert.fail();
        }

        Model resultModel = componentsExecutor.resultModel;
        // As long as there are any HTTP nodes, dummy system should crawl something.
        Assert.assertNotNull("resultModel", resultModel);

        LOGGER.info("Result model:\n{}", prettyModelString(resultModel));

        try {
            String dot = RdfHelper.getStringValue(resultModel, null, LDCBench.graphVisualization);
            Path dotPath = Files.createTempFile("orca-graph-", ".dot");
            Files.write(dotPath, Arrays.asList(dot), StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder("graph-easy", dotPath.toAbsolutePath().toString());
            Process p = pb.start();
            p.waitFor();
            System.out.println(IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
        }

        double recall = Double.parseDouble(RdfHelper.getStringValue(resultModel, null, LDCBench.macroRecall));
        Assert.assertTrue("Macro-recall > 0", recall > 0);

        int dereferencingHttpNodeWeight = Integer.parseInt(RdfHelper.getStringValue(benchmarkParameters, null, LDCBench.dereferencingHttpNodeWeight));
        if (dereferencingHttpNodeWeight > 0) {
            double minAverageCrawlDelayFulfillment = Double.parseDouble(RdfHelper.getStringValue(resultModel, null, LDCBench.minAverageCrawlDelayFulfillment));
            Assert.assertTrue("minAverageCrawlDelayFulfillment > 1", minAverageCrawlDelayFulfillment > 1);
            /*
            double maxAverageCrawlDelayFulfillment = Double.parseDouble(RdfHelper.getStringValue(resultModel, null, LDCBench.maxAverageCrawlDelayFulfillment));
            Assert.assertTrue("maxAverageCrawlDelayFulfillment < 1.5", maxAverageCrawlDelayFulfillment < 1.5);
            */
        } else {
            LOGGER.info("Crawl-delay assertions skipped.");
        }
    }

    public Model createBenchmarkParameters() throws IOException {
        return ModelsHandler.readModelFromFile("test-benchmark-parameters.ttl");
    }

    public Model createSystemParameters() throws IOException {
        return ModelsHandler.readModelFromFile("test-system-parameters.ttl");
    }

    private String prettyModelString(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.TURTLE);
        return writer.toString();
    }
}
