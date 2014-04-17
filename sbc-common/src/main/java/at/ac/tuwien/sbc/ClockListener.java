/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;

/**
 *
 * @author Christian
 */
public interface ClockListener {
    
    public void onClockAssembled(Clock clock);
    
    public void onClockChecked(Clock clock);
    
    public void onClockDelivered(Clock clock);
}
