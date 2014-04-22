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
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

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

	private static String CHASSIS_QUEUE = "queue/chassis";
	private static String CLOCKWORK_QUEUE = "queue/clockwork";
	private static String CLOCKHAND_QUEUE = "queue/clockhand";
	private static String WRISTBAND_QUEUE = "queue/wristband";
	private static String ASSEMBLED_QUEUE = "queue/assembled";
	private static String HIGH_QUALITY_QUEUE = "queue/high_qual";
	private static String MED_QUALITY_QUEUE = "queue/med_qual";
	private static String LOW_QUALITY_QUEUE = "queue/low_qual";
	private static String DELIVERED_QUEUE = "queue/delivered";

	private QueueSession session;
	private QueueConnection connection;

	private int port;

	public JmsConnector(int port) {
		this.port = port;
	}

	private QueueSender initAndGetProducer(String queue){
		try {
			init();

			QueueSender producer =  session.createSender(session.createQueue(queue));
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			return producer;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private MessageConsumer initAndGetConsumer(String queue){
		try {
			init();

			Destination dest =  session.createQueue(queue);

			return session.createConsumer(dest);

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void init() throws JMSException{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost"+port);
		connection = connectionFactory.createQueueConnection();
		connection.start();
		session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	private void close() throws JMSException{
		session.close();
		connection.stop();
		connection.close();
	}

	public void close(MessageConsumer consumer){
		try {
			consumer.close();
			close();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close(QueueSender producer){
		try{
			producer.close();
			close();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Subscription subscribeForClockParts(ClockPartListener listener) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Subscription subscribeForClocks(ClockListener listener) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addParts(List<ClockPart> parts) {
		try {
			for( ClockPart cp : parts ){
				QueueSender producer = null;
				switch (cp.getType()) {
				case GEHAEUSE:	producer = initAndGetProducer(CHASSIS_QUEUE);		break;
				case UHRWERK:	producer = initAndGetProducer(CLOCKWORK_QUEUE);		break;
				case ZEIGER:	producer = initAndGetProducer(CLOCKHAND_QUEUE);		break;
				case ARMBAND:	producer = initAndGetProducer(WRISTBAND_QUEUE);		break;
				}
				Message msg = session.createObjectMessage(cp);
				producer.send(msg);
				close(producer);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}

	@Override
	public void takeParts(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask) {
		try {
			int neededParts = 0;
			List<ClockPart> parts = new ArrayList<ClockPart>();
			for(ClockPartType t : neededClockParts.keySet()){
				MessageConsumer consumer = null;
				switch(t){
				case GEHAEUSE:	consumer = initAndGetConsumer(CHASSIS_QUEUE);	break;
				case UHRWERK:	consumer = initAndGetConsumer(CLOCKWORK_QUEUE);	break;
				case ZEIGER:	consumer = initAndGetConsumer(CLOCKHAND_QUEUE);	break;
				case ARMBAND:	consumer = initAndGetConsumer(WRISTBAND_QUEUE);	break;
				}
				for(int i=0, upper = neededClockParts.get(t); i < upper ; i++){
					neededParts++;
					ObjectMessage message = (ObjectMessage) consumer.receive(100);
					if(message != null){
						parts.add( (ClockPart) message.getObject() );
					}
				}
				close(consumer);
			}
			if(parts.size() == neededParts){
				transactionalTask.doWork(parts);
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void takeAssembled(TransactionalTask<Clock> transactionalTask) {
		try {
			MessageConsumer consumer = initAndGetConsumer(ASSEMBLED_QUEUE);
			ObjectMessage message = (ObjectMessage) consumer.receiveNoWait();
			close(consumer);
			if( message != null ){
				transactionalTask.doWork( (Clock) message.getObject() );
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean takeChecked(ClockQualityType type, long timeout, TransactionalTask<Clock> transactionalTask) {
		try {
			MessageConsumer consumer = null;
			switch(type){
			case A:	consumer = initAndGetConsumer(HIGH_QUALITY_QUEUE);	break;
			case B:	consumer = initAndGetConsumer(MED_QUALITY_QUEUE);	break;
			case C:	consumer = initAndGetConsumer(LOW_QUALITY_QUEUE);	break;
			}
			ObjectMessage message = (ObjectMessage) consumer.receive(timeout);
			close(consumer);
			if(message == null){
				return false;
			}
			transactionalTask.doWork((Clock) message.getObject());
			return true;
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return false;
	}

	@Override
	public void addAssembledClock(Clock clock) {
		try {
			QueueSender producer = initAndGetProducer(ASSEMBLED_QUEUE);
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void addCheckedClock(Clock clock, ClockQualityType type) {
		try {
			QueueSender producer = null;
			switch(type){
			case A:	producer = initAndGetProducer(HIGH_QUALITY_QUEUE);	break;
			case B:	producer = initAndGetProducer(MED_QUALITY_QUEUE);	break;
			case C:	producer = initAndGetProducer(LOW_QUALITY_QUEUE);	break;
			}
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void addDeliveredClock(Clock clock) {
		try {
			QueueSender producer = initAndGetProducer(DELIVERED_QUEUE);
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
