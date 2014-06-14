package at.ac.tuwien.sbc;

/**
 * Represents the subscription that resulted by registering a listener.
 */
public interface Subscription {

    /**
     * Cancels the subscription so that the listener registered earlier receives no more messages.
     */
    public void cancel();
}
