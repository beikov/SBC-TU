package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;
import java.util.List;

public class DistributorClockListener implements ClockListener {

    private final Runnable listener;
    private ClockList clockList;

    public DistributorClockListener(ClockList clockList, Runnable listener) {
        this.clockList = clockList;
        this.listener = listener;
    }

    @Override
    public void onClocksUpdated(List<Clock> clocks) {
        clockList.addAll(clocks);
        listener.run();
    }

}
