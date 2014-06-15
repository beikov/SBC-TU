package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.DistributorDemand;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mozartspaces.capi3.CoordinationData;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.IsolationLevel;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.capi3.Property;
import org.mozartspaces.capi3.Query;
import org.mozartspaces.capi3.QueryCoordinator;
import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;
import org.mozartspaces.notifications.Operation;

/**
 * A simple MozartSpaces implementation of the {@link Connector} interface.
 */
public class MozartSpacesConnector extends AbstractMozartSpacesComponent implements Connector {

    private final ContainerReference partsContainer;
    private final ContainerReference assembledClocksContainer;
    private final ContainerReference checkedClocksContainer;
    private final ContainerReference deliveredClocksContainer;
    private final ContainerReference disassembledClocksContainer;
    private final ContainerReference orderContainer;
    private final ContainerReference singleClockOrderContainer;
    private final ContainerReference clocksDeliveredToDistributorsContainer;

    private ContainerReference distributorDemandContainer;

    public MozartSpacesConnector(int serverPort) {
        this("localhost", serverPort);
    }

    public MozartSpacesConnector(String serverHost, int serverPort) {
        super(serverHost, serverPort);

        partsContainer = getOrCreateContainer(capi, MozartSpacesConstants.PARTS_CONTAINER_NAME, new LabelCoordinator(
                                              MozartSpacesConstants.PARTS_TYPE_COORDINATOR_NAME),
                                              new FifoCoordinator());
        assembledClocksContainer = getOrCreateContainer(capi, MozartSpacesConstants.ASSEMBLED_CLOCKS_CONTAINER_NAME,
                                                        new FifoCoordinator());
        checkedClocksContainer = getOrCreateContainer(capi, MozartSpacesConstants.CHECKED_CLOCKS_CONTAINER_NAME,
                                                      new LabelCoordinator(
                                                          MozartSpacesConstants.CLOCK_QUALITY_COORDINATOR_NAME),
                                                      new FifoCoordinator());
        deliveredClocksContainer = getOrCreateContainer(capi, MozartSpacesConstants.DELIVERED_CLOCKS_CONTAINER_NAME,
                                                        new QueryCoordinator(),
                                                        new FifoCoordinator());
        disassembledClocksContainer = getOrCreateContainer(capi, MozartSpacesConstants.DISASSEMBLED_CLOCKS_CONTAINER_NAME,
                                                           new FifoCoordinator());
        orderContainer = getOrCreateContainer(capi, MozartSpacesConstants.ORDER_CONTAINER_NAME, new FifoCoordinator());
        singleClockOrderContainer = getOrCreateContainer(capi, MozartSpacesConstants.SINGLE_CLOCK_ORDER_CONTAINER_NAME,
                                                         new LabelCoordinator(
                                                             MozartSpacesConstants.ORDER_TYPE_COORDINATOR_NAME),
                                                         new FifoCoordinator());

        clocksDeliveredToDistributorsContainer = getOrCreateContainer(
            capi, MozartSpacesConstants.DELIVERED_TO_DISTRIBUTORS_CONTAINER_NAME,
            new FifoCoordinator());

        distributorDemandContainer = getOrCreateContainer(capi, MozartSpacesConstants.DISTRIBUTOR_DEMAND_CONTAINER_NAME,
                                                          new QueryCoordinator(),
                                                          new FifoCoordinator());

    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        return subscribeListener(new MozartSpacesClockPartListener(listener), EnumSet.of(Operation.WRITE, Operation.TAKE,
                                                                                         Operation.DELETE), partsContainer);
    }

    @Override
    public List<ClockPart> getClockParts() {
        return containersAsList(partsContainer);
    }

    @Override
    public Subscription subscribeForClocks(final ClockListener listener) {
        return subscribeListener(new MozartSpacesClockListener(listener), EnumSet.of(Operation.WRITE), assembledClocksContainer,
                                 checkedClocksContainer, deliveredClocksContainer, disassembledClocksContainer);
    }

    @Override
    public List<Clock> getClocks() {
        return containersAsList(assembledClocksContainer, checkedClocksContainer, deliveredClocksContainer,
                                disassembledClocksContainer, clocksDeliveredToDistributorsContainer);
    }

    @Override
    public Subscription subscribeForOrders(OrderListener listener) {
        return subscribeListener(new MozartSpacesOrderListener(listener), EnumSet.of(Operation.WRITE), orderContainer,
                                 assembledClocksContainer);
    }

    @Override
    public List<Order> getOrders() {
        return containersAsList(orderContainer);
    }

    @Override
    public List<SingleClockOrder> getSingleClockOrders() {
        return containersAsList(singleClockOrderContainer);
    }

    private List<? extends CoordinationData> getPartCoordinationData(ClockPart part) {
        return Arrays.asList(LabelCoordinator.newCoordinationData(part.getType()
            .name(), MozartSpacesConstants.PARTS_TYPE_COORDINATOR_NAME));
    }

    private List<? extends CoordinationData> getSingleOrderCoordinationData(SingleClockOrder singleOrder) {
        return Arrays.asList(
            LabelCoordinator.newCoordinationData(singleOrder.getNeededType()
                .name() + singleOrder.getPriority()
                .name(), MozartSpacesConstants.ORDER_TYPE_COORDINATOR_NAME),
            FifoCoordinator.newCoordinationData());
    }

    @Override
    public void addParts(final List<ClockPart> parts) {
        final Entry[] entries = new Entry[parts.size()];

        for (int i = 0; i < parts.size(); i++) {
            ClockPart part = parts.get(i);
            entries[i] = new Entry(part, getPartCoordinationData(part));
        }

        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(partsContainer, MzsConstants.RequestTimeout.ZERO, tx, entries);
            }
        });
    }

    @Override
    public void takeParts(final Map<ClockPartType, Integer> neededClockParts, final TransactionalTask<List<ClockPart>> transactionalTask) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(final TransactionReference tx) throws MzsCoreException {
                List<ClockPart> clockParts = new ArrayList(neededClockParts.size());

                for (Map.Entry<ClockPartType, Integer> entry : neededClockParts.entrySet()) {
                    List<? extends Selector> selectors = Arrays.asList(LabelCoordinator.newSelector(entry.getKey()
                        .name(), entry.getValue(), MozartSpacesConstants.PARTS_TYPE_COORDINATOR_NAME));

                    // For each clock part type take the needed amount, if it is not possible it will fail with an exception
                    clockParts.addAll((Collection) capi
                        .take(partsContainer, selectors, MzsConstants.RequestTimeout.TRY_ONCE, tx,
                              IsolationLevel.READ_COMMITTED, null));
                }

                transactionalTask.doWork(clockParts);
            }
        });
    }

    @Override
    public void takeAssembled(final TransactionalTask<Clock> transactionalTask) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<Selector> selectors = new ArrayList<Selector>();
                selectors.add(FifoCoordinator.newSelector(1));

                // We need a timeout here because the transaction would otherwise stay alive forever
                Clock clock = (Clock) capi.take(assembledClocksContainer, selectors, MozartSpacesConstants.MAX_TIMEOUT_MILLIS,
                                                tx)
                    .get(0);
                transactionalTask.doWork(clock);
            }
        });
    }

    @Override
    public boolean takeChecked(final ClockQualityType type, final long timeout, final TransactionalTask<Clock> transactionalTask) {
        return tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<Selector> selectors = new ArrayList<Selector>();
                selectors
                    .add(LabelCoordinator.newSelector(type.name(), 1, MozartSpacesConstants.CLOCK_QUALITY_COORDINATOR_NAME));
                // Since we already have a transaction timeout we don't care about the request timeout
                Clock clock = (Clock) capi.take(checkedClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, tx)
                    .get(0);
                transactionalTask.doWork(clock);
            }
        }, timeout);
    }

    MozartSpacesSequence idSequence;

    @Override
    public void addAssembledClock(final Clock clock) {
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                // Instantiate the idSequence if necessary
                idSequence = idSequence != null ? idSequence : new MozartSpacesSequence(capi, serverUri, tm,
                                                                                        MozartSpacesConstants.ID_CONTAINER_NAME);
                clock.setSerialId(idSequence.getNextId());

                Entry entry = new Entry(clock);
                capi.write(assembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addCheckedClock(Clock clock) {
        final Entry entry = new Entry(clock, LabelCoordinator.newCoordinationData(ClockQualityType.fromQuality(clock
                                      .getQuality())
                                      .name(),
                                                                                  MozartSpacesConstants.CLOCK_QUALITY_COORDINATOR_NAME));
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(checkedClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addDeliveredClock(Clock clock) {
        final Entry entry = new Entry(clock);
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(deliveredClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addDisassembledClock(Clock clock) {
        final Entry entry = new Entry(clock);
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(disassembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addOrder(Order order) {
        final Entry entry = new Entry(order);
        final List<Entry> clockOrders = new ArrayList<Entry>();

        // Add a single clock orders for every clock needed
        for (ClockType type : ClockType.values()) {
            for (int i = 0; i < order.getNeededClocksOfType(type); i++) {
                SingleClockOrder singleOrder = new SingleClockOrder(order.getId(), type, order.getPriority());
                clockOrders.add(new Entry(singleOrder,
                                          getSingleOrderCoordinationData(singleOrder)));
            }
        }

        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(orderContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
                capi.write(clockOrders, singleClockOrderContainer, MzsConstants.RequestTimeout.ZERO, tx);
            }
        });
    }

    /**
     * A transactional work implementation for {@link JmsConnector#takeSingleClockOrder(at.ac.tuwien.sbc.model.OrderPriority, at.ac.tuwien.sbc.TransactionalTask)} that can be parameterized for better reuse.
     */
    private final class TakeSingleClockOrderWork implements TransactionalWork {

        private final boolean[] done;
        private final OrderPriority priority;
        private final TransactionalTask<SingleClockOrder> transactionalTask;
        private final String type;

        public TakeSingleClockOrderWork(boolean[] done, OrderPriority priority, TransactionalTask<SingleClockOrder> transactionalTask, String type) {
            this.done = done;
            this.priority = priority;
            this.transactionalTask = transactionalTask;
            this.type = type;
        }

        @Override
        public void doWork(TransactionReference tx) throws MzsCoreException {
            final List<Selector> selectors = new ArrayList<Selector>();
            selectors.add(LabelCoordinator.newSelector(type + priority.name(), 1,
                                                       MozartSpacesConstants.ORDER_TYPE_COORDINATOR_NAME));
            selectors.add(FifoCoordinator.newSelector(1));

            List<SingleClockOrder> singleOrders = null;

            try {
                singleOrders = capi.take(singleClockOrderContainer, selectors, MzsConstants.RequestTimeout.TRY_ONCE, tx);
            } catch (MzsCoreException e) {
                //okay we found no matching order
            }

            if (singleOrders != null && singleOrders.size() > 0) {
                transactionalTask.doWork(singleOrders.get(0));

                done[0] = !tm.isRollbackOnly();
            }
        }
    }

    
	@Override
	public boolean takeSingleClockOrder(OrderPriority priority, ClockType type,
			TransactionalTask<SingleClockOrder> transactionalTask) {
		
        final boolean[] done = { false };

        // Try any single clock order
        String typeString = (type == null) ? "" : type.name();
        		
        tm.transactional(new TakeSingleClockOrderWork(done, priority, transactionalTask, typeString));
        // If the work is done ore no single clock order available, return
        return done[0];

	}

    private Clock takeDeliveredClockOfNoOrder(final ClockType type) {
        final List<Clock> clocks = new ArrayList<Clock>();

        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<Selector> selectors = new ArrayList<Selector>();
                Query query = new Query()
                    .filter(Property.forName("type")
                        .equalTo(type))
                    .filter(Property.forName("orderId")
                        .equalTo(null));
                selectors.add(QueryCoordinator.newSelector(query, 1));
                clocks.addAll((List) capi
                    .take(deliveredClocksContainer, selectors, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx));
            }
        });

        return (clocks.size() > 0) ? clocks.get(0) : null;

    }

    @Override
    public void deliverDemandedClock(final UUID handlerId) {
        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                // Take a distributor demand
                DistributorDemand distributorDemand = (DistributorDemand) capi.take(distributorDemandContainer, Arrays.asList(
                                                                                    FifoCoordinator.newSelector(1)),
                                                                                    MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx)
                    .get(0);
                MozartSpacesDistributorStockConnector stockConnector = null;

                try {
                    // Connect to the distributors stock
                    stockConnector = new MozartSpacesDistributorStockConnector(distributorDemand.getUri(), distributorDemand
                                                                               .getDestinationName());

                    Map<ClockType, Integer> demandedClocks = distributorDemand.getNeededClocksPerType();
                    Map<ClockType, Integer> stockCount = stockConnector.getDistributorStock();

                    for (ClockType type : demandedClocks.keySet()) {
                        if (stockCount.get(type) < demandedClocks.get(type)) {
                            // Take a delivered clock that is not related to any order
                            Clock clock = takeDeliveredClockOfNoOrder(type);

                            if (clock != null) {
                                // Deliver the clock
                                clock.setDistributor(distributorDemand.getDestinationName());
                                clock.setHandlerId(handlerId);
                                clock.setDistributor(distributorDemand.getDestinationName());
                                stockConnector.deliver(clock);

                                // Push the clock back by adding it to the disassembled clock container
                                Entry entry = new Entry(clock);
                                capi.write(disassembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
                                break;
                            }
                        }

                    }
                } finally {
                    if (stockConnector != null) {
                        stockConnector.close();
                    }

                    // Push back the demand
                    capi.write(distributorDemandContainer, MzsConstants.RequestTimeout.INFINITE, tx,
                               new Entry(distributorDemand));
                }
            }
        });

    }



}
