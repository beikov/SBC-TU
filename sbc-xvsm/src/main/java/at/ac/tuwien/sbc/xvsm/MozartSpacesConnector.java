/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.Operation;

/**
 *
 * @author Christian
 */
public class MozartSpacesConnector extends AbstractMozartSpacesComponent implements Connector {

    private final ContainerReference partsContainer;
    private final ContainerReference assembledClocksContainer;
    private final ContainerReference checkedClocksContainer;
    private final ContainerReference deliveredClocksContainer;
    private final ContainerReference disassembledClocksContainer;
    private final ContainerReference orderContainer;
    private final ContainerReference singleClockOrderContainer;

    private ContainerReference distributorDemandContainer;
    private ContainerReference distributorStockContainer;

    public MozartSpacesConnector(int serverPort) {
        this("localhost", serverPort);
    }

    public MozartSpacesConnector(String serverHost, int serverPort/* , String localHost, int localPort */) {
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
        orderContainer = getOrCreateContainer(capi, MozartSpacesConstants.ORDER_CONTAINER_NAME, new QueryCoordinator(),
                                              new FifoCoordinator());
        singleClockOrderContainer = getOrCreateContainer(capi, MozartSpacesConstants.SINGLE_CLOCK_ORDER_CONTAINER_NAME,
                                                         new LabelCoordinator(
                                                             MozartSpacesConstants.ORDER_PRIORITY_COORDINATOR_NAME),
                                                         new FifoCoordinator());

        distributorDemandContainer = getOrCreateContainer(capi, MozartSpacesConstants.DISTRIBUTOR_DEMAND_CONTAINER_NAME,
                                                          new QueryCoordinator(),
                                                          new FifoCoordinator());
    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        try {
            Notification notification = notificationManager.createNotification(partsContainer,
                                                                               new MozartSpacesClockPartListener(listener),
                                                                               Operation.WRITE, Operation.TAKE, Operation.DELETE);
            return new MozartSpacesSubscription(Arrays.asList(notification));
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<ClockPart> getClockParts() {
        List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

        try {
            return capi.read(partsContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                             IsolationLevel.READ_COMMITTED, null);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForClocks(final ClockListener listener) {
        final List<Notification> notifications = new ArrayList<Notification>();
        final Set<Operation> operations = EnumSet.of(Operation.WRITE);
        final NotificationListener l = new MozartSpacesClockListener(listener);
        TransactionReference tx = null;

        try {
            notifications.add(notificationManager.createNotification(assembledClocksContainer, l, operations, tx, null));
            notifications.add(notificationManager.createNotification(checkedClocksContainer, l, operations, tx, null));
            notifications.add(notificationManager.createNotification(deliveredClocksContainer, l, operations, tx, null));
            notifications.add(notificationManager.createNotification(disassembledClocksContainer, l, operations, tx, null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }

        // Can't use transaction because we get some strage messages
        //        transactional(new TransactionalWork() {
        //
        //            @Override
        //            public void doWork(TransactionReference tx) throws MzsCoreException {
        //                try {
        //                    notifications.add(notificationManager.createNotification(assembledClocksContainer, l, operations, tx, null));
        //                    notifications.add(notificationManager.createNotification(checkedClocksContainer, l, operations, tx, null));
        //                    notifications.add(notificationManager.createNotification(deliveredClocksContainer, l, operations, tx, null));
        //                    notifications.add(notificationManager.createNotification(disassembledClocksContainer, l, operations, tx, null));
        //                } catch (InterruptedException ex) {
        //                    throw new RuntimeException(ex);
        //                }
        //            }
        //        });
        return new MozartSpacesSubscription(notifications);
    }

    @Override
    public List<Clock> getClocks() {
        List<Clock> clocks = new ArrayList<Clock>();
        List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

        try {
            clocks.addAll((List) capi.read(assembledClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                           IsolationLevel.READ_COMMITTED, null));
            clocks.addAll((List) capi.read(checkedClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                           IsolationLevel.READ_COMMITTED, null));
            clocks.addAll((List) capi.read(deliveredClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                           IsolationLevel.READ_COMMITTED, null));
            clocks.addAll((List) capi.read(disassembledClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                           IsolationLevel.READ_COMMITTED, null));
            return clocks;
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForOrders(OrderListener listener) {
        final List<Notification> notifications = new ArrayList<Notification>();
        final Set<Operation> operations = EnumSet.of(Operation.WRITE);
        final NotificationListener l = new MozartSpacesOrderListener(listener);
        TransactionReference tx = null;

        try {
            notifications.add(notificationManager.createNotification(orderContainer, l, operations, tx, null));
            notifications.add(notificationManager.createNotification(assembledClocksContainer, l, operations, tx, null));
        } catch (MzsCoreException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new MozartSpacesSubscription(notifications);
    }

    @Override
    public List<Order> getOrders() {
        List<Order> orders = new ArrayList<Order>();
        List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

        try {
            orders.addAll((List) capi.read(orderContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                           IsolationLevel.READ_COMMITTED, null));
            return orders;
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<? extends CoordinationData> getPartCoordinationData(ClockPart part) {
        return Arrays.asList(LabelCoordinator.newCoordinationData(part.getType()
            .name(), MozartSpacesConstants.PARTS_TYPE_COORDINATOR_NAME));
    }

    private List<? extends CoordinationData> getSingleOrderCoordinationData(SingleClockOrder singleOrder) {
        return Arrays.asList(LabelCoordinator.newCoordinationData(singleOrder.getPriority()
            .name(), MozartSpacesConstants.ORDER_PRIORITY_COORDINATOR_NAME));
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
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<ClockPart> clockParts = new ArrayList(neededClockParts.size());

                for (Map.Entry<ClockPartType, Integer> entry : neededClockParts.entrySet()) {
                    List<? extends Selector> selectors = Arrays.asList(LabelCoordinator.newSelector(entry.getKey()
                        .name(), entry.getValue(), MozartSpacesConstants.PARTS_TYPE_COORDINATOR_NAME));
                    // Use a timeout to avoid deadlocks
                    clockParts.addAll((Collection) capi
                        .take(partsContainer, selectors, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx,
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
				//                List<Selector> selectors = new ArrayList<Selector>();

                //                selectors.add(LabelCoordinator.newSelector(type.name(), 1, CLOCK_QUALITY_COORDINATOR_NAME));
                //                selectors.add(FifoCoordinator.newSelector(1));
                // Since we already have a transaction timeout we don't care about the request timeout
                Clock clock = (Clock) capi.take(checkedClocksContainer, Arrays.asList(LabelCoordinator.newSelector(type.name(),
                                                                                                                   1,
                                                                                                                   MozartSpacesConstants.CLOCK_QUALITY_COORDINATOR_NAME)),
                                                MzsConstants.RequestTimeout.INFINITE, tx)
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
                idSequence = idSequence != null ? idSequence : new MozartSpacesSequence(capi, serverUri, tm,
                                                                                        MozartSpacesConstants.ID_CONTAINER_NAME);
                clock.setSerialId(idSequence.getNextId());
                Entry entry = new Entry(clock);
                capi.write(assembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addCheckedClock(Clock clock, ClockQualityType type) {
        final Entry entry = new Entry(clock, LabelCoordinator.newCoordinationData(type.name(),
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

    @Override
    public boolean takeSingleClockOrder(final OrderPriority priority, final TransactionalTask<SingleClockOrder> transactionalTask) {
        final List<Selector> selectors = new ArrayList<Selector>();
        final Boolean[] done = { false };
		//        Query query = new Query().filter(Property.forName("priority")
        //            .equalTo(priority));

        //        selectors.add(QueryCoordinator.newSelector(query, 1));
        selectors.add(LabelCoordinator.newSelector(priority.name(), 1, MozartSpacesConstants.ORDER_PRIORITY_COORDINATOR_NAME));
        //        selectors.add(FifoCoordinator.newSelector(1));

        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<SingleClockOrder> singleOrders = null;

                try {
                    singleOrders = capi.take(singleClockOrderContainer, selectors, MzsConstants.RequestTimeout.ZERO, tx);
                } catch (MzsCoreException e) {
                    //okay we found no matching order
                }

                if (singleOrders != null && singleOrders.size() > 0) {
                    transactionalTask.doWork(singleOrders.get(0));
                    done[0] = true;
                }
            }
        });

        return done[0];
    }

    private Clock takeDeliveredClockOfNoOrder(ClockType type) {
        final List<Selector> selectors = new ArrayList<Selector>();
        Query query = new Query().filter(Property.forName("type")
            .equalTo(type));
        query.filter(Property.forName("orderId")
            .equalTo(null));
        selectors.add(QueryCoordinator.newSelector(query, 1));
        final List<Clock> clock = new ArrayList<Clock>();

        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                clock
                    .addAll((List) capi.take(deliveredClocksContainer, selectors, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx));
            }
        });

        return (clock.size() > 0) ? clock.get(0) : null;

    }

    private MozartSpacesDistributorStockConnector getStockConnector(URI distributorUri, String destinationName) {
        return new MozartSpacesDistributorStockConnector(distributorUri, destinationName);
    }

    @Override
    public void deliverDemandedClock() {
        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                DistributorDemand distributorDemand = (DistributorDemand) capi.take(distributorDemandContainer, Arrays.asList(
                                                                                    FifoCoordinator.newSelector(1)),
                                                                                    MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx)
                    .get(0);
                MozartSpacesDistributorStockConnector stockConnector = null;

                try {
                    stockConnector = getStockConnector(distributorDemand.getUri(), distributorDemand.getDestinationName());

                    Map<ClockType, Integer> demandedClocks = distributorDemand.getNeededClocksPerType();
                    Map<ClockType, Integer> stockCount = stockConnector.getDistributorStock();

                    for (ClockType type : demandedClocks.keySet()) {
                        if (stockCount.get(type) < demandedClocks.get(type)) {
                            Clock clock = takeDeliveredClockOfNoOrder(type);

                            if (clock != null) {
                                stockConnector.deliver(clock);
                                break;
                            }
                        }

                    }
                } finally {
                    if (stockConnector != null) {
                        stockConnector.close();
                    }
                }

                Entry entry = new Entry(distributorDemand);
                capi.write(entry, distributorDemandContainer, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx);
            }
        });

    }

}
