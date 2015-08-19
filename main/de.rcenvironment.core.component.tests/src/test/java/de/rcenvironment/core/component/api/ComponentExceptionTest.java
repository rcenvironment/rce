/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import junit.framework.TestCase;

/**
 * Test cases for the class {@link ComponentException}.
 * 
 * @author Doreen Seider
 */
public class ComponentExceptionTest extends TestCase {

    /**
     * A message for the tests.
     */
    private static final String EXCEPTION_MESSAGE = "Exception message for a NoSuchPublisherException!";

    /**
     * The class under test.
     */
    private ComponentException componentException = null;

    /**
     * A root exception for the tests.
     */
    private Throwable myCause = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myCause = new Exception("Root Exception");
        componentException = new ComponentException(EXCEPTION_MESSAGE, myCause);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myCause = null;
        componentException = null;
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
        componentException = new ComponentException(EXCEPTION_MESSAGE);
        componentException = new ComponentException(myCause);
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetMessageForSuccess() {
        componentException.getMessage();
    }

    /**
     * 
     * Test if the method can be called.
     * 
     */
    public final void testGetCauseForSuccess() {
        componentException.getCause();
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
        String message = componentException.getMessage();

        assertNotNull(message);
        assertEquals(EXCEPTION_MESSAGE, message);
    }

    /**
     * 
     * Test if the exception cause can be received.
     * 
     */
    public final void testGetCauseForSanity() {
        Throwable cause = componentException.getCause();

        assertNotNull(cause);
        assertEquals(myCause, cause);
    }

}
