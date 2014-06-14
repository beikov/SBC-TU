package at.ac.tuwien.sbc;

/**
 * A task that is executed within one transaction, reusing an existing transaction if possible.
 *
 * @param <V> the type of the parameter the task should receive
 */
public interface TransactionalTask<V> {

    /**
     * The work that should be done transactionally.
     *
     * @param param the parameter the task receives for it's work
     */
    public void doWork(V param);
}
