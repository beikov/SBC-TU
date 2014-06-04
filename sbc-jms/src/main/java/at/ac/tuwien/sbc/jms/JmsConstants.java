/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

/**
 *
 * @author Christian
 */
public class JmsConstants {

    public static final String ID_QUEUE = "queue/id";

    public static final long MAX_TIMEOUT_MILLIS = 2000;
    public static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;

    public static final String CLOCK_QUEUE = "queue/clock";
    public static final String CLOCK_TOPIC = "topic/clock";
    public static final String CLOCKPART_QUEUE = "queue/clockpart";
    public static final String CLOCKPART_TOPIC = "topic/clockpart";

    public static final String SINGLE_CLOCK_ORDER_QUEUE = "queue/singleclockorder";
    public static final String ORDER_QUEUE = "queue/order";
    public static final String ORDER_TOPIC = "topic/order";

    public static final String IS_DISASSEMBLED = "IS_DISASSEMBLED";
    public static final String IS_ASSEMBLED = "IS_ASSEMBLED";
    public static final String IS_DELIVERED = "IS_DELIVERED";
    public static final String IS_ORDERED = "IS_ORDERED";

    public static final String DISTRIBUTOR_DEMAND_QUEUE = "queue/distributordemand";
    public static final String DISTRIBUTOR_STOCK_QUEUE_PREFIX = "queue/distributorstock-";
    public static final String DISTRIBUTOR_STOCK_TOPIC_PREFIX = "topic/distributorstock-";

}
