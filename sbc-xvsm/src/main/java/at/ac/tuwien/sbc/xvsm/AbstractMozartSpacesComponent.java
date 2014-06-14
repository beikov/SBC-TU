package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.Subscription;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.mozartspaces.capi3.ContainerNameNotAvailableException;
import org.mozartspaces.capi3.Coordinator;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.IsolationLevel;
import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.NotificationManager;
import org.mozartspaces.notifications.Operation;

/**
 * An abstract base class for MozartSpaces components to reduce the boilerplate of implementations.
 */
public class AbstractMozartSpacesComponent {

    protected final URI serverUri;

    protected final Capi capi;
    protected final MozartSpacesTransactionManager tm;
    protected final NotificationManager notificationManager;

    private static URI getUri(String serverHost, int serverPort) {
        return URI.create("xvsm://" + serverHost + ":" + serverPort);
    }

    /**
     * Like {@link AbstractMozartSpacesComponent#AbstractMozartSpacesComponent(java.lang.String, int) } but with the default host <code>localhost</code>.
     *
     * @param serverPort the port to connect to
     */
    public AbstractMozartSpacesComponent(int serverPort) {
        this("localhost", serverPort);
    }

    /**
     * Creates a new MozartSpaces Component with the given host and port.
     *
     * @param serverHost the host to connect to
     * @param serverPort the port to connect to
     */
    public AbstractMozartSpacesComponent(String serverHost, int serverPort) {
        this.capi = new Capi(DefaultMzsCore.newInstance(0));
        this.serverUri = getUri(serverHost, serverPort);
        this.tm = new MozartSpacesTransactionManager(capi, serverUri);
        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    capi.getCore()
                    .shutdown(true);
                }
            });
        this.notificationManager = new NotificationManager(capi.getCore());
    }

    /**
     * Reuse of the given CAPI and transaction manager instead of creating a new one.
     *
     * @param capi      the capi to use
     * @param serverUri the server uri to use
     * @param tm        the transaction manager to use
     */
    public AbstractMozartSpacesComponent(Capi capi, URI serverUri, MozartSpacesTransactionManager tm) {
        this.serverUri = serverUri;
        this.capi = capi;
        this.tm = tm;
        this.notificationManager = new NotificationManager(capi.getCore());
    }

    /**
     * Closes the MozartSpacesCore created by this component
     */
    protected void close() {
        this.capi.getCore()
            .shutdown(true);
    }

    /**
     * Returns an existing or creates and returns the new container with the given CAPI at the server Space URI with the given name and coordinators.
     *
     * @param capi         the CAPI to lookup and create the container
     * @param name         the name of the container which to lookup or create
     * @param coordinators the coordinators that should be used for the container
     * @return never null, either the reference to the existing or the newly created container
     */
    protected ContainerReference getOrCreateContainer(Capi capi, String name, Coordinator... coordinators) {
        try {
            return capi.lookupContainer(name, serverUri, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, null);
        } catch (MzsCoreException ex) {
            try {
                return capi
                    .createContainer(name, serverUri, MzsConstants.Container.UNBOUNDED, Arrays.asList(coordinators), null, null);
            } catch (ContainerNameNotAvailableException ex2) {
                // Someone else was faster...
                return getOrCreateContainer(capi, name, coordinators);
            } catch (MzsCoreException ex2) {
                throw new RuntimeException(ex2);
            }
        }
    }

    /**
     * Returns an existing or creates and returns the new container with the given CAPI at the given Space URI with the given name and coordinators.
     *
     * @param capi         the CAPI to lookup and create the container
     * @param uri          the Space URI at which to lookup or create the container
     * @param name         the name of the container which to lookup or create
     * @param coordinators the coordinators that should be used for the container
     * @return never null, either the reference to the existing or the newly created container
     */
    protected ContainerReference getOrCreateContainer(Capi capi, URI uri, String name, Coordinator... coordinators) {
        try {
            return capi.lookupContainer(name, uri, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, null);
        } catch (MzsCoreException ex) {
            try {
                return capi
                    .createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(coordinators), null, null);
            } catch (ContainerNameNotAvailableException ex2) {
                // Someone else was faster...
                return getOrCreateContainer(capi, uri, name, coordinators);
            } catch (MzsCoreException ex2) {
                throw new RuntimeException(ex2);
            }
        }
    }

    /**
     * Creates a subscription for the given destinations.
     *
     * @param listener   the listener for this subscription
     * @param operations the operations for which the listener should be registered
     * @param containers the containers at which the listener should be registered
     * @return a subscription for the registration that can be cancelled
     */
    protected Subscription subscribeListener(NotificationListener listener, Set<Operation> operations, ContainerReference... containers) {
        final List<Notification> notifications = new ArrayList<Notification>();
        TransactionReference tx = null;

        try {
            for (ContainerReference container : containers) {
                notifications.add(notificationManager.createNotification(container, listener, operations, tx, null));
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }

        return new MozartSpacesSubscription(notifications);
    }

    /**
     * Returns the containers content as list
     *
     * @param <T>        the expected object type
     * @param containers the references of the containers
     * @return a list of objects
     */
    protected <T> List<T> containersAsList(ContainerReference... containers) {
        List<T> list = new ArrayList<T>();
        List<? extends Selector> selectors = Arrays.asList(FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL));

        try {
            for (ContainerReference container : containers) {
                list.addAll((List) capi.read(container, selectors, MzsConstants.RequestTimeout.INFINITE, null,
                                             IsolationLevel.READ_COMMITTED, null));
            }

            return list;
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }
}
