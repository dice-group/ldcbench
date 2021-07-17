package org.dice_research.ldcbench.nodes.http.simple.dump;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.CompressionStreamFactory;
import org.dice_research.ldcbench.nodes.utils.LangUtils;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import junit.framework.Assert;

@RunWith(Parameterized.class)
public class DumpFileBuilderTest {

    protected static final String[] RESOURCE_URI_TEMPLATES = new String[] { "http://domain0.org/%s-%s/%s-%s",
            "http://domain1.org/%s-%s/%s-%s" };
    protected static final String[] ACCESS_URI_TEMPLATES = new String[] { "http://domain0.org/%s-%s/%s-%s",
            "http://domain1.org/%s-%s/%s-%s" };

    private int domainId;
    private Graph[] graphs;
    private Lang lang;
    private CompressionStreamFactory compression;
    private Model expectedModel;

    public DumpFileBuilderTest(int domainId, Graph[] graphs, Lang lang, CompressionStreamFactory compression,
            Model expectedModel) {
        this.domainId = domainId;
        this.graphs = graphs;
        this.lang = lang;
        this.compression = compression;
        this.expectedModel = expectedModel;
    }

    @Test
    public void test() throws NoSuchMethodException, SecurityException, IOException, ReflectiveOperationException, ParserException, NotFoundException {
        DumpFileBuilder builder = new DumpFileBuilder(domainId, RESOURCE_URI_TEMPLATES, ACCESS_URI_TEMPLATES, graphs,
                lang, compression);
        System.out.println(
                "Testing " + lang + (compression == null ? "" : (" with compression " + compression.getMediaType())));

        File file = builder.build();

        if(!lang.equals(LangUtils.HDT_LANG)) {
            Model writtenModel = ModelFactory.createDefaultModel();
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                RDFParser parser = RDFParser.create().forceLang(lang).source(in).build();
                parser.parse(writtenModel.getGraph());
            }

            // compare the data
            Set<Statement> missingStmts = ModelComparisonHelper.getMissingStatements(writtenModel, expectedModel);
            if (!missingStmts.isEmpty()) {
                System.out.println("Missing statments: ");
                System.out.println(missingStmts.toString());
            }
            Set<Statement> wrongStmts = ModelComparisonHelper.getMissingStatements(expectedModel, writtenModel);
            if (!wrongStmts.isEmpty()) {
                System.out.println("Wrong statments: ");
                System.out.println(wrongStmts.toString());
            }
            Assert.assertTrue("There were missing or wrong statements.", missingStmts.isEmpty() && wrongStmts.isEmpty());
        }

        else {
            HDT hdt = HDTManager.loadHDT(file.getAbsolutePath(), null);
            Model readModel = ModelFactory.createDefaultModel();

            IteratorTripleString it = hdt.search("", "", "");
            while(it.hasNext()) {
                TripleString ts = it.next();
                Resource subject  = readModel.createResource(ts.getSubject().toString());
                Property predicate = readModel.createProperty(ts.getPredicate().toString());
                Resource object  = readModel.createResource(ts.getObject().toString());
                readModel.add(subject, predicate, object);
            }

            Set<Statement> missingHDTStmts = ModelComparisonHelper.getMissingStatements(readModel, expectedModel);
            if (!missingHDTStmts.isEmpty()) {
                System.out.println("Missing HDT statments: ");
                System.out.println(missingHDTStmts.toString());
            }
            Set<Statement> wrongHDTStmts = ModelComparisonHelper.getMissingStatements(expectedModel, readModel);
            if (!wrongHDTStmts.isEmpty()) {
                System.out.println("Wrong HDT statments: ");
                System.out.println(wrongHDTStmts.toString());
            }
            Assert.assertTrue("There were missing or wrong statements.", missingHDTStmts.isEmpty() && wrongHDTStmts.isEmpty());
        }
    
    }

    @Parameters
    public static List<Object[]> testCases() {
        List<Object[]> data = new ArrayList<>();
        GraphBuilder graph = null;
        Model expectedModel = null;
//        CompressionStreamFactory compFactory = null;
        Lang lang;

        graph = new GrphBasedGraph();
        int n1 = graph.addNode();
        int n2 = graph.addNode();
        int n3 = graph.addNode();
        graph.addEdge(n1, n2, 0);
        graph.addEdge(n1, n3, 0);
        graph.addEdge(n2, n3, 0);
        graph.setGraphIdOfNode(n3, 1, 0);
        graph.setEntranceNodes(new int[] { n1 });

        expectedModel = ModelFactory.createDefaultModel();
        Resource r1 = expectedModel.createResource("http://domain0.org/dataset-0/resource-0");
        Resource r2 = expectedModel.createResource("http://domain0.org/dataset-0/resource-1");
        Resource r3 = expectedModel.createResource("http://domain1.org/dataset-0/resource-0");
        Property p1 = expectedModel.createProperty("http://domain0.org/dataset-0/property-0");
        expectedModel.add(r1, p1, r2);
        expectedModel.add(r1, p1, r3);
        expectedModel.add(r2, p1, r3);

        for (int i = 0; i < LangUtils.getAllowedLangs().size(); ++i) {
            lang = LangUtils.getAllowedLangs().get(i);
            data.add(new Object[] { 0, new Graph[] { graph }, lang, null, expectedModel });
// TODO including compression would need additional effort for decompression
//            for (int j = 0; j < DumpFileBuilder.COMPRESSIONS.size(); ++j) {
//                compFactory = DumpFileBuilder.COMPRESSIONS.get(j);
//                data.add(new Object[] { 0, new Graph[] { graph }, lang, compFactory, expectedModel });
//            }
        }

        return data;
    }
}
