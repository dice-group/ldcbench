package org.dice_research.ldcbench.builders;

import org.hobbit.sdk.docker.builders.AbstractDockersBuilder;
import static org.dice_research.ldcbench.Constants.*;

public class CkanNodeBuilder extends LDCBenchNodeBuilder {
    private static final String name = CKANNODE_IMAGE_NAME;

    public CkanNodeBuilder(AbstractDockersBuilder builder) {
        super(builder);
    }

    @Override
    public String getName() {
        return name;
    }
}
