package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * A clock with a leather wristband.
 */
public class ClassicClock extends Clock implements Serializable {

    public ClassicClock(ClockPart chassis, ClockPart clockWork,
        ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2,
        UUID assemblerId) {
        super(chassis, clockWork, wristband, clockHand1, clockHand2, assemblerId, ClockType.KLASSISCH);

        if (wristband.getType() != ClockPartType.LEDERARMBAND) {
            throw new IllegalArgumentException("Invalid ClockPartType for wristband : " + wristband.getType()
                .toString());
        }
    }

}
