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
    public static final String CLOCK_PART_QUEUE = "queue/clockpart";
    public static final String CLOCK_PART_TOPIC = "topic/clockpart";

    public static final String SINGLE_CLOCK_ORDER_QUEUE = "queue/singleclockorder";
    public static final String ORDER_QUEUE = "queue/order";
    public static final String ORDER_TOPIC = "topic/order";

    // Clock part properties
    public static final String CLOCK_PART_REMOVED = "removed";
    public static final String CLOCK_PART_TYPE = "type";

    // Clock properties
    public static final String CLOCK_ID = "id";
    public static final String CLOCK_TYPE = "type";
    public static final String CLOCK_QUALITY = "quality";
    public static final String CLOCK_STATUS = "status";

    // Order properties
    public static final String SINGLE_CLOCK_TYPE = "type";
    public static final String ORDER_PRIORITY = "priority";

    // Distributor properties
    public static final String DISTRIBUTOR_ID = "id";

    public static final String DISTRIBUTOR_DEMAND_QUEUE = "queue/distributordemand";
    public static final String DISTRIBUTOR_STOCK_QUEUE_PREFIX = "queue/distributorstock-";
    public static final String DISTRIBUTOR_STOCK_TOPIC_PREFIX = "topic/distributorstock-";

}
