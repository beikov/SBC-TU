package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;

/**
 * A listener to obtain state changes of orders.
 */
public interface OrderListener {

    /**
     * Is called every time an order is added.
     *
     * @param order the added order
     */
    public void onOrderAdded(Order order);

    /**
     * Is called for every clock, which is part of an order, is assembled.
     *
     * @param clock the finished clock
     */
    public void onOrderClockFinished(Clock clock);
}
