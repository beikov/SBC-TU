/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 * @author Christian
 */
public class JmsDistributorStockConnector extends AbstractJmsComponent {

    private Queue distributorStockQueue;
    private Topic distributorStockTopic;

    private MessageProducer distributorStockQueueProducer;
    private MessageProducer distributorStockTopicProducer;
    private MessageConsumer distributorStockTopicConsumer;

    public JmsDistributorStockConnector(URI distributorUri, String destinationName) throws JMSException {
        super(distributorUri.getHost(), distributorUri.getPort());
        distributorStockQueue = createQueueIfNull(distributorStockQueue, JmsConstants.DISTRIBUTOR_STOCK_QUEUE_PREFIX
                                                  + destinationName);
        distributorStockTopic = createTopicIfNull(distributorStockTopic, JmsConstants.DISTRIBUTOR_STOCK_TOPIC_PREFIX
                                                  + destinationName);
        distributorStockQueueProducer = createProducerIfNull(distributorStockQueueProducer, distributorStockQueue);
        distributorStockTopicProducer = createProducerIfNull(distributorStockTopicProducer, distributorStockTopic);
    }

    public Map<ClockType, Integer> getDistributorStock() throws JMSException {
        List<Clock> clocks = queueAsList(distributorStockQueue.getQueueName());
        Map<ClockType, Integer> stock = new EnumMap<ClockType, Integer>(ClockType.class);

        for (ClockType t : ClockType.values()) {
            stock.put(t, 0);
        }

        for (Clock c : clocks) {
            stock.put(c.getType(), stock.get(c.getType()) + 1);
        }

        return stock;
    }

    public void removeClockFromStock(final Clock removedClock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                // The stock must already be connected
                // connectDistributorStock(distributorId);
                MessageConsumer distributorStockQueueConsumer = null;

                try {
                    distributorStockQueueConsumer = session.createConsumer(distributorStockQueue, "id=" + removedClock
                                                                         .getSerialId());
                    distributorStockQueueConsumer.receive();
                } finally {
                    if (distributorStockQueueConsumer != null) {
                        distributorStockQueueConsumer.close();
                    }
                }
            }
        });
    }

    public void deliver(final Clock clock) throws JMSException {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                ObjectMessage msg = session.createObjectMessage(clock);
                msg.setLongProperty("id", clock.getSerialId());
                distributorStockQueueProducer.send(msg);
                distributorStockTopicProducer.send(msg);
            }
        });
    }

    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        boolean close = true;
        Session s = null;

        try {
            s = connectDistributorListener();
            distributorStockTopicConsumer.setMessageListener(new JmsClockListener(listener));
            close = false;
            return new JmsSubscription(s);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (close && s != null) {
                try {
                    s.close();
                } catch (JMSException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private Session connectDistributorListener() throws JMSException {
        // distributorStockTopic must already be initialized
        Session s = createSession();
        distributorStockTopicConsumer = createConsumerIfNull(s, distributorStockTopicConsumer, distributorStockTopic);
        return s;
    }
}
