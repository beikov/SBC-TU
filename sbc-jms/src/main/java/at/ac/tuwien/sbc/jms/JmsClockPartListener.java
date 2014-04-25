package at.ac.tuwien.sbc.jms;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;

public class JmsClockPartListener implements MessageListener{

	private ClockPartListener listener;

	public JmsClockPartListener( ClockPartListener listener ){
		this.listener = listener;
	}

	@Override
	public void onMessage(Message message) {
		ObjectMessage mess = (ObjectMessage) message;
		try {
			if(mess.getBooleanProperty("IS_REMOVED")){
				ClockPart part = (ClockPart) mess.getObject();
				List<ClockPart> parts = new ArrayList<ClockPart>();
				parts.add(part);
				listener.onClockPartsRemoved(parts);
			}else{
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
