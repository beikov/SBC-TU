/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.ui;

import java.awt.EventQueue;

/**
 *
 * @author Christian
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
