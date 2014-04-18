/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.util;

import at.ac.tuwien.sbc.Connector;

/**
 *
 * @author Christian
 */
public final class SbcUtils {

    public static Connector getConnector(int port, String type) {
        Exception reason = null;

        try {
            if ("xvsm".equals(type)) {
                return (Connector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.xvsm.MozartSpacesConnector")
                    .getConstructor(int.class)
                    .newInstance(port);
            } else if ("jms".equals(type)) {
                return (Connector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.jms.JmsConnector")
                    .getConstructor(int.class)
                    .newInstance(port);
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        } catch (Exception ex) {
            reason = ex;
        }

        throw new IllegalArgumentException("Could not create the connector", reason);
    }
}
