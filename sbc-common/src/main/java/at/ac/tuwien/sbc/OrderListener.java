package at.ac.tuwien.sbc;

import java.util.List;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;

public interface OrderListener {

	public void onOrderAdded(List<Order> orders);

	public void onOrderClockFinished(List<Clock> clocks);
}
