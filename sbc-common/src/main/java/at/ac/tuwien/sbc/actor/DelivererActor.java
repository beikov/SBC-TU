/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockQualityType;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Christian
 */
public class DelivererActor extends AbstractActor {

    private final ClockQualityType type;

    public DelivererActor(Connector connector, ClockQualityType type) {
        super(connector);
        this.type = type;
    }
    
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: DeliveryActor PORT (xvsm|jms) (A|B)");
        }
        
        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        new DelivererActor(connector, ClockQualityType.valueOf(args[2])).run();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            if (connector.takeChecked(type, 1000, new TransactionalTask<Clock>() {

                @Override
                public void doWork(Clock clock) {
                    clock.check(id, random.get()
                        .nextInt(10) + 1);
                    connector.addDeliveredClock(clock);
                }

            })) {
                // Timeout occurred
                connector.takeChecked(ClockQualityType.C, 100, new TransactionalTask<Clock>() {

                    @Override
                    public void doWork(Clock clock) {
                        List<ClockPart> parts = new ArrayList<ClockPart>();
                        parts.add(clock.getChassis());
                        parts.add(clock.getClockHand1());
                        parts.add(clock.getClockHand2());
                        parts.add(clock.getWristband());
                        connector.addParts(parts);
                    }
                });
            }
        }
    }

}
