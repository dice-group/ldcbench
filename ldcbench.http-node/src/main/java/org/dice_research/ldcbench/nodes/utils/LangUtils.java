package org.dice_research.ldcbench.nodes.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.rdfhdt.hdt.enums.RDFNotation;

import java.util.Random;
/**
 *
 * Useful methods for Jena Lang issues
 *
 * @author Geraldo de Souza Jr
 *
 */

public class LangUtils {

    public static final Lang HDT_LANG = LangBuilder.create("HDT", "application/vnd.hdt").build();

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
        model.addLiteral(HOBBIT.Experiment, LDCBench.useN3Dumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useRdfXmlDumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useNtDumps, true);
        model.addLiteral(HOBBIT.Experiment, LDCBench.useHDTDumps, true);
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
        if (Boolean.parseBoolean(RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.useHDTDumps))) {
            langlist.add(HDT_LANG);
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

    /**
    *
    * Method that returns the RDFNotation of a Language
    *
    * @return a RDFNotation
    */
    public static RDFNotation getRDFNotation(Lang lang) {
        if (lang.equals(Lang.NT))
            return RDFNotation.NTRIPLES;
        if (lang.equals(Lang.TTL))
            return RDFNotation.TURTLE;
        if (lang.equals(Lang.RDFXML))
            return RDFNotation.RDFXML;
        if (lang.equals(Lang.N3))
            return RDFNotation.N3;
        return null;
    }

}
