/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Unit test for <code>ServiceCallResult</code>.
 * 
 * @author Robert Mischke
 */
public class ServiceCallResultTest extends TestCase {

    private static final String DUMMY_RETURN_VALUE = "dummy";

    private static final String EXCEPTION_TEXT = "exceptionText";

    private static final Exception DUMMY_EXCEPTION = new IOException(EXCEPTION_TEXT);

    /**
     * Basic behavior test.
     */
    public void testReturnValue() {
        ServiceCallResult result = ServiceCallResultFactory.wrapReturnValue(DUMMY_RETURN_VALUE);
        assertTrue(result.isSuccess());
        assertEquals(DUMMY_RETURN_VALUE, result.getReturnValue());
        assertEquals(null, result.getMethodExceptionType());
        assertEquals(null, result.getMethodExceptionMessage());
        assertEquals(null, result.getRemoteOperationExceptionMessage());
    }

    /**
     * Basic behavior test.
     */
    public void testMethodException() {
        ServiceCallResult result = ServiceCallResultFactory.wrapMethodException(DUMMY_EXCEPTION);
        assertFalse(result.isSuccess());
        assertEquals(null, result.getReturnValue());
        assertEquals("java.io.IOException", result.getMethodExceptionType());
        assertEquals(EXCEPTION_TEXT, result.getMethodExceptionMessage());
        assertEquals(null, result.getRemoteOperationExceptionMessage());
    }

}
