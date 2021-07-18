package org.dice_research.ldcbench.benchmark.cloud;

import org.apache.jena.rdf.model.Property;
import org.dice_research.ldcbench.benchmark.DataGenerator;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.dice_research.ldcbench.Constants;

/**
 * Manager class for an JsonLD node.
 *
 * @author Thoren Grüttemeier (thoreng@uni-paderborn.de)
 *
 */
public class JsonLdNodeManager extends AbstractNodeManager {
    public static Property getBenchmarkParameter() {
        return LDCBench.jsonldNodeWeight;
    }

    @Override
    public boolean canBeHub() {
        return false;
    }

    @Override
    public String[] getNodeEnvironment() {
        return new String[]{
            "LDCBENCH_USE_SINGLE_FILE=" + Boolean.TRUE.toString()
        };
    };

    @Override
    public String[] getDataGeneratorEnvironment(long averageRdfGraphDegree, long triplesPerNode) {
        String[] env = new String[]{
            DataGenerator.ENV_AVERAGE_DEGREE_KEY + "=" + averageRdfGraphDegree,
            DataGenerator.ENV_NUMBER_OF_EDGES_KEY + "=" + triplesPerNode,
        };
        return env;
    }

    @Override
    public String getNodeImageName() {
        return Constants.HENODE_IMAGE_NAME;
    }

    @Override
    public String getLabel() {
        return "JsonLD";
    }

    @Override
    public String getDataGeneratorImageName() {
        return Constants.JSONLDDATAGEN_IMAGE_NAME;
    }
}
