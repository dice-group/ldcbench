package org.dice_research.ldcbench.nodes.http.simple;

import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Collectors;

import org.simpleframework.http.Response;

import com.google.common.net.MediaType;

/**
 * A resource which provides robots.txt file
 * as described at https://en.wikipedia.org/wiki/Robots_exclusion_standard
 */
public class RobotsResource extends StringResource {
    private static String getContent(Set<String> paths, int crawlDelay) {
        StringBuilder s = new StringBuilder();
        if (crawlDelay != 0) {
            s.append("Crawl-delay: " + crawlDelay + "\n");
        }
        s.append(paths.stream().map(p -> "Disallow: " + p).collect(Collectors.joining("\n")));

        return s.length() != 0 ? "User-agent: *\n" + s.toString() : null;
    }

    public RobotsResource(Set<String> paths, int crawlDelay) {
        super(r -> r.getPath().toString().equals("/robots.txt"), getContent(paths, crawlDelay));
    }

    @Override
    protected boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException {
        return content != null ? super.handleRequest(target, responseType, response, out) : false;
    }
}
