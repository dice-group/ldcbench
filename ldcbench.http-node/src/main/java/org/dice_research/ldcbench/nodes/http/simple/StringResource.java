package org.dice_research.ldcbench.nodes.http.simple;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import com.google.common.net.MediaType;

/**
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class StringResource extends AbstractCrawleableResource implements CrawleableResource {

    private static final String DEFAULT_CONTENT_TYPE = "text/plain";

    protected final String content;

    public StringResource(Predicate<Request> predicate, String content) {
        this(predicate, content, DEFAULT_CONTENT_TYPE);
    }

    public StringResource(Predicate<Request> predicate, String content, String contentType) {
        super(predicate, contentType);
        this.content = content;
    }

    @Override
    protected boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException {
        try {
            out.write(content.getBytes(responseType.charset().or(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            new SimpleHttpException("Error while writing content.", e, Status.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

}
