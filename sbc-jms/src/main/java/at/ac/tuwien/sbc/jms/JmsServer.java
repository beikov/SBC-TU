/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.sbc.jms;

import org.apache.activemq.broker.BrokerService;

/**
 *
 * @author Christian
 */
public class JmsServer {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: JmsServer PORT");
        }
        
        BrokerService broker = new BrokerService();
        broker.addConnector("tcp://localhost:" + Integer.parseInt(args[0]));
        broker.start();
        System.out.println("Press CTRL+C to shutdown the server...");
        while(System.in.read() != -1);
    }
}
