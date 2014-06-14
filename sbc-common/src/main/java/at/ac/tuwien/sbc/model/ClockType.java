package at.ac.tuwien.sbc.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The type of a clock.
 */
public enum ClockType {

    KLASSISCH(1, 0, 1, 1, 2),
    SPORT(0, 1, 1, 1, 2),
    ZEITZONEN_SPORT(0, 1, 1, 1, 3);

    private final Map<ClockPartType, Integer> neededParts;

    private ClockType(int lederarmband, int metallarmband, int gehaeuse, int uhrwerk, int zeiger) {
        Map<ClockPartType, Integer> map = new EnumMap<ClockPartType, Integer>(ClockPartType.class);
        add(map, ClockPartType.LEDERARMBAND, lederarmband);
        add(map, ClockPartType.METALLARMBAND, metallarmband);
        add(map, ClockPartType.GEHAEUSE, gehaeuse);
        add(map, ClockPartType.UHRWERK, uhrwerk);
        add(map, ClockPartType.ZEIGER, zeiger);
        this.neededParts = Collections.unmodifiableMap(map);
    }

    private void add(Map<ClockPartType, Integer> map, ClockPartType clockPartType, int count) {
        if (count < 1) {
            return;
        }

        map.put(clockPartType, count);
    }

    /**
     * Returns the parts needed for this clock type.
     *
     * @return the parts needed for this clock type.
     */
    public Map<ClockPartType, Integer> getNeededParts() {
        return neededParts;
    }

}
