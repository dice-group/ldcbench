package org.dice_research.ldcbench.generate.seed;

import java.util.Arrays;

import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.GrphBasedGraph;
import org.junit.Assert;
import org.junit.Test;

public class DefaultSeedSearcherTest {

    @Test
    public void testStarGraph() {
        GraphBuilder builder = new GrphBasedGraph();
        builder.addNodes(4);
        builder.addEdge(3, 0, 0);
        builder.addEdge(3, 1, 2);
        builder.addEdge(3, 2, 1);

        SeedSearcher searcher = new DefaultSeedSearcher();
        int[] result = searcher.searchSeedNodes(builder);
        String resultString = Arrays.toString(result);
        Assert.assertTrue("The result has the wrong length " + resultString, result.length == 1);
        if ((result[0] != 3)) {
            Assert.fail("The seed node of " + resultString + " was expected to be 3.");
        }
    }

    @Test
    public void testPartitionedGraph() {
        // The graph is separated into two parts. A loop (0->1->5) and three other nodes
        //
        // 0 -> 1
        // 1 -> 5
        // 3 -> 2
        // 3 -> 4
        // 5 -> 0
        GraphBuilder builder = new GrphBasedGraph();
        builder.addNodes(6);
        builder.addEdge(0, 1, 0);
        builder.addEdge(1, 5, 0);
        builder.addEdge(3, 2, 0);
        builder.addEdge(3, 4, 0);
        builder.addEdge(5, 0, 0);

        SeedSearcher searcher = new DefaultSeedSearcher();
        int[] result = searcher.searchSeedNodes(builder);
        String resultString = Arrays.toString(result);
        Assert.assertTrue("The result has the wrong length " + resultString, result.length == 2);
        if ((result[0] != 3) && (result[1] != 3)) {
            Assert.fail("One of the two seed nodes of " + resultString + " was expected to be 3.");
        }
        if ((result[0] != 0) && (result[1] != 0) && (result[0] != 1) && (result[1] != 1) && (result[0] != 5)
                && (result[1] != 5)) {
            Assert.fail("One of the two seed nodes of " + resultString + " was expected to be 0, 1 or 5.");
        }
    }
}
