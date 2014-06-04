/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.sbc;

/**
 *
 * @author Christian
 */
public interface TransactionalTask<V> {

    public void doWork(V param);
}
