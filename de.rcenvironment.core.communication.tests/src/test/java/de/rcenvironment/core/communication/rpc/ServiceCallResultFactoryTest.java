/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;

/**
 * {@link ServiceCallResultFactory} test.
 *
 * @author Robert Mischke
 */
public class ServiceCallResultFactoryTest {

    private LogicalNodeSessionId targetNodeId;

    private LogicalNodeSessionId callerNodeId;

    /**
     * Test for
     * {@link de.rcenvironment.core.communication.rpc.ServiceCallResultFactory#representInternalErrorAtHandler(ServiceCallRequest, String)}.
     */
    @Test
    public void testRepresentationOfInternalErrorAtHandler() {
        final ServiceCallRequest scRequest = createDummyRequest();
        final String errorMessage = "test error message";
        final String exceptionMessage = "test exception message";
        final ServiceCallResult scResult =
            ServiceCallResultFactory.representInternalErrorAtHandler(scRequest, errorMessage, new IOException(exceptionMessage));

        // test the client-readable error message
        assertTrue(scResult.getRemoteOperationExceptionMessage().contains("error performing this remote operation"));
        assertTrue(scResult.getRemoteOperationExceptionMessage().contains("E#")); // error marker prefix; TODO (p3) replace with constant

        // verify other fields
        assertNull(scResult.getMethodExceptionType());
        assertNull(scResult.getMethodExceptionMessage());
        assertNull(scResult.getReturnValue());
        assertFalse(scResult.isSuccess());
    }

    /**
     * Test for {@link de.rcenvironment.core.communication.rpc.ServiceCallResultFactory#representNetworkErrorAsRemoteOperationException()}
     * in combination with {@link de.rcenvironment.core.communication.protocol.NetworkResponseFactory#generateResponseForNoRouteAtSender()}.
     */
    @Test
    public void testRepresentationOfNoRouteAtSender() {
        final ServiceCallRequest scRequest = createDummyRequest();
        final NetworkRequest nwRequestMock = EasyMock.createMock(NetworkRequest.class);
        final InstanceNodeSessionId errorReporterNodeId = NodeIdentifierTestUtils.createTestInstanceNodeSessionId();
        final NetworkResponse networkResponse =
            NetworkResponseFactory.generateResponseForNoRouteAtSender(nwRequestMock, errorReporterNodeId);
        final ServiceCallResult scResult =
            ServiceCallResultFactory.representNetworkErrorAsRemoteOperationException(scRequest,
                networkResponse);

        // test the client-readable error message
        assertTrue(scResult.getRemoteOperationExceptionMessage().contains(errorReporterNodeId.toString()));
        assertTrue(scResult.getRemoteOperationExceptionMessage().contains(errorReporterNodeId.getInstanceNodeSessionIdString()));
        assertFalse(scResult.getRemoteOperationExceptionMessage().contains("Failed to parse")); // Mantis #0014653

        // verify other fields
        assertNull(scResult.getMethodExceptionType());
        assertNull(scResult.getMethodExceptionMessage());
        assertNull(scResult.getReturnValue());
        assertFalse(scResult.isSuccess());
    }

    private ServiceCallRequest createDummyRequest() {
        targetNodeId = NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true);
        callerNodeId = NodeIdentifierTestUtils.createTestLogicalNodeSessionId(true);
        final ServiceCallRequest request =
            new ServiceCallRequest(targetNodeId, callerNodeId, "Service", "Method", new ArrayList<Serializable>(), null);
        return request;
    }

}
