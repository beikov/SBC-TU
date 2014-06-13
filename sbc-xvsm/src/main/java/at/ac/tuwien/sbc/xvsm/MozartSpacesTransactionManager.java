/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.xvsm;

import java.net.URI;
import org.mozartspaces.capi3.CountNotMetException;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.MzsTimeoutException;
import org.mozartspaces.core.TransactionReference;

/**
 *
 * @author Christian
 */
public class MozartSpacesTransactionManager {

	private final ThreadLocal<TransactionReference> currentTransaction = new ThreadLocal<TransactionReference>();
	private final ThreadLocal<Boolean> currentTransactionRollback = new ThreadLocal<Boolean>();
	private final Capi capi;
	private final URI serverUri;
	private boolean rollbackNeeded = false;

	public MozartSpacesTransactionManager(Capi capi, URI serverUri) {
		this.capi = capi;
		this.serverUri = serverUri;
	}

	private boolean commit(TransactionReference tx) {
		try {
			if(!rollbackNeeded){
				capi.commitTransaction(tx);
				return false;
			}else{
				rollback(tx);
				
				return true;
			}
		} catch (MzsTimeoutException ex) {
			return true;
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
			currentTransactionRollback.remove();
		}
	}

	public void rollback(TransactionReference tx) {
		try {
			capi.rollbackTransaction(tx);
		} catch (MzsTimeoutException ex) {
			// On timeout we dont have to rollback
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		} finally {
			currentTransaction.remove();
			rollbackNeeded = false;
		}
	}

	protected boolean transactional(TransactionalWork work) {
		return transactional(work, MozartSpacesConstants.MAX_TRANSACTION_TIMEOUT_MILLIS);
	}

	protected boolean transactional(TransactionalWork work, long timeoutInMillis) {
		boolean created = ensureCurrentTransaction(timeoutInMillis);
		TransactionReference tx = getCurrentTransaction();
		currentTransactionRollback.set(Boolean.TRUE);

		try {
			work.doWork(tx);

			if (created) {
				return commit(tx);
			} else {
				// Indicates no timeout occurred
				return false;
			}
		} catch (MzsTimeoutException ex) {
			// On timeout we dont have to rollback
//			created = false;
//			currentTransaction.remove();
//			currentTransactionRollback.remove();
			return true;
		} catch (CountNotMetException ex) {
			// This happens when try once is used and is handeled like a timeout
//			created = false;
//			currentTransaction.remove();
//			currentTransactionRollback.remove();
			return true;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Error ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		} finally {
			if (created && Boolean.TRUE == currentTransactionRollback.get()) {
				rollback(tx);
			}
		}
	}

	protected TransactionReference getCurrentTransaction() {
		return currentTransaction.get();
	}

	/**
	 * Returns true if the call resulted in creating the transaction.
	 *
	 * @param timeoutInMillis
	 * @return
	 */
	private boolean ensureCurrentTransaction(long timeoutInMillis) {
		try {
			TransactionReference tx = currentTransaction.get();
			if (tx == null) {
				tx = capi.createTransaction(timeoutInMillis, serverUri);
				currentTransaction.set(tx);
				return true;
			}
			return false;
		} catch (MzsCoreException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void setRollbackNeeded(boolean b) {
		rollbackNeeded = b;
	}
	
	public boolean isRollbackNeeded(){
		return rollbackNeeded;
	}
}
