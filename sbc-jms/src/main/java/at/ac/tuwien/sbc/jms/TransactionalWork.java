package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;

/**
 * A wrapper for a JMS task that should be done transactional.
 */
public interface TransactionalWork {

    /**
     * The work that should be done transactionally.
     *
     * @throws JMSException
     */
    public void doWork() throws JMSException;
}
