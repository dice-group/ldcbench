package org.dice_research.ldcbench.benchmark.dns;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 * This class provides a list of domain names that will be forwarded to an empty
 * server. So in practice, it blocks the domains.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class EmptyDomainProvider {

    private static final String BLOCKED_DOMAINS_FILE_NAME = "/blockedDomains.txt";

    public static String[] loadEmptyDomains() throws IOException {
        InputStream is = EmptyDomainProvider.class.getResourceAsStream(BLOCKED_DOMAINS_FILE_NAME);
        if(is == null) {
            throw new IOException("Couldn't load resource \"" + BLOCKED_DOMAINS_FILE_NAME + '"');
        }
        return IOUtils.readLines(is, StandardCharsets.UTF_8).toArray(new String[0]);
    }
}
