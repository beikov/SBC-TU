package at.ac.tuwien.sbc.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

public class DistributorDemand implements Serializable {

    private final URI uri;
    private final String destinationName;
    private final Map<ClockType, Integer> neededClocksPerType;

    public DistributorDemand(URI uri, String destinationName, Map<ClockType, Integer> neededClocksPerType) {
        this.uri = uri;
        this.destinationName = destinationName;
        this.neededClocksPerType = neededClocksPerType;
    }

    public URI getUri() {
        return uri;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public Map<ClockType, Integer> getNeededClocksPerType() {
        return neededClocksPerType;
    }

}
