package org.dice_research.ldcbench.nodes.http.simple.dump;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamOps;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.BrotliStreamFactory;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.CompressionStreamFactory;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.ReflectionBasedStreamFactory;
import org.dice_research.ldcbench.nodes.utils.TripleIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple class which builds a dump file from the given graph by serializing
 * all triples of the graph.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class DumpFileBuilder {

    public static final Lang DEFAULT_LANG = Lang.TTL;

    private static final Logger LOGGER = LoggerFactory.getLogger(DumpFileBuilder.class);

    public static final List<CompressionStreamFactory> COMPRESSIONS = Arrays.asList(
            ReflectionBasedStreamFactory.create("java.util.zip.GZIPOutputStream", "application/gzip", ".gz"),
            //ReflectionBasedStreamFactory.create("java.util.zip.ZipOutputStream", "application/zip", ".zip"),
            ReflectionBasedStreamFactory.create(
                    "org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream",
                    "application/x-bzip2", ".bz2"),
            new BrotliStreamFactory());

    protected final int domainId;
    protected final String[] resourceUriTemplates;
    protected final String[] accessUriTemplates;
    protected final Graph[] graphs;
    protected final Lang lang;
    protected final CompressionStreamFactory compression;
    protected File dumpFile;

    public DumpFileBuilder(int domainId, String[] resourceUriTemplates, String[] accessUriTemplates, Graph[] graphs,
            Lang lang, CompressionStreamFactory compression) {
        this.domainId = domainId;
        this.resourceUriTemplates = resourceUriTemplates;
        this.accessUriTemplates = accessUriTemplates;
        this.graphs = graphs;
        this.lang = lang;
        this.compression = compression;
    }

    public File build()
            throws IOException, NoSuchMethodException, SecurityException, ReflectiveOperationException {
        try (OutputStream out = generateOutputStream()) {
            streamData(out, lang);
        }
        return dumpFile;
    }

    @SuppressWarnings("resource")
    private OutputStream generateOutputStream()
            throws FileNotFoundException, IOException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // StringBuilder fileNameBuilder = new StringBuilder();
        // fileNameBuilder.append("");
        // List<String> fileExt = lang.getFileExtensions();
        // if (fileExt.size() > 0) {
        // fileNameBuilder.append('.');
        // fileNameBuilder.append(fileExt.get(0));
        // }
        dumpFile = File.createTempFile("ldcbench", ".dump");
        OutputStream out = new FileOutputStream(dumpFile);
        out = new BufferedOutputStream(out);
        if (compression != null) {
            out = compression.createCompressionStream(out);
        }
        return out;
    }

    private void streamData(OutputStream out, Lang lang) {
        int datasetId = 0;
        StreamRDF writerStream = null;
        try {
            writerStream = StreamRDFWriter.getWriterStream(out, lang);
        } catch(RiotException e ) {
            if(e.getMessage().startsWith("No serialization for language")) {
                LOGGER.warn("No serialization for language Lang: {}. Trying to write it from an in-memory model.", lang);
                writeData(out, lang);
            } else {
                throw e;
            }
        }
        // The stream has been created
        if(writerStream != null) {
        writerStream.start();
        TripleIterator iterator;
        LOGGER.info("Domain ID: " + domainId);
        LOGGER.info("graph size: " + graphs.length);

        for(Graph graph: graphs) {
            for (int i = 0; i < graph.getNumberOfNodes(); ++i) {
                iterator = new TripleIterator(graphs, domainId, resourceUriTemplates, accessUriTemplates, datasetId, i);
                StreamOps.sendTriplesToStream(iterator, writerStream);
            }
            datasetId++;
        }
        writerStream.finish();
        }
    }

    private void writeData(OutputStream out, Lang lang) {
        TripleIterator iterator;
        LOGGER.info("Domain ID: " + domainId);
        LOGGER.info("graph size: " + graphs.length);
        
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
        if (lang.equals(Lang.RDFXML)) {
            // Just RDFXML leads to StackOverflow exceptions
            RDFDataMgr.write(out, model, RDFFormat.RDFXML_PLAIN);
        } else {
            RDFDataMgr.write(out, model, lang);
        }
    }

    public String buildContentType() {
        if (compression != null) {
            return compression.getMediaType();
        }
        return lang.getHeaderString();
    }

}
