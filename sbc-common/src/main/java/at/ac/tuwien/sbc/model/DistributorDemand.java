package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class DistributorDemand implements Serializable{
	private final UUID distributorId;
	private final Map<ClockType, Integer> neededClocksPerType;
	
	public DistributorDemand(UUID distributorId, Map<ClockType,Integer> neededClocksPerType){
		this.distributorId = distributorId;
		this.neededClocksPerType = neededClocksPerType;
	}

	public UUID getDistributorId() {
		return distributorId;
	}

	public Map<ClockType, Integer> getNeededClocksPerType() {
		return neededClocksPerType;
	}
	
	
	
}
