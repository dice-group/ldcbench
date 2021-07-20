package org.dice_research.ldcbench;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.hobbit.sdk.utils.ModelsHandler;
import org.junit.Test;

/**
 * @author Pavel Smirnov
 */

public class BenchmarkIT extends BenchmarkTestBase {
    @Test
    public void executeBenchmark() throws Exception {
        executeBenchmark(true);
    }

    @Override
    public Model createBenchmarkParameters() throws IOException {
        return ModelsHandler.readModelFromFile("integration-benchmark-parameters.ttl");
    }
}
