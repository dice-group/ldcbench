package org.dice_research.ldcbench.utils.tar;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.dice_research.ldcbench.utils.CloseableHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TarGZBasedTTLModelIterator implements Iterator<Model>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TarGZBasedTTLModelIterator.class);

    protected TarArchiveInputStream tarStream;
    protected Model model = null;

    public TarGZBasedTTLModelIterator(TarArchiveInputStream tarStream) {
        this.tarStream = tarStream;
    }

    @Override
    public synchronized boolean hasNext() {
        if (model == null) {
            readModel();
        }
        return model != null;
    }

    @Override
    public synchronized Model next() {
        hasNext();
        Model result = model;
        model = null;
        return result;
    }

    private void readModel() {
        try {
            int readSize = 0;
            TarArchiveEntry entry = tarStream.getNextTarEntry();
            while (entry != null) {
                if (entry.isFile()) {
                    int size = (int) entry.getSize();
                    byte buffer[] = new byte[size];
                    readSize = 0;
                    while(readSize < size) {
                        readSize  += tarStream.read(buffer, readSize, size);
                    }
                    Model localModel = ModelFactory.createDefaultModel();
                    try (InputStream is = new ByteArrayInputStream(buffer, 0, readSize)) {
                        localModel.read(is, "", "TTL");
                    } catch (Exception e) {
                        System.err.println(new String(buffer));
                        throw new IOException("Error while reading entry \"" + entry.getName() + "\".", e);
                    }
                    model = localModel;
                    return;
                }
                // read the next entry
                entry = tarStream.getNextTarEntry();
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Exception while reading tar file. This is an unexpected error which causes a runtime exception.",
                    e);
            throw new IllegalStateException("Exception while reading tar file. This is an unexpected error.", e);
        }
    }

    @Override
    public void close() {
        CloseableHelper.closeQuietly(tarStream);
    }

    public static TarGZBasedTTLModelIterator create(File f) throws FileNotFoundException, IOException {
        return new TarGZBasedTTLModelIterator(
                new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))));
    }
}
