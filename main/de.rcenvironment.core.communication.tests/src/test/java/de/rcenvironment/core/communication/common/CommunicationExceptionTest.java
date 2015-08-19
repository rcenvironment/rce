/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import junit.framework.TestCase;

/**
 * Test cases for the class <code>CommunicationException</code>.
 * 
 * @author Doreen Seider
 */
public class CommunicationExceptionTest extends TestCase {

    /**
     * A message for the tests.
     */
    private static final String EXCEPTION_MESSAGE = "Exception message for a NoSuchPublisherException!";

    /**
     * The class under test.
     */
    private CommunicationException myCommunicationException = null;

    /**
     * A root exception for the tests.
     */
    private Throwable myCause = null;

    @Override
    protected void setUp() throws Exception {
        myCause = new Exception("Root Exception");
        myCommunicationException = new CommunicationException(EXCEPTION_MESSAGE, myCause);
    }

    @Override
    protected void tearDown() throws Exception {
        myCause = null;
        myCommunicationException = null;
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
        myCommunicationException = new CommunicationException(EXCEPTION_MESSAGE);
        myCommunicationException = new CommunicationException(myCause);
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetMessageForSuccess() {
        myCommunicationException.getMessage();
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetCauseForSuccess() {
        myCommunicationException.getCause();
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
        String message = myCommunicationException.getMessage();

        assertNotNull(message);
        assertEquals(EXCEPTION_MESSAGE, message);
    }

    /**
     * 
     * Test if the exception cause can be received.
     * 
     */
    public final void testGetCauseForSanity() {
        Throwable cause = myCommunicationException.getCause();

        assertNotNull(cause);
        assertEquals(myCause, cause);
    }

}
