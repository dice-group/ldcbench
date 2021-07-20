package org.dice_research.ldcbench.graph.rdf;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LiteralsRemovingStatementFilterTest extends AbstractStatementFilterTest {

    public LiteralsRemovingStatementFilterTest(StatementFilter filter, Statement statement, Boolean expectedResult) {
        super(filter, statement, expectedResult);
    }

    @Parameters
    public static List<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<Object[]>();
        LiteralsRemovingStatementFilter filter = new LiteralsRemovingStatementFilter();

        Resource r1 = ResourceFactory.createResource("http://example.org/r1");
        Resource r2 = ResourceFactory.createResource("http://example.org/r2");
        Resource b1 = ResourceFactory.createResource();
        Property p1 = ResourceFactory.createProperty("http://example.org/p1");
        Property p2 = ResourceFactory.createProperty("http://example.org/p2");
        Literal l1 = ResourceFactory.createPlainLiteral("Test");
        Literal l2 = ResourceFactory.createLangLiteral("test", "en");
        Literal l3 = ResourceFactory.createTypedLiteral(new Double(1.0));

        // Triple with 3 IRIs
        testCases.add(new Object[] { filter, new StatementImpl(r1, p1, r2), true });
        testCases.add(new Object[] { filter, new StatementImpl(p1, p2, p1), true });

        // Triples with blank nodes
        testCases.add(new Object[] { filter, new StatementImpl(r1, p1, b1), true });
        testCases.add(new Object[] { filter, new StatementImpl(b1, p1, b1), true });

        // Triples with literals
        testCases.add(new Object[] { filter, new StatementImpl(r1, p1, l1), false });
        testCases.add(new Object[] { filter, new StatementImpl(r1, p1, l2), false });
        testCases.add(new Object[] { filter, new StatementImpl(r1, p1, l3), false });
        
        return testCases;
    }
}
