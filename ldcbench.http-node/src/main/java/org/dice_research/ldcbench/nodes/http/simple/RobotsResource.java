package org.dice_research.ldcbench.nodes.http.simple;

import java.util.stream.Collectors;
import java.util.Set;

/**
 * A resource which provides robots.txt file
 * as described at https://en.wikipedia.org/wiki/Robots_exclusion_standard
 */
public class RobotsResource extends StringResource {
    private static String getContent(Set<String> paths) {
        return "User-agent: *\n" + paths.stream().map(p -> "Disallow: " + p).collect(Collectors.joining("\n"));
    }

    public RobotsResource(Set<String> paths) {
        super(r -> r.getPath().toString().equals("/robots.txt"), getContent(paths));
    }
}
