/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author Christian
 */
public class CollectingClockListener implements ClockListener {
    
    private final Runnable listener;
    private final ConcurrentSkipListMap<Clock, Clock> clocks = new ConcurrentSkipListMap<Clock, Clock>();

    public CollectingClockListener(Runnable listener) {
        this.listener = listener;
    }

    @Override
    public void onClocksUpdated(List<Clock> clocks) {
        for (Clock clock : clocks) {
            Clock clockToAdd = clock;
            
            // We do this to keep only the lastest clock
            // We know that only 2 threads can actually mutate the clocks map
            // where one thread only will access it once, so actually we could simplify this.
            // Still we chose to make it general for eventual further change
            while (clockToAdd != null) {
                Clock oldClock = this.clocks.put(clockToAdd, clockToAdd);

                if (oldClock != null && oldClock.isNewer(clockToAdd)) {
                    clockToAdd = oldClock;
                } else {
                    clockToAdd = null;
                }
            }
        }
        
        listener.run();
    }
    
}
