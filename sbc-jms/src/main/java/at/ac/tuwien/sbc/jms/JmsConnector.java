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
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.Demand;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class JmsConnector extends AbstractTransactionalJmsConnector implements Connector {

    // Package private so that JmsServer can also see it
	static final String ID_QUEUE = "queue/id";
    
	private static final long MAX_TIMEOUT_MILLIS = 2000;
	private static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;

	private static final String CLOCKPART_TOPIC = "topic/clockpart";
	private static final String CLOCK_TOPIC = "topic/clock";

	private static final String CLOCKPART_QUEUE = "queue/clockpart";
	private static final String CLOCK_QUEUE = "queue/clock";
    private static final String ORDER_QUEUE = "queue/order";
    private static final String SINGLE_CLOCK_ORDER_QUEUE = "queue/singleclockorder";
	
	private static final String IS_ASSEMBLED = "IS_ASSEMBLED";
	private static final String IS_DELIVERED = "IS_DELIVERED";
	private static final String IS_DISASSEMBLED = "IS_DISASSEMBLED";
	public static final String ID_COUNTER = "ID_COUNTER";

	private Topic clockPartTopic, clockTopic;
	private Queue clockPartQueue, clockQueue, orderQueue, singleClockOrderQueue, idQueue;

	private MessageProducer clockPartQueueProducer, clockQueueProducer, clockPartTopicProducer, clockTopicProducer, orderQueueProducer, singleClockOrderQueueProducer, idProducer;
	private MessageConsumer clockPartTopicConsumer, clockTopicConsumer, idConsumer;
    
    private final Map<ClockPartType, MessageConsumer> partTypeConsumers = new EnumMap<ClockPartType, MessageConsumer>(ClockPartType.class);
    private final Map<ClockQualityType, MessageConsumer> clockQualityConsumers = new EnumMap<ClockQualityType, MessageConsumer>(ClockQualityType.class);
    private final Map<OrderPriority, MessageConsumer> orderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(OrderPriority.class);
//    private final Map<OrderPriority, Map<ClockType, MessageConsumer>> singleClockOrderPriorityAndTypeConsumers = new EnumMap<OrderPriority, Map<ClockType, MessageConsumer>>(OrderPriority.class);
    private final Map<OrderPriority, MessageConsumer> singleClockOrderPriorityConsumers = new EnumMap<OrderPriority, MessageConsumer>(OrderPriority.class);
	private MessageConsumer assembledConsumer;

	public JmsConnector(int port) { 
        super(port);
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
//        if (singleClockOrderPriorityAndTypeConsumers.isEmpty()) {
//            for (OrderPriority priority : OrderPriority.values()) {
//                Map<ClockType, MessageConsumer> map = new EnumMap<ClockType, MessageConsumer>(ClockType.class);
//                singleClockOrderPriorityAndTypeConsumers.put(priority, map);
//                for (ClockType type : ClockType.values()) {
//                    map.put(type, session.createConsumer(orderQueue, "priority='" + priority.name() + "' AND type='" + type.name() + "'"));
//                }
//            }
//        }
        if (singleClockOrderPriorityConsumers.isEmpty()) {
            for (OrderPriority priority : OrderPriority.values()) {
                singleClockOrderPriorityConsumers.put(priority, session.createConsumer(orderQueue, "priority='" + priority.name() + "'"));
            }
        }
	}

    @Override
    public Subscription subscribeForOrders(OrderListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addOrder(final Order order) {
        transactional(new TransactionalWork() {

            @Override
            public void doWork() throws JMSException {
                for (ClockType type : ClockType.values()) {
                    for (int i = 0; i < order.getNeededClocksOfType(type); i++) {
                        ObjectMessage msg = session.createObjectMessage(new SingleClockOrder(order.getId(), type, order.getPriority()));
                        msg.setStringProperty("type", type.name());
                        msg.setStringProperty("priority", order.getPriority().name());
                        singleClockOrderQueueProducer.send(msg);
                    }
                }
                
                ObjectMessage msg = session.createObjectMessage(order);
                msg.setStringProperty("priority", order.getPriority().name());
                orderQueueProducer.send(msg);
            }
        });
    }

//    @Override
//    public List<ClockType> getPossibleClockTypes(List<ClockType> wantedTypes) {
//		final List<ClockType> possibleClockTypes = new ArrayList<ClockType>();
//        List<ClockPart> clockParts = queueAsList(CLOCKPART_QUEUE);
//        Iterator<ClockPart> iter = clockParts.iterator();
//        Map<ClockPartType, Integer> foundClockParts = new EnumMap<ClockPartType, Integer>(ClockPartType.class);
//        
//        for (ClockType clockType : wantedTypes) {
//            Map<ClockPartType, Integer> missingClockParts = new EnumMap<ClockPartType, Integer>(clockType.getNeededParts());
//            
//            // Precalculate the count of missing clock parts by comparing needed with found clock parts
//            for (Map.Entry<ClockPartType, Integer> entry : foundClockParts.entrySet()) {
//                Integer neededCount = missingClockParts.get(entry.getKey());
//                
//                if (neededCount != null) {
//                    if (entry.getValue() >= neededCount) {
//                        missingClockParts.remove(entry.getKey());
//                    } else {
//                        missingClockParts.put(entry.getKey(), neededCount - entry.getValue());
//                    }
//                }
//            }
//            
//            while (!missingClockParts.isEmpty() && iter.hasNext()) {
//                ClockPart p = iter.next();
//                
//                // Update overall counters
//                Integer currentCount = foundClockParts.get(p.getType());
//                foundClockParts.put(p.getType(), currentCount == null ? 1 : currentCount + 1);
//                
//                // Update counters for the current clock type
//                Integer missingCount = missingClockParts.get(p.getType());
//                
//                if (missingCount != null) {
//                    if (missingCount == 1) {
//                        missingClockParts.remove(p.getType());
//                    } else {
//                        missingClockParts.put(p.getType(), missingCount - 1);
//                    }
//                }
//            }
//            
//            if (missingClockParts.isEmpty()) {
//                possibleClockTypes.add(clockType);
//            }
//        }
//        
//        return possibleClockTypes;
//    }

//    @Override
//    public Order getPossibleOrderByPriority(final List<ClockType> possibleClockTypes) {
//        Finder<Order> finder = new Finder<Order>() {
//
//            @Override
//            public boolean accept(Order order) {
//                for(ClockType t : possibleClockTypes){
//                    if(order.isClockTypeNeeded(t) ){
//                        return true;
//                    }
//                }
//                
//                return false;
//            }
//        };
//        Order order = findInQueue(ORDER_QUEUE, "priority='" + OrderPriority.HOCH.name() + "'", finder);
//        order = order != null ? order : (Order) findInQueue(ORDER_QUEUE, "priority='" + OrderPriority.MITTEL.name() + "'", finder);
//        order = order != null ? order : (Order) findInQueue(ORDER_QUEUE, "priority='" + OrderPriority.NIEDRIG.name() + "'", finder);
//        return order;
//    }

    @Override
    public boolean takeSingleClockOrder(final OrderPriority priority, final TransactionalTask<SingleClockOrder> transactionalTask) {
        try {
            final Boolean[] done = { false };
            connectOrder();
            
//            Map<ClockType, MessageConsumer> consumerMap = singleClockOrderPriorityAndTypeConsumers.get(priority);
//            for (ClockType type : possibleClockTypes) {
//                    ObjectMessage message = (ObjectMessage) consumerMap.get(type).receiveNoWait();
//                    if (message != null) {
//                        return (SingleClockOrder) message.getObject();
//                    }
//            }
            transactional(new TransactionalWork() {

                @Override
                public void doWork() throws JMSException {
                    ObjectMessage message = (ObjectMessage) singleClockOrderPriorityConsumers.get(priority).receiveNoWait();
                    if (message != null) {
                        transactionalTask.doWork((SingleClockOrder) message.getObject());
                        done[0] = true;
                    }
                }
            });
            
            return done[0];
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void connectDistributor(UUID distributorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDemand(UUID distributorId, Map<ClockType, Integer> demandPerType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void takeDemandedClock(TransactionalTask<Map<Demand, Clock>> transactionalTask) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deliverDemandedClock(Demand demand, Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

	private void connectClockPartListener() throws JMSException {
        clockPartTopic = createTopicIfNull(clockPartTopic, CLOCKPART_TOPIC);
        clockPartTopicConsumer = createConsumerIfNull(clockPartTopicConsumer, clockPartTopic);
	}

	private void connectClockListener() throws JMSException {
        clockTopic = createTopicIfNull(clockTopic, CLOCK_TOPIC);
        clockTopicConsumer = createConsumerIfNull(clockTopicConsumer, clockTopic);
	}
	
	private long getNextId() throws JMSException{
        connectIdSequence();
        // The server creates the first message so we can just wait without a timeout
        ObjectMessage message = (ObjectMessage) idConsumer.receive();
        Long id = (Long) message.getObject();
        
        ObjectMessage msg = session.createObjectMessage(Long.valueOf(id + 1));
        idProducer.send(msg);
        
        return id;
	}

	private void connectSupplier() throws JMSException {
        clockPartQueue = createQueueIfNull(clockPartQueue, CLOCKPART_QUEUE);
        clockPartQueueProducer = createProducerIfNull(clockPartQueueProducer, clockPartQueue);

        clockPartTopic = createTopicIfNull(clockPartTopic, CLOCKPART_TOPIC);
        clockPartTopicProducer = createProducerIfNull(clockPartTopicProducer, clockPartTopic);
	}

	private void connectIdSequence() throws JMSException {
        idQueue = createQueueIfNull(idQueue, ID_QUEUE);
        idConsumer = createConsumerIfNull(idConsumer, idQueue);
        idProducer = createProducerIfNull(idProducer, idQueue);
    }

	private void connectAssembler() throws JMSException {
        connectIdSequence();
        
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
		connectSupplier();
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

	@Override
	public void addParts(final List<ClockPart> parts) {
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
                connectSupplier();
				for (ClockPart cp : parts) {
					ObjectMessage message = session.createObjectMessage(cp);
                    message.setStringProperty("type", cp.getType().name());
					clockPartQueueProducer.send(message);
					clockPartTopicProducer.send(message);
				}
			}
		});
	}

	@Override
	public void takeParts(final Map<ClockPartType, Integer> neededClockParts, final TransactionalTask<List<ClockPart>> transactionalTask) {
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
                connectAssembler();
				List<ClockPart> parts = new ArrayList<ClockPart>();
				for (ClockPartType t : neededClockParts.keySet()) {
					ObjectMessage message = null;
					for (int i = 0, upper = neededClockParts.get(t); i < upper; i++) {
                        message = (ObjectMessage) partTypeConsumers.get(t).receive(/*MAX_TIMEOUT_MILLIS*/);

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
		transactional(new TransactionalWork() {

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
		return transactional(new TransactionalWork() {

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
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
                connectAssembler();
                clock.setSerialId(getNextId());
				ObjectMessage msg = session.createObjectMessage(clock);
				msg.setBooleanProperty(IS_ASSEMBLED, true);
				clockQueueProducer.send(msg);
				clockTopicProducer.send(msg);
			}
		});

	}

	@Override
	public void addCheckedClock(final Clock clock, final ClockQualityType type) {
		transactional(new TransactionalWork() {

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
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
                connectDeliverer();
				Message msg = session.createObjectMessage(clock);
				msg.setBooleanProperty(IS_DELIVERED, true);
				clockTopicProducer.send(msg);
				clockQueueProducer.send(msg);
			}
		});
	}

	@Override
	public void addDisassembledClock(final Clock clock) {
		transactional(new TransactionalWork() {

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
