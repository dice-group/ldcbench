package org.dice_research.ldcbench.nodes.http.simple;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

/**
 * Detects when a disallowed path (from a provided set) is requested.
 */
public class DisallowedResource extends AbstractCrawleableResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpServerComponent.class);

    private Set<String> paths = new HashSet<>();
    private Set<String> requestedPaths = new HashSet<>();

    public DisallowedResource(Set<String> paths) {
        super(r -> paths.contains(r.getPath().toString()));
        this.paths = paths;
    }

    public int getTotalAmount() {
        return paths.size();
    }

    public int getRequestedAmount() {
        return requestedPaths.size();
    }

    @Override
    protected boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException {
        String path = target.replaceFirst("\\?.*$", "");
        if (!paths.contains(path)) {
            LOGGER.error("Target {} is not in a set of disallowed resources.", target);
            throw new IllegalStateException();
        }
        if (path.equals(target)) {
            LOGGER.info("Disallowed resource requested: {}", path);
        } else {
            LOGGER.info("Disallowed resource requested: {} ({})", path, target);
        }

        requestedPaths.add(path);

        try {
            response.setStatus(Status.NO_CONTENT);
        } catch (Exception e) {
            new SimpleHttpException("Error while handling request.", e, Status.INTERNAL_SERVER_ERROR);
        }
        return true;
    }
}
