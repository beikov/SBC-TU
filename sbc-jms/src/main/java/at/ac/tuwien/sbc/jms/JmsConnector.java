/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.jms;



import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
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


	private static String CHASSIS_QUEUE = "queue/chassis";
	private static String CLOCKWORK_QUEUE = "queue/clockwork";
	private static String CLOCKHAND_QUEUE = "queue/clockhand";
	private static String WRISTBAND_QUEUE = "queue/wristband";
	private static String ASSEMBLED_QUEUE = "queue/assembled";
	private static String HIGH_QUALITY_QUEUE = "queue/high_qual";
	private static String MED_QUALITY_QUEUE = "queue/med_qual";
	private static String LOW_QUALITY_QUEUE = "queue/low_qual";
	private static String DELIVERED_QUEUE = "queue/delivered";

	private Topic chassisTopic, clockworkTopic, clockhandTopic, wristbandTopic, assembledTopic, highQualityTopic, medQualityTopic, lowQualityTopic, deliveredTopic;
	private MessageProducer chassisProducer, clockworkProducer, clockhandProducer, wristbandProducer, assembledProducer, highQualityProducer, medQualityProducer, lowQualityProducer, deliveredProducer;
	private MessageConsumer chassisConsumer, clockworkConsumer, clockhandConsumer, wristbandConsumer, assembledConsumer, highQualityConsumer, medQualityConsumer, lowQualityConsumer;

	private Session session;
	private Connection connection;


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

			// create topics
			chassisTopic = session.createTopic(CHASSIS_QUEUE);
			clockworkTopic = session.createTopic(CLOCKWORK_QUEUE);
			clockhandTopic = session.createTopic(CLOCKHAND_QUEUE);
			wristbandTopic = session.createTopic(WRISTBAND_QUEUE);
			assembledTopic = session.createTopic(ASSEMBLED_QUEUE);
			highQualityTopic = session.createTopic(HIGH_QUALITY_QUEUE);
			medQualityTopic = session.createTopic(MED_QUALITY_QUEUE);
			lowQualityTopic = session.createTopic(LOW_QUALITY_QUEUE);
			deliveredTopic = session.createTopic(DELIVERED_QUEUE);

			// create producers
			chassisProducer = session.createProducer(chassisTopic);
			clockworkProducer = session.createProducer(clockworkTopic);
			clockhandProducer = session.createProducer(clockhandTopic);
			wristbandProducer = session.createProducer(wristbandTopic);
			assembledProducer = session.createProducer(assembledTopic);
			highQualityProducer = session.createProducer(highQualityTopic);
			medQualityProducer = session.createProducer(medQualityTopic);
			lowQualityProducer = session.createProducer(lowQualityTopic);
			deliveredProducer = session.createProducer(deliveredTopic);


			// create consumers
			chassisConsumer = session.createConsumer(chassisTopic );
			clockworkConsumer = session.createConsumer(clockworkTopic );
			clockhandConsumer = session.createConsumer(clockhandTopic );
			wristbandConsumer = session.createConsumer(wristbandTopic );
			assembledConsumer = session.createConsumer(assembledTopic);
			highQualityConsumer = session.createConsumer(highQualityTopic);
			medQualityConsumer = session.createConsumer(medQualityTopic );
			lowQualityConsumer = session.createConsumer(lowQualityTopic );


		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Subscription subscribeForClockParts(ClockPartListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subscription subscribeForClocks(ClockListener listener) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void addParts(List<ClockPart> parts) {
		try {
			for( ClockPart cp : parts ){
				switch (cp.getType()) {
				case GEHAEUSE:	chassisProducer.send(session.createObjectMessage(cp));		break;
				case UHRWERK:	clockworkProducer.send(session.createObjectMessage(cp));	break;
				case ZEIGER:	clockhandProducer.send(session.createObjectMessage(cp));	break;
				case ARMBAND:	wristbandProducer.send(session.createObjectMessage(cp));	break;
				}
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void takeParts(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask) {
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
					parts.add( (ClockPart) message.getObject() );
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
		try {
			Message msg = session.createObjectMessage(clock);
			assembledProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void addCheckedClock(Clock clock, ClockQualityType type) {
		try {
			MessageProducer producer = null;
			switch(type){
			case A:	producer = highQualityProducer;	break;
			case B:	producer = medQualityProducer;	break;
			case C:	producer = lowQualityProducer;	break;
			}
			Message msg = session.createObjectMessage(clock);
			producer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void addDeliveredClock(Clock clock) {
		try {
			Message msg = session.createObjectMessage(clock);
			deliveredProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



}
