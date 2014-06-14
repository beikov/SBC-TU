package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
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
import org.mozartspaces.notifications.Operation;

/**
 * A connector implementation to communicate with the stock of a distributor.
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

    /**
     * Returns the current stock of the distributor.
     *
     * @return the current stock of the distributor
     * @throws MzsCoreException
     */
    public Map<ClockType, Integer> getDistributorStock() throws MzsCoreException {
        ClockType[] types = { ClockType.KLASSISCH, ClockType.SPORT, ClockType.ZEITZONEN_SPORT };
        Map<ClockType, Integer> stock = new EnumMap<ClockType, Integer>(ClockType.class);

        for (ClockType type : types) {
            List<Selector> selectors = new ArrayList<Selector>();
            Query query = new Query().filter(Property.forName("type")
                .equalTo(type));
            selectors.add(QueryCoordinator.newSelector(query, MzsConstants.Selecting.COUNT_ALL));

            try {
                // Query the count for each type
                stock.put(type,
                          capi.test(distributorStockContainer, selectors, MzsConstants.RequestTimeout.TRY_ONCE, null,
                                    IsolationLevel.READ_COMMITTED, null));
            } catch (MzsCoreException e) {
                // No clocks of this type available
                stock.put(type, 0);
            }
        }

        return stock;

    }

    /**
     * Removes the given clock from the distributor stock.
     *
     * @param removedClock the clock to be removed
     */
    public void removeClockFromStock(final Clock removedClock) {
        final Query query = new Query().filter(Property.forName("serialId")
            .equalTo(removedClock.getSerialId()));
        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                // Remove by id
                capi.delete(distributorStockContainer, Arrays.asList(QueryCoordinator.newSelector(query)),
                            MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx);
            }
        });
    }

    /**
     * Delivers the given clock to the distributor stock.
     *
     * @param clock the clock to be delivered.
     */
    public void deliver(Clock clock) {
        final Entry entry = new Entry(clock);
        tm.transactional(new TransactionalWork() {

            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(entry, distributorStockContainer, MzsConstants.RequestTimeout.ZERO, tx);
            }
        });
    }

    /**
     * Registers a listener for clock updates in the distributor stock.
     *
     * @param listener the listener to be registered
     * @return a subscription for the registration that can be cancelled
     */
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        return subscribeListener(new MozartSpacesClockListener(listener), EnumSet.of(Operation.WRITE, Operation.TAKE,
                                                                                     Operation.DELETE),
                                 distributorStockContainer);
    }
}
