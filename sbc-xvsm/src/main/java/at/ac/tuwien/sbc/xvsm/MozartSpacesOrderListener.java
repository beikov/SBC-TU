package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;
import java.io.Serializable;
import java.util.List;
import org.mozartspaces.core.Entry;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 * A MozartSpaces notification listener that forwards objects messages for orders to a {@link OrderListener}.
 */
public class MozartSpacesOrderListener implements NotificationListener {

    private final OrderListener listener;

    public MozartSpacesOrderListener(OrderListener listener) {
        this.listener = listener;
    }

    @Override
    public void entryOperationFinished(Notification source,
        Operation operation, List<? extends Serializable> entries) {
        if (operation != Operation.WRITE) {
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);

            if (((Entry) entry).getValue() instanceof Order) {
                listener.onOrderAdded((Order) ((Entry) entry).getValue());
            } else if (((Entry) entry).getValue() instanceof Clock) {
                Clock clock = (Clock) ((Entry) entry).getValue();
                if (clock.getOrderId() != null) {
                    listener.onOrderClockFinished((Clock) ((Entry) entry).getValue());
                }
            }
        }

    }

}
