/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.DistributorConnector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.DistributorDemand;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;

/**
 *
 * @author Christian
 */
public class JmsDistributorConnector extends AbstractJmsComponent implements DistributorConnector {

    private final UUID distributorId;

    private URI distributorUri;
    private JmsDistributorStockConnector stockConnector;

    private Queue clockQueue;
    private MessageConsumer deliveredConsumer;

    // Distributor stuff
    private Queue distributorDemandQueue;
    private MessageProducer distributorDemandQueueProducer;
    private MessageConsumer distributorDemandQueueConsumer;

    public JmsDistributorConnector(UUID distributorId, int serverPort) {
        super(serverPort);
        this.distributorId = distributorId;
    }

    private void connectDistributor() throws JMSException {
        if (distributorUri != null) {
            return;
        }

        // Connect queues and topics
        connectDistributor0();

        // Start distributor server
        distributorUri = JmsServer.startServer(distributorId.toString());
        // The following uses the first ip address it can find as host instead of localhost
        // distributorURI = URI.create("tcp://" + SbcUtils.getLocalIpAddress() + ":" + distributorURI.getPort());

        // Connect stock connector to distributor server
        stockConnector = new JmsDistributorStockConnector(distributorUri, distributorId.toString());

        // Create initial demand
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                Map<ClockType, Integer> demand = new EnumMap<ClockType, Integer>(ClockType.class);
                demand.put(ClockType.KLASSISCH, 0);
                demand.put(ClockType.SPORT, 0);
                demand.put(ClockType.ZEITZONEN_SPORT, 0);

                DistributorDemand distributorDemand = new DistributorDemand(distributorUri, distributorId.toString(), demand);
                ObjectMessage msg = session.createObjectMessage(distributorDemand);
                msg.setStringProperty("id", distributorId.toString());
                distributorDemandQueueProducer.send(msg);
            }
        });
    }

    private void connectDistributor0() throws JMSException {
        distributorDemandQueue = createQueueIfNull(distributorDemandQueue, JmsConstants.DISTRIBUTOR_DEMAND_QUEUE);
        distributorDemandQueueProducer = createProducerIfNull(distributorDemandQueueProducer, distributorDemandQueue);
        distributorDemandQueueConsumer = createConsumerIfNull(distributorDemandQueueConsumer, distributorDemandQueue);

        clockQueue = createQueueIfNull(clockQueue, JmsConstants.CLOCK_QUEUE);
        deliveredConsumer = createConsumerIfNull(deliveredConsumer, clockQueue, JmsConstants.IS_DELIVERED + "=true AND "
                                                 + JmsConstants.IS_ORDERED
                                                 + "=true");
    }

    @Override
    public void setDemand(final Map<ClockType, Integer> demand) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDistributor();
                MessageConsumer consumer = null;

                try {
                    consumer = session.createConsumer(distributorDemandQueue, "id='" + distributorId.toString() + "'");
                    // This is like a take-operation
                    consumer.receive();

                    DistributorDemand distributorDemand = new DistributorDemand(distributorUri, distributorId.toString(), demand);
                    ObjectMessage msg = session.createObjectMessage(distributorDemand);
                    msg.setStringProperty("id", distributorId.toString());
                    distributorDemandQueueProducer.send(msg);
                } finally {
                    if (consumer != null) {
                        consumer.close();
                    }
                }
            }
        });
    }

    @Override
    public void removeClockFromStock(final Clock removedClock) {
        try {
            connectDistributor();
            stockConnector.removeClockFromStock(removedClock);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        try {
            connectDistributor();
            return stockConnector.subscribeForDistributorDeliveries(listener);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }
}
