package org.dice_research.ldcbench.generate;

import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Ignore;
import org.junit.Test;

public class SmallRandomCloudTest {
    @Test
    @Ignore
    public void testNodeTypes() {
        int[] nodetypes = {0, 1}; // First node of type 0, second node of type 1
        boolean[] ishub = {false, true, false};
        int[][] typeconn = {{1,0,0},{1,1,1},{1,0,0}};
        RandomCloudGraph rg = new RandomCloudGraph("Graph", nodetypes, ishub, typeconn);
        GrphBasedGraph g = new GrphBasedGraph();
        rg.generateGraph(2, 1.0, 0, g);
    }

    @Test
    @Ignore
    public void testNodesOfSameType() {
        int[] nodetypes = {0, 0, 0, 0};
        boolean[] ishub = {false, true, false};
        int[][] typeconn = {{1,0,0},{1,1,1},{1,0,0}};
        RandomCloudGraph rg = new RandomCloudGraph("Graph", nodetypes, ishub, typeconn);
        GrphBasedGraph g = new GrphBasedGraph();
        rg.generateGraph(4, 2.0, 0, g);
    }

    @Test
    @Ignore
    public void testNodeCounts() {
        int[] typecounts = {1, 1, 0}; // 1 node of first type, 1 node of second type, 0 nodes of third type
        int hcount = 0; // What to pass there?
        int[][] typeconn = {{1,0,0},{1,1,1},{1,0,0}};
        RandomCloudGraph rg = new RandomCloudGraph("Graph", typecounts, hcount, typeconn);
        GrphBasedGraph g = new GrphBasedGraph();
        rg.generateGraph(2, 1.0, 0, g);
    }

    @Test
    public void testNodeCounts2() {
        int[] typecounts = {1, 1}; // 1 node of first type, 1 node of second type, 0 nodes of third type
        int hcount = 0; // What to pass there?
        int[][] typeconn = {{1,0},{1,1}};
        RandomCloudGraph rg = new RandomCloudGraph("Graph", typecounts, hcount, typeconn);
        GrphBasedGraph g = new GrphBasedGraph();
        rg.generateGraph(2, 1.0, 0, g);
    }
}
