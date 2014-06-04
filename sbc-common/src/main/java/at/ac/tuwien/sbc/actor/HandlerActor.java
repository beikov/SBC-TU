package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.util.SbcUtils;

public class HandlerActor extends AbstractActor {

    public HandlerActor(Connector connector) {
        super(connector);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: HandlerActor PORT (xvsm|jms)");
        }

        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        AbstractActor actor = new HandlerActor(connector);
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
            try {
                connector.deliverDemandedClock();
            } catch (RuntimeException ex) {
                ex.printStackTrace(System.err);
            }
        }

    }
}
