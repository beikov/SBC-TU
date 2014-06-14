package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;

/**
 * A listener to obtain state changes of clocks.
 */
public interface ClockListener {

    /**
     * Is called every time a clock is updated.
     *
     * @param clock the updated clock
     */
    public void onClockUpdated(Clock clock);
}
