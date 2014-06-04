/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Christian
 */
public class AssemblyActor extends AbstractActor {

    private final boolean doWait;
    OrderPriority lastPriority = null;

    public AssemblyActor(Connector connector, boolean doWait) {
        super(connector);
        this.doWait = doWait;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: AssemblyActor PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        AbstractActor actor = new AssemblyActor(connector, true);
        Thread t = new Thread(actor);
        t.start();

        System.out.println("Starting " + actor.getClass()
            .getSimpleName() + " with id " + actor.getId());
        System.out.println("Press CTRL+C to shutdown...");
        while (System.in.read() != -1);
        t.interrupt();
    }

    @Override
    public void run() {

        while (!Thread.interrupted()) {
            if (doWait) {
                // Wait for 1-3 seconds
                sleepForSeconds(1, 3);
            }

            TransactionalTask<SingleClockOrder> productionTask = new TransactionalTask<SingleClockOrder>() {

                @Override
                public void doWork(SingleClockOrder order) {
                    produceClock(order.getNeededType(), order.getOrderId());
                }

            };

            // First try high priority single clock orders
            boolean done = connector.takeSingleClockOrder(OrderPriority.HOCH, productionTask);
            // If none can be found try middle priority single clock orders
            done = done ? true : connector.takeSingleClockOrder(OrderPriority.MITTEL, productionTask);

            // If none can be found check if a low priority single clock order should be tried
            if (!done && AssemblyActor.this.lastPriority == null) {
                // Try low priority single clock orders
                done = connector.takeSingleClockOrder(OrderPriority.NIEDRIG, productionTask);
                // Remember that we tried low priority so we can alternate
                AssemblyActor.this.lastPriority = OrderPriority.NIEDRIG;
            }

            if (!done) {
                AssemblyActor.this.lastPriority = null;
                produceClock(getRandomClockType(), null);
            }
        }
    }

    private ClockType getRandomClockType() {
        ClockType[] types = ClockType.values();
        return types[random.get()
            .nextInt(types.length)];
    }

    private void produceClock(final ClockType typeToProduce, final UUID orderId) {
        connector.takeParts(typeToProduce.getNeededParts(), new TransactionalTask<List<ClockPart>>() {

            @Override
            public void doWork(List<ClockPart> clockParts) {

                ClockPart chassis = null;
                ClockPart clockWork = null;
                ClockPart wristband = null;
                ClockPart clockHand1 = null;
                ClockPart clockHand2 = null;
                ClockPart clockHand3 = null;

                for (ClockPart p : clockParts) {
                    switch (p.getType()) {
                        case GEHAEUSE:
                            chassis = p;
                            break;
                        case UHRWERK:
                            clockWork = p;
                            break;
                        case LEDERARMBAND:
                        case METALLARMBAND:
                            wristband = p;
                            break;
                        case ZEIGER:
                            if (clockHand1 == null) {
                                clockHand1 = p;
                            } else if (clockHand2 == null) {
                                clockHand2 = p;
                            } else {
                                clockHand3 = p;
                            }
                            break;
                    }
                }

                if (chassis == null || clockWork == null || wristband == null || clockHand1 == null || clockHand2 == null) {
                    throw new IllegalArgumentException("Invalid clock parts have been given!\n" + clockParts);
                }

                Clock clock = null;

                switch (typeToProduce) {
                    case KLASSISCH:
                        clock = new ClassicClock(chassis, clockWork, wristband, clockHand1, clockHand2, id);
                        break;
                    case SPORT:
                        clock = new SportsClock(chassis, clockWork, wristband, clockHand1, clockHand2, id);
                        break;
                    case ZEITZONEN_SPORT:
                        if (clockHand3 == null) {
                            throw new IllegalArgumentException("Invalid clock parts have been given!\n" + clockHand3);
                        }
                        clock = new TimezoneSportsClock(chassis, clockWork, wristband, clockHand1, clockHand2, clockHand3, id);
                        break;
                }
                clock.setOrderId(orderId);
                connector.addAssembledClock(clock);
            }
        });
    }

}
