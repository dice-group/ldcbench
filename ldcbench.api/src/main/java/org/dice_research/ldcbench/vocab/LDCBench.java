package org.dice_research.ldcbench.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Representation of the LDCBench vocabulary as Java objects.
 */
public class LDCBench {

    protected static final String uri = "https://github.com/dice-group/ldcbench#";

    /**
     * returns the URI for this schema
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
        return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    // Properties sorted alphabetically
    public static final Property seed = property("seed");
    public static final Property numberOfNodes = property("number-of-nodes");
    public static final Property triplesPerNode = property("triples-per-node");

}