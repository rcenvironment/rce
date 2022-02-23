/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import junit.framework.TestCase;

/**
 * Test cases for the class {@link AuthenticationException}.
 * 
 * @author Doreen Seider
 */
public class AuthenticationExceptionTest extends TestCase {

    /**
     * A message for the tests.
     */
    private static final String EXCEPTION_MESSAGE = "Exception message for a NoSuchPublisherException!";

    /**
     * The class under test.
     */
    private AuthenticationException myException = null;

    /**
     * A root exception for the tests.
     */
    private Throwable myCause = null;

    @Override
    protected void setUp() throws Exception {
        myCause = new Exception("Root Exception");
        myException = new AuthenticationException(EXCEPTION_MESSAGE, myCause);
    }

    @Override
    protected void tearDown() throws Exception {
        myCause = null;
        myException = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test if all constructors can be called.
     * 
     */
    public final void testConstructorsForSuccess() {
        myException = new AuthenticationException(EXCEPTION_MESSAGE);
        myException = new AuthenticationException(myCause);
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetMessageForSuccess() {
        myException.getMessage();
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetCauseForSuccess() {
        myException.getCause();
    }

    /*
     * #################### Test for failure ####################
     */

    /*
     * #################### Test for sanity ####################
     */

    /**
     * 
     * Test if the exception message can be received.
     * 
     */
    public final void testGetMessageForSanity() {
        String message = myException.getMessage();

        assertNotNull(message);
        assertEquals(EXCEPTION_MESSAGE, message);
    }

    /**
     * 
     * Test if the exception cause can be received.
     * 
     */
    public final void testGetCauseForSanity() {
        Throwable cause = myException.getCause();

        assertNotNull(cause);
        assertEquals(myCause, cause);
    }

}
