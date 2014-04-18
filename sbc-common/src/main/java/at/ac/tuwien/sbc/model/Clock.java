/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Christian
 */
public class Clock implements Serializable  {
    
    private final UUID id;
    private final ClockPart chassis;
    private final ClockPart clockWork;
    private final ClockPart wristband;
    private final ClockPart clockHand1;
    private final ClockPart clockHand2;
    private final UUID assemblerId;
    
    private ClockStatus status;
    
    private int quality;
    private UUID qualityCheckerId;
    
    private UUID delivererId;

    public Clock(ClockPart chassis, ClockPart clockWork, ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2, UUID assemblerId) {
        this.id = UUID.randomUUID();
        this.chassis = chassis;
        this.clockWork = clockWork;
        this.wristband = wristband;
        this.clockHand1 = clockHand1;
        this.clockHand2 = clockHand2;
        this.assemblerId = assemblerId;
        this.status = ClockStatus.ASSEMBLED;
    }
    
    public void check(UUID qualityCheckerId, int quality) {
        if (qualityCheckerId == null) {
            throw new IllegalArgumentException("Invalid quality checker id!");
        }
        if (quality < 1 || quality > 10) {
            throw new IllegalArgumentException("Invalid quality value: " + quality);
        }
        if (status != ClockStatus.ASSEMBLED) {
            throw new IllegalStateException("Clock is either already checked or delivered!");
        }
        
        this.status = ClockStatus.CHECKED;
        this.qualityCheckerId = qualityCheckerId;
        this.quality = quality;
    }
    
    public void deliver(UUID delivererId) {
        if (delivererId == null) {
            throw new IllegalArgumentException("Invalid deliverer id!");
        }
        if (status != ClockStatus.CHECKED) {
            throw new IllegalStateException("Clock is either not yet checked or already delivered!");
        }
        
        this.status = ClockStatus.DELIVERED;
        this.delivererId = delivererId;
    }

    public UUID getId() {
        return id;
    }

    public ClockPart getChassis() {
        return chassis;
    }

    public ClockPart getClockWork() {
        return clockWork;
    }

    public ClockPart getWristband() {
        return wristband;
    }

    public ClockPart getClockHand1() {
        return clockHand1;
    }

    public ClockPart getClockHand2() {
        return clockHand2;
    }

    public UUID getAssemblerId() {
        return assemblerId;
    }

    public ClockStatus getStatus() {
        return status;
    }

    public int getQuality() {
        return quality;
    }

    public UUID getQualityCheckerId() {
        return qualityCheckerId;
    }

    public UUID getDelivererId() {
        return delivererId;
    }

}
