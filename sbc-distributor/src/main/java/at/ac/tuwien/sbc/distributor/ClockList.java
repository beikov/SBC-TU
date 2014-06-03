package at.ac.tuwien.sbc.distributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import at.ac.tuwien.sbc.model.Clock;
import at.ac.tuwien.sbc.model.ClockType;

public class ClockList {

	private List<Clock> clocks;
	private AtomicInteger classicClocks;
	private AtomicInteger sportsClocks;
	private AtomicInteger timezoneClocks;
	private AtomicInteger orderedClassicClocks;
	private AtomicInteger orderedSportsClocks;
	private AtomicInteger orderedTimezoneClocks;
	

	public ClockList(){
		clocks = Collections.synchronizedList(new ArrayList<Clock>());
		classicClocks = new AtomicInteger(0);
		sportsClocks = new AtomicInteger(0);
		timezoneClocks = new AtomicInteger(0);
		orderedClassicClocks = new AtomicInteger(0);
		orderedSportsClocks = new AtomicInteger(0);
		orderedTimezoneClocks = new AtomicInteger(0);
	}

	public List<Clock> getClocks() {
		return clocks;
	}

	public void addAll(List<Clock> clocks) {
		for (Clock clock : clocks) {
			switch(clock.getType()){
			case KLASSISCH: classicClocks.incrementAndGet(); orderedClassicClocks.decrementAndGet(); break;
			case SPORT: sportsClocks.incrementAndGet(); orderedSportsClocks.decrementAndGet(); break;
			case ZEITZONEN_SPORT: timezoneClocks.incrementAndGet(); orderedTimezoneClocks.decrementAndGet(); break;
			}
		}
		this.clocks.addAll(clocks);
	}

	public void remove(int index){
		Clock removedClock = clocks.remove(index);

		switch(removedClock.getType()){
		case KLASSISCH: classicClocks.decrementAndGet(); break;
		case SPORT: sportsClocks.decrementAndGet(); break;
		case ZEITZONEN_SPORT: timezoneClocks.decrementAndGet(); break;
		}
	}
	
	public int getClockCount(ClockType type){
		switch(type){
		case KLASSISCH:	return classicClocks.get();
		case SPORT: return sportsClocks.get();
		case ZEITZONEN_SPORT: return timezoneClocks.get();
		}
		return 0;
	}
	
	public int getOrderedClockCount(ClockType type){
		switch(type){
		case KLASSISCH:	return orderedClassicClocks.get();
		case SPORT: return orderedSportsClocks.get();
		case ZEITZONEN_SPORT: return orderedTimezoneClocks.get();
		}
		return 0;
	}
	
	public int size(){
		return clocks.size();
	}
}


