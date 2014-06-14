package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.Subscription;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * A JMS implementation of a subscription. This class assumes that consumers for a subscription are associated to a dedicated session.
 */
public class JmsSubscription implements Subscription {

    private final Session session;

    public JmsSubscription(Session session) {
        this.session = session;
    }

    @Override
    public void cancel() {
        try {
            session.close();
        } catch (JMSException ex) {
            // Ignore
        }
    }

}
