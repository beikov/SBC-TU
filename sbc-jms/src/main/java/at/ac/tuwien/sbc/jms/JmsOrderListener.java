package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;
import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * A JMS message listener that forwards objects messages for orders to a {@link OrderListener}.
 */
public class JmsOrderListener implements MessageListener {

    private final OrderListener listener;

    public JmsOrderListener(OrderListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMessage msg = (ObjectMessage) message;
        try {
            Serializable o = msg.getObject();

            if (o instanceof Order) {
                listener.onOrderAdded((Order) o);
            } else if (o instanceof Clock) {
                Clock clock = (Clock) o;
                if (clock.getOrderId() != null) {
                    listener.onOrderClockFinished(clock);
                }
            } else {
                throw new RuntimeException("Unknown object type: " + o.getClass()
                    .getName());
            }
        } catch (JMSException e) {
            e.printStackTrace(System.err);
        }
    }
}
