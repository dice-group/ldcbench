package org.dice_research.ldcbench.jsonld.gen;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.benchmark.DataGenerator;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.htmlembd.SimpleHEComponent;
import org.dice_research.ldcbench.nodes.utils.TripleIterator;
import org.dice_research.ldcbench.utils.tar.TarFileGenerator;
import org.hobbit.core.rabbit.RabbitQueueFactory;
import org.hobbit.core.rabbit.SimpleFileSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonLDDataGenerator extends DataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLDDataGenerator.class);


    protected static final String HTML_PREFIX = "<!DOCTYPE>\r\n<html>\r\n<head>\r\n   <title>Test</title>\r\n</head>\r\n<body>";
    protected static final String HTML_SUFFIX = "</body>\n</html>";

    protected static final String JSON_LD_PREFIX = "<script type=\"application/ld+json\">";
    protected static final String JSON_LD_SUFFIX = "</script>\r\n";

    protected void generateHtmlFiles(Graph[] graphs, File htmlOut) throws IOException {
        int domainId = 1;

        TripleIterator iterator;
        // convert Graph to jena.graph s.t. we can use the RDFDataMgr
        Model model = ModelFactory.createDefaultModel();
        org.apache.jena.graph.Graph modelGraph = model.getGraph();
        int datasetId = 0;
        for(Graph graph: graphs) {
            for (int i = 0; i < graph.getNumberOfNodes(); ++i) {
                iterator = new TripleIterator(graphs, domainId, resourceUriTemplates, accessUriTemplates, datasetId, i);
                while(iterator.hasNext()) {
                    modelGraph.add(iterator.next());
                }
            }
            datasetId++;
        }

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(htmlOut));

        out.write(HTML_PREFIX.getBytes(StandardCharsets.UTF_8));
        out.write(JSON_LD_PREFIX.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.write(out, model, Lang.JSONLD);
        out.write(JSON_LD_SUFFIX.getBytes(StandardCharsets.UTF_8));
        out.write(HTML_SUFFIX.getBytes(StandardCharsets.UTF_8));

        out.close();
    }

    @Override
    protected void sendFinalGraph(Graph graph) throws Exception {
        File htmlOut = new File(SimpleHEComponent.SINGLE_FILE_NAME);

        Graph[] g = {graph};
        generateHtmlFiles(g, htmlOut);


        SortedMap<String, File> htmlFiles = new TreeMap<>();

        htmlFiles.put(SimpleHEComponent.SINGLE_FILE_NAME, htmlOut);
        // SortedMap<String, File> ttlFiles = new TreeMap<>();

        // Generate HTML tar file
        generateAndSendTarFile(SimpleHEComponent.SINGLE_FILE_NAME + ApiConstants.FILE_ENDING_HTML_TAR_GZ, htmlFiles, outgoingDataQueuefactory, dataQueueName);
    }


    protected void generateAndSendTarFile(String file, SortedMap<String, File> files,
            RabbitQueueFactory outgoingDataQueuefactory, String dataQueueName) throws IOException {
        File tarFile = new File(file);
        // Create file
        TarFileGenerator generator = new TarFileGenerator();
        generator.generateTarFile(tarFile, files, true);

        // Send file
        sendTarFile(tarFile, outgoingDataQueuefactory, dataQueueName);
    }

    protected static void sendTarFile(File file, RabbitQueueFactory outgoingDataQueuefactory, String dataQueueName)
            throws IOException {
        // Create file
        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
                SimpleFileSender dataSender = SimpleFileSender.create(outgoingDataQueuefactory, dataQueueName);) {
            dataSender.streamData(is, file.getName());
        }
    }

}
