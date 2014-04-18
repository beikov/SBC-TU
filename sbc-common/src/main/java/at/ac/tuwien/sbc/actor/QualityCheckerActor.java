/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.TransactionalTask;
import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockQualityType;

/**
 *
 * @author Christian
 */
public class QualityCheckerActor extends AbstractActor {

    public QualityCheckerActor(Connector connector) {
        super(connector);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            connector.takeAssembled(new TransactionalTask<Clock>() {

                @Override
                public void doWork(Clock clock) {
                    clock.check(id, random.get()
                        .nextInt(10) + 1);
                    connector.addCheckedClock(clock, ClockQualityType.fromQuality(clock.getQuality()));
                }

            });
        }
    }

}
