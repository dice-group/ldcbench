package org.dice_research.ldcbench.nodes.htmlembd.singlefile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.http.simple.AbstractCrawleableResource;
import org.dice_research.ldcbench.nodes.http.simple.SimpleHttpException;
import org.dice_research.ldcbench.nodes.http.simple.dump.comp.CompressionStreamFactory;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class SingleFileResource extends AbstractCrawleableResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleFileResource.class);

    protected final String file;

    public SingleFileResource(Predicate<Request> predicate, String file) {
        super(predicate, "text/html");
        this.file = file;
    }

    @Override
    protected boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            IOUtils.copy(in, out);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error while writing file to stream.", e);
            throw new SimpleHttpException("Error while writing file to stream.", e, Status.INTERNAL_SERVER_ERROR);
        }
    }

}
