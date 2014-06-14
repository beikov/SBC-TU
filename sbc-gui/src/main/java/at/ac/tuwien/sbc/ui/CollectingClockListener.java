package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;

/**
 * A clock listener that adds clocks to a {@link ClockList} container and invokes a listener.
 */
public class CollectingClockListener implements ClockListener {

    private final Runnable listener;
    private final ClockList clockList;

    public CollectingClockListener(ClockList clockList, Runnable listener) {
        this.clockList = clockList;
        this.listener = listener;
    }

    @Override
    public void onClockUpdated(Clock clock) {
        clockList.addClocks(clock);
        listener.run();
    }

}
