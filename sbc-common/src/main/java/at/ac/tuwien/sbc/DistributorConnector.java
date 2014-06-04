/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.util.Map;

/**
 *
 * @author Christian
 */
public interface DistributorConnector {

    public void setDemand(Map<ClockType, Integer> demandPerType);

    public Subscription subscribeForDistributorDeliveries(ClockListener listener);

    public void removeClockFromStock(Clock removedClock);
}
