/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.jms;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;

/**
 *
 * @author Christian
 */
public class JmsConnector implements Connector {


	private static final String CLOCKPART_TOPIC = "topic/clockpart";
	private static final String CLOCK_TOPIC = "topic/clock";

	private static final String CLOCKPART_QUEUE = "queue/clockpart";
	private static final String CLOCK_QUEUE = "queue/clock";

	private static final String IS_CHASSIS = "IS_CHASSIS";
	private static final String IS_CLOCKWORK = "IS_CLOCKWORK";
	private static final String IS_CLOCKHAND = "IS_CLOCKHAND";
	private static final String IS_WRISTBAND = "IS_WRISTBAND";
	private static final String IS_ASSEMBLED = "IS_ASSEMBLED";
	private static final String IS_HIGH_QUALITY = "IS_HIGH_QUALITY";
	private static final String IS_MED_QUALITY = "IS_MED_QUALITY";
	private static final String IS_LOW_QUALITY = "IS_LOW_QUALITY";
	private static final String IS_DELIVERED = "IS_DELIVERED";
	private static final String IS_DISASSEMBLED = "IS_DISASSEMBLED";


	private Topic clockPartTopic, clockTopic;
	private Queue clockPartQueue, clockQueue;

	private MessageProducer clockPartQueueProducer, clockQueueProducer, clockPartTopicProducer, clockTopicProducer;
	private MessageConsumer clockPartTopicConsumer, clockTopicConsumer;
	private MessageConsumer chassisConsumer, clockworkConsumer, clockhandConsumer, wristbandConsumer, assembledConsumer, highQualityConsumer, medQualityConsumer, lowQualityConsumer, disassembledConsumer, deliveredConsumer;

	private Session session;
	private Connection connection;

	private List<ClockPart> clockParts;
	private List<Clock> clocks;

	public JmsConnector(int port){

		connect(port);

	}

	private void connect(int port){
		try {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
			ActiveMQPrefetchPolicy policy = new ActiveMQPrefetchPolicy();
			policy.setQueuePrefetch(0); 
			connectionFactory.setPrefetchPolicy(policy);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Subscription subscribeForClockParts(ClockPartListener listener) {
		try {
			connectClockPartListener();
			JmsClockPartListener cpListener = new JmsClockPartListener(listener);
			clockPartTopicConsumer.setMessageListener( cpListener );
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subscription subscribeForClocks(ClockListener listener) {
		try{
			connectClockListener();
			JmsClockListener cListener = new JmsClockListener(listener);
			clockTopicConsumer.setMessageListener(cListener);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return null;
	}
	
	private void connectClockPartListener(){
		try {
			if(clockPartTopic == null)
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			if(clockPartTopicConsumer == null)
				clockPartTopicConsumer = session.createConsumer(clockPartTopic);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connectClockListener(){
		try {
			if(clockTopic == null)
				clockTopic = session.createTopic(CLOCK_TOPIC);
			if(clockTopicConsumer == null)
				clockTopicConsumer = session.createConsumer(clockTopic);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connectSupplier(){
		try{
			if(clockPartQueue == null)	
				clockPartQueue = session.createQueue(CLOCKPART_QUEUE);

			if(clockPartQueueProducer == null) 
				clockPartQueueProducer = session.createProducer(clockPartQueue);

			if(clockPartTopic == null) 
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			if(clockPartTopicProducer == null) 
				clockPartTopicProducer = session.createProducer(clockPartTopic);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connectAssembler(){
		try {
			if(clockPartQueue == null)
				clockPartQueue = session.createQueue(CLOCKPART_QUEUE);
			if(clockQueue == null)
				clockQueue = session.createQueue(CLOCK_QUEUE);
			
			if(clockTopic == null)
				clockTopic = session.createTopic(CLOCK_TOPIC);

			if(chassisConsumer == null)
				chassisConsumer = session.createConsumer(clockPartQueue, IS_CHASSIS+" = true");
			if(clockworkConsumer == null)
				clockworkConsumer = session.createConsumer(clockPartQueue, IS_CLOCKWORK+" = true");
			if(clockhandConsumer == null)
				clockhandConsumer = session.createConsumer(clockPartQueue, IS_CLOCKHAND+" = true");
			if(wristbandConsumer == null)
				wristbandConsumer = session.createConsumer(clockPartQueue, IS_WRISTBAND+" = true");

			if(clockQueueProducer == null)
				clockQueueProducer = session.createProducer(clockQueue);
			
			if(clockTopicProducer == null)
				clockTopicProducer = session.createProducer(clockTopic);
			
			if(clockPartTopic == null)
				clockPartTopic = session.createTopic(CLOCKPART_TOPIC);
			
			if(clockPartTopicProducer == null)
				clockPartTopicProducer = session.createProducer(clockPartTopic);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connectChecker(){
		connectSupplier();
		try {
			if(clockQueue == null)
				clockQueue = session.createQueue(CLOCK_QUEUE);

			if(assembledConsumer == null)
				assembledConsumer = session.createConsumer(clockQueue, IS_ASSEMBLED+" = true");

			if(clockQueueProducer == null)
				clockQueueProducer = session.createProducer(clockQueue);

			if(clockTopic == null)
				clockTopic = session.createTopic(CLOCK_TOPIC);
			
			if(clockTopicProducer == null)
				clockTopicProducer = session.createProducer(clockTopic);
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void connectDeliverer(){
		try {
			if(clockQueue == null)
				clockQueue = session.createQueue(CLOCK_QUEUE);
			if(clockTopic == null)
				clockTopic = session.createTopic(CLOCK_TOPIC);

			if(highQualityConsumer == null)
				highQualityConsumer = session.createConsumer(clockQueue, IS_HIGH_QUALITY+" = true");
			if(medQualityConsumer == null)
				medQualityConsumer = session.createConsumer(clockQueue, IS_MED_QUALITY+" = true");
			if(lowQualityConsumer == null)
				lowQualityConsumer = session.createConsumer(clockQueue, IS_LOW_QUALITY+" = true");

			if(clockTopicProducer == null)
				clockTopicProducer = session.createProducer(clockTopic);

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addParts(List<ClockPart> parts) {
		connectSupplier();
		try {
			for( ClockPart cp : parts ){
				ObjectMessage message = session.createObjectMessage(cp);
				switch (cp.getType()) {
				case GEHAEUSE:	message.setBooleanProperty(IS_CHASSIS, true);		break;
				case UHRWERK:	message.setBooleanProperty(IS_CLOCKWORK, true);		break;
				case ZEIGER:	message.setBooleanProperty(IS_CLOCKHAND, true);		break;
				case ARMBAND:	message.setBooleanProperty(IS_WRISTBAND, true);		break;
				}
				clockPartQueueProducer.send(message);
				clockPartTopicProducer.send(message);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void takeParts(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask) {
		connectAssembler();
		try {
			List<ClockPart> parts = new ArrayList<ClockPart>();
			for(ClockPartType t : neededClockParts.keySet()){
				ObjectMessage message = null;
				for(int i=0, upper = neededClockParts.get(t); i < upper ; i++){
					switch(t){
					case GEHAEUSE:	message = (ObjectMessage) chassisConsumer.receive();	break;
					case UHRWERK:	message = (ObjectMessage) clockworkConsumer.receive();	break;
					case ZEIGER:	message = (ObjectMessage) clockhandConsumer.receive();	break;
					case ARMBAND:	message = (ObjectMessage) wristbandConsumer.receive();	break;
					}
					ClockPart part = (ClockPart) message.getObject();
					parts.add( part );
					ObjectMessage msg = session.createObjectMessage(part);
					msg.setBooleanProperty("IS_REMOVED", true);
					clockPartTopicProducer.send(msg);

				}
			}
			transactionalTask.doWork(parts);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void takeAssembled(TransactionalTask<Clock> transactionalTask) {
		connectChecker();
		try {
			ObjectMessage message = (ObjectMessage) assembledConsumer.receive();
			transactionalTask.doWork( (Clock) message.getObject() );
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean takeChecked(ClockQualityType type, long timeout, TransactionalTask<Clock> transactionalTask) {
		connectDeliverer();
		try {
			MessageConsumer consumer = null;
			switch(type){
			case A:	consumer = highQualityConsumer;	break;
			case B:	consumer = medQualityConsumer;	break;
			case C:	consumer = lowQualityConsumer;	break;
			}
			ObjectMessage message = (ObjectMessage) consumer.receive(timeout);
			if(message == null){
				return true;
			}
			transactionalTask.doWork((Clock) message.getObject());
			return false;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return true;
	}

	@Override
	public void addAssembledClock(Clock clock) {
		connectAssembler();
		try {
			Message msg = session.createObjectMessage(clock);
			msg.setBooleanProperty(IS_ASSEMBLED, true);
			clockQueueProducer.send(msg);
			clockTopicProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void addCheckedClock(Clock clock, ClockQualityType type) {
		connectChecker();
		try {
			ObjectMessage message = session.createObjectMessage(clock);
			switch(type){
			case A:	message.setBooleanProperty(IS_HIGH_QUALITY, true);	break;
			case B:	message.setBooleanProperty(IS_MED_QUALITY, true);	break;
			case C:	message.setBooleanProperty(IS_LOW_QUALITY, true);	break;
			}
			clockQueueProducer.send(message);
			clockTopicProducer.send(message);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void addDeliveredClock(Clock clock) {
		connectDeliverer();
		try {
			Message msg = session.createObjectMessage(clock);
			msg.setBooleanProperty(IS_DELIVERED, true);
			clockTopicProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addDisassembledClock(Clock clock) {
		connectDeliverer();
		try {
			Message msg = session.createObjectMessage(clock);
			msg.setBooleanProperty(IS_DISASSEMBLED, true);
			clockTopicProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public List<ClockPart> getClockParts() {
		//		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		if(clockParts == null){
			clockParts = new ArrayList<ClockPart>();
		}
		return clockParts;
	}

	@Override
	public List<Clock> getClocks() {
		//		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		if(clocks == null){
			clocks = new ArrayList<Clock>();
		}
		return clocks;
	}




}