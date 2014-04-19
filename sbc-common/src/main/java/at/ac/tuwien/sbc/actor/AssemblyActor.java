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
import at.ac.tuwien.sbc.model.ClockPartType;
import at.ac.tuwien.sbc.util.SbcUtils;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian
 */
public class AssemblyActor extends AbstractActor {
    
    private static final Map<ClockPartType, Integer> NEEDED_PARTS;
    
    static {
        Map<ClockPartType, Integer> map = new EnumMap<ClockPartType, Integer>(ClockPartType.class);
        map.put(ClockPartType.ARMBAND, 1);
        map.put(ClockPartType.GEHAEUSE, 1);
        map.put(ClockPartType.UHRWERK, 1);
        map.put(ClockPartType.ZEIGER, 2);
        NEEDED_PARTS = Collections.unmodifiableMap(map);
    }
    
    public AssemblyActor(Connector connector) {
        super(connector);
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: AssemblyActor PORT (xvsm|jms)");
        }
        
        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        new AssemblyActor(connector).run();
    }
    
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            connector.takeParts(NEEDED_PARTS, new TransactionalTask<List<ClockPart>>() {

                @Override
                public void doWork(List<ClockPart> clockParts) {
                    // Wait for 1-3 seconds
                    sleepForSeconds(1, 3);
                    ClockPart chassis = null;
                    ClockPart clockWork = null;
                    ClockPart wristband = null;
                    ClockPart clockHand1 = null;
                    ClockPart clockHand2 = null;

                    for (ClockPart p : clockParts) {
                        switch (p.getType()) {
                            case GEHAEUSE:
                                chassis = p;
                                break;
                            case UHRWERK:
                                clockWork = p;
                                break;
                            case ARMBAND:
                                wristband = p;
                                break;
                            case ZEIGER:
                                if (clockHand1 == null) {
                                    clockHand1 = p;
                                } else {
                                    clockHand2 = p;
                                }
                                break;
                        }
                    }

                    if (chassis == null || clockWork == null || wristband == null || clockHand1 == null || clockHand2 == null) {
                        throw new IllegalArgumentException("Invalid clock parts have been given!\n" + clockParts);
                    }

                    Clock clock = new Clock(chassis, clockWork, wristband, clockHand1, clockHand2, id);
                    connector.addAssembledClock(clock);
                }
            });
        }
    }
    
}
