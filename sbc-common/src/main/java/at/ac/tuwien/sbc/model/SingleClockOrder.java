package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.UUID;

public class SingleClockOrder implements Serializable {

    private final UUID orderId;
    private final ClockType neededType;
    private final OrderPriority priority;

    public SingleClockOrder(UUID orderId, ClockType neededType, OrderPriority priority) {
        this.orderId = orderId;
        this.neededType = neededType;
        this.priority = priority;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public ClockType getNeededType() {
        return neededType;
    }

    public OrderPriority getPriority() {
        return priority;
    }

	@Override
	public String toString() {
		return "SingleClockOrder [orderId=" + orderId + ", neededType="
				+ neededType + ", priority=" + priority + "]";
	}
    
    

}
