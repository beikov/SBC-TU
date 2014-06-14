package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import java.io.Serializable;
import java.util.List;
import org.mozartspaces.core.Entry;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 * A MozartSpaces notification listener that forwards objects messages for clock parts to a {@link ClockPartListener}.
 */
public class MozartSpacesClockPartListener implements NotificationListener {

    private final ClockPartListener listener;

    public MozartSpacesClockPartListener(ClockPartListener listener) {
        this.listener = listener;
    }

    @Override
    public void entryOperationFinished(Notification source, Operation operation, List<? extends Serializable> entries) {
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            ClockPart part;

            if (entry instanceof ClockPart) {
                part = (ClockPart) entry;
            } else {
                part = (ClockPart) ((Entry) entry).getValue();
            }

            if (operation == Operation.WRITE) {
                listener.onClockPartAdded(part);
            } else if (operation == Operation.TAKE || operation == Operation.DELETE) {
                listener.onClockPartRemoved(part);
            }
        }

    }

}
