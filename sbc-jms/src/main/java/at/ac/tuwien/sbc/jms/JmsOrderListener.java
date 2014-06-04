/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;
import java.io.Serializable;
import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 *
 * @author Christian
 */
public class JmsOrderListener implements MessageListener {

    private final OrderListener listener;

    public JmsOrderListener(OrderListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(Message message) {
        ObjectMessage msg = (ObjectMessage) message;
        try {
            Serializable o = msg.getObject();

            if (o instanceof Order) {
                listener.onOrderAdded(Arrays.asList((Order) o));
            } else if (o instanceof Clock) {
                Clock clock = (Clock) o;
                if (clock.getOrderId() != null) {
                    listener.onOrderClockFinished(Arrays.asList(clock));
                }
            } else {
                throw new RuntimeException("Unknown object type: " + o.getClass()
                    .getName());
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
