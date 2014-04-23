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
import javax.jms.ConnectionFactory;

/**
 *
 * @author Christian
 */
public class JmsConnector implements Connector {

	private static final String CHASSIS_QUEUE = "queue/chassis";
	private static final String CLOCKWORK_QUEUE = "queue/clockwork";
	private static final String CLOCKHAND_QUEUE = "queue/clockhand";
	private static final String WRISTBAND_QUEUE = "queue/wristband";
	private static final String ASSEMBLED_QUEUE = "queue/assembled";
	private static final String HIGH_QUALITY_QUEUE = "queue/high_qual";
	private static final String MED_QUALITY_QUEUE = "queue/med_qual";
	private static final String LOW_QUALITY_QUEUE = "queue/low_qual";
	private static final String DELIVERED_QUEUE = "queue/delivered";

	private final ConnectionFactory connectionFactory;
	private final Session session;
	private final Connection connection;

	public JmsConnector(int port) {
        this.connectionFactory = new ActiveMQConnectionFactory("vm://localhost:" + port);
        
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        }
	}

	private MessageProducer initAndGetProducer(String queue){
		try {
			MessageProducer producer =  session.createProducer(session.createQueue(queue));
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			return producer;
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	private MessageConsumer initAndGetConsumer(String queue){
		try {
			Destination dest =  session.createQueue(queue);
			return session.createConsumer(dest);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
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
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void close(MessageProducer producer){
		try{
			producer.close();
			close();
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
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
				MessageProducer producer = null;
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
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
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
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
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
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
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
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}	
	}

	@Override
	public void addAssembledClock(Clock clock) {
		try {
			MessageProducer producer = initAndGetProducer(ASSEMBLED_QUEUE);
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}

	}

	@Override
	public void addCheckedClock(Clock clock, ClockQualityType type) {
		try {
			MessageProducer producer = null;
			switch(type){
			case A:	producer = initAndGetProducer(HIGH_QUALITY_QUEUE);	break;
			case B:	producer = initAndGetProducer(MED_QUALITY_QUEUE);	break;
			case C:	producer = initAndGetProducer(LOW_QUALITY_QUEUE);	break;
			}
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}	
	}

	@Override
	public void addDeliveredClock(Clock clock) {
		try {
			MessageProducer producer = initAndGetProducer(DELIVERED_QUEUE);
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
			close(producer);
		} catch (JMSException ex) {
			throw new RuntimeException(ex);
		}

	}


}
