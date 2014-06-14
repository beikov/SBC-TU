package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;

/**
 * An order listener that adds orders to a {@link OrderList} or updated orders from that container and invokes a listener.
 */
public class CollectingOrderListener implements OrderListener {

    private final Runnable listener;
    private final OrderList orderList;

    public CollectingOrderListener(OrderList orderList, Runnable listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @Override
    public void onOrderAdded(Order order) {
        orderList.addAll(order);
        listener.run();
    }

    @Override
    public void onOrderClockFinished(Clock clock) {
        if (clock.getOrderId() != null) {
            orderList.getOrder(clock.getOrderId())
                .addFinishedClock(clock);
        }
        listener.run();

    }

}
