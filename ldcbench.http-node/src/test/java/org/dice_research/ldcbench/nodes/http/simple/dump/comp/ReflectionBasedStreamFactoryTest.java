package org.dice_research.ldcbench.nodes.http.simple.dump.comp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ReflectionBasedStreamFactoryTest {

    protected static final int DATA_SIZE = 1000;

    protected String compressionClassName;
    protected String decompressionClassName;

    public ReflectionBasedStreamFactoryTest(String compressionClassName, String decompressionClassName) {
        this.compressionClassName = compressionClassName;
        this.decompressionClassName = decompressionClassName;
    }

    @Parameters
    public static List<Object[]> testCases() {
        List<Object[]> data = new ArrayList<>();

        data.add(new Object[] { "java.util.zip.GZIPOutputStream", "java.util.zip.GZIPInputStream" });
        data.add(new Object[] { "org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream",
                "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream" });
        return data;
    }

    @Test
    public void test() throws IOException {
        CompressionStreamFactory factory = ReflectionBasedStreamFactory.create(compressionClassName, null, null);
        byte[] data = generateData();

        // Compress the data
        ByteArrayOutputStream bArrayOStream = new ByteArrayOutputStream(2 * DATA_SIZE);
        try (OutputStream os = factory.createCompressionStream(bArrayOStream)) {
            os.write(data);
        }
        // Read and decompress the data again
        ByteArrayInputStream bArrayIStream = new ByteArrayInputStream(bArrayOStream.toByteArray());

        int length = 0;
        byte redData[] = new byte[2 * DATA_SIZE];
        try (InputStream is = generateInputStream(bArrayIStream, decompressionClassName)) {
            length = IOUtils.read(is, redData);
        } catch (Exception e) {
            throw new AssertionError("Unexpected Exception.", e);
        }
        redData = Arrays.copyOf(redData, length);
        Assert.assertArrayEquals(data, redData);
    }

    protected static byte[] generateData() {
        long seed = System.currentTimeMillis();
        System.out.println("Using " + seed + " as seed to generate data.");
        Random random = new Random(seed);
        byte[] data = new byte[DATA_SIZE];
        random.nextBytes(data);
        return data;
    }

    @SuppressWarnings("unchecked")
    public static InputStream generateInputStream(InputStream is, String decompressionClassName)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<? extends InputStream> clazz = (Class<? extends InputStream>) Class.forName(decompressionClassName);
        Constructor<? extends InputStream> constructor = (Constructor<? extends InputStream>) clazz
                .getDeclaredConstructor(InputStream.class);
        return constructor.newInstance(is);
    }
}
