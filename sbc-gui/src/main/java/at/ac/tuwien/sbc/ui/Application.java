/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.ui;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.util.SbcUtils;

/**
 *
 * @author Christian
 */
public class Application {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: Application PORT (xvsm|jms)");
        }
        
        Connector connector = SbcUtils.getConnector(Integer.parseInt(args[0]), args[1]);
        MainFrame.start(connector);
    }
}
