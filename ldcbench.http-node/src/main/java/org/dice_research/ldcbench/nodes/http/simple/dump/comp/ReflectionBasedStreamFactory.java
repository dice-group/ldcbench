package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.OutputStream;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compression factory which relies on a given compression class and the
 * assumption that this class has a constructor which takes the given, original
 * {@link OutputStream} as only argument.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ReflectionBasedStreamFactory implements CompressionStreamFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionBasedStreamFactory.class);

    /**
     * The class implementing the compression.
     */
    private Constructor<? extends OutputStream> compressionConstructor;
    /**
     * The media type string of the compression.
     */
    private String mediaType;
    /**
     * Typical file name extension for this type of compression (including the '.' character).
     */
    private String fileNameExtension;

    /**
     * Constructor.
     * 
     * @param compressionConstructor
     *            the constructor of the class implementing the compression.
     *            <b>Note</b> that it will be assumed that the constructor takes a
     *            single {@link OutputStream} as only argument.
     * @param mediaType
     *            the media type string of the compression.
     * @param fileNameExtension
     *            the file name extension of this compression (including the '.' character).
     */
    public ReflectionBasedStreamFactory(Constructor<? extends OutputStream> compressionConstructor, String mediaType,
            String fileNameExtension) {
        this.compressionConstructor = compressionConstructor;
        this.mediaType = mediaType;
        this.fileNameExtension = fileNameExtension;
    }

    @Override
    public OutputStream createCompressionStream(OutputStream os) {
        try {
            return compressionConstructor.newInstance(os);
        } catch (Exception e) {
            LOGGER.error("Couldn't create compression instance. Returning null.", e);
            return null;
        }
    }

    @Override
    public String getMediaType() {
        return mediaType;
    }
    
    @Override
    public String getFileNameExtension() {
        return fileNameExtension;
    }

    /**
     * Method for creating an instance of the {@link ReflectionBasedStreamFactory}
     * class by using the given class name of the compression class and its media
     * type string.
     * 
     * @param className
     *            the class name of the compression class that should be used for
     *            compression.
     * @param mediaType
     *            the media type string of the compression.
     * @param fileNameExtension
     *            the file ending of this compression (including the '.' character).
     * @return An instance of the {@link ReflectionBasedStreamFactory} class or
     *         {@code null} if the given class can not be found.
     */
    @SuppressWarnings("unchecked")
    public static ReflectionBasedStreamFactory create(String className, String mediaType, String fileNameExtension) {
        try {
            return create((Class<? extends OutputStream>) Class.forName(className), mediaType, fileNameExtension);
        } catch (ClassNotFoundException e) {
            LOGGER.info("Error while trying to find compression class. Returning null.", e);
            return null;
        }
    }

    /**
     * Method for creating an instance of the {@link ReflectionBasedStreamFactory}
     * class by using the given class name of the compression class and its media
     * type string.
     * 
     * @param compressionClass
     *            the class that should be used for compression.
     * @param mediaType
     *            the media type string of the compression.
     * @param fileNameExtension
     *            the file name extension of this compression (including the '.' character).
     * @return An instance of the {@link ReflectionBasedStreamFactory} class or
     *         {@code null} if the given class can not be found.
     */
    public static ReflectionBasedStreamFactory create(Class<? extends OutputStream> compressionClass, String mediaType,
            String fileNameExtension) {
        try {
            return new ReflectionBasedStreamFactory(
                    (Constructor<? extends OutputStream>) compressionClass.getDeclaredConstructor(OutputStream.class),
                    mediaType, fileNameExtension);
        } catch (Exception e) {
            LOGGER.info("Error while trying to find compression class. Returning null.", e);
            return null;
        }
    }
}
