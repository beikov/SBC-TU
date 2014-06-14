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
import at.ac.tuwien.sbc.model.ClockStatus;
import at.ac.tuwien.sbc.model.ClockType;
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
 * A simple JMS implementation of the {@link Connector} interface.
 */
public class JmsConnector extends AbstractJmsComponent implements Connector {

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
    private final Map<ClockPartType, MessageConsumer> partTypeConsumers = new EnumMap<ClockPartType, MessageConsumer>(
        ClockPartType.class);
    private final Map<ClockQualityType, MessageConsumer> clockQualityConsumers = new EnumMap<ClockQualityType, MessageConsumer>(
        ClockQualityType.class);

    // Order stuff
    private Queue orderQueue;
    private Queue singleClockOrderQueue;
    private Topic orderTopic;
    private MessageProducer orderQueueProducer;
    private MessageProducer orderTopicProducer;
    private MessageProducer singleClockOrderQueueProducer;
    private final Map<OrderPriority, MessageConsumer> orderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(
        OrderPriority.class);
    private final Map<OrderPriority, Map<String, MessageConsumer>> singleClockOrderPriorityAndTypeConsumers = new EnumMap<OrderPriority, Map<String, MessageConsumer>>(
        OrderPriority.class);

    // Distributor stuff
    private Queue distributorDemandQueue;
    private MessageProducer distributorDemandQueueProducer;
    private MessageConsumer distributorDemandQueueConsumer;

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

                // Add a single clock orders for every clock needed
                for (ClockType type : ClockType.values()) {
                    for (int i = 0; i < order.getNeededClocksOfType(type); i++) {
                        ObjectMessage msg = session.createObjectMessage(new SingleClockOrder(order.getId(), type, order
                                                                                             .getPriority()));
                        msg.setStringProperty(JmsConstants.SINGLE_CLOCK_TYPE, type.name());
                        msg.setStringProperty(JmsConstants.ORDER_PRIORITY, order.getPriority()
                                              .name());
                        singleClockOrderQueueProducer.send(msg);
                    }
                }

                // Add the order
                ObjectMessage msg = session.createObjectMessage(order);
                msg.setStringProperty(JmsConstants.ORDER_PRIORITY, order.getPriority()
                                      .name());
                orderQueueProducer.send(msg);
                orderTopicProducer.send(msg);
            }
        });
    }

    /**
     * A transactional work implementation for {@link JmsConnector#takeSingleClockOrder(at.ac.tuwien.sbc.model.OrderPriority, at.ac.tuwien.sbc.TransactionalTask)} that can be parameterized for better reuse.
     */
    private final class TakeSingleClockOrderWork implements TransactionalWork {

        private final boolean[] done;
        private final boolean[] noneAvailable;
        private final OrderPriority priority;
        private final TransactionalTask<SingleClockOrder> transactionalTask;
        private final String type;

        public TakeSingleClockOrderWork(boolean[] done, boolean[] noneAvailable, OrderPriority priority, TransactionalTask<SingleClockOrder> transactionalTask, String type) {
            this.done = done;
            this.noneAvailable = noneAvailable;
            this.priority = priority;
            this.transactionalTask = transactionalTask;
            this.type = type;
        }

        @Override
        public void doWork() throws JMSException {
            connectOrder();
            ObjectMessage message = (ObjectMessage) singleClockOrderPriorityAndTypeConsumers.get(priority)
                .get(type)
                .receiveNoWait();
            if (message != null) {
                // The task will probably throw a TimeoutException if it wants to cancel the transaction
                transactionalTask.doWork((SingleClockOrder) message.getObject());
                // Still we also check the rollback only flag
                if (!tm.isRollbackOnly()) {
                    done[0] = true;
                }
            } else {
                // there is no single clock order of any type for this priority
                noneAvailable[0] = true;
            }
        }
    }

    @Override
    public boolean takeSingleClockOrder(final OrderPriority priority, final TransactionalTask<SingleClockOrder> transactionalTask) {
        final boolean[] done = { false };
        final boolean[] noneAvailable = { false };

        // Try any single clock order
        tm.transactional(new TakeSingleClockOrderWork(done, noneAvailable, priority, transactionalTask, "any"));
        // If the work is done ore no single clock order available, return
        if (done[0] || noneAvailable[0]) {
            return done[0];
        }

        // Try single clock orders of a specific type in the order they are in the clockTypes array
        ClockType[] clockTypes = new ClockType[]{ ClockType.ZEITZONEN_SPORT, ClockType.SPORT, ClockType.KLASSISCH };
        for (ClockType type : clockTypes) {
            tm.transactional(new TakeSingleClockOrderWork(done, noneAvailable, priority, transactionalTask, type.name()));

            // Return as soon as one work was successful
            if (done[0]) {
                return true;
            }
        }

        // Return false if no work for any single clock order was successful
        return false;
    }

    @Override
    public void deliverDemandedClock(final UUID handlerId) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectDistributor();
                // Take a distributor demand
                ObjectMessage demandMsg = (ObjectMessage) distributorDemandQueueConsumer.receive();
                DistributorDemand distributorDemand = (DistributorDemand) demandMsg.getObject();
                JmsDistributorStockConnector stockConnector = null;

                try {
                    // Connect to the distributors stock
                    stockConnector = new JmsDistributorStockConnector(distributorDemand.getUri(), distributorDemand
                                                                      .getDestinationName());

                    Map<ClockType, Integer> demandedClocks = distributorDemand.getNeededClocksPerType();
                    Map<ClockType, Integer> stockCount = stockConnector.getDistributorStock();

                    for (ClockType type : demandedClocks.keySet()) {
                        if (stockCount.get(type) < demandedClocks.get(type)) {
                            // Take a delivered clock that is not related to any order
                            Clock clock = takeDeliveredClockOfNoOrder(type);

                            if (clock != null) {
                                // Deliver the clock
                                clock.setDistributor(distributorDemand.getDestinationName()
                                    .substring(3));
                                clock.setHandlerId(handlerId);
                                stockConnector.deliver(clock);

                                // Push the clock back but marked as "done" by setting not setting CLOCK_TYPE
                                ObjectMessage msg = session.createObjectMessage(clock);
                                msg.setStringProperty(JmsConstants.CLOCK_STATUS, ClockStatus.DELIVERED.name());
                                clockQueueProducer.send(msg);
                                clockTopicProducer.send(msg);
                                break;
                            }
                        }

                    }
                } finally {
                    if (stockConnector != null) {
                        stockConnector.close();
                    }
                }

                // Push back the demand
                demandMsg = session.createObjectMessage(distributorDemand);
                demandMsg.setStringProperty(JmsConstants.DISTRIBUTOR_ID, distributorDemand.getDestinationName());
                distributorDemandQueueProducer.send(demandMsg);
            }
        });
    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        return subscribeTopicListener(new JmsClockPartListener(listener), JmsConstants.CLOCK_PART_TOPIC);
    }

    @Override
    public Subscription subscribeForClocks(ClockListener listener) {
        return subscribeTopicListener(new JmsClockListener(listener), JmsConstants.CLOCK_TOPIC);
    }

    @Override
    public Subscription subscribeForOrders(OrderListener listener) {
        return subscribeTopicSelectorListener(new JmsOrderListener(listener),
                                              JmsConstants.CLOCK_TOPIC, JmsConstants.CLOCK_STATUS + "='" + ClockStatus.ASSEMBLED
                                              .name() + "'",
                                              JmsConstants.ORDER_TOPIC);
    }

    @Override
    public void addParts(final List<ClockPart> parts) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectSupplier();
                for (ClockPart cp : parts) {
                    ObjectMessage message = session.createObjectMessage(cp);
                    // We need this property to extract specific clock part types
                    message.setStringProperty(JmsConstants.CLOCK_PART_TYPE, cp.getType()
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
                // Gather all clock party types
                for (ClockPartType t : neededClockParts.keySet()) {
                    ObjectMessage message;
                    // For each clock part type gather the needed count
                    for (int i = 0, upper = neededClockParts.get(t); i < upper; i++) {
                        // Take a clock part of the given type
                        message = (ObjectMessage) partTypeConsumers.get(t)
                            .receive(JmsConstants.MAX_TIMEOUT_MILLIS);

                        if (message == null) {
                            throw new TimeoutException();
                        }

                        ClockPart part = (ClockPart) message.getObject();
                        parts.add(part);
                        // Signal that this clock part is removed
                        ObjectMessage msg = session.createObjectMessage(part);
                        msg.setBooleanProperty(JmsConstants.CLOCK_PART_REMOVED, true);
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
                // Consume the clock of the given clock quality type
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
                // Instantiate the idSequence if necessary
                idSequence = idSequence != null ? idSequence : new JmsSequence(session, JmsConstants.ID_QUEUE);
                clock.setSerialId(idSequence.getNextId());

                ObjectMessage msg = session.createObjectMessage(clock);
                // We need this property for quality checkers so they can extract assembled clocks
                msg.setStringProperty(JmsConstants.CLOCK_STATUS, ClockStatus.ASSEMBLED.name());
                clockQueueProducer.send(msg);
                clockTopicProducer.send(msg);
            }
        });

    }

    @Override
    public void addCheckedClock(final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                connectChecker();
                ObjectMessage message = session.createObjectMessage(clock);
                // We need this property for deliverers so they can exract specific clock qualities
                message.setStringProperty(JmsConstants.CLOCK_QUALITY, ClockQualityType.fromQuality(clock.getQuality())
                                          .name());
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
                // We need this property for handlers andso they can extract delivered clocks
                msg.setStringProperty(JmsConstants.CLOCK_STATUS, ClockStatus.DELIVERED.name());
                if (clock.getOrderId() == null) {
                    // Setting the type makes it available for handler to ship clocks to distributors
                    msg.setStringProperty(JmsConstants.CLOCK_TYPE, clock.getType()
                                          .name());
                }
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
                // We need this property so that we can distinguish disassembled clocks from others
                msg.setStringProperty(JmsConstants.CLOCK_STATUS, ClockStatus.DISASSEMBLED.name());
                clockTopicProducer.send(msg);
                clockQueueProducer.send(msg);
            }
        });
    }

    @Override
    public List<ClockPart> getClockParts() {
        return queueAsList(JmsConstants.CLOCK_PART_QUEUE);
    }

    @Override
    public List<Clock> getClocks() {
        return queueAsList(JmsConstants.CLOCK_QUEUE);
    }

    @Override
    public List<SingleClockOrder> getSingleClockOrders() {
        return queueAsList(JmsConstants.SINGLE_CLOCK_ORDER_QUEUE);
    }

    @Override
    public List<Order> getOrders() {
        return queueAsList(JmsConstants.ORDER_QUEUE);
    }

    private Clock takeDeliveredClockOfNoOrder(ClockType type) throws JMSException {
        MessageConsumer deliveredConsumer = null;

        try {
            // Consume delivered clock of the given type
            deliveredConsumer = session.createConsumer(clockQueue, JmsConstants.CLOCK_STATUS + "='" + ClockStatus.DELIVERED
                                                       .name() + "'"
                                                       + " AND " + JmsConstants.CLOCK_TYPE + "='" + type.name() + "'");
            ObjectMessage msg = (ObjectMessage) deliveredConsumer.receive(JmsConstants.MAX_TIMEOUT_MILLIS);

            if (msg == null) {
                return null;
            }

            return (Clock) msg.getObject();
        } finally {
            if (deliveredConsumer != null) {
                deliveredConsumer.close();
            }
        }
    }

    private void connectSupplier() throws JMSException {
        clockPartQueue = createQueueIfNull(clockPartQueue, JmsConstants.CLOCK_PART_QUEUE);
        clockPartQueueProducer = createProducerIfNull(clockPartQueueProducer, clockPartQueue);

        clockPartTopic = createTopicIfNull(clockPartTopic, JmsConstants.CLOCK_PART_TOPIC);
        clockPartTopicProducer = createProducerIfNull(clockPartTopicProducer, clockPartTopic);
    }

    private void connectAssembler() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, JmsConstants.CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, JmsConstants.CLOCK_TOPIC);
        clockPartQueue = createQueueIfNull(clockPartQueue, JmsConstants.CLOCK_PART_QUEUE);
        clockPartTopic = createTopicIfNull(clockPartTopic, JmsConstants.CLOCK_PART_TOPIC);

        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);
        clockPartTopicProducer = createProducerIfNull(clockPartTopicProducer, clockPartTopic);

        if (partTypeConsumers.isEmpty()) {
            for (ClockPartType type : ClockPartType.values()) {
                // Clock part consumer for the given clock party type
                partTypeConsumers.put(type, session.createConsumer(clockPartQueue, JmsConstants.CLOCK_PART_TYPE + "='" + type
                                                                   .name() + "'"));
            }
        }
    }

    private void connectChecker() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, JmsConstants.CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, JmsConstants.CLOCK_TOPIC);
        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);

        // Clock consumer for assembled clocks
        assembledConsumer = createConsumerIfNull(assembledConsumer, clockQueue, JmsConstants.CLOCK_STATUS + "='"
                                                 + ClockStatus.ASSEMBLED.name() + "'");
    }

    private void connectDeliverer() throws JMSException {
        clockQueue = createQueueIfNull(clockQueue, JmsConstants.CLOCK_QUEUE);
        clockTopic = createTopicIfNull(clockTopic, JmsConstants.CLOCK_TOPIC);
        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);

        if (clockQualityConsumers.isEmpty()) {
            for (ClockQualityType type : ClockQualityType.values()) {
                // Clock consumer for the given clock quality
                clockQualityConsumers.put(type, session.createConsumer(clockQueue, JmsConstants.CLOCK_QUALITY + "='" + type
                                                                       .name() + "'"));
            }
        }
    }

    private void connectOrder() throws JMSException {
        orderQueue = createQueueIfNull(orderQueue, JmsConstants.ORDER_QUEUE);
        orderTopic = createTopicIfNull(orderTopic, JmsConstants.ORDER_TOPIC);
        singleClockOrderQueue = createQueueIfNull(singleClockOrderQueue, JmsConstants.SINGLE_CLOCK_ORDER_QUEUE);

        orderQueueProducer = createProducerIfNull(orderQueueProducer, orderQueue);
        orderTopicProducer = createProducerIfNull(orderTopicProducer, orderTopic);
        singleClockOrderQueueProducer = createProducerIfNull(singleClockOrderQueueProducer, singleClockOrderQueue);

        if (orderPriorityConsumers.isEmpty()) {
            for (OrderPriority priority : OrderPriority.values()) {
                // Order consumer for the given priority
                orderPriorityConsumers.put(priority, session.createConsumer(orderQueue, JmsConstants.ORDER_PRIORITY + "='"
                                                                            + priority.name() + "'"));
            }
        }
        if (singleClockOrderPriorityAndTypeConsumers.isEmpty()) {
            for (OrderPriority priority : OrderPriority.values()) {
                Map<String, MessageConsumer> innerMap = new HashMap<String, MessageConsumer>();
                for (ClockType type : ClockType.values()) {
                    // Single clock order consumer for the given clock type and the given priority
                    innerMap.put(type.name(),
                                 session.createConsumer(singleClockOrderQueue,
                                                        JmsConstants.ORDER_PRIORITY + "='"
                                                        + priority
                                                        .name() + "' AND "
                                                        + JmsConstants.SINGLE_CLOCK_TYPE + "='"
                                                        + type.name() + "'"));
                }
                // Single clock order consumer for any clock type and the given priority
                innerMap.put("any",
                             session.createConsumer(singleClockOrderQueue,
                                                    JmsConstants.ORDER_PRIORITY + "='"
                                                    + priority
                                                    .name() + "' AND "
                                                    + JmsConstants.SINGLE_CLOCK_TYPE + " <> 'any'"));
                singleClockOrderPriorityAndTypeConsumers.put(priority, innerMap);
            }
        }
    }

    private void connectDistributor() throws JMSException {
        distributorDemandQueue = createQueueIfNull(distributorDemandQueue, JmsConstants.DISTRIBUTOR_DEMAND_QUEUE);
        distributorDemandQueueProducer = createProducerIfNull(distributorDemandQueueProducer, distributorDemandQueue);
        distributorDemandQueueConsumer = createConsumerIfNull(distributorDemandQueueConsumer, distributorDemandQueue);
        clockTopic = createTopicIfNull(clockTopic, JmsConstants.CLOCK_TOPIC);

        clockQueue = createQueueIfNull(clockQueue, JmsConstants.CLOCK_QUEUE);
        clockQueueProducer = createProducerIfNull(clockQueueProducer, clockQueue);
        clockTopicProducer = createProducerIfNull(clockTopicProducer, clockTopic);
    }

}
