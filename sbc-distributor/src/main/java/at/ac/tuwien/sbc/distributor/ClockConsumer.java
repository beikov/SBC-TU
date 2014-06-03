/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.distributor;

import java.util.Random;

/**
 *
 * @author Christian
 */
public class ClockConsumer implements Runnable {

    private final Random random = new Random();
    private final ClockList clockList;
    private final Runnable removedListener;

    public ClockConsumer(ClockList clockList, Runnable removedListener) {
        this.clockList = clockList;
        this.removedListener = removedListener;
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
            if (clockList.removeAny()) {
                removedListener.run();
            }
        }
    }
}
