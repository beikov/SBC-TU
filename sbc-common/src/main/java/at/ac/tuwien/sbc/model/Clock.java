/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Christian
 */
public abstract class Clock implements Comparable<Clock>, Serializable {

    private final UUID id;
    private long serialId;
    private final long createdTime;
    private long updatedTime;
    private final ClockPart chassis;
    private final ClockPart clockWork;
    private final ClockPart wristband;
    private final ClockPart clockHand1;
    private final ClockPart clockHand2;
    private final UUID assemblerId;
    private final ClockType type;
    private UUID orderId;

    private ClockStatus status;

    private int quality;
    private UUID qualityCheckerId;

    private UUID delivererId;

    public Clock(ClockPart chassis, ClockPart clockWork, ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2, UUID assemblerId, ClockType type) {
        this.id = UUID.randomUUID();
        this.createdTime = this.updatedTime = System.currentTimeMillis();
        this.chassis = chassis;
        this.clockWork = clockWork;
        this.wristband = wristband;
        this.clockHand1 = clockHand1;
        this.clockHand2 = clockHand2;
        this.assemblerId = assemblerId;
        this.status = ClockStatus.ASSEMBLED;
        this.type = type;
    }

    public void check(UUID qualityCheckerId, int quality) {
        if (qualityCheckerId == null) {
            throw new IllegalArgumentException("Invalid quality checker id!");
        }
        if (quality < 1 || quality > 10) {
            throw new IllegalArgumentException("Invalid quality value: " + quality);
        }
        if (status != ClockStatus.ASSEMBLED) {
            throw new IllegalStateException("Clock is either already checked or delivered/disassembled!");
        }

        this.status = ClockStatus.CHECKED;
        this.qualityCheckerId = qualityCheckerId;
        this.quality = quality;
        this.updatedTime = System.currentTimeMillis();
    }

    public void deliver(UUID delivererId) {
        if (delivererId == null) {
            throw new IllegalArgumentException("Invalid deliverer id!");
        }
        if (status != ClockStatus.CHECKED) {
            throw new IllegalStateException("Clock is either not yet checked or already delivered/disassembled!");
        }

        this.status = ClockStatus.DELIVERED;
        this.delivererId = delivererId;
        this.updatedTime = System.currentTimeMillis();
    }

    public List<ClockPart> disassemble(UUID delivererId) {
        if (delivererId == null) {
            throw new IllegalArgumentException("Invalid deliverer id!");
        }
        if (quality < 1 || quality > 2) {
            throw new IllegalArgumentException("Invalid quality value: " + quality);
        }
        if (status != ClockStatus.CHECKED) {
            throw new IllegalStateException("Clock is either not yet checked or already delivered/disassembled!");
        }

        this.status = ClockStatus.DISASSEMBLED;
        this.delivererId = delivererId;
        this.updatedTime = System.currentTimeMillis();

        List<ClockPart> parts = new ArrayList<ClockPart>();
        parts.add(getChassis());
        parts.add(getClockHand1());
        parts.add(getClockHand2());
        parts.add(getWristband());
        return parts;
    }

    public UUID getId() {
        return id;
    }

    public long getSerialId() {
        return serialId;
    }

    public void setSerialId(long serialId) {
        this.serialId = serialId;
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

    public ClockType getType() {
        return type;
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

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    @Override
    public int compareTo(Clock o) {
        if (o == null) {
            throw new NullPointerException();
        }

        if (this.equals(o)) {
            return 0;
        }

        int result = assemblerId.compareTo(o.assemblerId);
        result = result != 0 ? result : (createdTime < o.createdTime) ? -1 : ((createdTime == o.createdTime) ? 0 : 1);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Clock other = (Clock) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public boolean isNewer(Clock clock) {
        return updatedTime > clock.updatedTime;
    }

}
