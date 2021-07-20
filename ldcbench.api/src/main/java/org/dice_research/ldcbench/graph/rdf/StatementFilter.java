package org.dice_research.ldcbench.graph.rdf;

import java.util.function.Predicate;

import org.apache.jena.rdf.model.Statement;

/**
 * A filter that implements true or false for a given {@link Statement}. It is
 * typically used by a converter to remove statements that can not be converted.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface StatementFilter extends Predicate<Statement> {

}
