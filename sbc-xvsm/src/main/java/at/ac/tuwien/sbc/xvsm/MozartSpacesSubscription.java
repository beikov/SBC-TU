package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.Subscription;
import java.util.List;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.notifications.Notification;

/**
 * A MozartSpaces implementation of a subscription.
 */
public class MozartSpacesSubscription implements Subscription {

    private final List<Notification> notifications;

    public MozartSpacesSubscription(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public void cancel() {
        for (Notification notification : notifications) {
            try {
                notification.destroy();
            } catch (MzsCoreException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
