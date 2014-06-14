package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.ClockPart;

/**
 * A listener to obtain state changes of clock parts.
 */
public interface ClockPartListener {

    /**
     * Is called every time a clock part is added.
     *
     * @param clockPart the added clock part
     */
    public void onClockPartAdded(ClockPart clockPart);

    /**
     * Is called every time a clock part are removed.
     *
     * @param clockPart the removed clock part
     */
    public void onClockPartRemoved(ClockPart clockPart);
}
