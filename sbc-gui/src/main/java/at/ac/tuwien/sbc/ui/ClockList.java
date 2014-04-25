/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author Christian
 */
public class ClockList {
    
    private final ConcurrentSkipListMap<Clock, Clock> clocks = new ConcurrentSkipListMap<Clock, Clock>();
    
    public void addClocks(List<Clock> clocks) {
        for (Clock clock : clocks) {
            Clock clockToAdd = clock;
            
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
    }

    public List<Clock> getList() {
        return new ArrayList<Clock>(clocks.values());
    }
}
