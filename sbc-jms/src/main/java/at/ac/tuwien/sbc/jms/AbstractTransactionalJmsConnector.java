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
public abstract class AbstractTransactionalJmsConnector {
    
	private final ThreadLocal<Boolean> currentTransaction = new ThreadLocal<Boolean>();
	private final ThreadLocal<Boolean> currentTransactionRollback = new ThreadLocal<Boolean>();
    
	protected final Session session;
	protected final Connection connection;

    public AbstractTransactionalJmsConnector(int port) {
		try {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);
//			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
			
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

	protected boolean commit() {
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

	protected void rollback() {
		try {
			session.rollback();
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
		}
	}

	protected boolean transactional(TransactionalWork work) {
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
    
    protected Topic createTopicIfNull(Topic t, String name) throws JMSException {
        return t == null ? session.createTopic(name) : t;
    }
    
    protected Queue createQueueIfNull(Queue q, String name) throws JMSException {
        return q == null ? session.createQueue(name) : q;
    }

    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination) throws JMSException {
        return consumer == null ? session.createConsumer(destination) : consumer;
    }

    protected MessageConsumer createConsumerIfNull(MessageConsumer consumer, Destination destination, String selector) throws JMSException {
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
                return (T) enumeration.nextElement().getObject();
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
                T object = (T) enumeration.nextElement().getObject();
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
            Enumeration<ObjectMessage> enumeration = s.createBrowser(s.createQueue(name)).getEnumeration();
            List<T> list = new LinkedList<T>();
            
            while (enumeration.hasMoreElements()) {
                list.add((T) enumeration.nextElement().getObject());
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
}
