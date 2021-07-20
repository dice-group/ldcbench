package org.dice_research.ldcbench.generate;

import static org.junit.Assert.assertEquals;

import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Before;
import org.junit.Test;
public class RandomLODTest {
    int N=1069;
    RandomCloudGraph rg;
    GrphBasedGraph g;
    // Number of nodes in each type
    int[] typecounts= {500,400,100,69};
    // which types connect to which
    int[][] typeconn= {{1,1,0,0},{0,1,1,0},{0,0,1,1},{1,1,1,1}};
    //    private static Random generator ;
    @Before
    public void setUp() {
        g = new GrphBasedGraph();
        rg = new RandomCloudGraph("Barabasi RandomLOD",typecounts,100,typeconn);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ValidDegree() throws IllegalArgumentException {
        rg.generateGraph(N,0.9,123L,g);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxDegree() throws IllegalArgumentException {
        rg.generateGraph(4.0,2,123L,g);
    }
    @Test
    public void LOD2017() {
        double degree=2*15654.0/N;
        //rg.generateGraph(5.0,200000,123L,g);
        rg.generateGraph(N,degree,123L,g);
        assertEquals("Number of nodes", N, g.getNumberOfNodes());
        assertEquals("Number of edges", 15654, g.getNumberOfEdges());
       // test type counts
        int[] nodeTypes=rg.getNodeTypes();
        int[] tmptypecounts=new int[typecounts.length];
        for(int i=0; i < nodeTypes.length; i++) tmptypecounts[nodeTypes[i]]++;

        for(int i=0;i<typecounts.length;i++) {
            assertEquals("Number of nodes in type: "+i, tmptypecounts[i], typecounts[i]);
        }
    }

    @Test
    public void totalConnectivity() {
        new RandomCloudGraph("Barabasi RandomLOD", new int[]{1, 1}, 0, new int[][]{{1, 1}, {1, 1}})
        .generateGraph(2, 1.0, 0L, g);
    }

    @Test
    public void oneNodeType() {
        new RandomCloudGraph("Barabasi RandomLOD", new int[]{2}, 0, new int[][]{{1}})
        .generateGraph(2, 1.0, 0L, g);
    }
}
