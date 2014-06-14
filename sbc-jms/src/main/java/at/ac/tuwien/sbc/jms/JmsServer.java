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
 * A wrapper around ActiveMQ to start a JMS Server.
 */
public class JmsServer {

    /**
     * The value at which the id sequenece starts.
     */
    private static final Long SEQUENCE_START = 0L;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: JmsServer PORT");
        }

        Integer port = Integer.parseInt(args[0]);
        BrokerService broker = new BrokerService();
        broker.addConnector("tcp://localhost:" + port);
        // We don't need persistence
        broker.setPersistent(false);
        // Clear everything, just to make sure
        broker.deleteAllMessages();
        broker.start();
        createIdSequence(port);
        URI uri = broker.getTransportConnectorByScheme("tcp")
            .getUri();
        System.out.println("Started JMS-Server at: " + uri.toString());
        System.out.println("Press CTRL+C to shutdown the server...");
        while (System.in.read() != -1);
    }

    public static URI startServer(String name) {
        try {
            BrokerService broker = new BrokerService();
            broker.setBrokerName(name);
            broker.addConnector("tcp://localhost:0");
            // We don't need persistence
            broker.setPersistent(false);
            // Clear everything, just to make sure
            broker.deleteAllMessages();
            broker.start();
            URI uri = broker.getTransportConnectorByScheme("tcp")
                .getUri();
            System.out.println("Started JMS-Server at: " + uri.toString());
            return uri;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Creates a queue that represents an id sequence.
     *
     * @param port the port at which the JMS Server listens
     * @throws JMSException
     */
    private static void createIdSequence(int port) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + port);

        ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
        policy.setQueuePrefetch(0);
        connectionFactory.setPrefetchPolicy(policy);
        Connection connection = null;

        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);

            MessageProducer idProducer = session.createProducer(
                session.createQueue(JmsConstants.ID_QUEUE));

            ObjectMessage message = session.createObjectMessage(SEQUENCE_START);
            idProducer.send(message);

            session.commit();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
