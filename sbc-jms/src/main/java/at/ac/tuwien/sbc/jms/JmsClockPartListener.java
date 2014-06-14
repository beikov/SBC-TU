package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * A JMS message listener that forwards objects messages for clock parts to a {@link ClockPartListener}.
 */
public class JmsClockPartListener implements MessageListener {

    private final ClockPartListener listener;

    public JmsClockPartListener(ClockPartListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMessage mess = (ObjectMessage) message;
        try {
            ClockPart part = (ClockPart) mess.getObject();
            if (mess.getBooleanProperty(JmsConstants.CLOCK_PART_REMOVED)) {
                listener.onClockPartRemoved(part);
            } else {
                listener.onClockPartAdded(part);
            }
        } catch (JMSException e) {
            e.printStackTrace(System.err);
        }
    }

}
