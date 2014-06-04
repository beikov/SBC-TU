/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.model.Clock;
import java.util.Random;

/**
 *
 * @author Christian
 */
public class ClockConsumer implements Runnable {

    private final Random random = new Random();
    private final ClockList clockList;
    private final Runnable removedListener;
    private final Connector connector;

    public ClockConsumer(ClockList clockList, Runnable removedListener, Connector connector) {
        this.clockList = clockList;
        this.removedListener = removedListener;
        this.connector = connector;
    }

    private void sleepForSeconds(int from, int to) {
        try {
            // Wait an amount of seconds between from and to
            Thread.sleep(from * 1000 + random.nextInt((to - from) * 1000));
        } catch (InterruptedException ex) {
            // Do nothing here since we are just waiting artificially
        }
    }

    @Override
    public void run() {
        while (true) {
            sleepForSeconds(5, 10);
            Clock removedClock = clockList.removeAny();
            if (removedClock != null) {
                connector.removeClockFromStock(removedClock);
                removedListener.run();
            }
        }
    }
}
