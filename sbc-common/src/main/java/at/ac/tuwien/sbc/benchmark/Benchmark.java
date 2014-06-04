/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.benchmark;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.actor.AbstractActor;
import at.ac.tuwien.sbc.actor.AssemblyActor;
import at.ac.tuwien.sbc.actor.DelivererActor;
import at.ac.tuwien.sbc.actor.HandlerActor;
import at.ac.tuwien.sbc.actor.QualityCheckerActor;
import at.ac.tuwien.sbc.actor.SupplierActor;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockStatus;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Christian
 */
public class Benchmark {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Benchmark PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        
        SupplierActor gehaeuseSupplier = new SupplierActor(connector, ClockPartType.GEHAEUSE, 1500);
        SupplierActor uhrwerkSupplier = new SupplierActor(connector, ClockPartType.UHRWERK, 1500);
        SupplierActor lederArmbandSupplier = new SupplierActor(connector, ClockPartType.LEDERARMBAND, 750);
        SupplierActor metallArmbandSupplier = new SupplierActor(connector, ClockPartType.METALLARMBAND, 750);
        SupplierActor zeigerSupplier = new SupplierActor(connector, ClockPartType.ZEIGER, 3750);
        
        gehaeuseSupplier.run();
        uhrwerkSupplier.run();
        lederArmbandSupplier.run();
        metallArmbandSupplier.run();
        zeigerSupplier.run();
        
        List<Thread> threads = new ArrayList<Thread>();
        AssemblyActor assembly1 = new AssemblyActor(connector, false);
        AssemblyActor assembly2 = new AssemblyActor(connector, false);
        QualityCheckerActor quality1 = new QualityCheckerActor(connector);
        DelivererActor delivererA = new DelivererActor(connector, ClockQualityType.A);
        DelivererActor delivererb = new DelivererActor(connector, ClockQualityType.B);
        
        threads.add(new Thread(assembly1));
        threads.add(new Thread(assembly2));
        threads.add(new Thread(quality1));
        threads.add(new Thread(delivererA));
        threads.add(new Thread(delivererb));
        
        for (Thread t : threads) {
            t.start();
        }

        System.out.println("Starting Benchmark");
        
        Thread.sleep(60000);
        
        for (Thread t : threads) {
            t.interrupt();
        }
        
        int count = 0;
        
        for (Clock c : connector.getClocks()) {
            if (c.getStatus() == ClockStatus.DELIVERED) {
                count++;
            }
        }
        
        System.out.println("Throughput: " + count);
    }
}
