package at.ac.tuwien.sbc.xvsm;

import java.net.URI;
import org.mozartspaces.capi3.CountNotMetException;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.MzsTimeoutException;
import org.mozartspaces.core.TransactionReference;

/**
 * A simple transaction manager for CAPI object that supports reuse of transaction in nested scenarios.
 */
public class MozartSpacesTransactionManager {

    private final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();
    private final Capi capi;
    private final URI serverUri;

    public MozartSpacesTransactionManager(Capi capi, URI serverUri) {
        this.capi = capi;
        this.serverUri = serverUri;
    }

    /**
     * Commits the current transaction.
     *
     * @return true if a timeout occurred, otherwise false
     */
    private boolean commit() {
        try {
            Transaction tx = currentTransaction.get();
            capi.commitTransaction(tx.getTxReference());
            return false;
        } catch (MzsTimeoutException ex) {
            return true;
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } finally {
            currentTransaction.remove();
        }
    }

    /**
     * Rolls back the current transaction.
     */
    public void rollback() {
        try {
            Transaction tx = currentTransaction.get();
            capi.rollbackTransaction(tx.getTxReference());
        } catch (MzsTimeoutException ex) {
            // On timeout we dont have to rollback
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        } finally {
            currentTransaction.remove();
        }
    }

    /**
     * Sets the current transaction as rollback only.
     */
    public void setRollbackOnly() {
        Transaction tx = currentTransaction.get();
        if (tx != null) {
            tx.setRollbackOnly();
        }
    }

    /**
     * Returns whether the currenct transaction is rollback only.
     *
     * @return whether the currenct transaction is rollback only
     */
    public boolean isRollbackOnly() {
        Transaction tx = currentTransaction.get();
        return tx == null ? false : tx.isRollbackOnly();
    }

    /**
     * Invokes the given work transactionally, joining an existing transaction if possible. It uses a default timeout for the transaction.
     *
     * @param work the work that should be done transactional
     * @return true if a timeout occurred, otherwise false
     */
    protected boolean transactional(TransactionalWork work) {
        return transactional(work, MozartSpacesConstants.MAX_TRANSACTION_TIMEOUT_MILLIS);
    }

    /**
     * Invokes the given work transactionally, joining an existing transaction if possible.
     *
     * @param work            the work that should be done transactional
     * @param timeoutInMillis the transaction timeout
     * @return true if a timeout occurred, otherwise false
     */
    protected boolean transactional(TransactionalWork work, long timeoutInMillis) {
        boolean created = ensureCurrentTransaction(timeoutInMillis);
        TransactionReference tx = getCurrentTransaction();

        try {
            work.doWork(tx);

            if (created) {
                if (!isRollbackOnly()) {
                    return commit();
                } else {
                    // Timout occurred
                    return true;
                }
            } else {
                // Indicates no timeout occurred
                return false;
            }
        } catch (MzsTimeoutException ex) {
            // On timeout we dont have to rollback
            setRollbackOnly();
            return true;
        } catch (CountNotMetException ex) {
            // This happens when try once is used and is handeled like a timeout
            setRollbackOnly();
            return true;
        } catch (RuntimeException ex) {
            setRollbackOnly();
            throw ex;
        } catch (Error ex) {
            setRollbackOnly();
            throw ex;
        } catch (Throwable ex) {
            setRollbackOnly();
            throw new RuntimeException(ex);
        } finally {
            if (created && isRollbackOnly()) {
                rollback();
            }
        }
    }

    /**
     * Returns the current transaction or null if none exists.
     *
     * @return the current transaction or null if none exists
     */
    protected TransactionReference getCurrentTransaction() {
        Transaction tx = currentTransaction.get();
        return tx == null ? null : tx.getTxReference();
    }

    /**
     * Returns true if the call resulted in creating the transaction.
     *
     * @param timeoutInMillis
     * @return
     */
    private boolean ensureCurrentTransaction(long timeoutInMillis) {
        try {
            Transaction tx = currentTransaction.get();
            if (tx == null) {
                tx = new Transaction(capi.createTransaction(timeoutInMillis, serverUri));
                currentTransaction.set(tx);
                return true;
            }
            return false;
        } catch (MzsCoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class Transaction {

        private final TransactionReference tx;
        private boolean rollbackOnly = false;

        public Transaction(TransactionReference tx) {
            this.tx = tx;
        }

        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        private TransactionReference getTxReference() {
            return tx;
        }
    }
}
