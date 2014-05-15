/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.broker.BrokerService;

/**
 *
 * @author Christian
 */
public class JmsServer {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("Usage: JmsServer PORT");
		}

		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:" + Integer.parseInt(args[0]));
//		broker.setPersistent(false);
		broker.deleteAllMessages();
		broker.start();
		createSerialId(Integer.parseInt(args[0]));
		System.out.println("Press CTRL+C to shutdown the server...");
		while(System.in.read() != -1);
	}

	private static void createSerialId(int port) throws JMSException{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);

		ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
		policy.setQueuePrefetch(0);
		connectionFactory.setPrefetchPolicy(policy);
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);

		MessageProducer idProducer = session.createProducer(
				session.createQueue(JmsConnector.ID_QUEUE));
		Integer startId = 0;
		ObjectMessage message = session.createObjectMessage(startId);
		message.setBooleanProperty(JmsConnector.ID_COUNTER, true);
		idProducer.send(message);

		session.commit();
		
		session.close();
		connection.close();
	}
}
