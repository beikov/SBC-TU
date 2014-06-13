/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import java.net.URI;
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
import javax.jms.Session;
import javax.jms.Topic;

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

/**
 *
 * @author Christian
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
	private MessageProducer orderTopicProducer;
	private MessageProducer singleClockOrderQueueProducer;
	private MessageConsumer orderTopicConsumer;
	private final Map<OrderPriority, MessageConsumer> orderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(
			OrderPriority.class);
	private final Map<OrderPriority, Map<String, MessageConsumer>> singleClockOrderPriorityAndTypeConsumers = new EnumMap<OrderPriority, 
			Map<String, MessageConsumer>>(OrderPriority.class);

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

				ObjectMessage msg = session.createObjectMessage(order);
				msg.setStringProperty(JmsConstants.ORDER_PRIORITY, order.getPriority()
						.name());
				orderQueueProducer.send(msg);
				orderTopicProducer.send(msg);
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

				ObjectMessage message = (ObjectMessage) singleClockOrderPriorityAndTypeConsumers.get(priority).get("any")
						.receiveNoWait();
				if (message != null) {
					transactionalTask.doWork((SingleClockOrder) message.getObject());
					done[0] = true;
				}
			}

		});
		if(done[0]){
			return done[0];
		}
		
		tm.transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				connectOrder();

				ObjectMessage message = (ObjectMessage) singleClockOrderPriorityAndTypeConsumers.get(priority).get(ClockType.ZEITZONEN_SPORT.name())
						.receiveNoWait();
				if (message != null) {
					transactionalTask.doWork((SingleClockOrder) message.getObject());
					done[0] = true;
				}
			}

		});
		if(done[0]){
			return done[0];
		}
		
		tm.transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				connectOrder();

				ObjectMessage message = (ObjectMessage) singleClockOrderPriorityAndTypeConsumers.get(priority).get(ClockType.SPORT.name())
						.receiveNoWait();
				if (message != null) {
					transactionalTask.doWork((SingleClockOrder) message.getObject());
					done[0] = true;
				}
			}

		});
		if(done[0]){
			return done[0];
		}
		
		tm.transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				connectOrder();

				ObjectMessage message = (ObjectMessage) singleClockOrderPriorityAndTypeConsumers.get(priority).get(ClockType.KLASSISCH.name())
						.receiveNoWait();
				if (message != null) {
					transactionalTask.doWork((SingleClockOrder) message.getObject());
					done[0] = true;
				}
			}

		});

		return done[0];
	}

	private Clock takeDeliveredClockOfNoOrder(ClockType type) throws JMSException {
		MessageConsumer deliveredConsumer = null;

		try {
			deliveredConsumer = session.createConsumer(clockQueue, JmsConstants.CLOCK_STATUS + "='" + ClockStatus.DELIVERED
					.name() + "'" + " AND "
					+ JmsConstants.CLOCK_TYPE + "='" + type.name() + "'");
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

	private JmsDistributorStockConnector getStockConnector(URI distributorUri, String destinationName) throws JMSException {
		return new JmsDistributorStockConnector(distributorUri, destinationName);
	}

	@Override
	public void deliverDemandedClock(final UUID handlerId) {
		tm.transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				connectDistributor();
				ObjectMessage demandMsg = (ObjectMessage) distributorDemandQueueConsumer.receive();
				DistributorDemand distributorDemand = (DistributorDemand) demandMsg.getObject();
				JmsDistributorStockConnector stockConnector = null;

				try {
					stockConnector = getStockConnector(distributorDemand.getUri(), distributorDemand.getDestinationName());

					Map<ClockType, Integer> demandedClocks = distributorDemand.getNeededClocksPerType();
					Map<ClockType, Integer> stockCount = stockConnector.getDistributorStock();

					for (ClockType type : demandedClocks.keySet()) {
						if (stockCount.get(type) < demandedClocks.get(type)) {
							Clock clock = takeDeliveredClockOfNoOrder(type);

							if (clock != null) {
								clock.setDistributor(distributorDemand.getDestinationName().substring(3));
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
		boolean close = true;
		Session s = null;

		try {
			s = connectClockPartListener();
			clockPartTopicConsumer.setMessageListener(new JmsClockPartListener(listener));
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

	@Override
	public Subscription subscribeForClocks(ClockListener listener) {
		boolean close = true;
		Session s = null;

		try {
			s = connectClockListener();
			clockTopicConsumer.setMessageListener(new JmsClockListener(listener));
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

	@Override
	public Subscription subscribeForOrders(OrderListener listener) {
		boolean close = true;
		Session s = null;

		try {
			s = connectOrderListener();
			JmsOrderListener l = new JmsOrderListener(listener);
			assembledTopicConsumer.setMessageListener(l);
			orderTopicConsumer.setMessageListener(l);
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

	private Session connectClockPartListener() throws JMSException {
		Session s = createSession();
		clockPartTopic = createTopicIfNull(s, clockPartTopic, JmsConstants.CLOCK_PART_TOPIC);
		clockPartTopicConsumer = createConsumerIfNull(s, clockPartTopicConsumer, clockPartTopic);
		return s;
	}

	private Session connectClockListener() throws JMSException {
		Session s = createSession();
		clockTopic = createTopicIfNull(s, clockTopic, JmsConstants.CLOCK_TOPIC);
		clockTopicConsumer = createConsumerIfNull(s, clockTopicConsumer, clockTopic);
		return s;
	}

	private Session connectOrderListener() throws JMSException {
		Session s = createSession();
		orderTopic = createTopicIfNull(s, orderTopic, JmsConstants.ORDER_TOPIC);
		clockTopic = createTopicIfNull(s, clockTopic, JmsConstants.CLOCK_TOPIC);
		assembledTopicConsumer = createConsumerIfNull(s, assembledTopicConsumer, clockTopic, JmsConstants.CLOCK_STATUS + "='"
				+ ClockStatus.ASSEMBLED.name() + "'");
		orderTopicConsumer = createConsumerIfNull(s, orderTopicConsumer, orderTopic);
		return s;
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
				orderPriorityConsumers.put(priority, session.createConsumer(orderQueue, JmsConstants.ORDER_PRIORITY + "='"
						+ priority.name() + "'"));
			}
		}
		if (singleClockOrderPriorityAndTypeConsumers.isEmpty()) {
			for (OrderPriority priority : OrderPriority.values()) {
				Map<String, MessageConsumer> innerMap = new HashMap<String, MessageConsumer>();
				for(ClockType type : ClockType.values()){
					innerMap.put(type.name(), 
							session.createConsumer(singleClockOrderQueue,
									JmsConstants.ORDER_PRIORITY + "='"
											+ priority
											.name() + "' AND NOT ("
											+ JmsConstants.SINGLE_CLOCK_TYPE + " LIKE '"
											+ type.name()+"')"));
				}
				innerMap.put("any", 
						session.createConsumer(singleClockOrderQueue,
								JmsConstants.ORDER_PRIORITY + "='"
										+ priority
										.name() + "' AND NOT ("
										+ JmsConstants.SINGLE_CLOCK_TYPE + " LIKE 'any')"));
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

	@Override
	public void addParts(final List<ClockPart> parts) {
		tm.transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				connectSupplier();
				for (ClockPart cp : parts) {
					ObjectMessage message = session.createObjectMessage(cp);
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
				for (ClockPartType t : neededClockParts.keySet()) {
					ObjectMessage message = null;
					for (int i = 0, upper = neededClockParts.get(t); i < upper; i++) {
						message = (ObjectMessage) partTypeConsumers.get(t)
								.receive(JmsConstants.MAX_TIMEOUT_MILLIS);

						if (message == null) {
							throw new TimeoutException();
						}

						ClockPart part = (ClockPart) message.getObject();
						parts.add(part);
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
				idSequence = idSequence != null ? idSequence : new JmsSequence(session, JmsConstants.ID_QUEUE);
				clock.setSerialId(idSequence.getNextId());
				ObjectMessage msg = session.createObjectMessage(clock);
				msg.setStringProperty(JmsConstants.CLOCK_STATUS, ClockStatus.ASSEMBLED.name());
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
				message.setStringProperty(JmsConstants.CLOCK_QUALITY, type.name());
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
	public List<Order> getOrders() {
		return queueAsList(JmsConstants.ORDER_QUEUE);
	}

}
