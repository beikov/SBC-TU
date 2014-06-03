package at.ac.tuwien.sbc.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ClassicClock extends Clock{

	public static final Map<ClockPartType, Integer> NEEDED_PARTS;
	
	static{
    Map<ClockPartType, Integer> map = new EnumMap<ClockPartType, Integer>(ClockPartType.class);
    map.put(ClockPartType.LEDERARMBAND, 1);
    map.put(ClockPartType.GEHAEUSE, 1);
    map.put(ClockPartType.UHRWERK, 1);
    map.put(ClockPartType.ZEIGER, 2);
    NEEDED_PARTS = Collections.unmodifiableMap(map);
	}
	
	public ClassicClock(ClockPart chassis, ClockPart clockWork,
			ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2,
			UUID assemblerId) {
		super(chassis, clockWork, wristband, clockHand1, clockHand2, assemblerId, ClockType.KLASSISCH);
		
		if(wristband.getType() != ClockPartType.LEDERARMBAND){
			throw new IllegalArgumentException("Invalid ClockPartType for wristband : " + wristband.getType().toString());
		}
	}

}
