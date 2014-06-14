package at.ac.tuwien.sbc.xvsm;

import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

/**
 * A wrapper for a MozartSpaces task that should be done transactional.
 */
public interface TransactionalWork {

    /**
     * The work that should be done transactionally.
     *
     * @param tx the transaction within this work runs
     * @throws MzsCoreException
     */
    public void doWork(TransactionReference tx) throws MzsCoreException;
}
