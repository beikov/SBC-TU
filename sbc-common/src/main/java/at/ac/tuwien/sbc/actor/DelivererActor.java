package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.util.SbcUtils;

/**
 * An actor that tries to take checked clocks of a specific quality from the factory stock and make them ready for delivery. If no clock with the wanted quality is available teh actor tries to disassemble faulty clocks.
 */
public class DelivererActor extends AbstractActor {

    private final ClockQualityType type;

    public DelivererActor(Connector connector, ClockQualityType type) {
        super(connector);
        this.type = type;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: DeliveryActor PORT (xvsm|jms) (A|B)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        AbstractActor actor = new DelivererActor(connector, ClockQualityType.valueOf(args[2]));
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
            // Try to take a checked clock, waiting at most 5 seconds to get one
            if (connector.takeChecked(type, 5000, new TransactionalTask<Clock>() {

                @Override
                public void doWork(Clock clock) {
                    clock.deliver(id);
                    connector.addDeliveredClock(clock);
                }

            })) {
                // Timeout occurred, try to disassemble faulty clocks, waiting at most 1 second get one
                connector.takeChecked(ClockQualityType.C, 1000, new TransactionalTask<Clock>() {

                    @Override
                    public void doWork(Clock clock) {
                        connector.addParts(clock.disassemble(id));
                        connector.addDisassembledClock(clock);
                    }
                });
            }
        }
    }

}
