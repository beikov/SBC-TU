/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc;

import at.ac.tuwien.sbc.model.ClockPart;
import java.util.List;

/**
 *
 * @author Christian
 */
public interface ClockPartListener {
    
    public void onClockPartsAdded(List<ClockPart> clockParts);
    
    public void onClockPartsRemoved(List<ClockPart> clockParts);
}
