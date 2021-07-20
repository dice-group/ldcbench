package org.dice_research.ldcbench.builders;

import org.hobbit.sdk.docker.builders.AbstractDockersBuilder;
import static org.dice_research.ldcbench.Constants.*;

public class SimpleHttpNodeBuilder extends LDCBenchNodeBuilder {
    private static final String name = HTTPNODE_IMAGE_NAME;

    public SimpleHttpNodeBuilder(AbstractDockersBuilder builder) {
        super(builder);
    }

    @Override
    public String getName() {
        return name;
    }
}
