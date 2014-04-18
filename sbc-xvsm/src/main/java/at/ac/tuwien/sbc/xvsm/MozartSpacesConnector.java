/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.ClockPartListener;
import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockStatus;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.KeyCoordinator;
import org.mozartspaces.capi3.LabelCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
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
import org.mozartspaces.notifications.NotificationManager;
import org.mozartspaces.notifications.Operation;

/**
 *
 * @author Christian
 */
public class MozartSpacesConnector implements Connector {

    private static final long MAX_TIMEOUT_MILLIS = 2000;
    private static final String PARTS_CONTAINER_NAME = "Fabrik/Teile";
    private static final String ASSEMBLED_CLOCKS_CONTAINER_NAME = "Fabrik/Uhren";
    private static final String CHECKED_CLOCKS_CONTAINER_NAME = "Fabrik/GepruefteUhren";
    private static final String DELIVERED_CLOCKS_CONTAINER_NAME = "Fabrik/AusgelieferteUhren";

    private final ThreadLocal<TransactionReference> currentTransaction = new ThreadLocal<TransactionReference>();
    private final Capi capi;
    private final URI uri;

    private final NotificationManager notificationManager;
    private final ContainerReference partsContainer;
    private final ContainerReference assembledClocksContainer;
    private final ContainerReference checkedClocksContainer;
    private final ContainerReference deliveredClocksContainer;

    public MozartSpacesConnector(int port) {
        capi = new Capi(DefaultMzsCore.newInstance(0));
        uri = URI.create("xvsm://localhost:" + port);
        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    capi.getCore()
                    .shutdown(true);
                }
            });
        notificationManager = new NotificationManager(capi.getCore());
        partsContainer = getOrCreateContainer(PARTS_CONTAINER_NAME);
        assembledClocksContainer = getOrCreateContainer(ASSEMBLED_CLOCKS_CONTAINER_NAME);
        checkedClocksContainer = getOrCreateContainer(CHECKED_CLOCKS_CONTAINER_NAME);
        deliveredClocksContainer = getOrCreateContainer(DELIVERED_CLOCKS_CONTAINER_NAME);
    }

    private ContainerReference createContainer(String name) {
        try {
            return capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(new FifoCoordinator()),
                                        Arrays.asList(new LindaCoordinator()), null);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ContainerReference getOrCreateContainer(String name) {
        try {
            return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
        } catch (MzsCoreException ex) {
            return createContainer(name);
        }
    }

    private void commitCurrentTransaction() {
        TransactionReference tx = currentTransaction.get();
        if (tx != null) {
            try {
                capi.commitTransaction(tx);
            } catch (MzsCoreException ex) {
                throw new RuntimeException(ex);
            } finally {
                currentTransaction.remove();
            }
        }
    }

    private void rollbackCurrentTransaction() {
        TransactionReference tx = currentTransaction.get();
        if (tx != null) {
            try {
                capi.rollbackTransaction(tx);
            } catch (MzsCoreException ex) {
                throw new RuntimeException(ex);
            } finally {
                currentTransaction.remove();
            }
        }
    }

    private void transactional(TransactionalWork work) {
        transactional(work, MzsConstants.TransactionTimeout.INFINITE);
    }

    private boolean transactional(TransactionalWork work, long timeoutInMillis) {
        boolean rollback = false;
        boolean created = ensureCurrentTransaction(timeoutInMillis);
        TransactionReference tx = getCurrentTransaction();

        try {
            work.doWork(tx);
            return false;
        } catch (MzsTimeoutException ex) {
            rollback = true;
            return true;
        } catch (MzsCoreException ex) {
            rollback = true;
            throw new RuntimeException(ex);
        } finally {
            if (created) {
                if (rollback) {
                    rollbackCurrentTransaction();
                } else {
                    commitCurrentTransaction();
                }
            }
        }
    }

    private TransactionReference getCurrentTransaction() {
        return currentTransaction.get();
    }

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
            Notification notification = notificationManager.createNotification(partsContainer, new MozartSpacesClockPartListener(listener), Operation.WRITE);
            return new MozartSpacesSubscription(Arrays.asList(notification));
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForClocks(ClockListener listener) {
        try {
            List<Notification> notifications = new ArrayList<Notification>();
            notifications.add(notificationManager.createNotification(assembledClocksContainer, new MozartSpacesClockListener(listener, ClockStatus.ASSEMBLED), Operation.WRITE));
            notifications.add(notificationManager.createNotification(checkedClocksContainer, new MozartSpacesClockListener(listener, ClockStatus.CHECKED), Operation.WRITE));
            notifications.add(notificationManager.createNotification(deliveredClocksContainer, new MozartSpacesClockListener(listener, ClockStatus.DELIVERED), Operation.WRITE));
            return new MozartSpacesSubscription(notifications);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addParts(final List<ClockPart> parts) {
        final Entry[] entries = new Entry[parts.size()];

        for (int i = 0; i < parts.size(); i++) {
            ClockPart part = parts.get(i);
            entries[i] = new Entry(part, KeyCoordinator.newCoordinationData(part.getType()
                .name()));
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
                List<Selector> selectors = new ArrayList<Selector>();

                for (Map.Entry<ClockPartType, Integer> entry : neededClockParts.entrySet()) {
                    selectors.add(KeyCoordinator.newSelector(entry.getKey()
                        .name(), entry.getValue()));
                }

                List<ClockPart> clockParts = capi.take(partsContainer, selectors, MzsConstants.RequestTimeout.INFINITE, tx);
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

                Clock clock = (Clock) capi.take(assembledClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, tx)
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

                selectors.add(LabelCoordinator.newSelector(type.name()));
                selectors.add(FifoCoordinator.newSelector(1));

                Clock clock = (Clock) capi.take(checkedClocksContainer, selectors, MzsConstants.RequestTimeout.INFINITE, tx)
                    .get(0);
                transactionalTask.doWork(clock);
            }
        }, timeout);
    }

    @Override
    public void addAssembledClock(Clock clock) {
        final Entry entry = new Entry(clock, KeyCoordinator.newCoordinationData(clock.getId()
            .toString()));
        transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(assembledClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addCheckedClock(Clock clock, ClockQualityType type) {
        final Entry entry = new Entry(clock, KeyCoordinator.newCoordinationData(clock.getId()
            .toString()), LabelCoordinator.newCoordinationData(type.name()));
        transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(checkedClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }

    @Override
    public void addDeliveredClock(Clock clock) {
        final Entry entry = new Entry(clock, KeyCoordinator.newCoordinationData(clock.getId()
            .toString()));
        transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(deliveredClocksContainer, MzsConstants.RequestTimeout.ZERO, tx, entry);
            }
        });
    }
}
