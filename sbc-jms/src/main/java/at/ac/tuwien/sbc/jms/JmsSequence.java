package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

/**
 * A wrapper around a JMS queue that acts as a sequence. This implementation assumes that there already exists an {@link ObjectMessage} containing a {@link Long}.
 */
public class JmsSequence extends AbstractJmsComponent {

    private final String queueName;
    private Queue idQueue;
    private MessageProducer idProducer;
    private MessageConsumer idConsumer;

    public JmsSequence(Session session, String queueName) {
        super(session);
        this.queueName = queueName;
    }

    private void connectIdSequence() throws JMSException {
        idQueue = createQueueIfNull(idQueue, queueName);
        idConsumer = createConsumerIfNull(idConsumer, idQueue);
        idProducer = createProducerIfNull(idProducer, idQueue);
    }

    /**
     * Returns the next value created by this sequence.
     *
     * @return the next value created by this sequence
     * @throws JMSException
     */
    public long getNextId() throws JMSException {
        connectIdSequence();
        // The server creates the first message so we can just wait without a timeout
        ObjectMessage message = (ObjectMessage) idConsumer.receive();
        Long id = (Long) message.getObject();

        // Push back the next value of the sequence
        ObjectMessage msg = session.createObjectMessage(Long.valueOf(id + 1));
        idProducer.send(msg);

        return id;
    }
}
