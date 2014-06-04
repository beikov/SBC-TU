/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.model.ClockPart;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Christian
 */
public final class ClockPartCounter {

    private final AtomicInteger chassisCounter = new AtomicInteger();
    private final AtomicInteger leatherWristbandCounter = new AtomicInteger();
    private final AtomicInteger metalWristbandCounter = new AtomicInteger();
    private final AtomicInteger clockHandCounter = new AtomicInteger();
    private final AtomicInteger clockWorkCounter = new AtomicInteger();

    public void increment(Collection<ClockPart> clockParts) {
        for (ClockPart clockPart : clockParts) {
            switch (clockPart.getType()) {
                case LEDERARMBAND:
                    leatherWristbandCounter.incrementAndGet();
                    break;
                case GEHAEUSE:
                    chassisCounter.incrementAndGet();
                    break;
                case UHRWERK:
                    clockWorkCounter.incrementAndGet();
                    break;
                case ZEIGER:
                    clockHandCounter.incrementAndGet();
                    break;
                case METALLARMBAND:
                    metalWristbandCounter.incrementAndGet();
                    break;

            }
        }
    }

    public void decrement(Collection<ClockPart> clockParts) {
        for (ClockPart clockPart : clockParts) {
            switch (clockPart.getType()) {
                case LEDERARMBAND:
                    leatherWristbandCounter.decrementAndGet();
                    break;
                case GEHAEUSE:
                    chassisCounter.decrementAndGet();
                    break;
                case UHRWERK:
                    clockWorkCounter.decrementAndGet();
                    break;
                case ZEIGER:
                    clockHandCounter.decrementAndGet();
                    break;
                case METALLARMBAND:
                    metalWristbandCounter.decrementAndGet();
                    break;
            }
        }
    }

    public AtomicInteger getChassisCounter() {
        return chassisCounter;
    }

    public AtomicInteger getLeatherWristbandCounter() {
        return leatherWristbandCounter;
    }

    public AtomicInteger getMetalWristbandCounter() {
        return metalWristbandCounter;
    }

    public AtomicInteger getClockHandCounter() {
        return clockHandCounter;
    }

    public AtomicInteger getClockWorkCounter() {
        return clockWorkCounter;
    }
}
