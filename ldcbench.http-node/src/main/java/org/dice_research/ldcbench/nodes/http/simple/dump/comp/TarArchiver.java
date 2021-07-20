package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TarArchiver extends AbstractArchiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TarArchiver.class);

    private static final String TAR_MEDIA_TYPE = "application/x-tar";
    private static final String TAR_FILE_NAME_EXTENSION = ".tar";
    private CompressionStreamFactory compressionFactory = null;

    /**
     * Create a simple Tar Archiver
     */
    public TarArchiver() {
        super(TAR_MEDIA_TYPE, TAR_FILE_NAME_EXTENSION);
    }

    /**
     * create an Archiver wrapped with compression (Tar.gz for example)
     *
     * @param compressionFactory
     */
    public TarArchiver(CompressionStreamFactory compressionFactory) {
        super(TAR_MEDIA_TYPE.concat(compressionFactory.getMediaType().substring(
                compressionFactory.getMediaType().indexOf("/")+1)),
                TAR_FILE_NAME_EXTENSION.concat(compressionFactory.getFileNameExtension()));
        this.compressionFactory = compressionFactory;
    }

    @Override
    public TarArchiveOutputStream createStream(File archive) {
		try {
			return new TarArchiveOutputStream(
					this.compressionFactory != null ? compressionFactory.createCompressionStream(new BufferedOutputStream(new FileOutputStream(archive)))
							: new BufferedOutputStream(new FileOutputStream(archive)));
		} catch (Exception e) {
			LOGGER.error("Couldn't create Archive Instance. Returning null. ", e);
			return null;
		}
	}

}
