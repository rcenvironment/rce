/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.messaging.internal.MessageEndpointHandlerImpl;
import de.rcenvironment.core.communication.messaging.internal.RPCNetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.internal.NetworkRoutingServiceImpl;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.api.RemotableCallbackService;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.LogUtils;

/**
 * Test case for {@link CallbackInvocationHandler}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (reworked for 2.5.0+; 8.0.0 id adaptations)
 */
public class CallbackInvocationHandlerTest {

    private final CallbackObject callbackObject = new DummyObject();

    private final String objectID = "callMe";

    private final String puffParam = "knaller";

    private final Integer puffIterations = 5;

    private final String puffMethod = "makePuff";

    private final String pengMethod = "makePeng";

    private final String throwMethod = "throwSomething";

    private final String puff1RetVal = "puff1";

    private final String puff2RetVal = "puff2";

    private final String pengRetVal = "peng";

    private final ServiceCallResult puffResult = ServiceCallResultFactory.wrapReturnValue(puff1RetVal);

    private final ServiceCallResult pengResult = ServiceCallResultFactory.wrapReturnValue(pengRetVal);

    private final InstanceNodeSessionId instanceSessionIdLocal = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName(
        "mockLocalNode");

    private final InstanceNodeSessionId instanceSessionIdRemote = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName(
        "mockRemoteNode");

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Tests various remote method calls.
     * 
     * @throws Throwable on unhandled exceptions
     */
    @Test
    public void test() throws Throwable {

        // create the simulated ServiceCallRequestPayloadHandler
        final MessageEndpointHandlerImpl scrHandler =
            new MessageEndpointHandlerImpl(NodeIdentifierTestUtils.getTestNodeIdentifierService());
        scrHandler.registerRequestHandler(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC, new RPCNetworkRequestHandler(
            new SimulatingServiceCallHandler(), EasyMock.createNiceMock(ReliableRPCStreamService.class)));

        // create the network layer mock
        MessageRoutingService messageRoutingServiceMock = EasyMock.createMock(MessageRoutingService.class);
        // create an EasyMock delegate that delegates to the ServiceCallRequestPayloadHandler
        // created above
        MessageRoutingService messageRoutingServiceDelegate = new NetworkRoutingServiceImpl() {

            @Override
            public NetworkResponse performRoutedRequest(final byte[] payload, final String messageType,
                final InstanceNodeSessionId receiver, int timeoutMsec) {
                NetworkRequest request = NetworkRequestFactory.createNetworkRequest(payload, messageType, instanceSessionIdLocal, receiver);
                NetworkResponse result;
                try {
                    NodeIdentifierTestUtils.attachTestNodeIdentifierServiceToCurrentThread();
                    result = scrHandler.onRequestArrivedAtDestination(request);
                } catch (RuntimeException e) {
                    // TODO review: is this a useful test approach?
                    String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log,
                        "Uncaught RuntimeException thrown by request handler", e);
                    result = NetworkResponseFactory.generateResponseForInternalErrorAtRecipient(request, errorId);
                }
                NodeIdentifierTestUtils.removeTestNodeIdentifierServiceFromCurrentThread();
                return result;
            }
        };

        EasyMock
            .expect(
                messageRoutingServiceMock.performRoutedRequest(EasyMock.anyObject(byte[].class),
                    EasyMock.eq(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC), EasyMock.eq(instanceSessionIdLocal)))
            .andDelegateTo(messageRoutingServiceDelegate).anyTimes();

        RemoteServiceCallSenderServiceImpl remoteServiceCallService = new RemoteServiceCallSenderServiceImpl();
        remoteServiceCallService.bindMessageRoutingService(messageRoutingServiceMock);

        // note that this approach only works for unit tests; the (currently unavoidable) singleton
        // nature of the holder would lead to erratic behaviour in multi-node tests - misc_ro
        new CallbackInvocationHandler.RemoteServiceCallServiceHolder().bindRemoteServiceCallService(remoteServiceCallService);

        EasyMock.replay(messageRoutingServiceMock);
        CallbackInvocationHandler handler =
            new CallbackInvocationHandler(callbackObject, objectID, instanceSessionIdLocal, instanceSessionIdRemote);

        Method method = CallbackProxy.class.getMethod("getObjectIdentifier", new Class[] {});
        assertEquals(objectID, handler.invoke(new Object(), method, new Object[] {}));

        method = CallbackProxy.class.getMethod("getHomePlatform", new Class[] {});
        assertEquals(instanceSessionIdLocal, handler.invoke(new Object(), method, new Object[] {}));

        method = DummyInterface.class.getMethod(puffMethod, new Class[] { String.class });
        assertEquals(puff1RetVal, handler.invoke(new Object(), method, new Object[] { puffParam }));

        method = DummyInterface.class.getMethod(puffMethod, new Class[] { Integer.class });
        assertEquals(puff2RetVal, handler.invoke(new Object(), method, new Object[] { puffIterations }));

        method = DummyInterface.class.getMethod(pengMethod, new Class[] {});
        assertEquals(pengRetVal, handler.invoke(new Object(), method, null));

        method = DummyInterface.class.getMethod(throwMethod, new Class[] {});

        try {
            handler.invoke(new Object(), method, new Object[] {});
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    /**
     * Test {@link RemoteServiceCallHandlerService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (changed from ServiceCallSender to ServiceCallHandler)
     */
    private final class SimulatingServiceCallHandler implements RemoteServiceCallHandlerService {

        @SuppressWarnings("unchecked")
        @Override
        public ServiceCallResult dispatchToLocalService(ServiceCallRequest serviceCallRequest) {
            if (serviceCallRequest.getTargetNodeId().isSameInstanceNodeSessionAs(instanceSessionIdLocal)
                && serviceCallRequest.getServiceName().equals(RemotableCallbackService.class.getCanonicalName())
                && serviceCallRequest.getMethodName().equals("callback")
                && serviceCallRequest.getParameterList().get(0).equals(objectID)) {

                if (serviceCallRequest.getParameterList().get(1).equals(pengMethod)
                    && ((ArrayList<Serializable>) serviceCallRequest.getParameterList().get(2)).size() == 0) {
                    return pengResult;
                } else if (serviceCallRequest.getParameterList().get(1).equals(puffMethod)
                    && ((ArrayList<Serializable>) serviceCallRequest.getParameterList().get(2)).size() == 1
                    && ((ArrayList<Serializable>) serviceCallRequest.getParameterList().get(2)).get(0).equals(puffParam)) {
                    return puffResult;
                } else if (serviceCallRequest.getParameterList().get(1).equals(throwMethod)
                    && ((ArrayList<Serializable>) serviceCallRequest.getParameterList().get(2)).size() == 0) {
                    // RPC target exceptions are wrapped in ServiceCallResults, so emulate this
                    return ServiceCallResultFactory.wrapMethodException(new NullPointerException());
                }
            }
            // TODO review: is this the appropriate wrapper factory method?
            return ServiceCallResultFactory.representInternalErrorAtSender(serviceCallRequest, "Test error: no service call match in "
                + SimulatingServiceCallHandler.class.getName());
        }

    }

    /**
     * Dummy interface.
     * 
     * @author Doreen Seider
     */
    private interface DummyInterface extends CallbackObject {

        @CallbackMethod
        Object makePuff(String string);

        Object makePuff(Integer iteration);

        @CallbackMethod
        Object makePeng();

        @CallbackMethod
        void throwSomething();

    }

    /**
     * Dummy object.
     * 
     * @author Doreen Seider
     */
    private class DummyObject implements DummyInterface {

        private static final long serialVersionUID = 5864698550749464575L;

        private static final String ERROR_MESSAGE = "should never be called, because annotated to be callback remotely.";

        @Override
        public Object makePuff(String string) {
            throw new RuntimeException(ERROR_MESSAGE);
        }

        @Override
        public Object makePeng() {
            throw new RuntimeException(ERROR_MESSAGE);
        }

        @Override
        public void throwSomething() {
            throw new RuntimeException(ERROR_MESSAGE);
        }

        @Override
        public Object makePuff(Integer iteration) {
            return puff2RetVal;
        }

        @Override
        public Class<?> getInterface() {
            return DummyInterface.class;
        }

    }
}
