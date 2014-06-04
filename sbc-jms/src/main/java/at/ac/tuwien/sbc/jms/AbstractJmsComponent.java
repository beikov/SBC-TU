/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

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

    private final Connection connection;
    protected final Session session;
    protected final JmsTransactionManager tm;

    public AbstractJmsComponent(int port) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);
//			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();

            ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
            policy.setQueuePrefetch(0);
            connectionFactory.setPrefetchPolicy(policy);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
            tm = new JmsTransactionManager(session);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
    }

    public AbstractJmsComponent(Session session) {
        this.connection = null;
        this.session = session;
        this.tm = new JmsTransactionManager(session);
    }

    protected Topic createTopicIfNull(Topic t, String name) throws JMSException {
        return t == null ? session.createTopic(name) : t;
    }

    protected Queue createQueueIfNull(Queue q, String name) throws JMSException {
        return q == null ? session.createQueue(name) : q;
    }

    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination) throws JMSException {
        return consumer == null ? session.createConsumer(destination) : consumer;
    }

    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination, String selector) throws
        JMSException {
        return consumer == null ? session.createConsumer(destination, selector) : consumer;
    }

    protected MessageProducer createProducerIfNull(MessageProducer producer, Destination destination) throws JMSException {
        return producer == null ? session.createProducer(destination) : producer;
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
