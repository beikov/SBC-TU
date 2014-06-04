/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import java.net.URI;
import java.util.Arrays;
import org.mozartspaces.capi3.ContainerNameNotAvailableException;
import org.mozartspaces.capi3.Coordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.notifications.NotificationManager;

/**
 *
 * @author Christian
 */
public class AbstractMozartSpacesComponent {

    protected final String serverHost;
    protected final int serverPort;
    protected final URI serverUri;
//  protected final String localHost;
//	protected final int localPort;

    protected final Capi capi;
    protected final MozartSpacesTransactionManager tm;
    protected final NotificationManager notificationManager;

    private static URI getUri(String serverHost, int serverPort) {
        return URI.create("xvsm://" + serverHost + ":" + serverPort);
    }

    public AbstractMozartSpacesComponent(int serverPort) {
        this("localhost", serverPort);
    }

    public AbstractMozartSpacesComponent(String serverHost, int serverPort/* , String localHost, int localPort */) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
//        this.localHost = localHost;
//        this.localPort = localPort;
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

    public AbstractMozartSpacesComponent(Capi capi, URI serverUri, MozartSpacesTransactionManager tm) {
        this.serverHost = null;
        this.serverPort = -1;
        this.serverUri = serverUri;
//        this.localHost = null;
//        this.localPort = null;
        this.capi = capi;
        this.tm = tm;
        this.notificationManager = new NotificationManager(capi.getCore());
    }

    protected void close() {
        this.capi.getCore()
            .shutdown(true);
    }

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
}
