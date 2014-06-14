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
import javax.jms.Topic;

/**
 * A connector implementation to communicate with the stock of a distributor.
 */
public class JmsDistributorStockConnector extends AbstractJmsComponent {

    private Queue distributorStockQueue;
    private Topic distributorStockTopic;

    private MessageProducer distributorStockQueueProducer;
    private MessageProducer distributorStockTopicProducer;

    public JmsDistributorStockConnector(URI distributorUri, String destinationName) throws JMSException {
        super(distributorUri.getHost(), distributorUri.getPort());
        distributorStockQueue = createQueueIfNull(distributorStockQueue, JmsConstants.DISTRIBUTOR_STOCK_QUEUE_PREFIX
                                                  + destinationName);
        distributorStockTopic = createTopicIfNull(distributorStockTopic, JmsConstants.DISTRIBUTOR_STOCK_TOPIC_PREFIX
                                                  + destinationName);
        distributorStockQueueProducer = createProducerIfNull(distributorStockQueueProducer, distributorStockQueue);
        distributorStockTopicProducer = createProducerIfNull(distributorStockTopicProducer, distributorStockTopic);
    }

    /**
     * Returns the current stock of the distributor.
     *
     * @return the current stock of the distributor
     * @throws JMSException
     */
    public Map<ClockType, Integer> getDistributorStock() throws JMSException {
        List<Clock> clocks = queueAsList(distributorStockQueue.getQueueName());
        Map<ClockType, Integer> stock = new EnumMap<ClockType, Integer>(ClockType.class);

        // Initialize the map with 0 values for each type
        for (ClockType t : ClockType.values()) {
            stock.put(t, 0);
        }

        // Increment the counter for every clock type
        for (Clock c : clocks) {
            stock.put(c.getType(), stock.get(c.getType()) + 1);
        }

        return stock;
    }

    /**
     * Removes the given clock from the distributor stock.
     *
     * @param removedClock the clock to be removed
     */
    public void removeClockFromStock(final Clock removedClock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                // The stock must already be connected
                // connectDistributorStock(distributorId);
                MessageConsumer distributorStockQueueConsumer = null;

                try {
                    // Consume the clock by id
                    distributorStockQueueConsumer = session.createConsumer(distributorStockQueue, JmsConstants.CLOCK_ID + "="
                                                                           + removedClock
                                                                           .getSerialId());
                    // This is like a take
                    distributorStockQueueConsumer.receive();
                } finally {
                    if (distributorStockQueueConsumer != null) {
                        distributorStockQueueConsumer.close();
                    }
                }
            }
        });
    }

    /**
     * Delivers the given clock to the distributor stock.
     *
     * @param clock the clock to be delivered.
     * @throws JMSException
     */
    public void deliver(final Clock clock) throws JMSException {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                ObjectMessage msg = session.createObjectMessage(clock);
                // We need this so we can remove it by id later
                msg.setLongProperty(JmsConstants.CLOCK_ID, clock.getSerialId());
                distributorStockQueueProducer.send(msg);
                distributorStockTopicProducer.send(msg);
            }
        });
    }

    /**
     * Registers a listener for clock updates in the distributor stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        return subscribeListener(new JmsClockListener(listener), distributorStockTopic);
    }
}
