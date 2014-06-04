/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc.jms;

import javax.jms.JMSException;

/**
 *
 * @author Christian
 */
public interface TransactionalWork {

    public void doWork() throws JMSException;
}
