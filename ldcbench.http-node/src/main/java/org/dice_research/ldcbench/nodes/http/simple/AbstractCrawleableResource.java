package org.dice_research.ldcbench.nodes.http.simple;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.function.Predicate;

import org.apache.http.HttpHeaders;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.common.net.MediaType;

public abstract class AbstractCrawleableResource implements CrawleableResource {

    /**
     * If the default content type is not set, this content type is used.
     */
    public static final String DEFAULT_CONTENT_TYPE = MediaType.OCTET_STREAM.toString();

    /**
     * A predicate deciding whether a given request is answered with this resources
     * or not.
     */

    protected final Predicate<Request> predicate;
    /**
     * The default content type which is used for all resources that do not define
     * their own content type.
     */
    protected MediaType defaultContentType;

    public AbstractCrawleableResource(Predicate<Request> predicate) {
        this(predicate, DEFAULT_CONTENT_TYPE);
    }

    public AbstractCrawleableResource(Predicate<Request> predicate, String defaultContentType) {
        this.predicate = predicate;
        setDefaultContentType(defaultContentType);
    }

    public boolean handleRequest(Request request, Response response, OutputStream out) throws SimpleHttpException {
        // Check whether the request can be answered by this resource
        if (!predicate.test(request)) {
            return false;
        }
        // Get the response type
        MediaType responseType = getResponseType(request.getValues(HttpHeaders.ACCEPT).iterator());
        if (responseType == null) {
            throw new SimpleHttpException("Couldn't find a fitting content type in the list of accepted types ("
                    + request.getValues(HttpHeaders.ACCEPT).toString() + ").", Status.NOT_ACCEPTABLE);
        }
        // Set the response type
        response.setContentType(responseType.toString());
        return handleRequest(request.getTarget(), responseType, response, out);
    }

    /**
     * This method decides which {@link MediaType} should be used to answer the
     * current request based on the given values of the HTTP Accept header. The
     * default implementation of this method always returns the
     * {@link #defaultContentType} while overriding methods might use more elegant
     * solutions.
     * 
     * @param iterator
     *            an iterator giving the media types which are accepted by the
     *            request
     * @return the media type with which the request should be answered or
     *         {@code null} if such a media type does not exist.
     */
    protected MediaType getResponseType(Iterator<String> iterator) {
        return defaultContentType;
    }

    /**
     * Internal method which answers the request that was asking for the given
     * target. The generated response should have the given response type and should
     * be written to the given output stream.
     * 
     * @param target
     *            the target of the request
     * @param responseType
     *            the media type the response should have negotiated by the super
     *            class
     * @param response
     *            the response object which can be used to set the response status
     * @param out
     *            the output stream that should be used to write the response
     * @return returns {@code true} if the request has been handled or {@code false}
     *         if the request couldn't be handled by this method
     * @throws SimpleHttpException
     */
    protected abstract boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException;

    /**
     * Sets the default content type.
     * 
     * @param defaultContentType
     *            the new default content type.
     * @throws IllegalArgumentException
     *             if the given content type can not be parsed as media type.
     */
    public void setDefaultContentType(String defaultContentType) throws IllegalArgumentException {
        setDefaultContentType(MediaType.parse(defaultContentType));
    }

    /**
     * Sets the default content type.
     * 
     * @param defaultContentType
     *            the new default content type.
     */
    public void setDefaultContentType(MediaType defaultContentType) {
        this.defaultContentType = defaultContentType;
    }
}
