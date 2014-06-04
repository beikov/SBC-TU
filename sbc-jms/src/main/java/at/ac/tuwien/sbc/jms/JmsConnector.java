/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.Demand;
import at.ac.tuwien.sbc.model.DistributorDemand;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 *
 * @author Christian
 */
public class JmsConnector extends AbstractJmsComponent implements Connector {

    // Package private so that JmsServer can also see it
    static final String ID_QUEUE = "queue/id";

    private static final long MAX_TIMEOUT_MILLIS = 2000;
    private static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;

    private static final String CLOCKPART_TOPIC = "topic/clockpart";
    private static final String CLOCK_TOPIC = "topic/clock";

    private static final String CLOCKPART_QUEUE = "queue/clockpart";
    private static final String CLOCK_QUEUE = "queue/clock";

    private static final String ORDER_QUEUE = "queue/order";
    private static final String ORDER_TOPIC = "topic/order";
    private static final String SINGLE_CLOCK_ORDER_QUEUE = "queue/singleclockorder";

    private static final String DISTRIBUTOR_DEMAND_QUEUE = "queue/distributordemand";
    private static final String DISTRIBUTOR_STOCK_QUEUE_PREFIX = "queue/distributorstock-";

    private static final String IS_ASSEMBLED = "IS_ASSEMBLED";
    private static final String IS_DELIVERED = "IS_DELIVERED";
    private static final String IS_DISASSEMBLED = "IS_DISASSEMBLED";
    private static final String IS_ORDERED = "IS_ORDERED";

    // Clock stuff
    private Topic clockPartTopic;
    private Topic clockTopic;
    private Queue clockPartQueue;
    private Queue clockQueue;
    private MessageProducer clockPartQueueProducer;
    private MessageProducer clockQueueProducer;
    private MessageProducer clockPartTopicProducer;
    private MessageProducer clockTopicProducer;
    private MessageConsumer assembledConsumer;
    private MessageConsumer deliveredConsumer;
    private MessageConsumer assembledTopicConsumer;
    private MessageConsumer clockPartTopicConsumer;
    private MessageConsumer clockTopicConsumer;
    private final Map<ClockPartType, MessageConsumer> partTypeConsumers = new EnumMap<ClockPartType, MessageConsumer>(
        ClockPartType.class);
    private final Map<ClockQualityType, MessageConsumer> clockQualityConsumers = new EnumMap<ClockQualityType, MessageConsumer>(
        ClockQualityType.class);

    // Order stuff
    private Queue orderQueue;
    private Queue singleClockOrderQueue;
    private Topic orderTopic;
    private MessageProducer orderQueueProducer;
    private MessageProducer singleClockOrderQueueProducer;
    private MessageConsumer orderTopicConsumer;
    private final Map<OrderPriority, MessageConsumer> orderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(
        OrderPriority.class);
    private final Map<OrderPriority, MessageConsumer> singleClockOrderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(
        OrderPriority.class);

    // Distributor stuff
    private Queue distributorDemandQueue;
    private MessageProducer distributorDemandQueueProducer;
    private MessageConsumer distributorDemandQueueConsumer;

    private Queue distributorStockQueue;
    private Topic distributorStockTopic;
    private MessageConsumer distributorStockTopicConsumer;

    private JmsSequence idSequence;

    public JmsConnector(int port) {
        super(port);
    }

    @Override
    public void addOrder(final Order order) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectOrder();

                for (ClockType type : ClockType.values()) {
                    for (int i = 0; i < order.getNeededClocksOfType(type); i++) {
                        ObjectMessage msg = session.createObjectMessage(new SingleClockOrder(order.getId(), type, order
                                                                                             .getPriority()));
                        msg.setStringProperty("type", type.name());
                        msg.setStringProperty("priority", order.getPriority()
                                              .name());
                        singleClockOrderQueueProducer.send(msg);
                    }
                }

                ObjectMessage msg = session.createObjectMessage(order);
                msg.setStringProperty("priority", order.getPriority()
                                      .name());
                orderQueueProducer.send(msg);
            }
        });
    }

    @Override
    public boolean takeSingleClockOrder(final OrderPriority priority, final TransactionalTask<SingleClockOrder> transactionalTask) {
        final Boolean[] done = { false };
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectOrder();
                ObjectMessage message = (ObjectMessage) singleClockOrderPriorityConsumers.get(priority)
                    .receiveNoWait();
                if (message != null) {
                    transactionalTask.doWork((SingleClockOrder) message.getObject());
                    done[0] = true;
                }
            }
        });

        return done[0];
    }

    @Override
    public void connectDistributor(final UUID distributorId) {
        Map<ClockType, Integer> demand = new EnumMap<ClockType, Integer>(ClockType.class);
        demand.put(ClockType.KLASSISCH, 0);
        demand.put(ClockType.SPORT, 0);
        demand.put(ClockType.ZEITZONEN_SPORT, 0);

        final DistributorDemand distributorDemand = new DistributorDemand(distributorId, demand);

        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDistributor();
                connectDistributorStock(distributorId);
                ObjectMessage msg = session.createObjectMessage(distributorDemand);
                msg.setStringProperty("id", distributorId.toString());
                distributorDemandQueueProducer.send(msg);
            }
        });
    }

    @Override
    public void setDemand(final UUID distributorId, Map<ClockType, Integer> demand) {
        final DistributorDemand distributorDemand = new DistributorDemand(distributorId, demand);

        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDistributor();
                MessageConsumer consumer = null;

                try {
                    consumer = session.createConsumer(distributorDemandQueue, "id='" + distributorId.toString() + "'");
                    // This is like a take-operation
                    consumer.receive();

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

    private Map<ClockType, Integer> getDistributorStock(UUID distributorId) throws JMSException {
        List<Clock> clocks = queueAsList(DISTRIBUTOR_STOCK_QUEUE_PREFIX + distributorId.toString());
        Map<ClockType, Integer> stock = new EnumMap<ClockType, Integer>(ClockType.class);

        for (ClockType t : ClockType.values()) {
            stock.put(t, 0);
        }

        for (Clock c : clocks) {
            stock.put(c.getType(), stock.get(c.getType()) + 1);
        }

        return stock;
    }

    private Clock takeDeliveredClockOfNoOrder(ClockType type) throws JMSException {
        ObjectMessage msg = (ObjectMessage) deliveredConsumer.receive(MAX_TIMEOUT_MILLIS);

        if (msg == null) {
            return null;
        }

        return (Clock) msg.getObject();
    }

    @Override
    public void takeDemandedClock(final TransactionalTask<Map<Demand, Clock>> transactionalTask) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDistributor();
                ObjectMessage msg = (ObjectMessage) distributorDemandQueueConsumer.receive();
                DistributorDemand distributorDemand = (DistributorDemand) msg.getObject();

                Map<ClockType, Integer> demandedClocks = distributorDemand.getNeededClocksPerType();
                Map<ClockType, Integer> stockCount = getDistributorStock(distributorDemand.getDistributorId());

                for (ClockType type : demandedClocks.keySet()) {
                    if (stockCount.get(type) < demandedClocks.get(type)) {
                        Clock clock = takeDeliveredClockOfNoOrder(type);

                        if (clock != null) {
                            Demand demand = new Demand(distributorDemand.getDistributorId(), type);
                            Map<Demand, Clock> param = new HashMap<Demand, Clock>();
                            param.put(demand, clock);
                            transactionalTask.doWork(param);
                            break;
                        }
                    }

                }

                msg = session.createObjectMessage(distributorDemand);
                distributorDemandQueueProducer.send(msg);
            }
        });
    }

    @Override
    public void removeClockFromStock(final Clock removedClock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                // The stock must already be connected
                // connectDistributorStock(distributorId);
                MessageConsumer distributorStockQueueConsumer = null;

                try {
                    distributorStockQueueConsumer = createConsumerIfNull(null, distributorStockQueue, "id=" + removedClock
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

    @Override
    public void deliverDemandedClock(final Demand demand, final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                Queue stockQueue = session.createQueue(DISTRIBUTOR_STOCK_QUEUE_PREFIX + demand.getDistributor()
                    .toString());
                MessageProducer producer = null;

                try {
                    producer = session.createProducer(stockQueue);
                    ObjectMessage msg = session.createObjectMessage(clock);
                    msg.setLongProperty("id", clock.getSerialId());
                    producer.send(msg);
                } finally {
                    if (producer != null) {
                        producer.close();
                    }
                }
            }
        });
    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        try {
            connectClockPartListener();
            clockPartTopicConsumer.setMessageListener(new JmsClockPartListener(listener));
            return new JmsSubscription(clockPartTopicConsumer);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForClocks(ClockListener listener) {
        try {
            connectClockListener();
            clockTopicConsumer.setMessageListener(new JmsClockListener(listener));
            return new JmsSubscription(clockTopicConsumer);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForOrders(OrderListener listener) {
        try {
            connectOrderListener();
            JmsOrderListener l = new JmsOrderListener(listener);
            assembledTopicConsumer.setMessageListener(l);
            orderTopicConsumer.setMessageListener(l);
            return new JmsSubscription(assembledTopicConsumer, orderTopicConsumer);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        try {
            // The stock must already be connected
            // connectDistributorStock(distributorId);
            distributorStockTopicConsumer.setMessageListener(new JmsClockListener(listener));
            return new JmsSubscription(distributorStockTopicConsumer);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void connectClockPartListener() throws JMSException {
        clockPartTopic = createTopicIfNull(clockPartTopic, CLOCKPART_TOPIC);
        clockPartTopicConsumer = createConsumerIfNull(clockPartTopicConsumer, clockPartTopic);
    }

    private void connectClockListener() throws JMSException {
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        clockTopicConsumer = createConsumerIfNull(clockTopicConsumer, clockTopic);
    }

    private void connectSupplier() throws JMSException {
        clockPartQueue = createQueueIfNull(clockPartQueue, CLOCKPART_QUEUE);
        clockPartQueueProducer = createProducerIfNull(clockPartQueueProducer, clockPartQueue);

        clockPartTopic = createTopicIfNull(clockPartTopic, CLOCKPART_TOPIC);
        clockPartTopicProducer = createProducerIfNull(clockPartTopicProducer, clockPartTopic);
    }

    private void connectAssembler() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        clockPartQueue = createQueueIfNull(clockPartQueue, CLOCKPART_QUEUE);
        clockPartTopic = createTopicIfNull(clockPartTopic, CLOCKPART_TOPIC);

        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);
        clockPartTopicProducer = createProducerIfNull(clockPartTopicProducer, clockPartTopic);

        if (partTypeConsumers.isEmpty()) {
            for (ClockPartType type : ClockPartType.values()) {
                partTypeConsumers.put(type, session.createConsumer(clockPartQueue, "type='" + type.name() + "'"));
            }
        }
    }

    private void connectChecker() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);

        assembledConsumer = createConsumerIfNull(assembledConsumer, clockQueue, IS_ASSEMBLED + " = true");
    }

    private void connectDeliverer() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);

        if (clockQualityConsumers.isEmpty()) {
            for (ClockQualityType type : ClockQualityType.values()) {
                clockQualityConsumers.put(type, session.createConsumer(clockQueue, "quality='" + type.name() + "'"));
            }
        }
    }

    private void connectOrder() throws JMSException {
        orderQueue = createQueueIfNull(orderQueue, ORDER_QUEUE);
        singleClockOrderQueue = createQueueIfNull(singleClockOrderQueue, SINGLE_CLOCK_ORDER_QUEUE);

        orderQueueProducer = createProducerIfNull(orderQueueProducer, orderQueue);
        singleClockOrderQueueProducer = createProducerIfNull(singleClockOrderQueueProducer, singleClockOrderQueue);

        if (orderPriorityConsumers.isEmpty()) {
            for (OrderPriority priority : OrderPriority.values()) {
                orderPriorityConsumers.put(priority, session.createConsumer(orderQueue, "priority='" + priority.name() + "'"));
            }
        }
        if (singleClockOrderPriorityConsumers.isEmpty()) {
            for (OrderPriority priority : OrderPriority.values()) {
                singleClockOrderPriorityConsumers.put(priority, session.createConsumer(orderQueue, "priority='" + priority
                                                                                       .name() + "'"));
            }
        }
    }

    private void connectOrderListener() throws JMSException {
        orderTopic = createTopicIfNull(orderTopic, ORDER_TOPIC);
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        assembledTopicConsumer = createConsumerIfNull(assembledTopicConsumer, clockTopic, IS_ASSEMBLED + "=true");
        orderTopicConsumer = createConsumerIfNull(orderTopicConsumer, orderTopic);
    }

    private void connectDistributor() throws JMSException {
        distributorDemandQueue = createQueueIfNull(distributorDemandQueue, DISTRIBUTOR_DEMAND_QUEUE);
        distributorDemandQueueProducer = createProducerIfNull(distributorDemandQueueProducer, distributorDemandQueue);
        distributorDemandQueueConsumer = createConsumerIfNull(distributorDemandQueueConsumer, distributorDemandQueue);

        clockQueue = createQueueIfNull(clockQueue, CLOCK_QUEUE);
        deliveredConsumer = createConsumerIfNull(deliveredConsumer, clockQueue, IS_DELIVERED + "=true AND " + IS_ORDERED
                                                 + "=true");
    }

    private void connectDistributorStock(UUID distributorId) throws JMSException {
        distributorStockQueue = createQueueIfNull(distributorStockQueue, DISTRIBUTOR_STOCK_QUEUE_PREFIX + distributorId
                                                  .toString());
        distributorStockTopic = createTopicIfNull(distributorStockTopic, DISTRIBUTOR_STOCK_QUEUE_PREFIX + distributorId
                                                  .toString());

        distributorStockTopicConsumer = createConsumerIfNull(distributorStockTopicConsumer, distributorStockTopic);
    }

    @Override
    public void addParts(final List<ClockPart> parts) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectSupplier();
                for (ClockPart cp : parts) {
                    ObjectMessage message = session.createObjectMessage(cp);
                    message.setStringProperty("type", cp.getType()
                                              .name());
                    clockPartQueueProducer.send(message);
                    clockPartTopicProducer.send(message);
                }
            }
        });
    }

    @Override
    public void takeParts(final Map<ClockPartType, Integer> neededClockParts, final TransactionalTask<List<ClockPart>> transactionalTask) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectAssembler();
                List<ClockPart> parts = new ArrayList<ClockPart>();
                for (ClockPartType t : neededClockParts.keySet()) {
                    ObjectMessage message = null;
                    for (int i = 0, upper = neededClockParts.get(t); i < upper; i++) {
                        message = (ObjectMessage) partTypeConsumers.get(t)
                            .receive(MAX_TIMEOUT_MILLIS);

                        if (message == null) {
                            throw new TimeoutException();
                        }

                        ClockPart part = (ClockPart) message.getObject();
                        parts.add(part);
                        ObjectMessage msg = session.createObjectMessage(part);
                        msg.setBooleanProperty("IS_REMOVED", true);
                        clockPartTopicProducer.send(msg);

                    }
                }
                transactionalTask.doWork(parts);
            }
        });
    }

    @Override
    public void takeAssembled(final TransactionalTask<Clock> transactionalTask) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectChecker();
                ObjectMessage message = (ObjectMessage) assembledConsumer.receive();
                transactionalTask.doWork((Clock) message.getObject());
            }
        });

    }

    @Override
    public boolean takeChecked(final ClockQualityType type, final long timeout, final TransactionalTask<Clock> transactionalTask) {
        return tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDeliverer();
                MessageConsumer consumer = clockQualityConsumers.get(type);
                ObjectMessage message = (ObjectMessage) consumer.receive(timeout);
                if (message == null) {
                    throw new TimeoutException();
                }
                transactionalTask.doWork((Clock) message.getObject());
            }
        });
    }

    @Override
    public void addAssembledClock(final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectAssembler();
                idSequence = idSequence != null ? idSequence : new JmsSequence(session, ID_QUEUE);
                clock.setSerialId(idSequence.getNextId());
                ObjectMessage msg = session.createObjectMessage(clock);
                msg.setBooleanProperty(IS_ASSEMBLED, true);
                clockQueueProducer.send(msg);
                clockTopicProducer.send(msg);
            }
        });

    }

    @Override
    public void addCheckedClock(final Clock clock, final ClockQualityType type) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectChecker();
                ObjectMessage message = session.createObjectMessage(clock);
                message.setStringProperty("quality", type.name());
                clockQueueProducer.send(message);
                clockTopicProducer.send(message);
            }
        });
    }

    @Override
    public void addDeliveredClock(final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDeliverer();
                Message msg = session.createObjectMessage(clock);
                msg.setBooleanProperty(IS_DELIVERED, true);
                msg.setBooleanProperty(IS_ORDERED, clock.getOrderId() != null);
                clockTopicProducer.send(msg);
                clockQueueProducer.send(msg);
            }
        });
    }

    @Override
    public void addDisassembledClock(final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDeliverer();
                Message msg = session.createObjectMessage(clock);
                msg.setBooleanProperty(IS_DISASSEMBLED, true);
                clockTopicProducer.send(msg);
                clockQueueProducer.send(msg);
            }
        });
    }

    @Override
    public List<ClockPart> getClockParts() {
        return queueAsList(CLOCKPART_QUEUE);
    }

    @Override
    public List<Clock> getClocks() {
        return queueAsList(CLOCK_QUEUE);
    }

    @Override
    public List<Order> getOrders() {
        return queueAsList(ORDER_QUEUE);
    }

}
