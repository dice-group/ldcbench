package org.dice_research.ldcbench.nodes.http.simple;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.simpleframework.http.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

public abstract class AbstractNegotiatingResource extends AbstractCrawleableResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNegotiatingResource.class);

    protected Set<MediaType> availableContentTypes;

    public AbstractNegotiatingResource(Predicate<Request> predicate, String[] contentTypes) {
        super(predicate);
        this.availableContentTypes = Arrays.stream(contentTypes).map(c -> MediaType.parse(c))
                .collect(Collectors.toSet());
    }

    protected MediaType getResponseType(Iterator<String> iterator) {
        // If the request header is empty
        if (!iterator.hasNext()) {
            if (availableContentTypes.size() > 0) {
                return availableContentTypes.stream().findFirst().get();
            } else {
                return null;
            }
        }
        String typeString;
        MediaType requestedType;
        while (iterator.hasNext()) {
            typeString = iterator.next();
            try {
                // A single star is not allowed in the MediaType class that we use
                if ("*".equals(typeString)) {
                    typeString = "*/*";
                }
                requestedType = MediaType.parse(typeString);
                for (MediaType availableType : availableContentTypes) {
                    if (availableType.is(requestedType)) {
                        return availableType;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Couldn't parse requested media type \"{}\".", typeString);
            }
        }
        return null;
    }

    @Deprecated
    protected String negotiate(Set<String> availableValues, Iterator<String> requestedValues, String defaultValue) {
        String result = null;
        if (availableValues.size() > 0) {
            // Search for a matching content type
            while ((result == null) && requestedValues.hasNext()) {
                result = requestedValues.next();
                if (!availableValues.contains(result)) {
                    result = null;
                }
            }
        } else {
            // If this crawleable resource has no content type restriction, take the first
            // one of the request
            if (requestedValues.hasNext()) {
                result = requestedValues.next();
            } else {
                result = defaultValue;
            }
        }
        return result;
    }

}
