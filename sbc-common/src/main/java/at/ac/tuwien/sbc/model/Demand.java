package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.UUID;

public class Demand implements Serializable{
	private final UUID distributor;
	private final ClockType type;
	
	public Demand(UUID distributor, ClockType type){
		this.distributor = distributor;
		this.type = type;
	}

	public UUID getDistributor() {
		return distributor;
	}

	public ClockType getType() {
		return type;
	}
	
	
}
