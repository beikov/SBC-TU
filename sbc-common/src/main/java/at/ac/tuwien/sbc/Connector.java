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
import java.util.UUID;

/**
 * A connector for the factory clients to manage clock parts, clocks, orders and distributor deliveries.
 */
public interface Connector {

    /**
     * Registers a listener for clock part updates in the factory stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForClockParts(ClockPartListener listener);

    /**
     * Registers a listener for clock updates in the factory stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForClocks(ClockListener listener);

    /**
     * Registers a listener for order updates in the factory stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForOrders(OrderListener listener);

    /**
     * Returns a snapshot of the currently available clock parts in the stock.
     *
     * @return a snapshot of the clock parts
     */
    public List<ClockPart> getClockParts();

    /**
     * Returns a snapshot of the currently available clocks in the stock.
     *
     * @return a snapshot of the clock parts
     */
    public List<Clock> getClocks();

    /**
     * Returns all orders that were made in this factory.
     *
     * @return all orders
     */
    public List<Order> getOrders();

    /**
     * Adds the given parts to the stock of the factory.
     *
     * @param parts the parts to be added
     */
    public void addParts(List<ClockPart> parts);

    /**
     * Takes the needed clock parts as given by <code>neededClockParts</code> from the factory stock if possible and invokes the given transactional task with the parts as argument.
     * The take operations and the transactional task share the same transaction, resulting in a rollback if any operation fails.
     *
     * @param neededClockParts  the clock parts needed for the transactional task
     * @param transactionalTask the task that uses the needed clock parts
     */
    public void takeParts(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask);

    /**
     * Takes an assembled clock from the factory stock with an infinite timeout and invokes the given transactional task with the clock as argument.
     * The take operations and the transactional task share the same transaction, resulting in a rollback if any operation fails.
     *
     * @param transactionalTask the task that uses the assembled clock
     */
    public void takeAssembled(TransactionalTask<Clock> transactionalTask);

    /**
     * Takes a checked clock of the given clock type quality from the factory stock with the given timout and invokes the given transactional task with the clock as argument.
     * The take operations and the transactional task share the same transaction, resulting in a rollback if any operation fails.
     *
     * @param type              the quality type needed
     * @param timeout           the time it should wait for a checked clock of the given quality type
     * @param transactionalTask the task that uses the checked clock
     * @return true if a timeout occured, otherwise false
     */
    public boolean takeChecked(ClockQualityType type, long timeout, TransactionalTask<Clock> transactionalTask);

    /**
     * Takes a single clock order of the given order priority from the unfinished clock orders of the factory and invokes the given transactional task with the single clock order as argument.
     * The take operations and the transactional task share the same transaction, resulting in a rollback if any operation fails.
     *
     * @param priority          the order priority needed
     * @param transactionalTask the task that uses the single clock order
     * @return true if a single clock order has successfully been processed
     */
    public boolean takeSingleClockOrder(OrderPriority priority, TransactionalTask<SingleClockOrder> transactionalTask);

    /**
     * Adds the given clock as assembled clock.
     *
     * @param clock the clock to be added
     */
    public void addAssembledClock(Clock clock);

    /**
     * Adds the given clock as checked clock.
     *
     * @param clock the clock to be added
     */
    public void addCheckedClock(Clock clock);

    /**
     * Adds the given clock as delivered clock.
     *
     * @param clock the clock to be added
     */
    public void addDeliveredClock(Clock clock);

    /**
     * Adds the given clock as disassembled clock.
     *
     * @param clock the clock to be added
     */
    public void addDisassembledClock(Clock clock);

    /**
     * Adds the given order to the list of unfinished orders of the factory.
     *
     * @param order the order to be added
     */
    public void addOrder(Order order);

    /**
     * Tries to deliver demanded clocks to the distributors based on their current stock contents.
     *
     * @param handlerId the id of the handler that tries to deliver the demanded clocks
     */
    public void deliverDemandedClock(UUID handlerId);

    public List<SingleClockOrder> getSingleClockOrders();

}
