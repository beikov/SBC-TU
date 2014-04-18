/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.jms;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian
 */
public class JmsConnector implements Connector {
    
    public JmsConnector(int port) {
        
    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Subscription subscribeForClocks(ClockListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addParts(List<ClockPart> parts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void takeTransactional(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addClock(Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addCheckedClock(Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
