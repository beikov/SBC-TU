package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A container for clocks that also groups clocks by their clock type.
 */
public class ClockList {

    private final ConcurrentSkipListMap<Clock, Clock> clocks = new ConcurrentSkipListMap<Clock, Clock>();
    private final ConcurrentSkipListMap<Clock, Clock> assembledClocks = new ConcurrentSkipListMap<Clock, Clock>();
    private final ConcurrentSkipListMap<Clock, Clock> checkedClocks = new ConcurrentSkipListMap<Clock, Clock>();
    private final ConcurrentSkipListMap<Clock, Clock> deliveredClocks = new ConcurrentSkipListMap<Clock, Clock>();
    private final ConcurrentSkipListMap<Clock, Clock> disassembledClocks = new ConcurrentSkipListMap<Clock, Clock>();

    public void addClocks(Clock clock) {
        Clock clockToAdd = clock;

        switch (clock.getStatus()) {
            case DISASSEMBLED:
                disassembledClocks.put(clock, clock);
                checkedClocks.put(clock, clock);
                assembledClocks.put(clock, clock);
                break;
            case DELIVERED:
                deliveredClocks.put(clock, clock);
                checkedClocks.put(clock, clock);
                assembledClocks.put(clock, clock);
                break;
            case CHECKED:
                checkedClocks.put(clock, clock);
                assembledClocks.put(clock, clock);
                break;
            case ASSEMBLED:
                assembledClocks.put(clock, clock);
                break;

        }

        // We do this to keep only the lastest clock
        // We know that only 2 threads can actually mutate the clocks map
        // where one thread only will access it once, so actually we could simplify this.
        // Still we chose to make it general for eventual further change
        while (clockToAdd != null) {
            Clock oldClock = this.clocks.remove(clockToAdd);

            if (oldClock != null && oldClock.isNewer(clockToAdd)) {
                clockToAdd = oldClock;
            } else {
                oldClock = this.clocks.putIfAbsent(clockToAdd, clockToAdd);

                if (oldClock != null && oldClock.isNewer(clockToAdd)) {
                    clockToAdd = oldClock;
                } else {
                    clockToAdd = null;
                }
            }
        }
    }

    public List<Clock> getList() {
        return new ArrayList<Clock>(clocks.values());
    }

    public List<Clock> getAssembledClocks() {
        return new ArrayList<Clock>(assembledClocks.values());
    }

    public List<Clock> getCheckedClocks() {
        return new ArrayList<Clock>(checkedClocks.values());
    }

    public List<Clock> getDeliveredClocks() {
        return new ArrayList<Clock>(deliveredClocks.values());
    }

    public List<Clock> getDisassembledClocks() {
        return new ArrayList<Clock>(disassembledClocks.values());
    }
}
