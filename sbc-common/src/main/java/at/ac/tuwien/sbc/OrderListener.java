package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Order;
import java.util.List;

public interface OrderListener {

    public void onOrderAdded(List<Order> orders);

    public void onOrderClockFinished(List<Clock> clocks);
}
