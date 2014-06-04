/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian
 */
public interface Connector {

    public Subscription subscribeForClockParts(ClockPartListener listener);

    public Subscription subscribeForClocks(ClockListener listener);

    public Subscription subscribeForOrders(OrderListener listener);

    public List<ClockPart> getClockParts();

    public List<Clock> getClocks();

    public List<Order> getOrders();

    public void addParts(List<ClockPart> parts);

    public void takeParts(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask);

    public void takeAssembled(TransactionalTask<Clock> transactionalTask);

    public boolean takeChecked(ClockQualityType type, long timeout, TransactionalTask<Clock> transactionalTask);

    public boolean takeSingleClockOrder(OrderPriority priority, TransactionalTask<SingleClockOrder> transactionalTask);

    public void addAssembledClock(Clock clock);

    public void addCheckedClock(Clock clock, ClockQualityType type);

    public void addDeliveredClock(Clock clock);

    public void addDisassembledClock(Clock clock);

    public void addOrder(Order order);

    // Distributer stuff
    public void deliverDemandedClock();

}
