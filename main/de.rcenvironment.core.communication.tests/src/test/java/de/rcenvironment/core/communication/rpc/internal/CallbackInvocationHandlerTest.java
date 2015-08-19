/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.messaging.internal.MessageEndpointHandlerImpl;
import de.rcenvironment.core.communication.messaging.internal.RPCRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.routing.internal.NetworkRoutingServiceImpl;
import de.rcenvironment.core.communication.rpc.ServiceCallHandler;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

/**
 * Test case for {@link CallbackInvocationHandler}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (reworked for 2.5.0+)
 */
public class CallbackInvocationHandlerTest {

    private final CallbackObject callbackObject = new DummyObject();

    private final String objectID = "callMe";

    private final String puffParam = "knaller";

    private final Integer puffIterations = 5;

    private final String puffMethod = "makePuff";

    private final String pengMethod = "makePeng";

    private final String throwMethod = "throwSomething";

    private final String puff1RV = "puff1";

    private final String puff2RV = "puff2";

    private final String pengRV = "peng";

    private final ServiceCallResult puffResult = new ServiceCallResult(puff1RV);

    private final ServiceCallResult pengResult = new ServiceCallResult(pengRV);

    private final NodeIdentifier piLocal = NodeIdentifierFactory.fromNodeId("mockLocalNodeId");

    private final NodeIdentifier piRemote = NodeIdentifierFactory.fromNodeId("mockRemoteNodeId");

    /**
     * Tests various remote method calls.
     * 
     * @throws Throwable on unhandled exceptions
     */
    @Test
    public void test() throws Throwable {

        // create the simulated ServiceCallRequestPayloadHandler
        final MessageEndpointHandlerImpl scrHandler = new MessageEndpointHandlerImpl();
        scrHandler.registerRequestHandler(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC, new RPCRequestHandler(
            new SimulatingServiceCallHandler()));

        // create the network layer mock
        MessageRoutingService messageRoutingServiceMock = EasyMock.createMock(MessageRoutingService.class);
        // create an EasyMock delegate that delegates to the ServiceCallRequestPayloadHandler
        // created above
        MessageRoutingService messageRoutingServiceDelegate = new NetworkRoutingServiceImpl() {

            @Override
            public Future<NetworkResponse> performRoutedRequest(final byte[] payload, final String messageType,
                final NodeIdentifier receiver) {
                return SharedThreadPool.getInstance().submit(new Callable<NetworkResponse>() {

                    @Override
                    public NetworkResponse call() throws Exception {
                        NetworkRequest request = NetworkRequestFactory.createNetworkRequest(payload, messageType, piLocal, receiver);
                        NetworkResponse result;
                        try {
                            result = scrHandler.onRequestArrivedAtDestination(request);
                        } catch (RuntimeException e) {
                            result = NetworkResponseFactory.generateResponseForExceptionAtDestination(request, e);
                        }
                        return result;
                    }
                });
            }
        };

        EasyMock
            .expect(
                messageRoutingServiceMock.performRoutedRequest(EasyMock.anyObject(byte[].class),
                    EasyMock.eq(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC), EasyMock.eq(piLocal)))
            .andDelegateTo(messageRoutingServiceDelegate).anyTimes();

        RemoteServiceCallServiceImpl remoteServiceCallService = new RemoteServiceCallServiceImpl();
        remoteServiceCallService.bindMessageRoutingService(messageRoutingServiceMock);

        // note that this approach only works for unit tests; the (currently unavoidable) singleton
        // nature of the holder would lead to erratic behaviour in multi-node tests - misc_ro
        CallbackInvocationHandler.RemoteServiceCallServiceHolder.bindRemoteServiceCallService(remoteServiceCallService);

        EasyMock.replay(messageRoutingServiceMock);
        CallbackInvocationHandler handler =
            new CallbackInvocationHandler(callbackObject, objectID, piLocal, piRemote);

        Method method = CallbackProxy.class.getMethod("getObjectIdentifier", new Class[] {});
        assertEquals(objectID, handler.invoke(new Object(), method, new Object[] {}));

        method = CallbackProxy.class.getMethod("getHomePlatform", new Class[] {});
        assertEquals(piLocal, handler.invoke(new Object(), method, new Object[] {}));

        method = DummyInterface.class.getMethod(puffMethod, new Class[] { String.class });
        assertEquals(puff1RV, handler.invoke(new Object(), method, new Object[] { puffParam }));

        method = DummyInterface.class.getMethod(puffMethod, new Class[] { Integer.class });
        assertEquals(puff2RV, handler.invoke(new Object(), method, new Object[] { puffIterations }));

        method = DummyInterface.class.getMethod(pengMethod, new Class[] {});
        assertEquals(pengRV, handler.invoke(new Object(), method, null));

        method = DummyInterface.class.getMethod(throwMethod, new Class[] {});

        try {
            handler.invoke(new Object(), method, new Object[] {});
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    /**
     * Test {@link ServiceCallHandler} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (changed from ServiceCallSender to ServiceCallHandler)
     */
    private final class SimulatingServiceCallHandler implements ServiceCallHandler {

        @SuppressWarnings("unchecked")
        @Override
        public ServiceCallResult handle(ServiceCallRequest serviceCallRequest) {
            if (serviceCallRequest.getRequestedPlatform().equals(piLocal)
                && serviceCallRequest.getService().equals(CallbackService.class.getCanonicalName())
                && serviceCallRequest.getServiceMethod().equals("callback")
                && serviceCallRequest.getServiceProperties() == null
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
                    return new ServiceCallResult(new NullPointerException());
                }
            }
            return new ServiceCallResult(new IllegalStateException("Test error: no match in "
                + SimulatingServiceCallHandler.class.getName()));
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
            return puff2RV;
        }

        @Override
        public Class<?> getInterface() {
            return DummyInterface.class;
        }

    }
}
