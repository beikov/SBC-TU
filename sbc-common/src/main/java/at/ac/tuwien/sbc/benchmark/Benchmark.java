package at.ac.tuwien.sbc.benchmark;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.actor.AssemblyActor;
import at.ac.tuwien.sbc.actor.DelivererActor;
import at.ac.tuwien.sbc.actor.QualityCheckerActor;
import at.ac.tuwien.sbc.actor.SupplierActor;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.model.ClockStatus;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * A benchmark that simulates load to measure throughput.
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
        AssemblyActor assembly1 = new AssemblyActor(SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]), false);
        AssemblyActor assembly2 = new AssemblyActor(SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]), false);
        QualityCheckerActor quality1 = new QualityCheckerActor(SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]));
        DelivererActor delivererA = new DelivererActor(SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]),
                                                       ClockQualityType.A);
        DelivererActor delivererb = new DelivererActor(SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]),
                                                       ClockQualityType.B);

        threads.add(new Thread(assembly1));
        threads.add(new Thread(assembly2));
        threads.add(new Thread(quality1));
        threads.add(new Thread(delivererA));
        threads.add(new Thread(delivererb));

        Thread.UncaughtExceptionHandler eh = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // ignore
            }
        };

        for (Thread t : threads) {
            t.start();
        }

        System.out.println("Starting Benchmark");
        long startTime = System.currentTimeMillis();

        int sleptTime = 10000;
        int sleepTime = 5000;
        boolean productionDone = false;
        int deliveredCount = -1;
        boolean deliveryDone = false;

        // First sleep 10 seconds then start checking whether its done yet
        Thread.sleep(sleptTime);
        do {
            if (!productionDone) {
                productionDone = productionDone(connector);
            }
            if (productionDone) {
                if (deliveredCount == -1) {
                    deliveredCount = getDeliveredCount(connector, false);
                }
                if (deliveredCount != -1) {
                    deliveryDone = true;
                    break;
                }
            }

            Thread.sleep(sleepTime);
            sleptTime += sleepTime;
        } while (sleptTime != 60000);

        int count = deliveryDone ? deliveredCount : getDeliveredCount(connector, true);

        // We stop every worker thread before measuring
        for (Thread t : threads) {
            t.setUncaughtExceptionHandler(eh);
            t.interrupt();
        }

        System.out.println("Throughput: " + count);
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
        System.out.println("Done faster: " + deliveryDone);
    }

    private static boolean productionDone(Connector connector) {
        List<ClockPart> parts = connector.getClockParts();
        int size = parts.size();

        int gehaeuse = 0;
        int uhrwerk = 0;
        int zeiger = 0;
        int lederarmband = 0;
        int metallarmband = 0;

        for (int i = 0; i < size; i++) {
            ClockPart p = parts.get(i);

            switch (p.getType()) {
                case GEHAEUSE:
                    gehaeuse++;
                    break;
                case UHRWERK:
                    uhrwerk++;
                    break;
                case ZEIGER:
                    zeiger++;
                    break;
                case LEDERARMBAND:
                    lederarmband++;
                    break;
                case METALLARMBAND:
                    metallarmband++;
                    break;
            }

            if (gehaeuse != 0 && uhrwerk != 0 && zeiger > 1 && (lederarmband != 0 || metallarmband != 0)) {
                // We can produce a classic or sport clock, so production is not done
                return false;
            }
        }

        // If we get here, we couldn't find enough clock parts in the store for production
        return true;
    }

    /**
     * If all clocks are delivered returns the count of delivered clocks, otherwise -1.
     *
     * @param connector   the connector to use to get the clocks
     * @param returnCount if true, always the count is returned, otherwise only if all clocks are delivered
     * @return The count of delivered clocks if all are delivered, otherwise -1.
     */
    private static int getDeliveredCount(Connector connector, boolean returnCount) {
        List<Clock> clocks = connector.getClocks();
        int size = clocks.size();
        int count = 0;

        if (returnCount) {
            for (int i = 0; i < size; i++) {
                if (clocks.get(i)
                    .getStatus() == ClockStatus.DELIVERED) {
                    count++;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (clocks.get(i)
                    .getStatus() == ClockStatus.DELIVERED) {
                    count++;
                } else {
                    return -1;
                }
            }
        }

        return count;
    }
}
