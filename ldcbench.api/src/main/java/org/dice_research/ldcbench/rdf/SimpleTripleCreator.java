package org.dice_research.ldcbench.rdf;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * A simple {@link TripleCreator} implementation relying on a base graph Id and
 * a list of domain names of the existing graphs. Node that it makes this class
 * being bound to a single graph.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SimpleTripleCreator implements TripleCreator {

    protected int baseGraphId;
    protected String domains[];

    /**
     * Constructor.
     * 
     * @param baseGraphId
     *            the graph Id of nodes that are not external nodes of the graph for
     *            which this triple creator is used.
     * @param domains
     *            a mapping from graph Ids to domain names.
     */
    public SimpleTripleCreator(int baseGraphId, String[] domains) {
        this.baseGraphId = baseGraphId;
        this.domains = domains;
    }

    @Override
    public Triple createTriple(int sourceId, int propertyId, int targetId, int targetExtId, int targetExtGraphId) {
        return new Triple(createNode(sourceId, -1, -1, false), createNode(propertyId, -1, -1, true),
                createNode(targetId, targetExtId, targetExtGraphId, false));
    }

    /**
     * Creates a {@link Node} instance based on the given information.
     * 
     * @param nodeId
     *            the internal Id of the node
     * @param externalId
     *            the external Id of the node if it belongs to a different graph or
     *            {@code -1} if it is an internal node
     * @param extGraphId
     *            the Id of the graph to which this node belongs to or {@code -1} if
     *            it is an internal node
     * @param isProperty
     *            a flag indicating whether the node is a property
     * @return the created {@link Node} instance
     */
    protected Node createNode(int nodeId, int externalId, int extGraphId, boolean isProperty) {
        String domain;
        if (externalId < 0) {
            externalId = nodeId;
            domain = domains[baseGraphId];
        } else {
            domain = domains[extGraphId];
            // TODO get the datasetId on the other server
        }
        Node n;
        if (isProperty) {
            n = ResourceFactory.createProperty(UriHelper.creatUri(domain, 0, UriHelper.PROPERTY_NODE_TYPE, externalId))
                    .asNode();
        } else {
            n = ResourceFactory.createResource(UriHelper.creatUri(domain, 0, UriHelper.RESOURCE_NODE_TYPE, externalId))
                    .asNode();
        }
        return n;
    }
}
