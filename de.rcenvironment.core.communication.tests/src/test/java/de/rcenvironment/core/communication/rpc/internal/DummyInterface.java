/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
