/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

/**
 *
 * @author Christian
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

    public long getNextId() throws JMSException {
        connectIdSequence();
        // The server creates the first message so we can just wait without a timeout
        ObjectMessage message = (ObjectMessage) idConsumer.receive();
        Long id = (Long) message.getObject();

        ObjectMessage msg = session.createObjectMessage(Long.valueOf(id + 1));
        idProducer.send(msg);

        return id;
    }
}
