package at.ac.tuwien.sbc.xvsm;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mozartspaces.capi3.ContainerNameNotAvailableException;
import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.Selector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCoreException;

/**
 * A wrapper around a MozartSpaces container that acts as a sequence.
 */
public class MozartSpacesSequence extends AbstractMozartSpacesComponent {

    private final ContainerReference idContainer;

    public MozartSpacesSequence(Capi capi, URI serverUri, MozartSpacesTransactionManager tm, String containerName) {
        super(capi, serverUri, tm);
        idContainer = getOrCreateIdContainer(capi, containerName);
    }

    private ContainerReference getOrCreateIdContainer(Capi capi, String name) {
        try {
            return capi.lookupContainer(name, serverUri, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, null);
        } catch (MzsCoreException ex) {
            try {
                ContainerReference container = capi.createContainer(name, serverUri, MzsConstants.Container.UNBOUNDED, Arrays
                                                                    .asList(new FifoCoordinator()), null, null);
                // since id container got created we need to write the idCounter into it
                Long id = 0L;
                Entry entry = new Entry(id);
                capi.write(container, entry);

                return container;
            } catch (ContainerNameNotAvailableException ex2) {
                // Someone else was faster...
                return getOrCreateIdContainer(capi, name);
            } catch (MzsCoreException ex2) {
                throw new RuntimeException(ex2);
            }
        }
    }

    /**
     * Returns the next value created by this sequence.
     *
     * @return the next value created by this sequence
     * @throws MzsCoreException
     */
    public long getNextId() throws MzsCoreException {
        List<Selector> selectors = new ArrayList<Selector>();
        selectors.add(FifoCoordinator.newSelector(1));
        Long id = (Long) capi.take(idContainer, selectors, MozartSpacesConstants.MAX_TIMEOUT_MILLIS, tm.getCurrentTransaction())
            .get(0);

        Entry entry = new Entry(id + 1);
        capi.write(idContainer, MzsConstants.RequestTimeout.ZERO, tm.getCurrentTransaction(), entry);

        return id;
    }
}
