package org.dice_research.ldcbench.generate;

import java.io.File;

import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Assert;
import org.junit.Test;

public class FileBasedGraphGeneratorTest {

    /**
     * Tests using graph1.ttl; content: A --p-> B .
     */
    @Test
    public void testGraph1() {
        FileBasedGraphGenerator generator = new FileBasedGraphGenerator("file://"
                + (new File("src/test/resources/org/dice_research/ldcbench/generate/graph1.ttl")).getAbsolutePath(), "",
                "TTL");
        GraphBuilder graph = new GrphBasedGraph();
        generator.generateGraph(0, 0D, 0L, graph);

        Assert.assertEquals(2, graph.getNumberOfNodes());
        Assert.assertEquals(1, graph.getNumberOfEdges());
    }

    /**
     * Tests using graph2.ttl; content: A --p-> B . A --q-> C .
     */
    @Test
    public void testGraph2() {
        FileBasedGraphGenerator generator = new FileBasedGraphGenerator("file://"
                + (new File("src/test/resources/org/dice_research/ldcbench/generate/graph2.ttl")).getAbsolutePath(), "",
                "TTL");
        GraphBuilder graph = new GrphBasedGraph();
        generator.generateGraph(0, 0D, 0L, graph);

        Assert.assertEquals(3, graph.getNumberOfNodes());
        Assert.assertEquals(2, graph.getNumberOfEdges());

        // Find the A node
        int A = -1;
        for (int i = 0; i < 3; ++i) {
            if (graph.outgoingEdgeCount(i) > 0) {
                Assert.assertEquals("The found node should have exactly 2 outgoing edges", 2,
                        graph.outgoingEdgeCount(i));
                Assert.assertEquals("Found the A node two times", -1, A);
                A = i;
            }
        }
        Assert.assertTrue("Couldn't find the A node", A != -1);

        for (int i = 0; i < 3; ++i) {
            if (A != i) {
                // Check that they have exactly 1 incoming edge and no outgoing edge
                Assert.assertEquals("B or C has an outgoing edge", 0, graph.outgoingEdgeCount(i));
                Assert.assertEquals("B or C have not the expected single incoming edge", 1, graph.incomingEdgeCount(i));
            }
        }
        int[] outTypes = graph.outgoingEdgeTypes(A);
        Assert.assertTrue("The edges should have different types", outTypes[0] != outTypes[1]);
    }
}
