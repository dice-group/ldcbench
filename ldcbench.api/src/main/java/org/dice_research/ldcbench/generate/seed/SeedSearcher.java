package org.dice_research.ldcbench.generate.seed;

import org.dice_research.ldcbench.graph.Graph;

/**
 * A seed searcher offers to search for seed nodes of a given graph.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface SeedSearcher {

    /**
     * This method identifies seed nodes within the given graph and returns them as
     * an array of node IDs.
     * 
     * @param graph the graph for which the seed nodes should be searched
     * @return the identified seed nodes
     */
    public int[] searchSeedNodes(Graph graph);

}
