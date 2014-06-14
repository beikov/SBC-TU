package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A clock part listener that increments or decrements counters for the respective clock part types in a {@link ClockPartCounter}.
 * By default this class first collects elements until {@link CountingClockPartListener#setCurrentClockParts(java.util.List) } is invoked. This is done to not loose elements in the two step process of registering a listener and the retrieving all elements.
 * The listener is only invoked on events that happen after switching from collect mode to count mode.
 */
public class CountingClockPartListener implements ClockPartListener {

    private final ClockPartCounter counter;
    private final Runnable listener;
    private final Set<ClockPart> collectedClockParts = new HashSet<ClockPart>();

    private volatile boolean collect = true;

    public CountingClockPartListener(ClockPartCounter counter, Runnable listener) {
        this.counter = counter;
        this.listener = listener;
    }

    @Override
    public void onClockPartAdded(ClockPart clockPart) {
        // Use double-checked locking
        boolean shouldCollect = collect;
        if (shouldCollect) {
            synchronized (collectedClockParts) {
                shouldCollect = collect;
                if (shouldCollect) {
                    // Just add elements, no need to invoke the listener
                    collectedClockParts.add(clockPart);
                }
            }
        }
        if (!shouldCollect) {
            // We are in count mode
            counter.increment(clockPart);
            listener.run();
        }
    }

    @Override
    public void onClockPartRemoved(ClockPart clockPart) {
        // Use double-checked locking
        boolean shouldCollect = collect;
        if (shouldCollect) {
            synchronized (collectedClockParts) {
                shouldCollect = collect;
                if (shouldCollect) {
                    // Just remove elements, no need to invoke the listener
                    collectedClockParts.remove(clockPart);
                }
            }
        }
        if (!shouldCollect) {
            // We are in count mode
            counter.decrement(clockPart);
            listener.run();
        }
    }

    void setCurrentClockParts(List<ClockPart> clockParts) {
        synchronized (collectedClockParts) {
            collectedClockParts.addAll(clockParts);
            for (ClockPart part : collectedClockParts) {
                counter.increment(part);
            }
            // Switch to count mode
            collect = false;
            collectedClockParts.clear();
            listener.run();
        }
    }

}
