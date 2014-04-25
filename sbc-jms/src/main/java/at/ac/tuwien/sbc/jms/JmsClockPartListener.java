package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import java.util.ArrayList;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

public class JmsClockPartListener implements MessageListener {

    private final ClockPartListener listener;

    public JmsClockPartListener(ClockPartListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMessage mess = (ObjectMessage) message;
        try {
            if (mess.getBooleanProperty("IS_REMOVED")) {
                ClockPart part = (ClockPart) mess.getObject();
                List<ClockPart> parts = new ArrayList<ClockPart>();
                parts.add(part);
                listener.onClockPartsRemoved(parts);
            } else {
                ClockPart part = (ClockPart) mess.getObject();
                List<ClockPart> parts = new ArrayList<ClockPart>();
                parts.add(part);
                listener.onClockPartsAdded(parts);
            }
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
