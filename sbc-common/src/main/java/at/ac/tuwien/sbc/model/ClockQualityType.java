package at.ac.tuwien.sbc.model;

/**
 * The quality of a clock.
 */
public enum ClockQualityType {

    A(8, 10),
    B(3, 7),
    C(0, 2);

    private final int lowerBound;
    private final int upperBound;

    private ClockQualityType(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public static ClockQualityType fromQuality(int quality) {
        if (quality > 10) {
            throw new IllegalArgumentException("Invalid quality: " + quality);
        } else if (quality > 7) {
            return A;
        } else if (quality > 2) {
            return B;
        } else if (quality > 0) {
            return C;
        }

        throw new IllegalArgumentException("Invalid quality: " + quality);
    }
}
