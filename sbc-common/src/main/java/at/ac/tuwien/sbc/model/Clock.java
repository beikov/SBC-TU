/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.model;

import java.util.UUID;

/**
 *
 * @author Christian
 */
public class Clock {
    
    private final ClockPart chassis;
    private final ClockPart clockWork;
    private final ClockPart wristband;
    private final ClockPart clockHand1;
    private final ClockPart clockHand2;
    private final UUID assemblerId;

    public Clock(ClockPart chassis, ClockPart clockWork, ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2, UUID assemblerId) {
        this.chassis = chassis;
        this.clockWork = clockWork;
        this.wristband = wristband;
        this.clockHand1 = clockHand1;
        this.clockHand2 = clockHand2;
        this.assemblerId = assemblerId;
    }

}
