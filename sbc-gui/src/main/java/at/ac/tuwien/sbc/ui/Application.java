/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.actor.AssemblyActor;
import at.ac.tuwien.sbc.actor.DelivererActor;
import at.ac.tuwien.sbc.actor.QualityCheckerActor;
import at.ac.tuwien.sbc.actor.SupplierActor;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Christian
 */
public class Application {
    
    // NOTE: The following main is just for local testing
    
//    public static void main(String[] args) {
//        System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
//        Connector connector = SbcUtils.getConnector(4242, "xvsm");
//        ExecutorService threadPool = Executors.newCachedThreadPool();
//        threadPool.submit(new AssemblyActor(connector));
//        threadPool.submit(new QualityCheckerActor(connector));
//        threadPool.submit(new DelivererActor(connector, ClockQualityType.A));
//        threadPool.submit(new DelivererActor(connector, ClockQualityType.B));
//        MainFrame.start(connector, threadPool);
//    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }
        
        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        // Thread pool for suppliers
        ExecutorService threadPool = Executors.newCachedThreadPool();
        MainFrame.start(connector, threadPool);
    }
}
