package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.util.SbcUtils;

/**
 * An actor that takes assembled clocks from the factory stock and check their qualities.
 */
public class QualityCheckerActor extends AbstractActor {

    public QualityCheckerActor(Connector connector) {
        super(connector);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        AbstractActor actor = new QualityCheckerActor(connector);
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
            connector.takeAssembled(new TransactionalTask<Clock>() {

                @Override
                public void doWork(Clock clock) {
                    // Get a random quality between 1 and 10
                    clock.check(id, random.get()
                                .nextInt(10) + 1);
                    connector.addCheckedClock(clock);
                }

            });
        }
    }

}
