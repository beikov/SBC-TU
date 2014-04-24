/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.Clock;
import java.util.List;

/**
 *
 * @author Christian
 */
public interface ClockListener {
    
    public void onClocksUpdated(List<Clock> clocks);
}
