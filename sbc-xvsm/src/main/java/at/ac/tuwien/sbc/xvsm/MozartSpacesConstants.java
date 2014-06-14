package at.ac.tuwien.sbc.xvsm;

/**
 * Some constants for containers and coordinator names used by the MozartSpaces connector.
 */
public class MozartSpacesConstants {

    public static final long MAX_TIMEOUT_MILLIS = 2000;
    public static final long MAX_TRANSACTION_TIMEOUT_MILLIS = 10000;

    public static final String ID_CONTAINER_NAME = "idcontainer";

    public static final String PARTS_CONTAINER_NAME = "Fabrik/Teile";
    public static final String ASSEMBLED_CLOCKS_CONTAINER_NAME = "Fabrik/Uhren";
    public static final String DELIVERED_CLOCKS_CONTAINER_NAME = "Fabrik/AusgelieferteUhren";
    public static final String CHECKED_CLOCKS_CONTAINER_NAME = "Fabrik/GepruefteUhren";
    public static final String DISASSEMBLED_CLOCKS_CONTAINER_NAME = "Fabrik/SchlechteUhren";
    public static final String DELIVERED_TO_DISTRIBUTORS_CONTAINER_NAME = "Fabrik/AnGrosshaendlerGelieferteUhren";

    public static final String SINGLE_CLOCK_ORDER_CONTAINER_NAME = "Fabrik/EinzelneUhrenBestellungen";
    public static final String ORDER_CONTAINER_NAME = "Fabrik/Bestellungen";

    public static final String DISTRIBUTOR_DEMAND_CONTAINER_NAME = "Grosshaendler/Bedarf";
    public static final String DISTRIBUTOR_STOCK_CONTAINER_NAME = "Grosshaendler/Lager";

    public static final String PARTS_TYPE_COORDINATOR_NAME = "type";
    public static final String ORDER_PRIORITY_COORDINATOR_NAME = "priority";
    public static final String CLOCK_QUALITY_COORDINATOR_NAME = "quality";
    public static final String ORDER_TYPE_COORDINATOR_NAME = "order_type";

}
