package org.dice_research.ldcbench.benchmark.dns;

import java.io.IOException;

import org.junit.Test;

import junit.framework.Assert;

public class EmptyDomainProviderTest {

    @Test
    public void test() throws IOException {
        final String EXPECTED_DOMAIN = "creativecommons.org";

        String[] domains = EmptyDomainProvider.loadEmptyDomains();

        Assert.assertTrue("List of domains is empty", domains.length > 0);

        for (int i = 0; i < domains.length; ++i) {
            if (EXPECTED_DOMAIN.equals(domains[i])) {
                return;
            }
        }
        Assert.fail("Couldn't find " + EXPECTED_DOMAIN);
    }
}
