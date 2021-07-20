package org.dice_research.ldcbench.graph.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Assert;
import org.junit.Test;

public class SimpleRDF2GrphConverterTest {

    @Test
    public void testSimpleExample() {
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource(), model.createProperty("http://example.org/p1"), model.createResource());

        SimpleRDF2GrphConverter converter = new SimpleRDF2GrphConverter();

        GrphBasedGraph graph = converter.convert(model);

        Assert.assertEquals(1, graph.getNumberOfEdges());
        Assert.assertEquals(2, graph.getNumberOfNodes());
        Assert.assertArrayEquals(new int[] {}, graph.incomingEdgeSources(0));
        Assert.assertArrayEquals(new int[] {0}, graph.outgoingEdgeTypes(0));
        Assert.assertArrayEquals(new int[] {1}, graph.outgoingEdgeTargets(0));
        Assert.assertArrayEquals(new int[] {0}, graph.incomingEdgeSources(1));
        Assert.assertArrayEquals(new int[] {0}, graph.incomingEdgeTypes(1));
        Assert.assertArrayEquals(new int[] {}, graph.outgoingEdgeTargets(1));
    }

    @Test
    public void testCircleExample() {
        Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("http://example.org/r1"), model.createProperty("http://example.org/p1"),
                model.createResource("http://example.org/r1"));

        SimpleRDF2GrphConverter converter = new SimpleRDF2GrphConverter();

        GrphBasedGraph graph = converter.convert(model);

        Assert.assertEquals(1, graph.getNumberOfEdges());
        Assert.assertEquals(1, graph.getNumberOfNodes());
        Assert.assertArrayEquals(new int[] {0}, graph.incomingEdgeTypes(0));
        Assert.assertArrayEquals(new int[] {0}, graph.incomingEdgeSources(0));
        Assert.assertArrayEquals(new int[] {0}, graph.outgoingEdgeTypes(0));
        Assert.assertArrayEquals(new int[] {0}, graph.outgoingEdgeTargets(0));
    }

}
