/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author Christian
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
    
    protected void sleepForSeconds(int from, int to) {
        try {
            // Wait an amount of seconds between from and to
            Thread.sleep(from * 1000 + random.get().nextInt((to - from) * 1000));
        } catch (InterruptedException ex) {
            // Do nothing here since we are just waiting artificially
        }
    }
}
