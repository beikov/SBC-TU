/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.ClassicClock;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.model.ClockType;
import at.ac.tuwien.sbc.model.OrderPriority;
import at.ac.tuwien.sbc.model.SingleClockOrder;
import at.ac.tuwien.sbc.model.SportsClock;
import at.ac.tuwien.sbc.model.TimezoneSportsClock;
import at.ac.tuwien.sbc.util.SbcUtils;

/**
 *
 * @author Christian
 */
public class AssemblyActor extends AbstractActor {

	OrderPriority lastPriority = null;

	public AssemblyActor(Connector connector) {
		super(connector);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: AssemblyActor PORT (xvsm|jms)");
		}

		Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
		AbstractActor actor = new AssemblyActor(connector);
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

			// Wait for 1-3 seconds
			sleepForSeconds(1, 3);

			connector.takeParts(lastPriority, new TransactionalTask<OrderPriority>() {

				@Override
				public void doWork(OrderPriority lastPriority) {

					List<ClockType> wantedTypes = new ArrayList<ClockType>();
					wantedTypes.add(ClockType.KLASSISCH);
					wantedTypes.add(ClockType.SPORT);
					wantedTypes.add(ClockType.ZEITZONEN_SPORT);

					//check which clocktypes are possible
					List<ClockType> possibleClockTypes = connector.getPossibleClockTypes(wantedTypes);

					//if no clock possible return
					if(possibleClockTypes.isEmpty()){
						//System.out.println("no clocktype possible at the moment");
						return;
					}

					//select single watch order with high priority and a clocktype that is possible
					SingleClockOrder sOrder = connector.getSingleClockOrder(OrderPriority.HOCH, possibleClockTypes);

					//if none was found with high priority try mid priority
					if(sOrder == null){
						sOrder = connector.getSingleClockOrder(OrderPriority.MITTEL, possibleClockTypes);
					}

					//if neither a high nor a medium priority order is available determine if a low priority order should be processed
					if(sOrder == null && AssemblyActor.this.lastPriority == null){
						//System.out.println("trying for lowPrior");
						sOrder = connector.getSingleClockOrder(OrderPriority.NIEDRIG, possibleClockTypes);
						AssemblyActor.this.lastPriority = OrderPriority.NIEDRIG;
					}

					Random r = new Random();
					ClockType typeToProduce = null;
					if(sOrder != null){
						typeToProduce = sOrder.getNeededType();
						//System.out.println("getting type: "+typeToProduce.toString()+" for priority: "+sOrder.getPriority());
					}else{
						typeToProduce = possibleClockTypes.get(r.nextInt(possibleClockTypes.size()));
						AssemblyActor.this.lastPriority = null;
						//System.out.println("getting type: "+typeToProduce.toString()+" for priority: NO ORDER");
					}

					//take the parts and produce the clock
					UUID orderId = ( sOrder != null) ? sOrder.getOrderId() : null;

					//System.out.println("processed for order: "+orderId);
					produceClock(typeToProduce, orderId);

				}

			});
		}
	}

	private void produceClock(final ClockType typeToProduce, final UUID orderId){
		Map<ClockPartType, Integer> neededParts = null;

		switch(typeToProduce){
		case KLASSISCH:	
			neededParts = ClassicClock.NEEDED_PARTS; 
			break;
		case SPORT:		
			neededParts = SportsClock.NEEDED_PARTS; 
			break;
		case ZEITZONEN_SPORT: 
			neededParts = TimezoneSportsClock.NEEDED_PARTS; 
			break;
		}

		connector.takeParts(neededParts, new TransactionalTask<List<ClockPart>>() {

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
						} else if (clockHand2 == null){
							clockHand2 = p;
						} else {
							clockHand3 = p;
						}
						break;
					}
				}
				//System.out.println("set parts");

				if (chassis == null || clockWork == null || wristband == null || clockHand1 == null || clockHand2 == null) {
					throw new IllegalArgumentException("Invalid clock parts have been given!\n" + clockParts);
				}

				Clock clock = null;

				switch(typeToProduce){
				case KLASSISCH: 
					clock = new ClassicClock(chassis, clockWork, wristband, clockHand1, clockHand2, id);
					break;
				case SPORT:
					clock = new SportsClock(chassis, clockWork, wristband, clockHand1, clockHand2, id);
					break;
				case ZEITZONEN_SPORT:
					if(clockHand3 == null){
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
