package at.ac.tuwien.sbc.util;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.DistributorConnector;
import java.util.UUID;

/**
 * A utility class for creating connectors
 */
public final class SbcUtils {

    /**
     * Creates a factory connector for the given port and type.
     *
     * @param port the port at which the server listens
     * @param type the type of the connector, either xvsm or jms
     * @return a new connector instance
     */
    public static Connector getConnector(int port, String type) {
        Exception reason = null;

        try {
            if ("xvsm".equals(type)) {
                return (Connector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.xvsm.MozartSpacesConnector")
                    .getConstructor(int.class)
                    .newInstance(port);
            } else if ("jms".equals(type)) {
                return (Connector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.jms.JmsConnector")
                    .getConstructor(int.class)
                    .newInstance(port);
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        } catch (Exception ex) {
            reason = ex;
        }

        throw new IllegalArgumentException("Could not create the connector", reason);
    }

    /**
     * Creates a distributor connector for the given port and type.
     *
     * @param distributorId the id of the distributor
     * @param port          the port at which the server listens
     * @param type          the type of the distributor connector, either xvsm or jms
     * @return a new distributor connector instance
     */
    public static DistributorConnector getDistributorConnector(UUID distributorId, int port, String type) {
        Exception reason = null;

        try {
            if ("xvsm".equals(type)) {
                return (DistributorConnector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.xvsm.MozartSpacesDistributorConnector")
                    .getConstructor(UUID.class, int.class)
                    .newInstance(distributorId, port);
            } else if ("jms".equals(type)) {
                return (DistributorConnector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.jms.JmsDistributorConnector")
                    .getConstructor(UUID.class, int.class)
                    .newInstance(distributorId, port);
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        } catch (Exception ex) {
            reason = ex;
        }

        throw new IllegalArgumentException("Could not create the connector", reason);
    }
}
