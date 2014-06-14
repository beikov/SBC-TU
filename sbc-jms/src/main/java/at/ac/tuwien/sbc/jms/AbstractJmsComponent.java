package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.Subscription;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;

/**
 * An abstract base class for JMS components to reduce the boilerplate of implementations.
 */
public abstract class AbstractJmsComponent {

    private final URI serverUri;
    private final Connection connection;
    protected final Session session;
    protected final JmsTransactionManager tm;

    private static URI getUri(String serverHost, int serverPort) {
        return URI.create("tcp://" + serverHost + ":" + serverPort);
    }

    /**
     * Like {@link AbstractJmsComponent#AbstractJmsComponent(java.lang.String, int) } but with the default host <code>localhost</code>.
     *
     * @param serverPort the port to connect to
     */
    public AbstractJmsComponent(int serverPort) {
        this("localhost", serverPort);
    }

    /**
     * Creates a new JMS Component that connects to the JMS Server at the given host and port.
     *
     * @param serverHost the host to connect to
     * @param serverPort the port to connect to
     */
    public AbstractJmsComponent(String serverHost, int serverPort) {
        this.serverUri = getUri(serverHost, serverPort);
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(serverUri);
            RedeliveryPolicy redeliveryPolicy = connectionFactory.getRedeliveryPolicy();
            redeliveryPolicy.setMaximumRedeliveries(-1);
            redeliveryPolicy.setInitialRedeliveryDelay(0);
            ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
            policy.setQueuePrefetch(0);
            connectionFactory.setPrefetchPolicy(policy);
            connection = connectionFactory.createConnection();

            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            tm = new JmsTransactionManager(session);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Reuse the given session instead of creating a new connection and session.
     *
     * @param session the session to use
     */
    public AbstractJmsComponent(Session session) {
        this.serverUri = null;
        this.connection = null;
        this.session = session;
        this.tm = new JmsTransactionManager(session);
    }

    /**
     * Creates a new transacted session if this component has a valid connection.
     *
     * @return a new transacted session
     * @throws JMSException
     */
    protected Session createSession() throws JMSException {
        return connection == null ? null : connection.createSession(true, Session.SESSION_TRANSACTED);
    }

    /**
     * Closes the connection created by this component
     */
    protected void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                // Ignore
            }
        }
    }

    /**
     * Creates a new topic for the given name if the given topic is null.
     *
     * @param session the session to create the topic
     * @param t       the topic to check if null
     * @param name    the name of the topic
     * @return never null, either the existing or the newly created topic
     * @throws JMSException
     */
    protected Topic createTopicIfNull(Session session, Topic t, String name) throws JMSException {
        return t == null ? session.createTopic(name) : t;
    }

    /**
     * Creates a new queue for the given name if the given queue is null.
     *
     * @param session the session to create the queue
     * @param q       the queue to check if null
     * @param name    the name of the queue
     * @return never null, either the existing or the newly created queue
     * @throws JMSException
     */
    protected Queue createQueueIfNull(Session session, Queue q, String name) throws JMSException {
        return q == null ? session.createQueue(name) : q;
    }

    /**
     * Creates a new consumer for the given destination if the given consumer is null.
     *
     * @param session     the session to create the consumer
     * @param consumer    the consumer to check if null
     * @param destination the wanted consumer destination
     * @return never null, either the existing or the newly created consumer
     * @throws JMSException
     */
    protected MessageConsumer createConsumerIfNull(Session session, MessageConsumer consumer, Destination destination) throws
        JMSException {
        return consumer == null ? session.createConsumer(destination) : consumer;
    }

    /**
     * Creates a new consumer for the given destination with the given selector if the given consumer is null.
     *
     * @param session     the session to create the consumer
     * @param consumer    the consumer to check if null
     * @param destination the wanted consumer destination
     * @param selector    the wanted consumer selector
     * @return never null, either the existing or the newly created consumer
     * @throws JMSException
     */
    protected MessageConsumer createConsumerIfNull(Session session, MessageConsumer consumer, Destination destination, String selector)
        throws
        JMSException {
        return consumer == null ? session.createConsumer(destination, selector) : consumer;
    }

    /**
     * Creates a new producer for the given destination if the given producer is null.
     *
     * @param session     the session to create the producer
     * @param producer    the producer to check if null
     * @param destination the wanted producer destination
     * @return never null, either the existing or the newly created producer
     * @throws JMSException
     */
    protected MessageProducer createProducerIfNull(Session session, MessageProducer producer, Destination destination) throws
        JMSException {
        return producer == null ? session.createProducer(destination) : producer;
    }

    /**
     * Creates a new topic for the given name if the given topic is null.
     *
     * @param t    the topic to check if null
     * @param name the name of the topic
     * @return never null, either the existing or the newly created topic
     * @throws JMSException
     */
    protected Topic createTopicIfNull(Topic t, String name) throws JMSException {
        return createTopicIfNull(session, t, name);
    }

    /**
     * Creates a new queue for the given name if the given queue is null.
     *
     * @param q    the queue to check if null
     * @param name the name of the queue
     * @return never null, either the existing or the newly created queue
     * @throws JMSException
     */
    protected Queue createQueueIfNull(Queue q, String name) throws JMSException {
        return createQueueIfNull(session, q, name);
    }

    /**
     * Creates a new consumer for the given destination if the given consumer is null.
     *
     * @param consumer    the consumer to check if null
     * @param destination the wanted consumer destination
     * @return never null, either the existing or the newly created consumer
     * @throws JMSException
     */
    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination) throws JMSException {
        return createConsumerIfNull(session, consumer, destination);
    }

    /**
     * Creates a new consumer for the given destination with the given selector if the given consumer is null.
     *
     * @param consumer    the consumer to check if null
     * @param destination the wanted consumer destination
     * @param selector    the wanted consumer selector
     * @return never null, either the existing or the newly created consumer
     * @throws JMSException
     */
    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination, String selector) throws
        JMSException {
        return createConsumerIfNull(session, consumer, destination, selector);
    }

    /**
     * Creates a new producer for the given destination if the given producer is null.
     *
     * @param producer    the producer to check if null
     * @param destination the wanted producer destination
     * @return never null, either the existing or the newly created producer
     * @throws JMSException
     */
    protected MessageProducer createProducerIfNull(MessageProducer producer, Destination destination) throws JMSException {
        return createProducerIfNull(session, producer, destination);
    }

    /**
     * A simple interface to check elements.
     *
     * @param <T> the object type to check
     */
    protected static interface Finder<T> {

        /**
         * Returns true if the given element should be accepted.
         *
         * @param element the element to check
         * @return true if the element is accepted, otherwise false
         */
        public boolean accept(T element);
    }

    /**
     * Finds an element in the queue and returns it.
     *
     * @param <T>      the expected object message type
     * @param name     the name of the queue
     * @param selector the selector to use for filtering messages
     * @return a list of objects
     */
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

    /**
     * Finds an element in the queue and returns it by checking if a finder accepts the object.
     *
     * @param <T>      the expected object message type
     * @param name     the name of the queue
     * @param selector the selector to use for filtering messages
     * @param finder   the finder to use for checking objects
     * @return a list of objects
     */
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

    /**
     * Returns the queue's content as list.
     *
     * @param <T>  the expected object message type
     * @param name the name of the queue
     * @return a list of objects
     */
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

    /**
     * Creates a subscription for the given topic names and selectors.
     *
     * @param listener               the listener for this subscription
     * @param topicNamesAndSelectors the alternating names and selectors of the topics at which the listener should be registered
     * @return a subscription for the registration that can be cancelled
     */
    protected Subscription subscribeTopicSelectorListener(MessageListener listener, String... topicNamesAndSelectors) {
        boolean close = true;
        Session s = null;

        try {
            s = createSession();
            int size = topicNamesAndSelectors.length / 2 + topicNamesAndSelectors.length % 2;
            Destination[] destinations = new Destination[size];
            String[] selectors = new String[size];

            for (int i = 0, j = 0; j < topicNamesAndSelectors.length; i++, j += 2) {
                destinations[i] = s.createTopic(topicNamesAndSelectors[j]);

                if (topicNamesAndSelectors.length > j + 1) {
                    selectors[i] = topicNamesAndSelectors[j + 1];
                }
            }
            for (int i = 0; i < size; i++) {
                MessageConsumer consumer;

                if (selectors[i] == null) {
                    consumer = s.createConsumer(destinations[i]);
                } else {
                    consumer = s.createConsumer(destinations[i], selectors[i]);
                }

                consumer.setMessageListener(listener);
            }
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

    /**
     * Creates a subscription for the given topic names.
     *
     * @param listener   the listener for this subscription
     * @param topicNames the names of the topics at which the listener should be registered
     * @return a subscription for the registration that can be cancelled
     */
    protected Subscription subscribeTopicListener(MessageListener listener, String... topicNames) {
        boolean close = true;
        Session s = null;

        try {
            s = createSession();
            Destination[] destinations = new Destination[topicNames.length];

            for (int i = 0; i < topicNames.length; i++) {
                destinations[i] = s.createTopic(topicNames[i]);
            }
            for (Destination destination : destinations) {
                s.createConsumer(destination)
                    .setMessageListener(listener);
            }
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

    /**
     * Creates a subscription for the given destinations.
     *
     * @param listener     the listener for this subscription
     * @param destinations the destinations at which the listener should be registered
     * @return a subscription for the registration that can be cancelled
     */
    protected Subscription subscribeListener(MessageListener listener, Destination... destinations) {
        boolean close = true;
        Session s = null;

        try {
            s = createSession();
            for (Destination destination : destinations) {
                s.createConsumer(destination)
                    .setMessageListener(listener);
            }
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
}
