package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;

/**
 * Compression factory which relies on a the Brotli Compression.
 *
 */
public class BrotliStreamFactory implements CompressionStreamFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrotliStreamFactory.class);

    private static final String MEDIA_TYPE = "application/br";
    private static final String FILE_NAME_EXTENSION = ".br";

    /**
     * Constructor
     *
     * Ensure Brotli native library is available.
     */
    public BrotliStreamFactory() {
        Brotli4jLoader.ensureAvailability();
    }

    @Override
    public OutputStream createCompressionStream(OutputStream os) {
        try {
            return new BrotliOutputStream(os);
        } catch (IOException e) {
            LOGGER.error("Couldn't create compression instance. Returning null.", e);
            return null;
        }
    }

    @Override
    public String getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileNameExtension() {
        return FILE_NAME_EXTENSION;
    }

}
