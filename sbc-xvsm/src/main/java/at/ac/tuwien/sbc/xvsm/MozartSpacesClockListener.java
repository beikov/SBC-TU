package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;
import java.io.Serializable;
import java.util.List;
import org.mozartspaces.core.Entry;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 * A MozartSpaces notification listener that forwards objects messages for clocks to a {@link ClockListener}.
 */
public class MozartSpacesClockListener implements NotificationListener {

    private final ClockListener listener;

    public MozartSpacesClockListener(ClockListener listener) {
        this.listener = listener;
    }

    @Override
    public void entryOperationFinished(Notification source, Operation operation, List<? extends Serializable> entries) {
        if (operation == Operation.WRITE) {
            for (int i = 0; i < entries.size(); i++) {
                Object entry = entries.get(i);

                if (entry instanceof ClassicClock) {
                    listener.onClockUpdated((Clock) entry);
                } else if (entry instanceof SportsClock) {
                    listener.onClockUpdated((Clock) entry);
                } else if (entry instanceof TimezoneSportsClock) {
                    listener.onClockUpdated((Clock) entry);
                } else {
                    listener.onClockUpdated((Clock) ((Entry) entry).getValue());
                }
            }
        }
    }

}
