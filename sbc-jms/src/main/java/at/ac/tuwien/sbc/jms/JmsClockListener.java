package at.ac.tuwien.sbc.jms;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;

public class JmsClockListener implements MessageListener {

	private ClockListener listener;
	
	public JmsClockListener( ClockListener listener ){
		this.listener = listener;
	}
	
	
	@Override
	public void onMessage(Message message) {
		ObjectMessage mess = (ObjectMessage) message;
		try {
			Clock c = (Clock) mess.getObject();
		
			List<Clock> clocks = new ArrayList<Clock>();
			clocks.add(c);
			listener.onClocksUpdated(clocks);
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
