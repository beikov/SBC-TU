/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import java.net.URI;
import javax.jms.Connection;
import javax.jms.JMSException;
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

    private static final Long SEQUENCE_START = 0L;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: JmsServer PORT");
        }

        Integer port = Integer.parseInt(args[0]);
        BrokerService broker = new BrokerService();
        broker.addConnector("tcp://localhost:" + port);
		broker.setPersistent(false);
        broker.deleteAllMessages();
        broker.start();
        createIdSequence(port);
        URI uri = broker.getTransportConnectorByScheme("tcp").getUri();
        System.out.println("Started JMS-Server at: " + uri.toString());
        System.out.println("Press CTRL+C to shutdown the server...");
        while (System.in.read() != -1);
    }

    public static URI startServer(String name) {
        try {
            BrokerService broker = new BrokerService();
            broker.setBrokerName(name);
            broker.addConnector("tcp://localhost:0");
            broker.setPersistent(false);
            broker.deleteAllMessages();
            broker.start();
            URI uri = broker.getTransportConnectorByScheme("tcp").getUri();
            System.out.println("Started JMS-Server at: " + uri.toString());
            return uri;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void createIdSequence(int port) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);

        ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
        policy.setQueuePrefetch(0);
        connectionFactory.setPrefetchPolicy(policy);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);

        MessageProducer idProducer = session.createProducer(
            session.createQueue(JmsConstants.ID_QUEUE));

        ObjectMessage message = session.createObjectMessage(SEQUENCE_START);
        idProducer.send(message);

        session.commit();

        session.close();
        connection.close();
    }
}
