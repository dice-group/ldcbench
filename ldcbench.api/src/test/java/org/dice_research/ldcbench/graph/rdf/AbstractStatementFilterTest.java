package org.dice_research.ldcbench.graph.rdf;

import org.apache.jena.rdf.model.Statement;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractStatementFilterTest {

    private StatementFilter filter;
    private Statement statement;
    private Boolean expectedResult;

    public AbstractStatementFilterTest(StatementFilter filter, Statement statement, Boolean expectedResult) {
        this.filter = filter;
        this.statement = statement;
        this.expectedResult = expectedResult;
    }

    @Test
    public void test() {
        Assert.assertEquals("The filter does not give the right answer for " + statement.toString(), expectedResult,
                filter.test(statement));
    }
}
