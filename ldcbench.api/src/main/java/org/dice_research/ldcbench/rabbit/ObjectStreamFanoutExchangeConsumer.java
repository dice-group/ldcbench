package org.dice_research.ldcbench.rabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.hobbit.core.rabbit.RabbitQueueFactory;

/**
 * This class implements consuming of Serializable object from messages coming through fanout exchange.
 */
public class ObjectStreamFanoutExchangeConsumer<T> extends FanoutExchangeConsumer {
    /**
     * Initializes consuming of messages coming through specified exchange.
     *
     * @param queueFactory
     *            factory to create queues with
     * @param exchangeName
     *            name of the exchange
     */
    public ObjectStreamFanoutExchangeConsumer(RabbitQueueFactory queueFactory, String exchangeName) throws IOException {
        super(queueFactory, exchangeName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void handle(byte[] body) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
            Object object = ois.readObject();
            handle((T) object); // FIXME
        } catch (Exception e) {
            handle((T) null);
        }
    }

    /**
     * Abstract method which is called when a message is received.
     *
     * @param object
     *            message body as an object
     */
    public void handle(T object) {};
}
