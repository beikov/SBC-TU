package at.ac.tuwien.sbc.model;

import java.util.UUID;

public class ClassicClock extends Clock{
	
	public ClassicClock(ClockPart chassis, ClockPart clockWork,
			ClockPart wristband, ClockPart clockHand1, ClockPart clockHand2,
			UUID assemblerId) {
		super(chassis, clockWork, wristband, clockHand1, clockHand2, assemblerId, ClockType.KLASSISCH);
		
		if(wristband.getType() != ClockPartType.LEDERARMBAND){
			throw new IllegalArgumentException("Invalid ClockPartType for wristband : " + wristband.getType().toString());
		}
	}

}
