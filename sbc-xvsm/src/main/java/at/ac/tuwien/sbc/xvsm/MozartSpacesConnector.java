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
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;

/**
 *
 * @author Christian
 */
public class MozartSpacesConnector implements Connector {

    private static final int DEFAULT_PORT = 9877;
    private static final int MAX_TIMEOUT_MILLIS = 2000;
    
    private final int port;
    private final Capi capi;
    private final URI uri;

    public MozartSpacesConnector() {
        port = DEFAULT_PORT;
        capi = new Capi(DefaultMzsCore.newInstance(port));
        uri = URI.create("xvsm://localhost:" + port);
        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        capi.clearSpace(uri);
                    } catch (MzsCoreException e) {
                        e.printStackTrace(System.err);
                    }
                }
            });
    }
    
    private ContainerReference createContainer(String name) {
        try {
            return capi.createContainer(name, uri, MzsConstants.Container.UNBOUNDED, Arrays.asList(new FifoCoordinator()), Arrays.asList(new LindaCoordinator()), null);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private ContainerReference getContainer(String name) {
        try {
            return capi.lookupContainer(name, uri, MAX_TIMEOUT_MILLIS, null);
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Subscription subscribeForClockParts(ClockPartListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Subscription subscribeForClocks(ClockListener listener) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addParts(List<ClockPart> parts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void takeTransactional(Map<ClockPartType, Integer> neededClockParts, TransactionalTask<List<ClockPart>> transactionalTask) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addClock(Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addCheckedClock(Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
