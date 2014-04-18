/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockStatus;
import java.io.Serializable;
import java.util.List;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 *
 * @author Christian
 */
public class MozartSpacesClockListener implements NotificationListener {

    private final ClockListener listener;
    private final ClockStatus forStatus;

    public MozartSpacesClockListener(ClockListener listener, ClockStatus forStatus) {
        this.listener = listener;
        this.forStatus = forStatus;
    }

    @Override
    public void entryOperationFinished(Notification source, Operation operation, List<? extends Serializable> entries) {
        switch (forStatus) {
            case ASSEMBLED:
                if (operation == Operation.WRITE) {
                    for (Serializable entry : entries) {
                        listener.onClockAssembled((Clock) entry);
                    }
                }
                break;
            case CHECKED:
                if (operation == Operation.WRITE) {
                    for (Serializable entry : entries) {
                        listener.onClockAssembled((Clock) entry);
                    }
                }
                break;
            case DELIVERED:
                if (operation == Operation.WRITE) {
                    for (Serializable entry : entries) {
                        listener.onClockAssembled((Clock) entry);
                    }
                }
                break;
        }
    }

}
