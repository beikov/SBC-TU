package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.DistributorConnector;
import at.ac.tuwien.sbc.model.Clock;
import java.util.Random;

/**
 * An artificial consumer that consumes any clock every 5 to 10 seconds from the distributor stock.
 */
public class ClockConsumer implements Runnable {

    private final Random random = new Random();
    private final ClockList clockList;
    private final Runnable removedListener;
    private final DistributorConnector connector;

    public ClockConsumer(ClockList clockList, Runnable removedListener, DistributorConnector connector) {
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
