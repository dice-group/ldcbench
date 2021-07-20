package org.dice_research.ldcbench.utils;

import java.io.Closeable;

import org.slf4j.Logger;

/**
 * A simple helper class that is used to close objects quietly.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class CloseableHelper {

    /**
     * Closes the given object quietly, i.e., no exception is thrown.
     * 
     * @param closeable the object that should be closed
     */
    public static void closeQuietly(Closeable closeable) {
        closeQuietly(closeable, null);
    }

    /**
     * Closes the given object quietly, i.e., no exception is thrown. However, in
     * case of an error, the error is logged by the given logger (with the INFO log
     * level).
     * 
     * @param closeable the object that should be closed
     * @param logger    the logger that is used to log exceptions that might be
     *                  thrown
     */
    public static void closeQuietly(Closeable closeable, Logger logger) {
        try {
            closeable.close();
        } catch (Throwable e) {
            if (logger != null) {
                logger.info("Catched exception while closing object.", e);
            }
        }
    }
}
