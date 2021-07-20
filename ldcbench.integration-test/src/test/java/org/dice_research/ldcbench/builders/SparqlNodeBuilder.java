package org.dice_research.ldcbench.builders;

import org.hobbit.sdk.docker.builders.AbstractDockersBuilder;
import static org.dice_research.ldcbench.Constants.*;

public class SparqlNodeBuilder extends LDCBenchNodeBuilder {
    private static final String name = SPARQLNODE_IMAGE_NAME;

    public SparqlNodeBuilder(AbstractDockersBuilder builder) {
        super(builder);
    }

    @Override
    public String getName() {
        return name;
    }
}
