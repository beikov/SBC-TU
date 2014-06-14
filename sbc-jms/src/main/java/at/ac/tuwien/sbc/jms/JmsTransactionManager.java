package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * A simple transaction manager for a session that supports reuse of transaction in nested scenarios.
 */
public class JmsTransactionManager {

    private final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();
    private final Session session;

    public JmsTransactionManager(Session session) {
        this.session = session;
    }

    /**
     * Commits the current transaction.
     *
     * @return true if a timeout occurred, otherwise false
     */
    protected boolean commit() {
        try {
            session.commit();
            return false;
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        } finally {
            currentTransaction.remove();
        }
    }

    /**
     * Rolls back the current transaction.
     */
    protected void rollback() {
        try {
            session.rollback();
        } catch (JMSException ex) {
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
     * Invokes the given work transactionally, joining an existing transaction if possible.
     *
     * @param work the work that should be done transactional
     * @return true if a timeout occurred, otherwise false
     */
    protected boolean transactional(TransactionalWork work) {
        boolean created = ensureCurrentTransaction();
        Transaction tx = currentTransaction.get();

        try {
            work.doWork();

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
        } catch (TimeoutException ex) {
            if (!created) {
                // Propagate the timeout to the outermost block
                tx.setRollbackOnly();
                throw ex;
            } else {
                tx.setRollbackOnly();
                return true;
            }
        } catch (RuntimeException ex) {
            tx.setRollbackOnly();
            throw ex;
        } catch (Error ex) {
            tx.setRollbackOnly();
            throw ex;
        } catch (Throwable ex) {
            tx.setRollbackOnly();
            throw new RuntimeException(ex);
        } finally {
            if (created && isRollbackOnly()) {
                rollback();
            }
        }
    }

    /**
     * Returns true if the call resulted in creating the transaction.
     *
     * @param timeoutInMillis
     * @return
     */
    private boolean ensureCurrentTransaction() {
        Transaction tx = currentTransaction.get();
        if (tx == null) {
            tx = new Transaction();
            currentTransaction.set(tx);
            return true;
        }
        return false;
    }

    private static class Transaction {

        private boolean rollbackOnly = false;

        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
    }
}
