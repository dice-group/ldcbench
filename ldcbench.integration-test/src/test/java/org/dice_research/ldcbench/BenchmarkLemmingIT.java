package org.dice_research.ldcbench;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.hobbit.sdk.utils.ModelsHandler;

/**
 * @author Pavel Smirnov
 */
public class BenchmarkLemmingIT extends BenchmarkIT {

    @Override
    public Model createBenchmarkParameters() throws IOException {
        return ModelsHandler.readModelFromFile("integration-benchmark-parameters-lemming.ttl");
    }
}
