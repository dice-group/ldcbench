package org.dice_research.ldcbench.benchmark;

import org.dice_research.ldcbench.generate.FileBasedGraphGenerator;
import org.dice_research.ldcbench.generate.GraphGenerator;
import org.hobbit.utils.EnvVariables;

/**
 * This extension of the default {@link DataGenerator} loads an RDF file from a
 * given location and converts it into a graph representation.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class FileBasedRDFGraphGenerator extends DataGenerator {

    public static final String RDF_FILE_LOCATION_KEY = "LDCBENCH_RDF_FILE_LOCATION";
    public static final String RDF_FILE_LANG_KEY = "LDCBENCH_RDF_FILE_LANG";

    @Override
    protected GraphGenerator createRDFGraphGenerator() {
        String fileLocation = EnvVariables.getString(RDF_FILE_LOCATION_KEY, LOGGER);
        String fileLang = EnvVariables.getString(RDF_FILE_LANG_KEY, LOGGER);
        LOGGER.info("Loading data from \"{}\" using the \"{}\" serialization...", fileLocation, fileLang);
        return new FileBasedGraphGenerator(fileLocation, "", fileLang);
    }
}
