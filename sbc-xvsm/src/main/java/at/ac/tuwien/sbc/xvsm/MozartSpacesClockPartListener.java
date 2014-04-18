/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import java.io.Serializable;
import java.util.List;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 *
 * @author Christian
 */
public class MozartSpacesClockPartListener implements NotificationListener {
    
    private final ClockPartListener listener;

    public MozartSpacesClockPartListener(ClockPartListener listener) {
        this.listener = listener;
    }

    @Override
    public void entryOperationFinished(Notification source, Operation operation, List<? extends Serializable> entries) {
        if (operation == Operation.WRITE) {
            for (Serializable entry : entries) {
                listener.onClockPartAdded((ClockPart) entry);
            }
        } else if (operation == Operation.TAKE || operation == Operation.DELETE) {
            for (Serializable entry : entries) {
                listener.onClockPartRemoved((ClockPart) entry);
            }
        }
    }
    
}
