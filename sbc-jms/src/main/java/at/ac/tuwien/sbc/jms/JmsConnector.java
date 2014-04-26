/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;

/**
 *
 * @author Christian
 */
public class JmsConnector implements Connector {

	private static final long MAX_TIMEOUT_MILLIS = 2000;
	private static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;

	private static final String CLOCKPART_TOPIC = "topic/clockpart";
	private static final String CLOCK_TOPIC = "topic/clock";

	private static final String CLOCKPART_QUEUE = "queue/clockpart";
	private static final String CLOCK_QUEUE = "queue/clock";

	private static final String IS_CHASSIS = "IS_CHASSIS";
	private static final String IS_CLOCKWORK = "IS_CLOCKWORK";
	private static final String IS_CLOCKHAND = "IS_CLOCKHAND";
	private static final String IS_WRISTBAND = "IS_WRISTBAND";
	private static final String IS_ASSEMBLED = "IS_ASSEMBLED";
	private static final String IS_HIGH_QUALITY = "IS_HIGH_QUALITY";
	private static final String IS_MED_QUALITY = "IS_MED_QUALITY";
	private static final String IS_LOW_QUALITY = "IS_LOW_QUALITY";
	private static final String IS_DELIVERED = "IS_DELIVERED";
	private static final String IS_DISASSEMBLED = "IS_DISASSEMBLED";

	private Topic clockPartTopic, clockTopic;
	private Queue clockPartQueue, clockQueue;

	private MessageProducer clockPartQueueProducer, clockQueueProducer, clockPartTopicProducer, clockTopicProducer;
	private MessageConsumer clockPartTopicConsumer, clockTopicConsumer;
	private MessageConsumer chassisConsumer, clockworkConsumer, clockhandConsumer, wristbandConsumer, assembledConsumer, highQualityConsumer, medQualityConsumer, lowQualityConsumer;

	private Session session, browserPartSession, browserClockSession;
	private Connection connection;

	private final ThreadLocal<Boolean> currentTransaction = new ThreadLocal<Boolean>();
	private final ThreadLocal<Boolean> currentTransactionRollback = new ThreadLocal<Boolean>();

	private boolean commit() {
		try {
			session.commit();
			return false;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
			currentTransactionRollback.remove();
		}
	}

	private void rollback() {
		try {
			session.rollback();
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
		}
	}

	private boolean transactional(TransactionalWork work) {
		boolean created = ensureCurrentTransaction();
		currentTransactionRollback.set(Boolean.TRUE);

		try {
			work.doWork();

			if (created) {
				return commit();
			} else {
				// Indicates no timeout occurred
				return false;
			}
		} catch (TimeoutException ex) {
			created = false;
			currentTransaction.remove();
			currentTransactionRollback.remove();
			return true;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Error ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		} finally {
			if (created && Boolean.TRUE == currentTransactionRollback.get()) {
				rollback();
			}
		}
	}

	/**
	 * Returns true if the call resulted in creating the transaction.
	 *
	 * @param timeoutInMillis
	 * @return
	 */
	private boolean ensureCurrentTransaction() {
		Boolean tx = currentTransaction.get();
		if (tx == null) {
			tx = Boolean.TRUE;
			currentTransaction.set(tx);
			return true;
		}
		return false;
	}

	public JmsConnector(int port) {
		connect(port);
	}

	private void connect(int port) {
		try {
			//			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(/*"tcp://localhost:" + port*/);
			ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
			policy.setQueuePrefetch(0);
			connectionFactory.setPrefetchPolicy(policy);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Subscription subscribeForClockParts(ClockPartListener listener) {
		try {
			connectClockPartListener();
			JmsClockPartListener cpListener = new JmsClockPartListener(listener);
			clockPartTopicConsumer.setMessageListener(cpListener);
			return new JmsSubscription(clockPartTopicConsumer);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Subscription subscribeForClocks(ClockListener listener) {
		try {
			connectClockListener();
			JmsClockListener cListener = new JmsClockListener(listener);
			clockTopicConsumer.setMessageListener(cListener);
			return new JmsSubscription(clockTopicConsumer);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectClockPartListener() {
		try {
			if (clockPartTopic == null) {
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			}
			if (clockPartTopicConsumer == null) {
				clockPartTopicConsumer = session.createConsumer(clockPartTopic);
			}
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectClockListener() {
		try {
			if (clockTopic == null) {
				clockTopic = session.createTopic(CLOCK_TOPIC);
			}
			if (clockTopicConsumer == null) {
				clockTopicConsumer = session.createConsumer(clockTopic);
			}
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectSupplier() {
		try {
			if (clockPartQueue == null) {
				clockPartQueue = session.createQueue(CLOCKPART_QUEUE);
			}

			if (clockPartQueueProducer == null) {
				clockPartQueueProducer = session.createProducer(clockPartQueue);
			}

			if (clockPartTopic == null) {
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			}
			if (clockPartTopicProducer == null) {
				clockPartTopicProducer = session.createProducer(clockPartTopic);
			}
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectAssembler() {
		try {
			if (clockPartQueue == null) {
				clockPartQueue = session.createQueue(CLOCKPART_QUEUE);
			}
			if (clockQueue == null) {
				clockQueue = session.createQueue(CLOCK_QUEUE);
			}

			if (clockTopic == null) {
				clockTopic = session.createTopic(CLOCK_TOPIC);
			}

			if (chassisConsumer == null) {
				chassisConsumer = session.createConsumer(clockPartQueue, IS_CHASSIS + " = true");
			}
			if (clockworkConsumer == null) {
				clockworkConsumer = session.createConsumer(clockPartQueue, IS_CLOCKWORK + " = true");
			}
			if (clockhandConsumer == null) {
				clockhandConsumer = session.createConsumer(clockPartQueue, IS_CLOCKHAND + " = true");
			}
			if (wristbandConsumer == null) {
				wristbandConsumer = session.createConsumer(clockPartQueue, IS_WRISTBAND + " = true");
			}

			if (clockQueueProducer == null) {
				clockQueueProducer = session.createProducer(clockQueue);
			}

			if (clockTopicProducer == null) {
				clockTopicProducer = session.createProducer(clockTopic);
			}

			if (clockPartTopic == null) {
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			}

			if (clockPartTopicProducer == null) {
				clockPartTopicProducer = session.createProducer(clockPartTopic);
			}
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectChecker() {
		connectSupplier();
		try {
			if (clockQueue == null) {
				clockQueue = session.createQueue(CLOCK_QUEUE);
			}

			if (assembledConsumer == null) {
				assembledConsumer = session.createConsumer(clockQueue, IS_ASSEMBLED + " = true");
			}

			if (clockQueueProducer == null) {
				clockQueueProducer = session.createProducer(clockQueue);
			}

			if (clockTopic == null) {
				clockTopic = session.createTopic(CLOCK_TOPIC);
			}

			if (clockTopicProducer == null) {
				clockTopicProducer = session.createProducer(clockTopic);
			}

		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void connectDeliverer() {
		try {
			if (clockQueue == null) {
				clockQueue = session.createQueue(CLOCK_QUEUE);
			}
			if (clockTopic == null) {
				clockTopic = session.createTopic(CLOCK_TOPIC);
			}

			if (highQualityConsumer == null) {
				highQualityConsumer = session.createConsumer(clockQueue, IS_HIGH_QUALITY + " = true");
			}
			if (medQualityConsumer == null) {
				medQualityConsumer = session.createConsumer(clockQueue, IS_MED_QUALITY + " = true");
			}
			if (lowQualityConsumer == null) {
				lowQualityConsumer = session.createConsumer(clockQueue, IS_LOW_QUALITY + " = true");
			}

			if (clockTopicProducer == null) {
				clockTopicProducer = session.createProducer(clockTopic);
			}

			if (clockQueueProducer == null){
				clockQueueProducer = session.createProducer(clockQueue);
			}

		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void addParts(final List<ClockPart> parts) {
		connectSupplier();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				for (ClockPart cp : parts) {
					ObjectMessage message = session.createObjectMessage(cp);
					switch (cp.getType()) {
					case GEHAEUSE:
						message.setBooleanProperty(IS_CHASSIS, true);
						break;
					case UHRWERK:
						message.setBooleanProperty(IS_CLOCKWORK, true);
						break;
					case ZEIGER:
						message.setBooleanProperty(IS_CLOCKHAND, true);
						break;
					case ARMBAND:
						message.setBooleanProperty(IS_WRISTBAND, true);
						break;
					}
					clockPartQueueProducer.send(message);
					clockPartTopicProducer.send(message);
				}
			}
		});
	}

	@Override
	public void takeParts(final Map<ClockPartType, Integer> neededClockParts, final TransactionalTask<List<ClockPart>> transactionalTask) {
		connectAssembler();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				List<ClockPart> parts = new ArrayList<ClockPart>();
				for (ClockPartType t : neededClockParts.keySet()) {
					ObjectMessage message = null;
					for (int i = 0, upper = neededClockParts.get(t); i < upper; i++) {
						switch (t) {
						case GEHAEUSE:
							message = (ObjectMessage) chassisConsumer.receive(/*MAX_TIMEOUT_MILLIS*/);
							break;
						case UHRWERK:
							message = (ObjectMessage) clockworkConsumer.receive(/*MAX_TIMEOUT_MILLIS*/);
							break;
						case ZEIGER:
							message = (ObjectMessage) clockhandConsumer.receive(/*MAX_TIMEOUT_MILLIS*/);
							break;
						case ARMBAND:
							message = (ObjectMessage) wristbandConsumer.receive(/*MAX_TIMEOUT_MILLIS*/);
							break;
						}

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
		connectChecker();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				ObjectMessage message = (ObjectMessage) assembledConsumer.receive();
				transactionalTask.doWork((Clock) message.getObject());
			}
		});

	}

	@Override
	public boolean takeChecked(final ClockQualityType type, final long timeout, final TransactionalTask<Clock> transactionalTask) {
		connectDeliverer();
		return transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				MessageConsumer consumer = null;
				switch (type) {
				case A:
					consumer = highQualityConsumer;
					break;
				case B:
					consumer = medQualityConsumer;
					break;
				case C:
					consumer = lowQualityConsumer;
					break;
				}
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
		connectAssembler();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				Message msg = session.createObjectMessage(clock);
				msg.setBooleanProperty(IS_ASSEMBLED, true);
				clockQueueProducer.send(msg);
				clockTopicProducer.send(msg);
			}
		});

	}

	@Override
	public void addCheckedClock(final Clock clock, final ClockQualityType type) {
		connectChecker();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				ObjectMessage message = session.createObjectMessage(clock);
				switch (type) {
				case A:
					message.setBooleanProperty(IS_HIGH_QUALITY, true);
					break;
				case B:
					message.setBooleanProperty(IS_MED_QUALITY, true);
					break;
				case C:
					message.setBooleanProperty(IS_LOW_QUALITY, true);
					break;
				}
				clockQueueProducer.send(message);
				clockTopicProducer.send(message);
			}
		});
	}

	@Override
	public void addDeliveredClock(final Clock clock) {
		connectDeliverer();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				Message msg = session.createObjectMessage(clock);
				msg.setBooleanProperty(IS_DELIVERED, true);
				clockTopicProducer.send(msg);
				clockQueueProducer.send(msg);
			}
		});
	}

	@Override
	public void addDisassembledClock(final Clock clock) {
		connectDeliverer();
		transactional(new TransactionalWork() {

			@Override
			public void doWork() throws JMSException {
				Message msg = session.createObjectMessage(clock);
				msg.setBooleanProperty(IS_DISASSEMBLED, true);
				clockTopicProducer.send(msg);
				clockQueueProducer.send(msg);
			}
		});
	}

	@Override
	public List<ClockPart> getClockParts() {
		try {
			QueueBrowser clockPartBrowser = createClockPartBrowser();
			List<ObjectMessage> list = Collections.list(clockPartBrowser.getEnumeration());
			List<ClockPart> clockParts = new ArrayList<ClockPart>();
			for (ObjectMessage m : list) {
				clockParts.add((ClockPart) m.getObject());
			}
			clockPartBrowser.close();
			browserPartSession.close();
			return clockParts;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public List<Clock> getClocks() {
		try {
			QueueBrowser clockBrowser = createClockBrowser();
			List<ObjectMessage> list = Collections.list(clockBrowser.getEnumeration());
			List<Clock> clocks = new ArrayList<Clock>();
			for (ObjectMessage m : list) {
				clocks.add((Clock) m.getObject());
			}
			clockBrowser.close();
			browserClockSession.close();
			return clocks;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private QueueBrowser createClockBrowser() {
		try {
			if( browserClockSession == null ){
				browserClockSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}

			if (clockQueue == null) {
				clockQueue = session.createQueue(CLOCK_QUEUE);
			}
			return browserClockSession.createBrowser(clockQueue);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private QueueBrowser createClockPartBrowser() {
		try {
			if( browserPartSession == null ){
				browserPartSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}

			if (clockPartQueue == null) {
				clockPartQueue = session.createQueue(CLOCKPART_QUEUE);
			}
			return browserPartSession.createBrowser(clockPartQueue);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

}
