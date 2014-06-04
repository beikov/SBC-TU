/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.model.ClockPart;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Christian
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
    public void onClockPartsAdded(List<ClockPart> clockParts) {
        boolean shouldCollect = collect;
        if (shouldCollect) {
            synchronized (collectedClockParts) {
                shouldCollect = collect;
                if (shouldCollect) {
                    collectedClockParts.addAll(clockParts);
                }
            }
        }
        if (!shouldCollect) {
            counter.increment(clockParts);
            listener.run();
        }
    }

    @Override
    public void onClockPartsRemoved(List<ClockPart> clockParts) {
        boolean shouldCollect = collect;
        if (shouldCollect) {
            synchronized (collectedClockParts) {
                shouldCollect = collect;
                if (shouldCollect) {
                    collectedClockParts.removeAll(clockParts);
                }
            }
        }
        if (!shouldCollect) {
            counter.decrement(clockParts);
            listener.run();
        }
    }

    void setCurrentClockParts(List<ClockPart> clockParts) {
        synchronized (collectedClockParts) {
            collectedClockParts.addAll(clockParts);
            counter.increment(collectedClockParts);
            collect = false;
            listener.run();
        }
    }

}
