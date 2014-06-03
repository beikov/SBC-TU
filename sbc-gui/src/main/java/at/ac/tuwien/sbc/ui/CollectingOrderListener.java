package at.ac.tuwien.sbc.ui;

import java.util.List;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;


public class CollectingOrderListener implements OrderListener {

	private final Runnable listener;
	private OrderList orderList;

	public CollectingOrderListener(OrderList orderList, Runnable listener){
		this.orderList = orderList;
		this.listener = listener;
	}

	@Override
	public void onOrderAdded(List<Order> orders) {
		System.out.println("adding orders in: CollectingOrderListener");
		
		orderList.addAll(orders);
		listener.run();
	}

	@Override
	public void onOrderClockFinished(List<Clock> clocks) {
		for (Clock clock : clocks) {
			orderList.getOrder(clock.getOrderId()).addFinishedClock(clock);
		}
			
	}

}
