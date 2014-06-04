/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.DistributorConnector;
import at.ac.tuwien.sbc.Subscription;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.DistributorDemand;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.Property;
import org.mozartspaces.capi3.Query;
import org.mozartspaces.capi3.QueryCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

/**
 *
 * @author Christian
 */
public class MozartSpacesDistributorConnector extends AbstractMozartSpacesComponent implements DistributorConnector {

    private final UUID distributorId;

    private final URI distributorUri;
    private final MozartSpacesDistributorStockConnector stockConnector;

    private final ContainerReference distributorDemandContainer;

    public MozartSpacesDistributorConnector(UUID distributorId, int serverPort) {
        super(serverPort);
        this.distributorId = distributorId;

        distributorDemandContainer = getOrCreateContainer(capi, MozartSpacesConstants.DISTRIBUTOR_DEMAND_CONTAINER_NAME,
                                                          new QueryCoordinator(),
                                                          new FifoCoordinator());

        String oldConfigurationFile = System.getProperty("mozartspaces.configurationFile");
        System.setProperty("mozartspaces.configurationFile", "mozartspaces-server.xml");
        final Capi distributorCapi = new Capi(DefaultMzsCore.newInstance(0));
        System.setProperty("mozartspaces.configurationFile", oldConfigurationFile);
        distributorUri = distributorCapi.getCore()
            .getConfig()
            .getSpaceUri();
        // The following uses the first ip address it can find as host instead of localhost
        // distributorURI = URI.create("xvsm://" + SbcUtils.getLocalIpAddress() + ":" + distributorURI.getPort());
        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    distributorCapi.getCore()
                    .shutdown(true);
                }
            });
        stockConnector = new MozartSpacesDistributorStockConnector(distributorUri, distributorId.toString());

        Map<ClockType, Integer> demand = new EnumMap<ClockType, Integer>(ClockType.class);
        demand.put(ClockType.KLASSISCH, 0);
        demand.put(ClockType.SPORT, 0);
        demand.put(ClockType.ZEITZONEN_SPORT, 0);

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
    public void setDemand(Map<ClockType, Integer> demand) {
        DistributorDemand distributorDemand = new DistributorDemand(distributorUri, distributorId.toString(), demand);
        final Entry entry = new Entry(distributorDemand);

        tm.transactional(new TransactionalWork() {
            @Override
            public void doWork(TransactionReference tx) throws MzsCoreException {
                Query query = new Query().filter(Property.forName("distributorId")
                    .equalTo(distributorId));
                capi.delete(distributorDemandContainer, QueryCoordinator.newSelector(query),
                            MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tx);
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
