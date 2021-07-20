package org.dice_research.ldcbench.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Representation of the LDCBench vocabulary as Java objects.
 */
public class LDCBench {

    protected static final String uri = "http://w3id.org/dice-research/orca/ontology#";

    public static final Resource ExponentialDistNodeSize = resource("ExponentialDistNodeSize");
    public static final Resource StaticNodeSize = resource("StaticNodeSize");

    // Properties sorted alphabetically
    public static final Property averageCrawlDelay = property("averageCrawlDelay");
    public static final Property averageDisallowedRatio = property("averageDisallowedRatio");
    public static final Property averageDiskUsage = property("averageDiskUsage");
    public static final Property averageMemoryUsage = property("averageMemoryUsage");
    public static final Property averageNodeGraphDegree = property("averageNodeGraphDegree");
    public static final Property averageRdfGraphDegree = property("averageRdfGraphDegree");
    public static final Property averageTriplesPerNode = property("averageTriplesPerNode");
    public static final Property ckanNodeWeight = property("ckanNodeWeight");
    public static final Property dereferencingHttpNodeWeight = property("dereferencingHttpNodeWeight");
    public static final Property graphVisualization = property("graphVisualization");
    public static final Property httpDumpNodeCompressedRatio = property("httpDumpNodeCompressedRatio");
    public static final Property httpDumpNodeWeight = property("httpDumpNodeWeight");
    public static final Property numberOfNodes = property("numberOfNodes");
    public static final Property macroRecall = property("macroRecall");
    public static final Property microRecall = property("microRecall");
    public static final Property nodeSizeDeterminer = property("nodeSizeDeterminer");
    public static final Property numberOfDisallowedResources = property("numberOfDisallowedResources");
    public static final Property ratioOfRequestedDisallowedResources = property("ratioOfRequestedDisallowedResources");
    public static final Property rdfaNodeWeight = property("rdfaNodeWeight");
    public static final Property resourceUsageOverTime = property("resourceUsageOverTime");
    public static final Property runtime = property("runtime");
    public static final Property seed = property("seed");
    public static final Property sparqlNodeWeight = property("sparqlNodeWeight");
    public static final Property totalCpuUsage = property("totalCpuUsage");
    public static final Property tripleCountOverTime = property("tripleCountOverTime");
    public static final Property triplesEvaluated = property("triplesEvaluated");
    public static final Property truePositives = property("truePositives");
    public static final Property useN3Dumps = property("useN3Dumps");
    public static final Property useNtDumps = property("useNtDumps");
    public static final Property useRdfXmlDumps = property("useRdfXmlDumps");
    public static final Property useTurtleDumps = property("useTurtleDumps");
    public static final Property microAverageCrawlDelayFulfillment = property("microAverageCrawlDelayFulfillment");
    public static final Property macroAverageCrawlDelayFulfillment = property("macroAverageCrawlDelayFulfillment");
    public static final Property minCrawlDelay = property("minCrawlDelay");
    public static final Property maxCrawlDelay = property("maxCrawlDelay");
    public static final Property minAverageCrawlDelayFulfillment = property("minAverageCrawlDelayFulfillment");
    public static final Property maxAverageCrawlDelayFulfillment = property("maxAverageCrawlDelayFulfillment");
    
    public static final Property lemmingDataset = property("lemmingDataset");
    public static final Property lemmingDatasetDirectory = property("datasetDirectory");

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

}
