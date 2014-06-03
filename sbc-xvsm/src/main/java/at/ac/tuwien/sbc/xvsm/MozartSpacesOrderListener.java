package at.ac.tuwien.sbc.xvsm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mozartspaces.core.Entry;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;

public class MozartSpacesOrderListener implements NotificationListener {

	private final OrderListener listener;

	public MozartSpacesOrderListener(OrderListener listener) {
		this.listener = listener;
	}

	@Override
	public void entryOperationFinished(Notification source,
			Operation operation, List<? extends Serializable> entries) {

		System.out.println("got notificiation in MozartSpacesOrderListener");

		if (operation != Operation.WRITE) {
			return;
		}

		List<Order> orders = new ArrayList<Order>(entries.size());
		List<Clock> clocks = new ArrayList<Clock>(entries.size());

		for (int i = 0; i < entries.size(); i++) {
			Object entry = entries.get(i);

			if ( ((Entry)entry).getValue() instanceof Order) {
				System.out.println("incance of order");
				orders.add((Order) ((Entry)entry).getValue());
			} else if ( ((Entry)entry).getValue() instanceof Clock ) {
				Clock clock = (Clock) ((Entry)entry).getValue();
				if(clock.getOrderId() != null){
					clocks.add((Clock) ((Entry)entry).getValue());
				}
			}
		}

		listener.onOrderAdded(orders);
		listener.onOrderClockFinished(clocks);

	}

}
