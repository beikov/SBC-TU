package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClockList {

    private final Queue<Clock> clocks;
    private final AtomicInteger classicClocks;
    private final AtomicInteger sportsClocks;
    private final AtomicInteger timezoneClocks;
    private final AtomicInteger orderedClassicClocks;
    private final AtomicInteger orderedSportsClocks;
    private final AtomicInteger orderedTimezoneClocks;

    public ClockList() {
        clocks = new ConcurrentLinkedQueue<Clock>();
        classicClocks = new AtomicInteger(0);
        sportsClocks = new AtomicInteger(0);
        timezoneClocks = new AtomicInteger(0);
        orderedClassicClocks = new AtomicInteger(0);
        orderedSportsClocks = new AtomicInteger(0);
        orderedTimezoneClocks = new AtomicInteger(0);
    }

    public List<Clock> getClocks() {
        return new ArrayList<Clock>(clocks);
    }

    public void addAll(List<Clock> clocks) {
        for (Clock clock : clocks) {
            switch (clock.getType()) {
                case KLASSISCH:
                    classicClocks.incrementAndGet();
                    orderedClassicClocks.decrementAndGet();
                    break;
                case SPORT:
                    sportsClocks.incrementAndGet();
                    orderedSportsClocks.decrementAndGet();
                    break;
                case ZEITZONEN_SPORT:
                    timezoneClocks.incrementAndGet();
                    orderedTimezoneClocks.decrementAndGet();
                    break;
            }
        }
        this.clocks.addAll(clocks);
    }

    public Clock removeAny() {
        Clock removedClock = clocks.poll();

        if (removedClock == null) {
            return removedClock;
        }

        switch (removedClock.getType()) {
            case KLASSISCH:
                classicClocks.decrementAndGet();
                break;
            case SPORT:
                sportsClocks.decrementAndGet();
                break;
            case ZEITZONEN_SPORT:
                timezoneClocks.decrementAndGet();
                break;
        }

        return removedClock;
    }

    public int getClockCount(ClockType type) {
        switch (type) {
            case KLASSISCH:
                return classicClocks.get();
            case SPORT:
                return sportsClocks.get();
            case ZEITZONEN_SPORT:
                return timezoneClocks.get();
        }
        return 0;
    }

    public int getDemandCount(ClockType type) {
        switch (type) {
            case KLASSISCH:
                return orderedClassicClocks.get() - classicClocks.get();
            case SPORT:
                return orderedSportsClocks.get() - sportsClocks.get();
            case ZEITZONEN_SPORT:
                return orderedTimezoneClocks.get() - timezoneClocks.get();
        }
        return 0;
    }

    public void setOrderCount(ClockType type, int value) {
        switch (type) {
            case KLASSISCH:
                orderedClassicClocks.set(value);
            case SPORT:
                orderedSportsClocks.set(value);
            case ZEITZONEN_SPORT:
                orderedTimezoneClocks.set(value);
        }
    }

    public int getOrderCount(ClockType type) {
        switch (type) {
            case KLASSISCH:
                return orderedClassicClocks.get();
            case SPORT:
                return orderedSportsClocks.get();
            case ZEITZONEN_SPORT:
                return orderedTimezoneClocks.get();
        }
        return 0;
    }
}
