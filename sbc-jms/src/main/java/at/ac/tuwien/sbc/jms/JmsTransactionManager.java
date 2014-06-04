/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 *
 * @author Christian
 */
public class JmsTransactionManager {

    private final ThreadLocal<Boolean> currentTransaction = new ThreadLocal<Boolean>();
    private final ThreadLocal<Boolean> currentTransactionRollback = new ThreadLocal<Boolean>();
    private final Session session;

    public JmsTransactionManager(Session session) {
        this.session = session;
    }

    protected boolean commit() {
        try {
            session.commit();
            return false;
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        } finally {
            currentTransaction.remove();
            currentTransactionRollback.remove();
        }
    }

    protected void rollback() {
        try {
            session.rollback();
        } catch (JMSException ex) {
            throw new RuntimeException(ex);
        } finally {
            currentTransaction.remove();
            currentTransactionRollback.remove();
        }
    }

    protected boolean transactional(TransactionalWork work) {
        boolean created = ensureCurrentTransaction();
        currentTransactionRollback.set(Boolean.TRUE);

        try {
            work.doWork();

            if (created) {
                return commit();
            } else {
                // Indicates no timeout occurred
                return false;
            }
        } catch (TimeoutException ex) {
            if (!created) {
                // Propagate the timeout to the outermost block
                throw ex;
            } else {
                return true;
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            if (created && Boolean.TRUE == currentTransactionRollback.get()) {
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
        Boolean tx = currentTransaction.get();
        if (tx == null) {
            tx = Boolean.TRUE;
            currentTransaction.set(tx);
            return true;
        }
        return false;
    }
}
