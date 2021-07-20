package org.dice_research.ldcbench.vocab;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.hobbit.vocab.DataCube;

/**
 * Representation of the LDCBench vocabulary as Java objects.
 */
public class LDCBenchDiagrams {

    protected static final String TRIPLES_PER_TIME_URI = "http://w3id.org/dice-research/orca/triples-per-time#";
    protected static final String TRIPLES_PER_TIME_INSTANCE_URI = "http://w3id.org/dice-research/orca/triples-per-time/instance#";

    // Properties sorted alphabetically
    public static final Property time = property("time");
    public static final Property triples = property("triples");

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(TRIPLES_PER_TIME_URI, local);
    }

    public static Resource createDataset(Model model, String identifier) {
        String datasetUri = TRIPLES_PER_TIME_INSTANCE_URI + identifier;
        Resource dataset = model.createResource(datasetUri);
        model.add(dataset, RDF.type, DataCube.DataSet);
        Resource structureNode = model.createResource(datasetUri + "-structure");
        model.add(dataset, DataCube.structure, structureNode);
        model.add(structureNode, RDF.type, DataCube.DataStructureDefinition);
        Resource dimensionNode = model.createResource(datasetUri + "-dimension-component");
        model.add(structureNode, DataCube.component, dimensionNode);
        model.add(dimensionNode, DataCube.dimension, time);
        Resource measureNode = model.createResource(datasetUri + "-measure-component");
        model.add(structureNode, DataCube.component, measureNode);
        model.add(measureNode, DataCube.measure, triples);

        model.add(time, RDF.type, RDF.Property);
        model.add(time, RDF.type, DataCube.DimensionProperty);
        model.add(time, RDFS.label, model.createLiteral("runtime (in ms)", "en"));
        model.add(time, RDFS.comment, model.createLiteral("The runtime of the crawler in milliseconds.", "en"));
        model.add(time, RDFS.range, XSD.xlong);

        model.add(triples, RDF.type, RDF.Property);
        model.add(triples, RDF.type, DataCube.MeasureProperty);
        model.add(triples, RDFS.label, model.createLiteral("triples", "en"));
        model.add(triples, RDFS.comment, model.createLiteral("The number of triples the crawler has stored.", "en"));
        model.add(triples, RDFS.range, XSD.xlong);

        return dataset;
    }

    public static void addPoint(Model model, Resource dataset, String identifier, int pointId, long timestamp,
            long triples) {
        Resource observation = model.createResource(dataset.getURI() + "-" + pointId);
        model.add(observation, RDF.type, DataCube.Observation);
        model.add(observation, DataCube.dataSet, dataset);
        model.addLiteral(observation, LDCBenchDiagrams.time, timestamp);
        model.addLiteral(observation, LDCBenchDiagrams.triples, triples);
    }

}
