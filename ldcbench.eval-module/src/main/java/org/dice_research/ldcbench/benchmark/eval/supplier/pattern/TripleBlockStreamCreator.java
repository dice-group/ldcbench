package org.dice_research.ldcbench.benchmark.eval.supplier.pattern;

import java.util.stream.Stream;

import org.apache.jena.sparql.syntax.ElementTriplesBlock;

/**
 * The interface of a class creating a stream of {@link ElementTriplesBlock}
 * instances used for checking a graph for its completeness. The class is based
 * on a graph representation that has been serialized in a file and is not
 * further defined. Therefore, it offers an {@link #accepts(int, String, String[], String[])} method
 * which should be used to determine whether the creator can use the given graph
 * representation to create the stream.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 */
public interface TripleBlockStreamCreator {

    /**
     * This method should check whether this creator can use the given object to
     * create a stream of {@link ElementTriplesBlock} instances. Note that if
     * {@code true} is returned, the {@link #createStreamIfPossible(Object)} method
     * assumes that the given object can be casted into the template type {@code T}.
     * 
     * @param graphId              the id of the given graph
     * @param graphFile            the file containing the graph to be used by this
     *                             creator instance.
     * @param resourceUriTemplates the resource URI templates all graphs
     * @param accessUriTemplates   the access URI templates for all graphs
     * @return {@code true} if a stream of {@link ElementTriplesBlock} instances
     *         could be created based on the given object, else {@code false}.
     */
    public boolean accepts(int graphId, String graphFile, String[] resourceUriTemplates, String[] accessUriTemplates);

    /**
     * This method creates a stream of {@link ElementTriplesBlock} instances using
     * the given object.
     * 
     * @param graphId              the id of the given graph
     * @param graphFile            the file containing the graph to be used by this
     *                             creator instance.
     * @param resourceUriTemplates the resource URI templates all graphs
     * @param accessUriTemplates   the access URI templates for all graphs
     * @return the newly created stream of {@link ElementTriplesBlock} instances
     */
    public Stream<ElementTriplesBlock> createStream(int graphId, String graphFile, String[] resourceUriTemplates,
            String[] accessUriTemplates);
}
