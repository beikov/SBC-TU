/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Christian
 */
public class ClockPart implements Serializable {
    
    private final ClockPartType type;
    private final UUID supplierId;

    public ClockPart(ClockPartType type, UUID supplierId) {
        this.type = type;
        this.supplierId = supplierId;
    }

    public ClockPartType getType() {
        return type;
    }

    public UUID getSupplierId() {
        return supplierId;
    }
}
