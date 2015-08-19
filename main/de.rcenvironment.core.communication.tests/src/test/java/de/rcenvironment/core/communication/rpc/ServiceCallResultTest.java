/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import junit.framework.TestCase;

/**
 * Unit test for <code>ServiceCallResult</code>.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class ServiceCallResultTest extends TestCase {

    /**
     * Test method for sanity.
     * 
     */
    public void testException() {
        Exception exception1 = new Exception();
        ServiceCallResult communicationResult = new ServiceCallResult(exception1);
        Exception exception2 = (Exception) communicationResult.getThrowable();
        assertEquals(exception1, exception2);
    }

    /**
     * Test method for sanity.
     */
    public void testReturnValue() {
        String returnValue1 = "hallo";
        ServiceCallResult communicationResult = new ServiceCallResult(returnValue1);
        String returnValue2 = (String) communicationResult.getReturnValue();
        assertEquals(returnValue1, returnValue2);
    }

}
