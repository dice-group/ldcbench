package org.dice_research.ldcbench.nodes.http.simple;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

public class FileBasedResource extends AbstractCrawleableResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedResource.class);

    protected Map<String, File> files;

    public FileBasedResource(Map<String, File> files, String contentType) {
        super(r -> files.containsKey(r.getPath().toString()), contentType);
        this.files = files;
    }

    @Override
    protected boolean handleRequest(String target, MediaType responseType, Response response, OutputStream out)
            throws SimpleHttpException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(files.get(target)))) {
            IOUtils.copy(in, out);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error while writing file to stream.", e);
            throw new SimpleHttpException("Error while writing dump file to stream.", e, Status.INTERNAL_SERVER_ERROR);
        }
    }

}
