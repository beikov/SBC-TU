package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import java.util.Random;
import java.util.UUID;

/**
 * An abstract actor that has an id and offers a nice sleep method.
 */
public abstract class AbstractActor implements Runnable {

    protected static final ThreadLocal<Random> random = new ThreadLocal<Random>() {

        @Override
        protected Random initialValue() {
            return new Random();
        }

    };

    protected final UUID id;
    protected final Connector connector;

    public AbstractActor(Connector connector) {
        this.id = UUID.randomUUID();
        this.connector = connector;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Sleeps for a time between <code>from</code> and <code>to</code> seconds.
     *
     * @param from minimum time to sleep in seconds
     * @param to   maximum time to sleep in seconds
     */
    protected void sleepForSeconds(int from, int to) {
        try {
            // Wait an amount of seconds between from and to
            Thread.sleep(from * 1000 + random.get()
                .nextInt((to - from) * 1000));
        } catch (InterruptedException ex) {
            // Do nothing here since we are just waiting artificially
        }
    }
}
