package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;

/**
 * A clock listener that adds clocks to a {@link ClockList} container and invokes a listener.
 */
public class DistributorClockListener implements ClockListener {

    private final Runnable listener;
    private final ClockList clockList;

    public DistributorClockListener(ClockList clockList, Runnable listener) {
        this.clockList = clockList;
        this.listener = listener;
    }

    @Override
    public void onClockUpdated(Clock clocks) {
        clockList.add(clocks);
        listener.run();
    }

}
