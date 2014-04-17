/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian
 */
public interface Connector {
    
    public Subscription subscribeForClockParts(ClockPartListener listener);
    
    public Subscription subscribeForClocks(ClockListener listener);

    public void addParts(List<ClockPart> parts);

    public void takeTransactional(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask);

    public void addClock(Clock clock);
    
    public void addCheckedClock(Clock clock);
    
}
