/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cloudbees.plugins.flow;

/**
 *
 * @author Julia S.Simon <julia@tuenti.com>
 */
class CouldNotScheduleJobException extends RuntimeException {
    public CouldNotScheduleJobException(Exception e) {
            super(e);
    }
    
    public CouldNotScheduleJobException(String message, Exception e) {
            super(message, e);
    }
}
