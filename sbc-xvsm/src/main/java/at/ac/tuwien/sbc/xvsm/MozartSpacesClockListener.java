/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mozartspaces.core.Entry;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;

/**
 *
 * @author Christian
 */
public class MozartSpacesClockListener implements NotificationListener {

    private final ClockListener listener;

    public MozartSpacesClockListener(ClockListener listener) {
        this.listener = listener;
    }

    @Override
    public void entryOperationFinished(Notification source, Operation operation, List<? extends Serializable> entries) {
        List<Clock> clocks = new ArrayList<Clock>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);

            if (entry instanceof ClassicClock) {
                clocks.add((ClassicClock) entry);
            } else if (entry instanceof SportsClock) {
                clocks.add((SportsClock) entry);
            } else if (entry instanceof TimezoneSportsClock){
                clocks.add((TimezoneSportsClock) entry);
            }else {
                clocks.add((Clock) ((Entry) entry).getValue());
            }
        }
        
        if (operation == Operation.WRITE) {
            listener.onClocksUpdated(clocks);
        }
    }

}
