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
    private final ClockList clockList;

    public CollectingClockListener(ClockList clockList, Runnable listener) {
        this.clockList = clockList; 
        this.listener = listener;
    }

    @Override
    public void onClocksUpdated(List<Clock> clocks) {
        clockList.addClocks(clocks);
        listener.run();
    }
    
}
