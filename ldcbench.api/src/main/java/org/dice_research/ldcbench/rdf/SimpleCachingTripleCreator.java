package org.dice_research.ldcbench.rdf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;

/**
 * A simple extension of the {@link SimpleTripleCreator} which uses internal
 * maps for caching created nodes. Note that the used maps have no size
 * restriction.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SimpleCachingTripleCreator extends SimpleTripleCreator {

    protected Map<Integer, Node> resourceCache = Collections.synchronizedMap(new HashMap<Integer, Node>());
    protected Map<Integer, Node> propertyCache = Collections.synchronizedMap(new HashMap<Integer, Node>());

    /**
     * Constructor.
     * 
     * @param baseGraphId
     *            the graph Id of nodes that are not external nodes of the graph for
     *            which this triple creator is used.
     * @param resourceUriTemplates
     *            a mapping from graph Ids to resource IRI templates.
     * @param accessUriTemplates
     *            a mapping from graph Ids to access URL templates.
     */
    public SimpleCachingTripleCreator(int baseGraphId, String[] resourceUriTemplates, String[] accessUriTemplates) {
        super(baseGraphId, resourceUriTemplates, accessUriTemplates);
    }

    @Override
    public Node createNode(int nodeId, int externalId, int extGraphId, boolean isProperty) {
        Map<Integer, Node> cache = isProperty ? propertyCache : resourceCache;
        if (cache.containsKey(nodeId)) {
            return cache.get(nodeId);
        }
        Node n = super.createNode(nodeId, externalId, extGraphId, isProperty);
        cache.put(nodeId, n);
        return n;
    }
}
