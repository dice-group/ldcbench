package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.aayushatharva.brotli4j.decoder.BrotliInputStream;

public class BrotliStreamFactoryTest {
    protected static final int DATA_SIZE = 1000;

    @Test
    public void test() throws IOException {
        BrotliStreamFactory factory = new BrotliStreamFactory();
        byte[] data = ReflectionBasedStreamFactoryTest.generateData();

        // Compress the data
        ByteArrayOutputStream bArrayOStream = new ByteArrayOutputStream(2 * DATA_SIZE);
        try (OutputStream os = factory.createCompressionStream(bArrayOStream)) {
            os.write(data);
        }
        // Read and decompress the data again
        ByteArrayInputStream bArrayIStream = new ByteArrayInputStream(bArrayOStream.toByteArray());

        int length = 0;
        byte redData[] = new byte[2 * DATA_SIZE];
        try (InputStream is = generateInputStream(bArrayIStream)) {
            length = IOUtils.read(is, redData);
        } catch (Exception e) {
            throw new AssertionError("Unexpected Exception.", e);
        }
        redData = Arrays.copyOf(redData, length);
        Assert.assertArrayEquals(data, redData);
    }

    public static InputStream generateInputStream(InputStream is) throws IOException {
        BrotliInputStream brIn = new BrotliInputStream(is);
        return brIn;
    }
}
