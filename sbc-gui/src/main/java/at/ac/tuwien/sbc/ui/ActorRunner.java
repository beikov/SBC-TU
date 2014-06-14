package at.ac.tuwien.sbc.ui;

import java.awt.EventQueue;

/**
 * A simple wrapper that adds a callback to the Swing event queue after a target has finished.
 */
public class ActorRunner implements Runnable {

    private final Runnable target;
    private final Runnable callback;

    public ActorRunner(Runnable target, Runnable callback) {
        this.target = target;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            target.run();
        } finally {
            EventQueue.invokeLater(callback);
        }
    }

}
