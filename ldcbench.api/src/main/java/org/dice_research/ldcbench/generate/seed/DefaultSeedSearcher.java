package org.dice_research.ldcbench.generate.seed;

import java.util.BitSet;
import java.util.stream.IntStream;

import org.dice_research.ldcbench.graph.Graph;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Default implementation of the seed searcher interface that follows partially
 * a greedy algorithm. First, it identifies vertices that have no incoming edge
 * within the given graph. These nodes are used as first seed nodes. If these
 * nodes do not cover the complete graph, a greedy algorithm iterates over the
 * remaining vertices and adds more of them to the list of seed nodes until all
 * vertices of the graph are covered.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class DefaultSeedSearcher implements SeedSearcher {

    @Override
    public int[] searchSeedNodes(Graph graph) {
        int numberOfNodes = graph.getNumberOfNodes();
        // Search for authority nodes, i.e., nodes that have no incoming edges
        int[] hubs = searchHubs(graph);
        BitSet visitedNodes = new BitSet();
        for (int i = 0; i < hubs.length; ++i) {
            visitNodes(hubs[i], graph, visitedNodes);
        }
        // If we can already reach all nodes
        if(visitedNodes.cardinality() >= numberOfNodes) {
            return hubs;
        }
        // While there are still nodes unreachable, apply a greedy approach
        int nextStartNode = 0;
        IntSet seedNodes = new IntOpenHashSet(hubs);
        while (visitedNodes.cardinality() < numberOfNodes) {
            nextStartNode = visitedNodes.nextClearBit(nextStartNode);
            visitNodes(nextStartNode, graph, visitedNodes);
            seedNodes.add(nextStartNode);
        }
        return seedNodes.toIntArray();
    }

    /**
     * Recursive function visiting nodes starting at the given node.
     */
    private void visitNodes(int node, Graph graph, BitSet visitedNodes) {
        visitedNodes.set(node);
        if (graph.outgoingEdgeCount(node) > 0) {
            int[] targets = graph.outgoingEdgeTargets(node);
            for (int i = 0; i < targets.length; ++i) {
                if (!visitedNodes.get(targets[i])) {
                    visitNodes(targets[i], graph, visitedNodes);
                }
            }
        }
    }

    /**
     * This method identifies hub nodes, i.e., nodes that have no incoming edge.
     * 
     * @param graph the graph from which these nodes should be derived
     * @return the IDs of the hub nodes
     */
    private int[] searchHubs(Graph graph) {
        return IntStream.range(0, graph.getNumberOfNodes()).filter(i -> graph.incomingEdgeCount(i) <= 0).toArray();
    }
}
