package org.dice_research.ldcbench.graph.rdf;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.GrphBasedGraph;

public class SimpleRDF2GrphConverter {

    protected StatementFilter stmtFilter = new LiteralsRemovingStatementFilter();

    public SimpleRDF2GrphConverter() {
    }

    /**
     * Converts the given model to a {@link GrphBasedGraph} object.
     * 
     * @param model the RDF model that should be converted
     * @return the Grph-based graph comprising the data of the given model
     */
    public GrphBasedGraph convert(Model model) {
        GrphBasedGraph graph = new GrphBasedGraph();
        convert(model, graph);
        return graph;
    }

    /**
     * Adds the triples of the given model to the given graph.
     * 
     * @param model the RDF model that should be converted
     * @param graph the graph to which the triples of the model weill be added
     */
    public void convert(Model model, GraphBuilder graph) {
        Map<String, Integer> nodeMapping = new HashMap<>();
        Map<String, Integer> propertiesMapping = new HashMap<>();

        StmtIterator iterator = model.listStatements();
        Statement s;
        // Iterate over all triples
        while (iterator.hasNext()) {
            s = iterator.next();
            // If the triple can be added to the graph
            if (stmtFilter.test(s)) {
                // Add the triple
                addTripleToGraph(s, graph, nodeMapping, propertiesMapping);
            }
        }
    }

    protected void addTripleToGraph(Statement s, GraphBuilder graph, Map<String, Integer> nodeMapping,
            Map<String, Integer> propertiesMapping) {
        // Get IDs for the single elements of the triple
        int sourceId = getOrAddId(s.getSubject(), nodeMapping);
        if (graph.getNumberOfNodes() <= sourceId) {
            graph.addNode();
        }
        int typeId = getOrAddId(s.getPredicate(), propertiesMapping);
        int targetId = getOrAddId(s.getObject(), nodeMapping);
        if (graph.getNumberOfNodes() <= targetId) {
            graph.addNode();
        }
        if (!graph.addEdge(sourceId, targetId, typeId)) {
            throw new IllegalStateException("Couldn't add the statement " + s.toString());
        }
    }

    /**
     * Gets the ID for the given {@link RDFNode} from the given mapping or adds it
     * with a new ID to the given mapping.
     * 
     * @param node    the {@link RDFNode} that is used to search for the ID
     * @param mapping the mapping that should contain the ID or to which the newly
     *                generated ID will be added
     * @return the ID for the given {@link RDFNode}
     */
    protected int getOrAddId(RDFNode node, Map<String, Integer> mapping) {
        String key;
        if (node.isURIResource()) {
            key = node.asResource().getURI();
        } else if (node.isLiteral()) {
            key = node.asLiteral().getLexicalForm();
        } else if (node.isAnon()) {
            key = node.asResource().getId().getLabelString();
        } else {
            // Shouldn't be possible
            throw new IllegalArgumentException(
                    "Got an RDF node that is neither a URI, a literal nor a blank node: " + node.toString());
        }
        return getOrAddId(key, mapping);
    }

    /**
     * Gets the ID for the given key from the given mapping or adds it with a new ID
     * to the given mapping.
     * 
     * @param key     the key that is used to search for the ID
     * @param mapping the mapping that should contain the ID or to which the newly
     *                generated ID will be added
     * @return the ID for the given key
     */
    protected int getOrAddId(String key, Map<String, Integer> mapping) {
        if (mapping.containsKey(key)) {
            return mapping.get(key);
        } else {
            int id = mapping.size();
            mapping.put(key, id);
            return id;
        }
    }
}
