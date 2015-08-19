/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.legacy.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCALHOST;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RMI_PORT;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RMI_PROTOCOL;
import junit.framework.TestCase;

/**
 * Test cases for <code>CommunicationContact</code>.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke (refactoring)
 */
public class NetworkContactTest extends TestCase {

    /**
     * Entry to test.
     */
    private static final NetworkContact TEST_CONTACT = new NetworkContact(LOCALHOST, RMI_PROTOCOL, RMI_PORT);

    @Override
    protected void setUp() throws Exception {}

    @Override
    protected void tearDown() throws Exception {}

    /**
     * Test getHostname().
     */
    public void testGetHostname() {
        assertEquals(TEST_CONTACT.getHost(), LOCALHOST);
    }

    /**
     * Test getProtocol().
     */
    public void testGetProtocol() {
        assertEquals(TEST_CONTACT.getProtocol(), RMI_PROTOCOL);
    }

    /**
     * Test getPort().
     */
    public void testGetPort() {
        assertEquals(TEST_CONTACT.getPort().intValue(), RMI_PORT);
    }

    /**
     * Test equals().
     */
    public void testEquals() {
        assertEquals(TEST_CONTACT, TEST_CONTACT);
    }

    /**
     * Test toString().
     */
    public void testToString() {
        assertEquals(TEST_CONTACT.toString(), LOCALHOST + ":" + RMI_PROTOCOL + ":" + RMI_PORT);
    }

    /**
     * Test hashCode().
     */
    public void testHashCode() {
        assertEquals(TEST_CONTACT.toString().hashCode(), TEST_CONTACT.hashCode());
    }

    /**
     * Test Constructor for Failure().
     */
    public void testConstructorForFailure() {
        try {
            new NetworkContact(null, RMI_PROTOCOL, RMI_PORT);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new NetworkContact(LOCALHOST, null, RMI_PORT);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new NetworkContact(LOCALHOST, RMI_PROTOCOL, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
