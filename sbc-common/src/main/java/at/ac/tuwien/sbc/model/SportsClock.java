package at.ac.tuwien.sbc.model;

import java.util.UUID;

public class SportsClock extends Clock{
	
	public SportsClock(ClockPart chassis, ClockPart clockWork,
			ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2,
			UUID assemblerId) {
		super(chassis, clockWork, wristband, clockHand1, clockHand2, assemblerId, ClockType.SPORT);
		if(wristband.getType() != ClockPartType.METALLARMBAND){
			throw new IllegalArgumentException("Invalid ClockPartType for wristband : " + wristband.getType().toString());
		}
	}

}
