package at.ac.tuwien.sbc.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimezoneSportsClock extends Clock{

	public static final Map<ClockPartType, Integer> NEEDED_PARTS;
	
	static{
    Map<ClockPartType, Integer> map = new EnumMap<ClockPartType, Integer>(ClockPartType.class);
    map.put(ClockPartType.METALLARMBAND, 1);
    map.put(ClockPartType.GEHAEUSE, 1);
    map.put(ClockPartType.UHRWERK, 1);
    map.put(ClockPartType.ZEIGER, 3);
    NEEDED_PARTS = Collections.unmodifiableMap(map);
	}
	
	private final ClockPart clockHand3;
	
	public TimezoneSportsClock(ClockPart chassis, ClockPart clockWork,
			ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2,
			ClockPart clockHand3,
			UUID assemblerId) {
		super(chassis, clockWork, wristband, clockHand1, clockHand2, assemblerId, ClockType.ZEITZONEN_SPORT);
		if(wristband.getType() != ClockPartType.METALLARMBAND){
			throw new IllegalArgumentException("Invalid ClockPartType for wristband : " + wristband.getType().toString());
		}
		this.clockHand3 = clockHand3;
	}
	
	public ClockPart getClockHand3(){
		return clockHand3;
	}
	
	@Override
	public List<ClockPart> disassemble(UUID delivererId) {
		List<ClockPart> parts = super.disassemble(delivererId);
		parts.add(clockHand3);
		return parts;
	}

}
