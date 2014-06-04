package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;
import java.util.List;

public class CollectingOrderListener implements OrderListener {

    private final Runnable listener;
    private final OrderList orderList;

    public CollectingOrderListener(OrderList orderList, Runnable listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @Override
    public void onOrderAdded(List<Order> orders) {
        orderList.addAll(orders);
        listener.run();
    }

    @Override
    public void onOrderClockFinished(List<Clock> clocks) {
        for (Clock clock : clocks) {
            if (clock.getOrderId() != null) {
                orderList.getOrder(clock.getOrderId())
                    .addFinishedClock(clock);
            }
        }

    }

}
