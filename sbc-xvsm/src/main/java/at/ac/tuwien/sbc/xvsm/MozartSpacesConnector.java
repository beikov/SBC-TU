/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.mozartspaces.capi3.ContainerNameNotAvailableException;
import org.mozartspaces.capi3.CoordinationData;
import org.mozartspaces.capi3.Coordinator;
import org.mozartspaces.capi3.CountNotMetException;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.IsolationLevel;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.capi3.Property;
import org.mozartspaces.capi3.Query;
import org.mozartspaces.capi3.QueryCoordinator;
import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.MzsTimeoutException;
import org.mozartspaces.core.TransactionReference;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.NotificationManager;
import org.mozartspaces.notifications.Operation;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.OrderListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.Demand;
import at.ac.tuwien.sbc.model.Order;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian
 */
public class MozartSpacesConnector implements Connector {

	private static final String SPACE_BASE_URI = "xvsm://localhost:";

	private static final long MAX_TIMEOUT_MILLIS = 2000;
	private static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;
	private static final String PARTS_CONTAINER_NAME = "Fabrik/Teile";
	private static final String ASSEMBLED_CLOCKS_CONTAINER_NAME = "Fabrik/Uhren";
	private static final String CHECKED_CLOCKS_CONTAINER_NAME = "Fabrik/GepruefteUhren";
	private static final String DELIVERED_CLOCKS_CONTAINER_NAME = "Fabrik/AusgelieferteUhren";
	private static final String DISASSEMBLED_CLOCKS_CONTAINER_NAME = "Fabrik/SchlechteUhren";
	private static final String ORDER_CONTAINER_NAME = "Fabrik/Bestellungen";
	private static final String SINGLE_CLOCK_ORDER_CONTAINER_NAME = "Fabrik/EinzelneUhrenBestellungen";
	private static final String DISTRIBUTOR_REFERENCE_CONTAINER_NAME = "Fabrik/Grosshaendler";

	private static final String DISTRIBUTOR_DEMAND_CONTAINER_NAME = "Grosshaendler/Bedarf";
	private static final String DISTRIBUTOR_STOCK_CONTAINER_NAME = "Grosshaendler/Lager";

	private static final String ID_CONTAINER_NAME = "idcontainer";


	private static final String PARTS_TYPE_COORDINATOR_NAME = "type";
	private static final String CLOCK_QUALITY_COORDINATOR_NAME = "quality";
	private static final String ORDER_PRIORITY_COORDINATOR_NAME = "priority";


	private final ThreadLocal<TransactionReference> currentTransaction = new ThreadLocal<TransactionReference>();
	private final ThreadLocal<Boolean> currentTransactionRollback = new ThreadLocal<Boolean>();
	private final Capi capi;
	private final URI uri;
	private final int port;

	private Capi distributorCapi;
	private URI distributorURI;

	private final NotificationManager notificationManager;
	private final ContainerReference partsContainer;
	private final ContainerReference assembledClocksContainer;
	private final ContainerReference checkedClocksContainer;
	private final ContainerReference deliveredClocksContainer;
	private final ContainerReference disassembledClocksContainer;
	private final ContainerReference orderContainer;
	private final ContainerReference singleClockOrderContainer;
	private final ContainerReference distributors;

	private ContainerReference distributorDemandContainer;
	private ContainerReference distributorStockContainer;

	private final ContainerReference idContainer;

	public MozartSpacesConnector(int port) {
		this.port = port;
		capi = new Capi(DefaultMzsCore.newInstance(0));
		uri = URI.create(SPACE_BASE_URI + port);
		Runtime.getRuntime()
		.addShutdownHook(new Thread() {
			@Override
			public void run() {
				capi.getCore()
				.shutdown(true);
			}
		});
		notificationManager = new NotificationManager(capi.getCore());
		partsContainer = getOrCreateContainer(capi, PARTS_CONTAINER_NAME, new LabelCoordinator(PARTS_TYPE_COORDINATOR_NAME),
				new FifoCoordinator());
		assembledClocksContainer = getOrCreateContainer(capi, ASSEMBLED_CLOCKS_CONTAINER_NAME, new FifoCoordinator());
		checkedClocksContainer = getOrCreateContainer(capi, CHECKED_CLOCKS_CONTAINER_NAME, new LabelCoordinator(
				CLOCK_QUALITY_COORDINATOR_NAME), new FifoCoordinator());
		deliveredClocksContainer = getOrCreateContainer(capi, DELIVERED_CLOCKS_CONTAINER_NAME, new QueryCoordinator(), new FifoCoordinator());
		disassembledClocksContainer = getOrCreateContainer(capi, DISASSEMBLED_CLOCKS_CONTAINER_NAME, new FifoCoordinator());
		orderContainer = getOrCreateContainer(capi, ORDER_CONTAINER_NAME, new QueryCoordinator(), new FifoCoordinator());
		singleClockOrderContainer = getOrCreateContainer(capi, SINGLE_CLOCK_ORDER_CONTAINER_NAME, new QueryCoordinator(), new FifoCoordinator());

		distributorDemandContainer = getOrCreateContainer(capi, DISTRIBUTOR_DEMAND_CONTAINER_NAME, new QueryCoordinator(), new FifoCoordinator());
		distributors = getOrCreateContainer(capi, DISTRIBUTOR_REFERENCE_CONTAINER_NAME, new FifoCoordinator());

		idContainer = getOrCreateIdContainer(capi, ID_CONTAINER_NAME);
	}

	private ContainerReference getOrCreateIdContainer(Capi capi, String name){
		try {
			return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
		} catch (MzsCoreException ex) {
			try {
				ContainerReference idContainer = capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(new FifoCoordinator()), null, null);
				// since id container got created we need to write the idCounter into it
				Integer id = 0;
				Entry entry = new Entry(id);
				capi.write(idContainer, entry);

				return idContainer;
			} catch (ContainerNameNotAvailableException ex2) {
				// Someone else was faster...
				return getOrCreateContainer(capi, name);
			} catch (MzsCoreException ex2) {
				throw new RuntimeException(ex2);
			}
		}
	}

	private ContainerReference getOrCreateContainer(Capi capi, String name, Coordinator... coordinators) {
		try {
			return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
		} catch (MzsCoreException ex) {
			try {
				return capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(coordinators), null, null);
			} catch (ContainerNameNotAvailableException ex2) {
				// Someone else was faster...
				return getOrCreateContainer(capi, name, coordinators);
			} catch (MzsCoreException ex2) {
				throw new RuntimeException(ex2);
			}
		}
	}
	
	private ContainerReference getOrCreateIdContainer(Capi capi,URI uri, String name){
		try {
			return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
		} catch (MzsCoreException ex) {
			try {
				ContainerReference idContainer = capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(new FifoCoordinator()), null, null);
				// since id container got created we need to write the idCounter into it
				Integer id = 0;
				Entry entry = new Entry(id);
				capi.write(idContainer, entry);

				return idContainer;
			} catch (ContainerNameNotAvailableException ex2) {
				// Someone else was faster...
				return getOrCreateContainer(capi,uri, name);
			} catch (MzsCoreException ex2) {
				throw new RuntimeException(ex2);
			}
		}
	}

	private ContainerReference getOrCreateContainer(Capi capi,URI uri, String name, Coordinator... coordinators) {
		try {
			return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
		} catch (MzsCoreException ex) {
			try {
				return capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(coordinators), null, null);
			} catch (ContainerNameNotAvailableException ex2) {
				// Someone else was faster...
				return getOrCreateContainer(capi,uri, name, coordinators);
			} catch (MzsCoreException ex2) {
				throw new RuntimeException(ex2);
			}
		}
	}

	private boolean commit(TransactionReference tx) {
		try {
			capi.commitTransaction(tx);
			return false;
		} catch (MzsTimeoutException ex) {
			return true;
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
			currentTransactionRollback.remove();
		}
	}

	private void rollback(TransactionReference tx) {
		try {
			//System.out.println("rollback");
			capi.rollbackTransaction(tx);
		} catch (MzsTimeoutException ex) {
			// On timeout we dont have to rollback
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
		}
	}

	private boolean transactional(TransactionalWork work) {
		return transactional(work, MAX_TRANSACTION_TIMEOUT_MILLIS);
	}

	private boolean transactional(TransactionalWork work, long timeoutInMillis) {
		boolean created = ensureCurrentTransaction(timeoutInMillis);
		TransactionReference tx = getCurrentTransaction();
		currentTransactionRollback.set(Boolean.TRUE);

		try {
			work.doWork(tx);

			if (created) {
				return commit(tx);
			} else {
				// Indicates no timeout occurred
				return false;
			}
		} catch (MzsTimeoutException ex) {
			// On timeout we dont have to rollback
			//System.out.println("timeout");
			created = false;
			currentTransaction.remove();
			currentTransactionRollback.remove();
			return true;
		} catch (CountNotMetException ex) {
			// This happens when try once is used and is handeled like a timeout
			created = false;
			currentTransaction.remove();
			currentTransactionRollback.remove();
			return true;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Error ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		} finally {
			if (created && Boolean.TRUE == currentTransactionRollback.get()) {
				rollback(tx);
			}
		}
	}

	private TransactionReference getCurrentTransaction() {
		return currentTransaction.get();
	}

	/**
	 * Returns true if the call resulted in creating the transaction.
	 *
	 * @param timeoutInMillis
	 * @return
	 */
	private boolean ensureCurrentTransaction(long timeoutInMillis) {
		try {
			TransactionReference tx = currentTransaction.get();
			if (tx == null) {
				tx = capi.createTransaction(timeoutInMillis, uri);
				currentTransaction.set(tx);
				return true;
			}
			return false;
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		}
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
			return capi.read(partsContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null);
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
		//System.out.println("getClocks()");
		
		List<Clock> clocks = new ArrayList<Clock>();
		List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

		try {
			clocks.addAll((List) capi.read(assembledClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null));
			clocks.addAll((List) capi.read(checkedClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null));
			clocks.addAll((List) capi.read(deliveredClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null));
			clocks.addAll((List) capi.read(disassembledClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null));
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

		//System.out.println("subscribed for oders");

		return new MozartSpacesSubscription(notifications);
	}

	@Override
	public List<Order> getOrders() {
		List<Order> orders = new ArrayList<Order>();
		List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

		try {
			orders.addAll((List) capi.read(orderContainer, selectors, MzsConstants.RequestTimeout.INFINITE, null, IsolationLevel.READ_COMMITTED, null));
			return orders;
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<? extends CoordinationData> getPartCoordinationData(ClockPart part) {
		return Arrays.asList(LabelCoordinator.newCoordinationData(part.getType()
				.name(), PARTS_TYPE_COORDINATOR_NAME));
	}

	@Override
	public void addParts(final List<ClockPart> parts) {
		final Entry[] entries = new Entry[parts.size()];

		for (int i = 0; i < parts.size(); i++) {
			ClockPart part = parts.get(i);
			entries[i] = new Entry(part, getPartCoordinationData(part));
		}

		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				capi.write(partsContainer, MzsConstants.RequestTimeout.ZERO, tx, entries);
			}
		});
	}

	@Override
	public void takeParts(final Map<ClockPartType, Integer> neededClockParts, final TransactionalTask<List<ClockPart>> transactionalTask) {
		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				List<ClockPart> clockParts = new ArrayList(neededClockParts.size());

				for (Map.Entry<ClockPartType, Integer> entry : neededClockParts.entrySet()) {
					List<? extends Selector> selectors = Arrays.asList(LabelCoordinator.newSelector(entry.getKey()
							.name(), entry.getValue(), PARTS_TYPE_COORDINATOR_NAME));
					// Use a timeout to avoid deadlocks
					clockParts.addAll((Collection) capi.take(partsContainer, selectors, MAX_TIMEOUT_MILLIS, tx, IsolationLevel.READ_COMMITTED, null));
				}

				transactionalTask.doWork(clockParts);
			}
		});
	}
	
	@Override
	public void takeAssembled(final TransactionalTask<Clock> transactionalTask) {
		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				List<Selector> selectors = new ArrayList<Selector>();
				selectors.add(FifoCoordinator.newSelector(1));

				// We need a timeout here because the transaction would otherwise stay alive forever
				Clock clock = (Clock) capi.take(assembledClocksContainer, selectors, MAX_TIMEOUT_MILLIS, tx)
						.get(0);
				transactionalTask.doWork(clock);
			}
		});
	}

	@Override
	public boolean takeChecked(final ClockQualityType type, final long timeout, final TransactionalTask<Clock> transactionalTask) {
		return transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				List<Selector> selectors = new ArrayList<Selector>();

				selectors.add(LabelCoordinator.newSelector(type.name(), 1, CLOCK_QUALITY_COORDINATOR_NAME));
				selectors.add(FifoCoordinator.newSelector(1));

				// Since we already have a transaction timeout we don't care about the request timeout
				Clock clock = (Clock) capi.take(checkedClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, tx)
						.get(0);
				transactionalTask.doWork(clock);
			}
		}, timeout);
	}

	@Override
	public void addAssembledClock(final Clock clock) {

		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				List<Selector> selectors = new ArrayList<Selector>();
				selectors.add(FifoCoordinator.newSelector(1));
				Integer id = (Integer) capi.take(idContainer, selectors, MAX_TIMEOUT_MILLIS, tx)
						.get(0);
				clock.setSerialId(id);
				id += 1;
				Entry entry = new Entry(id);
				capi.write(idContainer, MzsConstants.RequestTimeout.ZERO, tx,entry);
				entry = new Entry(clock);
				capi.write(assembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
			}
		});
	}

	@Override
	public void addCheckedClock(Clock clock, ClockQualityType type) {
		final Entry entry = new Entry(clock, LabelCoordinator.newCoordinationData(type.name(), CLOCK_QUALITY_COORDINATOR_NAME));
		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				capi.write(checkedClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
			}
		});
	}

	@Override
	public void addDeliveredClock(Clock clock) {
		final Entry entry = new Entry(clock);
		transactional(new TransactionalWork() {

			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				capi.write(deliveredClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
			}
		});
	}

	@Override
	public void addDisassembledClock(Clock clock) {
		final Entry entry = new Entry(clock);
		transactional(new TransactionalWork() {

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

		for (int i=0, max=order.getNeededClocksOfType(ClockType.KLASSISCH) ; i < max ; i++ ){
			clockOrders.add(
					new Entry(new SingleClockOrder(order.getId(), ClockType.KLASSISCH, order.getPriority()),
							QueryCoordinator.newCoordinationData(), FifoCoordinator.newCoordinationData()));
		}
		for (int i=0, max=order.getNeededClocksOfType(ClockType.SPORT) ; i < max ; i++ ){
			clockOrders.add(
					new Entry(new SingleClockOrder(order.getId(), ClockType.SPORT, order.getPriority()),
							QueryCoordinator.newCoordinationData(), FifoCoordinator.newCoordinationData()));
		}
		for (int i=0, max=order.getNeededClocksOfType(ClockType.ZEITZONEN_SPORT) ; i < max ; i++ ){
			clockOrders.add(
					new Entry(new SingleClockOrder(order.getId(), ClockType.ZEITZONEN_SPORT, order.getPriority()),
							QueryCoordinator.newCoordinationData(), FifoCoordinator.newCoordinationData()));
		}

		transactional(new TransactionalWork() {
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
        Query query = new Query().filter(Property.forName("priority").equalTo(priority));

        selectors.add(QueryCoordinator.newSelector(query, 1));
        selectors.add(FifoCoordinator.newSelector(1));

		transactional(new TransactionalWork() {
			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
                List<SingleClockOrder> singleOrders = null;
                
                try{
                    singleOrders = capi.take(singleClockOrderContainer, selectors, 0, tx);
                } catch (MzsCoreException e){
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
    
	@Override
	public void connectDistributor(UUID distributorId) {
        distributorCapi = new Capi(DefaultMzsCore.newInstance(0));
		distributorURI = URI.create("xvsm://localhost:"+port+"/"+distributorId);
		Runtime.getRuntime()
		.addShutdownHook(new Thread() {
			@Override
			public void run() {
				capi.getCore()
				.shutdown(true);
			}
		});
		distributorStockContainer = getOrCreateContainer(distributorCapi, distributorURI, DISTRIBUTOR_STOCK_CONTAINER_NAME, new FifoCoordinator());
		distributorDemandContainer = getOrCreateContainer(distributorCapi, distributorURI, DISTRIBUTOR_DEMAND_CONTAINER_NAME, new QueryCoordinator(), new FifoCoordinator());

		final Entry entry = new Entry(distributorId);
		transactional(new TransactionalWork() {
			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				capi.write(distributors, MzsConstants.RequestTimeout.ZERO, tx, entry);
			}
		});

	}

	@Override
	public void setDemand(UUID delivererId, Map<ClockType, Integer> demand) {
		final List<Entry> demandEntries = new ArrayList<Entry>();

		for (ClockType type : demand.keySet()) {
			for(int i=0; i<demand.get(type); i++){
				demandEntries.add(new Entry(new Demand(delivererId, type)));
			}
		}

		transactional(new TransactionalWork() {
			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				distributorCapi.write(demandEntries, distributorDemandContainer, MzsConstants.RequestTimeout.ZERO, tx);
				//System.out.println("added demand");
			}
		});
		
	}

	@Override
	public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
		try {
			Notification notification = notificationManager.createNotification(distributorStockContainer,
					new MozartSpacesClockListener(listener),
					Operation.WRITE, Operation.TAKE, Operation.DELETE);
//			//System.out.println("subscribed");
			return new MozartSpacesSubscription(Arrays.asList(notification));
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void takeDemandedClock(final TransactionalTask<Map<Demand, Clock>> transactionalTask) {
		transactional( new TransactionalWork() {
			@Override
			public void doWork(TransactionReference tx) throws MzsCoreException {
				UUID distributorID = (UUID) capi.take(distributors, Arrays.asList(FifoCoordinator.newSelector(1)), MAX_TIMEOUT_MILLIS,tx).get(0);
			
//				//System.out.println("found distributor: "+distributorID);
				try {
					distributorDemandContainer = capi.lookupContainer(DISTRIBUTOR_DEMAND_CONTAINER_NAME, new URI("xvsm://localhost:"+port+"/"+distributorID), MAX_TIMEOUT_MILLIS, null);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Entry entry = new Entry(distributorID);
				capi.write(entry, distributors, MAX_TIMEOUT_MILLIS, tx);
				
//				//System.out.println("trying to take demand");
				Demand demand = (Demand) capi.take(distributorDemandContainer, Arrays.asList(FifoCoordinator.newSelector(1)), MAX_TIMEOUT_MILLIS, tx).get(0);
//				//System.out.println("took demand");
			
				
				Property property = Property.forName("type");
				final List<Selector> selectors = new ArrayList<Selector>();
				Query query = new Query();
				query.filter(property.equalTo(demand.getType()));
				selectors.add(QueryCoordinator.newSelector(query,1));
//				//System.out.println("trying to take clock");
				//TODO change to clock that doesn't belong to order
				Clock clock = (Clock) capi.take(deliveredClocksContainer, selectors, 0, tx).get(0);
//				//System.out.println("took clock");
				Map<Demand, Clock> param = new HashMap<Demand, Clock>();
				param.put(demand, clock);
				
				//deliver demanded clock
				transactionalTask.doWork(param);
			}
		});

	}

	@Override
	public void deliverDemandedClock(Demand demand, Clock clock) {
		try {
			final ContainerReference delivererContainer = capi.lookupContainer(DISTRIBUTOR_STOCK_CONTAINER_NAME, new URI("xvsm://localhost:"+port+"/"+demand.getDistributor()), MAX_TIMEOUT_MILLIS, null);
			final Entry entry = new Entry(clock);
			transactional( new TransactionalWork() {

				@Override
				public void doWork(TransactionReference tx) throws MzsCoreException {
					capi.write(entry, delivererContainer,  MzsConstants.RequestTimeout.ZERO, tx);
					//System.out.println("successfully delivered clock to distributor");
				}
			});
		} catch (MzsCoreException e) {
			//deliverer not reachable anymore, ignore demand
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

}
