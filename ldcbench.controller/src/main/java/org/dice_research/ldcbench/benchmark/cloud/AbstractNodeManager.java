package org.dice_research.ldcbench.benchmark.cloud;

import org.dice_research.ldcbench.data.NodeMetadata;

public abstract class AbstractNodeManager {
    public int weightOfLinkFrom(Class<?> nodeManager) {
        return 1;
    };

    public abstract boolean canBeHub();

    public String[] getNodeEnvironment() {
        return new String[]{};
    };

    public abstract String[] getDataGeneratorEnvironment(long averageRdfGraphDegree, long triplesPerNode);

    public abstract String getImageName();

    public abstract String getLabel();

    public NodeMetadata getMetadata() {
        return new NodeMetadata();
    }
}
