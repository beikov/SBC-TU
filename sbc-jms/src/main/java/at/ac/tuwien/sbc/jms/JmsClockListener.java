package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * A JMS message listener that forwards objects messages for clocks to a {@link ClockListener}.
 */
public class JmsClockListener implements MessageListener {

    private final ClockListener listener;

    public JmsClockListener(ClockListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMessage mess = (ObjectMessage) message;
        try {
            Clock clock = (Clock) mess.getObject();
            listener.onClockUpdated(clock);
        } catch (JMSException e) {
            e.printStackTrace(System.err);
        }
    }

}
