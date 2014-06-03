package at.ac.tuwien.sbc.distributor;

import java.util.List;

import at.ac.tuwien.sbc.ClockListener;
import at.ac.tuwien.sbc.model.Clock;

public class DistributorClockListener implements ClockListener {
    
    private final Runnable listener;
	private ClockList clockList;
	
	public DistributorClockListener(ClockList clockList, Runnable listener) {
		this.clockList = clockList;
		this.listener = listener;
	}

	@Override
	public void onClocksUpdated(List<Clock> clocks) {
		//System.out.println("adding to clocklist in distributorlistener");
		clockList.addAll(clocks);
		listener.run();
	}

}
