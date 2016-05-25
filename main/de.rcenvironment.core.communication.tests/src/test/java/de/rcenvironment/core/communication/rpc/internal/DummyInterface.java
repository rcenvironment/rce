/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * Test interface used for test callback object.
 * 
 * @author Doreen Seider
 */
public interface DummyInterface extends CallbackObject {

    /**
     * Dummy method.
     * @return dummy return value
     **/
    String someMethod();
    
    /** 
     * Dummy callback method. 
     * @return dummy return value
     **/
    @CallbackMethod
    String someCallbackMethod();
}
