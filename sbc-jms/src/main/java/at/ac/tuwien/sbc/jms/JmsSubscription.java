package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.Subscription;
import java.util.Arrays;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

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
