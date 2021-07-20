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
public class ResourceUsageDiagrams {

    protected static final String BASE_URI = "http://w3id.org/dice-research/orca/resource-usage-per-time#";
    protected static final String BASE_INSTANCE_URI = "http://w3id.org/dice-research/orca/resource-usage-per-time/instance#";

    // Properties sorted alphabetically
    public static final Property time = property("time");
    public static final Property totalCpuUsage = property("totalCpuUsage");
    public static final Property totalDiskUsage = property("totalDiskUsage");
    public static final Property totalMemoryUsage = property("totalMemoryUsage");

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(BASE_URI, local);
    }

    protected static final void addMeasure(Model model, String datasetUri, Resource dataStructureDefinition, Property measure, String label, String comment, Resource range) {
        Resource measureNode = model.createResource(datasetUri + "-measure-component-" + measure.getLocalName());
        model.add(dataStructureDefinition, DataCube.component, measureNode);
        model.add(measureNode, DataCube.measure, measure);

        model.add(measure, RDF.type, RDF.Property);
        model.add(measure, RDF.type, DataCube.MeasureProperty);
        model.add(measure, RDFS.label, model.createLiteral(label, "en"));
        model.add(measure, RDFS.comment, model.createLiteral(comment, "en"));
        model.add(measure, RDFS.range, range);
    }

    public static Resource createDataset(Model model, String identifier) {
        String datasetUri = BASE_INSTANCE_URI + identifier;
        Resource dataset = model.createResource(datasetUri);
        model.add(dataset, RDF.type, DataCube.DataSet);
        Resource structureNode = model.createResource(datasetUri + "-structure");
        model.add(dataset, DataCube.structure, structureNode);
        model.add(structureNode, RDF.type, DataCube.DataStructureDefinition);
        Resource dimensionNode = model.createResource(datasetUri + "-dimension-component-time");
        model.add(structureNode, DataCube.component, dimensionNode);
        model.add(dimensionNode, DataCube.dimension, time);

        model.add(time, RDF.type, RDF.Property);
        model.add(time, RDF.type, DataCube.DimensionProperty);
        model.add(time, RDFS.label, model.createLiteral("runtime (in ms)", "en"));
        model.add(time, RDFS.comment, model.createLiteral("The runtime of the crawler in milliseconds.", "en"));
        model.add(time, RDFS.range, XSD.xlong);

        addMeasure(model, datasetUri, structureNode, totalCpuUsage, "CPU", "The sum of the overall CPU usage in ms.", XSD.xlong);
        addMeasure(model, datasetUri, structureNode, totalDiskUsage, "Disk", "Total file system usage", XSD.xlong);
        addMeasure(model, datasetUri, structureNode, totalMemoryUsage, "Memory", "Total memory usage", XSD.xlong);

        return dataset;
    }

    public static void addPoint(Model model, Resource dataset, String identifier, int pointId, long timestamp,
            long cpuUsage, long diskUsage, long memoryUsage) {
        Resource observation = model.createResource(dataset.getURI() + "-" + pointId);
        model.add(observation, RDF.type, DataCube.Observation);
        model.add(observation, DataCube.dataSet, dataset);
        model.addLiteral(observation, time, timestamp);
        model.addLiteral(observation, totalCpuUsage, cpuUsage);
        model.addLiteral(observation, totalDiskUsage, diskUsage);
        model.addLiteral(observation, totalMemoryUsage, memoryUsage);
    }

}
