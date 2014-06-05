/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
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
public abstract class AbstractJmsComponent {

	private final String serverHost;
	private final int serverPort;
	private final URI serverUri;
	//    private final String localHost;
	//	private final int localPort;
	private final Connection connection;
	protected final Session session;
	protected final JmsTransactionManager tm;

	private static URI getUri(String serverHost, int serverPort) {
		return URI.create("tcp://" + serverHost + ":" + serverPort);
	}

	public AbstractJmsComponent(int serverPort) {
		this("localhost", serverPort);
	}

	public AbstractJmsComponent(String serverHost, int serverPort/* , String localHost, int localPort */) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.serverUri = getUri(serverHost, serverPort);
		//        this.localHost = localHost;
		//        this.localPort = localPort;
		try {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(serverUri);

//			ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
//			policy.setQueuePrefetch(0);
//			connectionFactory.setPrefetchPolicy(policy);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
			tm = new JmsTransactionManager(session);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	public AbstractJmsComponent(Session session) {
		this.serverHost = null;
		this.serverPort = -1;
		this.serverUri = null;
		//        this.localHost = null;
		//        this.localPort = null;
		this.connection = null;
		this.session = session;
		this.tm = new JmsTransactionManager(session);
	}

	protected Session createSession() throws JMSException {
		return connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
	}

	protected void close() {
		try {
			connection.close();
		} catch (JMSException ex) {
			// Ignore
		}
	}

	protected Topic createTopicIfNull(Session session, Topic t, String name) throws JMSException {
		return t == null ? session.createTopic(name) : t;
	}

	protected Queue createQueueIfNull(Session session, Queue q, String name) throws JMSException {
		return q == null ? session.createQueue(name) : q;
	}

	protected MessageConsumer createConsumerIfNull(Session session, MessageConsumer consumer, Destination destination) throws
	JMSException {
		return consumer == null ? session.createConsumer(destination) : consumer;
	}

	protected MessageConsumer createConsumerIfNull(Session session, MessageConsumer consumer, Destination destination, String selector)
			throws
			JMSException {
		return consumer == null ? session.createConsumer(destination, selector) : consumer;
	}

	protected MessageProducer createProducerIfNull(Session session, MessageProducer producer, Destination destination) throws
	JMSException {
		return producer == null ? session.createProducer(destination) : producer;
	}

	protected Topic createTopicIfNull(Topic t, String name) throws JMSException {
		return createTopicIfNull(session, t, name);
	}

	protected Queue createQueueIfNull(Queue q, String name) throws JMSException {
		return createQueueIfNull(session, q, name);
	}

	protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination) throws JMSException {
		return createConsumerIfNull(session, consumer, destination);
	}

	protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination, String selector) throws
	JMSException {
		return createConsumerIfNull(session, consumer, destination, selector);
	}

	protected MessageProducer createProducerIfNull(MessageProducer producer, Destination destination) throws JMSException {
		return createProducerIfNull(session, producer, destination);
	}

	protected static interface Finder<T> {

		public boolean accept(T element);
	}

	protected <T> T findInQueue(String name, String selector) {
		Session s = null;

		try {
			s = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			QueueBrowser browser = s.createBrowser(s.createQueue(name), selector);
			Enumeration<ObjectMessage> enumeration = browser.getEnumeration();

			if (enumeration.hasMoreElements()) {
				return (T) enumeration.nextElement()
						.getObject();
			}

			return null;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (JMSException ex) {
					// Ignore
				}
			}
		}
	}

	protected <T> T findInQueue(String name, String selector, Finder<T> finder) {
		Session s = null;

		try {
			s = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			QueueBrowser browser = s.createBrowser(s.createQueue(name), selector);
			Enumeration<ObjectMessage> enumeration = browser.getEnumeration();
			while (enumeration.hasMoreElements()) {
				T object = (T) enumeration.nextElement()
						.getObject();
				if (finder.accept(object)) {
					return object;
				}
			}

			return null;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (JMSException ex) {
					// Ignore
				}
			}
		}
	}

	protected <T> List<T> queueAsList(String name) {
		Session s = null;

		try {
			s = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Enumeration<ObjectMessage> enumeration = s.createBrowser(s.createQueue(name))
					.getEnumeration();
			List<T> list = new LinkedList<T>();

			while (enumeration.hasMoreElements()) {
				list.add((T) enumeration.nextElement()
						.getObject());
			}

			return list;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (JMSException ex) {
					// Ignore
				}
			}
		}
	}
}
