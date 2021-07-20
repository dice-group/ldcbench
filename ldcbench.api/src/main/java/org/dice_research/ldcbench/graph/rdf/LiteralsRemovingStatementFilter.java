package org.dice_research.ldcbench.graph.rdf;

import org.apache.jena.rdf.model.Statement;

/**
 * This filter removes (i.e., returns {@code false}) all statements that have a
 * literal as object.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class LiteralsRemovingStatementFilter implements StatementFilter {

    @Override
    public boolean test(Statement s) {
        return !s.getObject().isLiteral();
    }

}
