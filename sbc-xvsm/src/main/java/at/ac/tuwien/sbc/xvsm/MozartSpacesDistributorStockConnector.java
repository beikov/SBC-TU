/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.IsolationLevel;
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
import org.mozartspaces.notifications.Operation;

/**
 *
 * @author Christian
 */
public class MozartSpacesDistributorStockConnector extends AbstractMozartSpacesComponent {

    private final ContainerReference distributorStockContainer;

    public MozartSpacesDistributorStockConnector(URI distributorUri, String destinationName) {
        super(distributorUri.getHost(), distributorUri.getPort());

        distributorStockContainer = getOrCreateContainer(capi, distributorUri,
                                                         MozartSpacesConstants.DISTRIBUTOR_STOCK_CONTAINER_NAME
                                                         + destinationName,
                                                         new QueryCoordinator(), new FifoCoordinator());
    }

    public Map<ClockType, Integer> getDistributorStock() throws MzsCoreException {
        ClockType[] types = { ClockType.KLASSISCH, ClockType.SPORT, ClockType.ZEITZONEN_SPORT };
        Map<ClockType, Integer> stock = new EnumMap<ClockType, Integer>(ClockType.class);

        for (ClockType type : types) {
            List<Selector> selectors = new ArrayList<Selector>();
            Query query = new Query().filter(Property.forName("type")
                .equalTo(type));
            selectors.add(QueryCoordinator.newSelector(query, MzsConstants.Selecting.COUNT_ALL));

            try {
                stock.put(type,
                          capi.test(distributorStockContainer, selectors, MzsConstants.RequestTimeout.TRY_ONCE, null,
                                    IsolationLevel.READ_COMMITTED, null));
            } catch (MzsCoreException e) {
                //okay no clocks of this type available
                stock.put(type, 0);
            }
        }

        return stock;

    }

    public void removeClockFromStock(final Clock removedClock) {
        final Query query = new Query().filter(Property.forName("serialId")
            .equalTo(removedClock.getSerialId()));
        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.delete(distributorStockContainer, Arrays.asList(QueryCoordinator.newSelector(query)),
                            MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx);
            }
        });
    }

    public void deliver(Clock clock) {
        final Entry entry = new Entry(clock);
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(entry, distributorStockContainer, MzsConstants.RequestTimeout.ZERO, tx);
            }
        });
    }

    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        try {
            Notification notification = notificationManager.createNotification(distributorStockContainer,
                                                                               new MozartSpacesClockListener(listener),
                                                                               Operation.WRITE, Operation.TAKE, Operation.DELETE);
            return new MozartSpacesSubscription(Arrays.asList(notification));
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
