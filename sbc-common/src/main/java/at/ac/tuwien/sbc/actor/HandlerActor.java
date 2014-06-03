package at.ac.tuwien.sbc.actor;

import java.util.Map;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.Demand;
import at.ac.tuwien.sbc.util.SbcUtils;

public class HandlerActor extends AbstractActor{

	public HandlerActor(Connector connector) {
		super(connector);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
		}

		Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
		AbstractActor actor = new HandlerActor(connector);
		Thread t = new Thread(actor);
		t.start();

		System.out.println("Starting " + actor.getClass().getSimpleName() + " with id " + actor.getId());
		System.out.println("Press CTRL+C to shutdown...");
		while(System.in.read() != -1);
		t.interrupt();
	}


	@Override
	public void run() {
		while (!Thread.interrupted()) {
//			System.out.println("trying to get demand");

			connector.takeDemandedClock(new TransactionalTask<Map<Demand, Clock>>() {
				@Override
				public void doWork(Map<Demand, Clock> param) {
					Demand demand = (Demand) param.keySet().toArray()[0];
					Clock clock = (Clock) param.values().toArray()[0];
					connector.deliverDemandedClock(demand, clock);
				}
			});
		}

	}
}



