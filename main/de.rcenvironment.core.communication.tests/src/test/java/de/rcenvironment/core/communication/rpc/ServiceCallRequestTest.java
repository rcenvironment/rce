/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_PLATFORM;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.METHOD;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.PARAMETER_LIST;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REMOTE_PLATFORM;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.SERVICE;

import java.io.Serializable;
import java.util.List;

import junit.framework.TestCase;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Unit test for the <code>ServiceCallRequest</code> class.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 */
public class ServiceCallRequestTest extends TestCase {

    /**
     * Class under test.
     */
    private ServiceCallRequest myCommunicationRequest;

    @Override
    protected void setUp() throws Exception {
        myCommunicationRequest = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM,
            SERVICE, METHOD, PARAMETER_LIST);
    }

    @Override
    protected void tearDown() throws Exception {
        myCommunicationRequest = null;
    }

    /*
     * test for success
     */

    /**
     * Test Constructor for success.
     */
    public void testForSuccess() {
        new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, SERVICE, METHOD, PARAMETER_LIST);
        new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, SERVICE, METHOD, null);
    }

    /*
     * test for failure
     */

    /**
     * Test Constructor for failure.
     */
    public void testForFailure() {
        try {
            new ServiceCallRequest(null, REMOTE_PLATFORM, SERVICE, METHOD, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_PLATFORM, null, SERVICE, METHOD, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, null, METHOD, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, "", METHOD, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, SERVICE, null, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM, SERVICE, null, PARAMETER_LIST);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /*
     * test for success
     */

    /** Test. */
    public void testRequestedPlatform() {
        assertEquals(LOCAL_PLATFORM, myCommunicationRequest.getDestination());
    }

    /** Test. */
    public void testGetCallingPlatform() {
        assertEquals(REMOTE_PLATFORM, myCommunicationRequest.getSender());
    }

    /**
     * Test method for success.
     * 
     */
    public void testGetServiceForSuccess() {
        myCommunicationRequest.getServiceName();
    }

    /**
     * Test method for success.
     * 
     */
    public void testGetServiceMethodForSuccess() {
        myCommunicationRequest.getMethodName();
    }

    /**
     * Test method for success.
     * 
     */
    public void testGetParameterListForSuccess() {
        myCommunicationRequest.getParameterList();
    }

    /**
     * Test method for sanity.
     * 
     */
    public void testGetHostForSanity() {
        NodeIdentifier host = myCommunicationRequest.getDestination();
        assertEquals(host, LOCAL_PLATFORM);
    }

    /**
     * Test method for sanity.
     * 
     */
    public void testGetServiceForSanity() {
        String service = myCommunicationRequest.getServiceName();
        assertEquals(service, SERVICE);

    }

    /**
     * Test method for sanity.
     * 
     */
    public void testGetServiceMethodForSanity() {
        String serviceMethod = myCommunicationRequest.getMethodName();
        assertEquals(serviceMethod, METHOD);
    }

    /**
     * Test method for sanity.
     * 
     */
    public void testGetParameterListForSanity() {
        List<? extends Serializable> list = myCommunicationRequest.getParameterList();
        assertEquals(list, PARAMETER_LIST);
    }

}
