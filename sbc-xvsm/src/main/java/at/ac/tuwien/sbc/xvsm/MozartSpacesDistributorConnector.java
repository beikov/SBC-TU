package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.DistributorConnector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.DistributorDemand;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mozartspaces.capi3.FifoCoordinator;
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
import org.mozartspaces.core.TransactionReference;

/**
 * A simple MozartSpaces implemenation of the {@link DistributorConnector}.
 */
public class MozartSpacesDistributorConnector extends AbstractMozartSpacesComponent implements DistributorConnector {

    private final UUID distributorId;

    private final URI distributorUri;
    private final MozartSpacesDistributorStockConnector stockConnector;

    private final ContainerReference distributorDemandContainer;

    public MozartSpacesDistributorConnector(UUID distributorId, int serverPort) {
        super(serverPort);
        this.distributorId = distributorId;

        // Retrieve demand container located at the fatory
        distributorDemandContainer = getOrCreateContainer(capi, MozartSpacesConstants.DISTRIBUTOR_DEMAND_CONTAINER_NAME,
                                                          new QueryCoordinator(),
                                                          new FifoCoordinator());

        // Create a space for the distributor
        String oldConfigurationFile = System.getProperty("mozartspaces.configurationFile");
        System.setProperty("mozartspaces.configurationFile", "mozartspaces-server.xml");
        final Capi distributorCapi = new Capi(DefaultMzsCore.newInstance(0));
        System.setProperty("mozartspaces.configurationFile", oldConfigurationFile);
        distributorUri = distributorCapi.getCore()
            .getConfig()
            .getSpaceUri();
        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    distributorCapi.getCore()
                    .shutdown(true);
                }
            });
        // Connect to the distributor space through the stock connector
        stockConnector = new MozartSpacesDistributorStockConnector(distributorUri, distributorId.toString());

        Map<ClockType, Integer> demand = new EnumMap<ClockType, Integer>(ClockType.class);
        demand.put(ClockType.KLASSISCH, 0);
        demand.put(ClockType.SPORT, 0);
        demand.put(ClockType.ZEITZONEN_SPORT, 0);

        // Save an initial demand of zero
        DistributorDemand distributorDemand = new DistributorDemand(distributorUri, distributorId.toString(), demand);
        final Entry demandEntry = new Entry(distributorDemand);

        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                capi.write(distributorDemandContainer, MzsConstants.RequestTimeout.ZERO, tx, demandEntry);
            }
        });

    }

    @Override
    public void setDemand(final Map<ClockType, Integer> demand) {
        DistributorDemand distributorDemand = new DistributorDemand(distributorUri, distributorId.toString(), demand);
        final Entry entry = new Entry(distributorDemand);

        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                List<Selector> selectors = new ArrayList<Selector>();
                Query query = new Query()
                    .filter(Property.forName("uri")
                        .equalTo(distributorUri))
                    .filter(Property.forName("destinationName")
                        .equalTo(distributorId.toString()));
                selectors.add(QueryCoordinator.newSelector(query, MzsConstants.Selecting.COUNT_ALL));

                // Delete the old demand
                capi.delete(distributorDemandContainer, selectors, MzsConstants.RequestTimeout.TRY_ONCE, tx);
                // Save the new demand
                capi.write(entry, distributorDemandContainer, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx);
             
            }
        });

    }

    @Override
    public void removeClockFromStock(Clock removedClock) {
        stockConnector.removeClockFromStock(removedClock);
    }

    @Override
    public Subscription subscribeForDistributorDeliveries(ClockListener listener) {
        return stockConnector.subscribeForDistributorDeliveries(listener);
    }
}
