package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public int getNeededClocksOfType(ClockType type) {
        Integer needed = neededClocks.get(type)[0];
        return (needed != null) ? needed : 0;
    }

    public boolean isClockTypeNeeded(ClockType type) {
        Integer needed = neededClocks.get(type)[0];
        return (needed != null) ? needed > 0 : false;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", neededClocks=" + neededClocks
            + ", clocks=" + clocks + ", priority=" + priority + "]";
    }

    public void addFinishedClock(Clock clock) {
        Integer[] needed = neededClocks.get(clock.getType());
        needed[1] += 1;
        neededClocks.put(clock.getType(), needed);
    }

}
