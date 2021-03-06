package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A clock that can have multiple states and is composed of multiple clock parts.
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

    private String distributorId;
    private UUID handlerId;

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

    /**
     * Checks this clock as the quality checker with the given id.
     *
     * @param qualityCheckerId the id of the quality checker that checks this clock
     * @throws IllegalStateException if this clock is either alreafy checked or delivered/disassembled
     */
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

    /**
     * Delivers this clock as the deliverer with the given id.
     *
     * @param delivererId the id of the deliverer that delivers this clock
     * @throws IllegalStateException if this clock is either not yet checked or already delivered/disassembled
     */
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

    /**
     * Disassembles this clock as the deliverer with the given id.
     *
     * @param delivererId the id of the deliverer that disassembles this clock
     * @throws IllegalStateException if this clock is either not yet checked or already delivered/disassembled
     * @return the clock parts of the disassembled clock
     */
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

    public void setDistributor(String distributorId) {
        this.distributorId = distributorId;
    }

    public String getDistributor() {
        return distributorId;
    }

    public UUID getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(UUID handlerId) {
        this.handlerId = handlerId;
    }

    @Override
    public int compareTo(Clock o) {
        return (serialId < o.serialId) ? -1 : ((serialId == o.serialId) ? 0 : 1);
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

    /**
     * Checks if this clock is newer than the given clock.
     *
     * @param clock the clock to be checked against this
     * @return true if this clock's last updated time is later than the one's given.
     */
    public boolean isNewer(Clock clock) {
        return updatedTime > clock.updatedTime;
    }

}
