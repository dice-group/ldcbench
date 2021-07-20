package org.dice_research.ldcbench.generate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.dice_research.ldcbench.generate.seed.DefaultSeedSearcher;
import org.dice_research.ldcbench.generate.seed.SeedSearcher;
import org.dice_research.ldcbench.graph.GraphBuilder;
import org.dice_research.ldcbench.graph.rdf.SimpleRDF2GrphConverter;

/**
 * A simple graph generator that loads the graph from a given RDF file and
 * converts it into a graph object.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class FileBasedGraphGenerator implements GraphGenerator {

    protected String modelLocation;
    protected String modelBase;
    protected String modelLang;

    /**
     * Constructor.
     * 
     * @param modelLocation the location of the RDF file
     * @param modelBase     the base IRI of the RDF model
     * @param modelLang     the serialization of the RDF model
     */
    public FileBasedGraphGenerator(String modelLocation, String modelBase, String modelLang) {
        this.modelLocation = modelLocation;
        this.modelBase = modelBase;
        this.modelLang = modelLang;
    }

    @Override
    public void generateGraph(int numberOfNodes, double avgDegree, long seed, GraphBuilder builder) {
        generateGraph(builder);
    }

    @Override
    public void generateGraph(double avgDegree, int numberOfEdges, long seed, GraphBuilder builder) {
        generateGraph(builder);
    }

    /**
     * Internal method that generates a graph by loading it from a file and writing
     * the converted graph (and the array of entrance nodes) to the given graph
     * builder.
     * 
     * @param builder the builder that should be used to write the graph data
     */
    protected void generateGraph(GraphBuilder builder) {
        convert(loadRDFModel(), builder);
        setEntranceNodes(builder);
    }

    protected Model loadRDFModel() {
        Model model = ModelFactory.createDefaultModel();
        model.read(modelLocation, modelBase, modelLang);
        return model;
    }

    /**
     * Converts the given RDF model into a graph by using the given graph builder
     * 
     * @param model   the RDF model that should be converted
     * @param builder the builder to which the result will be written
     */
    protected void convert(Model model, GraphBuilder builder) {
        SimpleRDF2GrphConverter converter = new SimpleRDF2GrphConverter();
        converter.convert(model, builder);
    }

    /**
     * Sets the entrance nodes of the given graph builder.
     * 
     * @param builder the graph of which the entrance nodes sould be identified and
     *                set
     */
    protected void setEntranceNodes(GraphBuilder builder) {
        SeedSearcher searcher = new DefaultSeedSearcher();
        builder.setEntranceNodes(searcher.searchSeedNodes(builder));
    }
}
