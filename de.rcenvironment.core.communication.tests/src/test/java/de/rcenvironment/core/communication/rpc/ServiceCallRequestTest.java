/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_LOGICAL_NODE_SESSION_ID;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.METHOD;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.PARAMETER_LIST;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REMOTE_LOGICAL_NODE_SESSION_ID;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.SERVICE;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import junit.framework.TestCase;

/**
 * Unit test for the <code>ServiceCallRequest</code> class.
 * 
 * @author Thijs Metsch
 * @author Heinrich Wendel
 * @author Robert Mischke
 */
public class ServiceCallRequestTest extends TestCase {

    /**
     * Class under test.
     */
    private ServiceCallRequest myCommunicationRequest;

    @Override
    protected void setUp() throws Exception {
        myCommunicationRequest = new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID,
            SERVICE, METHOD, PARAMETER_LIST, null);
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
        new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, SERVICE, METHOD, PARAMETER_LIST, null);
        new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, SERVICE, METHOD, null, null);
    }

    /*
     * test for failure
     */

    /**
     * Test Constructor for failure.
     */
    public void testForFailure() {
        try {
            new ServiceCallRequest(null, REMOTE_LOGICAL_NODE_SESSION_ID, SERVICE, METHOD, PARAMETER_LIST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, null, SERVICE, METHOD, PARAMETER_LIST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, null, METHOD, PARAMETER_LIST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, "", METHOD, PARAMETER_LIST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, SERVICE, null, PARAMETER_LIST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, REMOTE_LOGICAL_NODE_SESSION_ID, SERVICE, null, PARAMETER_LIST, null);
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
        assertEquals(LOCAL_LOGICAL_NODE_SESSION_ID, myCommunicationRequest.getTargetNodeId());
    }

    /** Test. */
    public void testGetCallingPlatform() {
        assertEquals(REMOTE_LOGICAL_NODE_SESSION_ID, myCommunicationRequest.getCallerNodeId());
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
        LogicalNodeSessionId host = myCommunicationRequest.getTargetNodeId();
        assertEquals(host, LOCAL_LOGICAL_NODE_SESSION_ID);
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
