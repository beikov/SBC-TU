package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.util.Map;

/**
 * A connector for distributor clients to manage their demands and stock.
 */
public interface DistributorConnector {

    /**
     * Lets the factory know what the current demand of the distributor is.
     *
     * @param demandPerType the demand of the distributer by type
     */
    public void setDemand(Map<ClockType, Integer> demandPerType);

    /**
     * Registers a listener for clock updates in the distributor stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForDistributorDeliveries(ClockListener listener);

    /**
     * Removes the given clock from the distributor stock.
     *
     * @param removedClock the clock to be removed.
     */
    public void removeClockFromStock(Clock removedClock);
}
