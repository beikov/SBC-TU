package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An order for specific amounts of different clock types with a specific priority.
 */
public class Order implements Serializable {

    private final UUID id;
    private final Map<ClockType, Integer[]> neededClocks;
    private final List<Clock> clocks;
    private final OrderPriority priority;

    public Order(Map<ClockType, Integer[]> neededClocks, OrderPriority priority) {
        this.id = UUID.randomUUID();
        this.neededClocks = neededClocks;
        this.priority = priority;
        clocks = new ArrayList<Clock>();
    }

    public UUID getId() {
        return id;
    }

    public Map<ClockType, Integer[]> getNeededClocks() {
        return neededClocks;
    }

    public List<Clock> getClocks() {
        return clocks;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    /**
     * Returns the amount of clocks needed of the given type to fulfill this order.
     *
     * @param type the clock type to check
     * @return the amount of clocks needed
     */
    public int getNeededClocksOfType(ClockType type) {
        Integer needed = neededClocks.get(type)[0];
        return (needed != null) ? needed : 0;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", neededClocks=" + neededClocks
            + ", clocks=" + clocks + ", priority=" + priority + "]";
    }

    /**
     * Associates the given finished clock to this order.
     *
     * @param clock the finished clock to be associated to this order
     */
    public void addFinishedClock(Clock clock) {
        Integer[] needed = neededClocks.get(clock.getType());
        needed[1] += 1;
        neededClocks.put(clock.getType(), needed);
    }

}
