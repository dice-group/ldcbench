package org.dice_research.ldcbench.nodes.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;

import java.util.Random;
/**
 *
 * Useful methods for Jena Lang issues
 *
 * @author Geraldo de Souza Jr
 *
 */

public class LangUtils {

    /**
     *
     * Method that returns a list of Lang Types, that could be used by the dumpFile Node
     *
     *
     * @return List of Lang Types
     */
    public static List<Lang> getAllowedLangs(){
        Model model = ModelFactory.createDefaultModel();
        model.addLiteral(HOBBIT.Experiment, LDCBench.useTurtleDumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useJsonLdDumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useN3Dumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useRdfXmlDumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useNtDumps, true);
        return getAllowedLangs(model);
    }

  /**
   *
   * Method that returns a list of Lang Types, that could be used by the dumpFile Node
   *
   *
   * @return List of Lang Types
   */
    public static List<Lang> getAllowedLangs(Model benchmarkParamModel){
        List<Lang> langlist = new ArrayList<Lang>();

        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useTurtleDumps))) {
            langlist.add(Lang.TTL);
        }
        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useN3Dumps))) {
            langlist.add(Lang.N3);
        }
        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useRdfXmlDumps))) {
            langlist.add(Lang.RDFXML);
        }
        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useNtDumps))) {
            langlist.add(Lang.NT);
        }
        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useJsonLdDumps))) {
            langlist.add(Lang.JSONLD);
        }

        return langlist;
    }

    /**
     *
     * Method that randomize a position in a list returned by
     * the getAllowedLangs() method and return a random
     * Lang Type
     *
     * @return a random Lang Type
     */
    public static Lang getRandomLang(Model benchmarkParamModel, Random rand) {
        List<Lang> langlist = getAllowedLangs(benchmarkParamModel);
        int langPos = rand.nextInt(langlist.size());
        return langlist.get(langPos);
    }

}
