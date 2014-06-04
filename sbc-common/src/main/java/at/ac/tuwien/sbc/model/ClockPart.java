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

    private final UUID id;
    private final ClockPartType type;
    private final UUID supplierId;

    public ClockPart(ClockPartType type, UUID supplierId) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.supplierId = supplierId;
    }

    public UUID getId() {
        return id;
    }

    public ClockPartType getType() {
        return type;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClockPart other = (ClockPart) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }
}
