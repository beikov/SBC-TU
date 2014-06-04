/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.util;

import at.ac.tuwien.sbc.Connector;
import at.ac.tuwien.sbc.DistributorConnector;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.UUID;

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

    public static DistributorConnector getDistributorConnector(UUID distributorId, int port, String type) {
        Exception reason = null;

        try {
            if ("xvsm".equals(type)) {
                return (DistributorConnector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.xvsm.MozartSpacesDistributorConnector")
                    .getConstructor(UUID.class, int.class)
                    .newInstance(distributorId, port);
            } else if ("jms".equals(type)) {
                return (DistributorConnector) SbcUtils.class.getClassLoader()
                    .loadClass("at.ac.tuwien.sbc.jms.JmsDistributorConnector")
                    .getConstructor(UUID.class, int.class)
                    .newInstance(distributorId, port);
            }
            throw new IllegalArgumentException("Unsupported type: " + type);
        } catch (Exception ex) {
            reason = ex;
        }

        throw new IllegalArgumentException("Could not create the connector", reason);
    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback()) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    if (addresses.hasMoreElements()) {
                        return addresses.nextElement()
                            .getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace(System.err);
        }

        return InetAddress.getLoopbackAddress()
            .getHostAddress();
    }
}
