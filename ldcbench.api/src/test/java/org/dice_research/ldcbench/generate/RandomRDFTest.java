//package org.dice_research.ldcbench;
package org.dice_research.ldcbench.generate;

import static org.junit.Assert.assertEquals;

import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RandomRDFTest {
    int N;
    RandomRDF rg;
    GrphBasedGraph g;

    // private static Random generator ;
    @Before
    public void setUp() {
        g = new GrphBasedGraph();
        rg = new RandomRDF("Barabasi Random RDF");
        N = 40000;

    }

    @Test(expected = IllegalArgumentException.class)
    public void ValidDegree() throws IllegalArgumentException {
        rg.generateGraph(N, 0.9, 123L, g);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxDegree() throws IllegalArgumentException {
        rg.generateGraph(4.0, 2, 123L, g);
    }

    @Test
    public void RealDegree() {
        rg.generateGraph(N, 2.5, 123L, g);
        assertEquals("Number of edges", N*2.5/2, g.getNumberOfEdges());
    }

    @Test
    public void IntDegree() {
        rg.generateGraph(10.0, 200000, 10L, g);
        assertEquals("Number of nodes", 40000, g.getNumberOfNodes());
//        assertEquals("Number of nodes", 36395, g.getNumberOfNodes());//when using uniform degree
        assertEquals("Number of edges", 200000, g.getNumberOfEdges());

        // g.print();
        // g.saveToFile(String.format("D:\\RandGj_N%d.txt", N));
    }

    @Test
    public void OneNodeGraph() {
        rg.generateGraph(1, 0.0, 123L, g);
        assertEquals("Number of nodes", 1, g.getNumberOfNodes());
        assertEquals("Number of edges", 0, g.getNumberOfEdges());

    }

    @Test
    public void EmptyNodeExample() {
        // With these numbers it was observed that node 19 has neither incoming nor
        // outgoing edges. This should not be possible.
        rg.generateGraph(10.0, 100, 496, g);
//        g.print();
        assertEquals("Number of edges", 100, g.getNumberOfEdges());
        int nodeCnt=g.getNumberOfNodes();
        for(int nn=0;nn < nodeCnt;nn++) 
           Assert.assertTrue(String.format("Node %d has no edges!",nn), (g.incomingEdgeTypes(nn).length + g.outgoingEdgeTypes(nn).length) > 0);
    }

}
