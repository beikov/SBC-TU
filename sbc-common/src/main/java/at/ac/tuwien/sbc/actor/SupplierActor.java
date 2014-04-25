/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.actor;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.model.ClockPart;
import at.ac.tuwien.sbc.model.ClockPartType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Christian
 */
public class SupplierActor extends AbstractActor {
    
    private final ClockPartType partType;
    private final int amount;

    public SupplierActor(Connector connector, ClockPartType partType, int amount) {
        super(connector);
        this.partType = partType;
        this.amount = amount;
    }

    public ClockPartType getPartType() {
        return partType;
    }

    public int getAmount() {
        return amount;
    }
    
    @Override
    public void run() {
        // Wait for 1-2 seconds
        sleepForSeconds(1, 2);
        List<ClockPart> parts = new ArrayList<ClockPart>(amount);
        
        for (int i = 0; i < amount; i++) {
            parts.add(new ClockPart(partType, id));
        }
        
        connector.addParts(parts);
    }
    
}
