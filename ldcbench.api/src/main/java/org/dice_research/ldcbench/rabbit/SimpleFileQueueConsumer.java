package org.dice_research.ldcbench.rabbit;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.hobbit.core.rabbit.RabbitQueueFactory;
import org.hobbit.core.rabbit.SimpleFileReceiver;

/**
 * This class implements receiving SimpleFile-s coming through a queue.
 */
public class SimpleFileQueueConsumer implements Closeable {
    private String outputDir = getTempDir();
    private SimpleFileReceiver receiver;
    private Thread thread;

    /**
     * Initializes consuming of messages coming through specified queue.
     *
     * @param queueFactory
     *            factory to create queues with
     * @param queueName
     *            name of the queue
     */
    public SimpleFileQueueConsumer(RabbitQueueFactory queueFactory, String queueName) throws IOException {
        receiver = SimpleFileReceiver.create(queueFactory, queueName);

        thread = new Thread(){
            @Override
            public void run() {
                try {
                    String[] files = receiver.receiveData(outputDir);
                    for (int i = 0; i < files.length; i++) {
                        files[i] = new File(outputDir, files[i]).getAbsolutePath();
                    }
                    handle(files);
                } catch (IOException | InterruptedException e) {
                    handle(null);
                }
            }
        };
        thread.start();
    }

    private static String getTempDir() throws IOException {
        return Files.createTempDirectory("GraphQueueConsumer").toAbsolutePath().toString();
    }

    /**
     * Terminates the receiving and waits until remaining data is processed.
     */
    @Override
    public void close() {
        if (receiver != null) {
            try {
                receiver.terminate();
            } catch (Exception e) {
            }
        }
        if (thread != null) {
            try {
                thread.join();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Abstract method which is called when receiving is terminated.
     *
     * @param files
     *            array of file names received
     */
    public void handle(String[] files) {};
}
