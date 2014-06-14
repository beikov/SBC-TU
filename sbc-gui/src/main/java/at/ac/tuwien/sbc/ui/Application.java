package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The entry point for the factory GUI.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        // Thread pool for suppliers
        ExecutorService threadPool = Executors.newCachedThreadPool();
        System.out.println("Starting FactoryGUI...");
        MainFrame.start(connector, threadPool);
    }
}
