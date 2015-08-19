/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.Serializable;

/**
 * Result that is transfered back to the service caller (client).
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 */
public class ServiceCallResult implements Serializable {

    private static final long serialVersionUID = -7511179849159143497L;

    /**
     * The return value.
     */
    private Serializable myReturnValue = null;

    /**
     * Thrown exception.
     */
    private Throwable myThrowable = null;

    /**
     * Constructor that takes a return value.
     * 
     * @param returnValue Return value to set.
     */
    public ServiceCallResult(Serializable returnValue) {

        if (returnValue instanceof Throwable) {
            myThrowable = (Throwable) returnValue;
        } else {
            myReturnValue = returnValue;
        }
    }

    public Serializable getReturnValue() {
        return myReturnValue;
    }

    public Throwable getThrowable() {
        return myThrowable;
    }
}
