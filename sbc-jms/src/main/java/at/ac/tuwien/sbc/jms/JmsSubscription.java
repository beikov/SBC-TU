package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.Subscription;
import java.util.Arrays;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

public class JmsSubscription implements Subscription{

    private final List<MessageConsumer> consumers;

    public JmsSubscription(MessageConsumer... consumers) {
        this.consumers = Arrays.asList(consumers);
    }
    
	@Override
	public void cancel() {
		for (MessageConsumer consumer : consumers) {
            try {
                consumer.setMessageListener(null);
            } catch (JMSException ex) {
                // Ignore
            }
        }
	}

}
