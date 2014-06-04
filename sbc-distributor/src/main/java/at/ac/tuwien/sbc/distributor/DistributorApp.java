package at.ac.tuwien.sbc.distributor;

import at.ac.tuwien.sbc.DistributorConnector;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.UUID;

/**
 * Hello world!
 *
 */
public class DistributorApp {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }

        UUID id = UUID.randomUUID();
        DistributorConnector connector = SbcUtils.getDistributorConnector(id, Integer.parseInt(args[0]), args[1]);
        System.out.println("Starting DistributerGUI with id " + id.toString());
        DistributorFrame.start(id, connector);
    }
}
