package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.util.SbcUtils;

/**
 * Hello world!
 *
 */
public class DistributorApp {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        DistributorFrame.start(connector);
    }
}
