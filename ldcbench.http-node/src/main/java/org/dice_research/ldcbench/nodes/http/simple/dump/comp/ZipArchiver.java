package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipArchiver extends AbstractArchiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipArchiver.class);

    private static final String MEDIA_TYPE = "application/zip";
    private static final String FILE_NAME_EXTENSION = ".zip";

    /**
     * Constructor.
     */
    public ZipArchiver() {
        super(MEDIA_TYPE, FILE_NAME_EXTENSION);
    }

    @Override
    public ZipArchiveOutputStream createStream(File archive) {
		try {
			return new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(archive)));
		} catch (Exception e) {
			LOGGER.error("Couldn't create Archive Instance. Returning null. ", e);
			return null;
		}
	}
}
