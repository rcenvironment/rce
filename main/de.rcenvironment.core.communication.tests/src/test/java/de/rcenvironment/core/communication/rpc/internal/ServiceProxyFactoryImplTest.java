/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_PLATFORM;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

/**
 * Test cases for {@link ServiceProxyFactoryImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (reworked for 2.5.0+)
 */
public class ServiceProxyFactoryImplTest {

    private static final NodeIdentifier LOCAL_NODE_ID = NodeIdentifierFactory.fromNodeId("mockNodeId");

    private final int sum = 3;

    private final ServiceCallResult addResult = new ServiceCallResult(sum);

    private final ServiceCallResult callbackResult = new ServiceCallResult(new Serializable() {

        private static final long serialVersionUID = -35724097511389572L;
    });

    private final String value = new String("value");

    private final ServiceCallResult valueResult = new ServiceCallResult(value);

    private final IOException exception = new IOException("Simulated test exception");

    private final ServiceCallResult exceptionResult = new ServiceCallResult(exception);

    private ServiceProxyFactoryImpl serviceProxyFactory;

    private RemoteServiceCallServiceImpl remoteServiceCallService;

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        // note that the routing service is bound in the individual tests - misc_ro
        remoteServiceCallService = new RemoteServiceCallServiceImpl();

        serviceProxyFactory = new ServiceProxyFactoryImpl();
        serviceProxyFactory.bindCallbackService(new DummyCallbackService());
        serviceProxyFactory.bindCallbackProxyService(EasyMock.createNiceMock(CallbackProxyService.class));
        serviceProxyFactory.bindPlatformService(new DummyPlatformService());
        serviceProxyFactory.bindRemoteServiceCallService(remoteServiceCallService);
    }

    /**
     * Common teardown.
     */
    @After
    public void tearDown() {
        serviceProxyFactory = null;
    }

    /** Test. **/
    @Test
    public void testCreateServiceProxyForSuccess() {

        Object proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class, null, (String) null);
        assertTrue(proxy instanceof MethodCallerTestMethods);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
            new Class<?>[] { BundleActivator.class, Bundle.class }, (Map<String, String>) null);
        assertTrue(proxy instanceof MethodCallerTestMethods);
        assertTrue(proxy instanceof BundleActivator);
        assertTrue(proxy instanceof Bundle);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class, null, "(&(rumpel=false)(pumpel=true)");
        assertTrue(proxy instanceof MethodCallerTestMethods);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
            null, REQUEST.getServiceProperties());
        assertTrue(proxy instanceof MethodCallerTestMethods);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("rumpel", "false");
        properties.put("pumpel", "true");

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class, null, properties);
        assertTrue(proxy instanceof MethodCallerTestMethods);

    }

    /**
     * Tests an RPC call with an return value of type "int".
     */
    @Test
    public void testNativeReturnValue() {
        MethodCallerTestMethods proxy =
            (MethodCallerTestMethods) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
                null, (String) null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = Integer.valueOf(new MethodCallerTestMethodsImpl().add(1, 2));
        MessageRoutingService routingServiceMock = createSingleCallNetworkMock(expectedReturnValue);
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        assertEquals(3, proxy.add(1, 2));

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests an RPC call with a String return value.
     */
    @Test
    public void testStringReturnValue() {

        MethodCallerTestMethods proxy =
            (MethodCallerTestMethods) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
                null, (String) null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = new MethodCallerTestMethodsImpl().getValue();
        MessageRoutingService routingServiceMock = createSingleCallNetworkMock(expectedReturnValue);
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        assertEquals(expectedReturnValue, proxy.getValue());

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests that passing a callback object as a remote method parameter succeeds.
     **/
    @Test
    public void testCallbackParameterDoesNotFail() {

        MethodCallerTestMethods proxy =
            (MethodCallerTestMethods) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
                null, (String) null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = Integer.valueOf(3);
        MessageRoutingService routingServiceMock = createSingleCallNetworkMock(expectedReturnValue);
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        // TODO check: test probably too weak
        // TODO add test for callback object return value, too?
        proxy.callbackTest(new DummyObject());

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests that exceptions throw by the target method are correctly forwarded to the caller.
     */
    @Test
    public void testRemoteExceptionThrowing() {

        MethodCallerTestMethods proxy =
            (MethodCallerTestMethods) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallerTestMethods.class,
                null, (String) null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = exception;
        MessageRoutingService routingServiceMock = createSingleCallNetworkMock(expectedReturnValue);
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        try {
            proxy.exceptionFunction();
            fail();
        } catch (IOException e) {
            assertTrue(true);
        }

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Test.
     * 
     * TODO improve Javadoc
     */
    @Test
    public void testCreateServiceProxyForFailure() {
        try {
            serviceProxyFactory.createServiceProxy(null, MethodCallerTestMethods.class, null, (String) null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            serviceProxyFactory.createServiceProxy(LOCAL_PLATFORM, null, null, (String) null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    private MessageRoutingService createSingleCallNetworkMock(final Serializable mockReturnValue) {
        MessageRoutingService routingServiceMock = EasyMock.createMock(MessageRoutingService.class);
        // define expected calls
        Future<NetworkResponse> networkResponseMock = SharedThreadPool.getInstance().submit(new Callable<NetworkResponse>() {

            @Override
            public NetworkResponse call() throws Exception {
                // request is not available in this mock, so fake a request
                NetworkRequest request =
                    NetworkRequestFactory
                        .createNetworkRequest(null, ProtocolConstants.VALUE_MESSAGE_TYPE_RPC, LOCAL_NODE_ID, LOCAL_NODE_ID);
                NetworkResponse response =
                    NetworkResponseFactory.generateSuccessResponse(request,
                        MessageUtils.serializeSafeObject(new ServiceCallResult(mockReturnValue)));
                return response;
            }
        });
        EasyMock.expect(routingServiceMock.performRoutedRequest(EasyMock.anyObject(byte[].class),
            EasyMock.eq(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC), EasyMock.eq(LOCAL_NODE_ID)))
            .andReturn(networkResponseMock);
        return routingServiceMock;
    }

    /**
     * Test {@link ServiceCallSenderFactory} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyServiceCallSenderFactory implements ServiceCallSenderFactory {

        @Override
        public ServiceCallSender createServiceCallSender(NetworkContact contact) throws CommunicationException {
            return new DummyServiceCallSender();
        }

    }

    /**
     * Test {@link ServiceCallSender} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyServiceCallSender implements ServiceCallSender {

        @Override
        public void initialize(NetworkContact contact) throws CommunicationException {}

        @Override
        public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws CommunicationException {
            ServiceCallResult result = null;
            if (serviceCallRequest.getRequestedPlatform().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getService().equals(MethodCallerTestMethods.class.getCanonicalName())
                && serviceCallRequest.getServiceMethod().equals("add")
                && serviceCallRequest.getServiceProperties() == null
                && serviceCallRequest.getParameterList().get(0).equals(1)
                && serviceCallRequest.getParameterList().get(1).equals(2)
                && serviceCallRequest.getParameterList().size() == 2) {

                result = addResult;
            } else if (serviceCallRequest.getRequestedPlatform().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getService().equals(MethodCallerTestMethods.class.getCanonicalName())
                && serviceCallRequest.getServiceMethod().equals("callbackTest")
                && serviceCallRequest.getServiceProperties() == null
                && (serviceCallRequest.getParameterList().get(0) instanceof CallbackProxy)
                && serviceCallRequest.getParameterList().size() == 1) {

                result = callbackResult;
            } else if (serviceCallRequest.getRequestedPlatform().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getService().equals(MethodCallerTestMethods.class.getCanonicalName())
                && serviceCallRequest.getServiceMethod().equals("getValue")
                && serviceCallRequest.getServiceProperties() == null
                && serviceCallRequest.getParameterList().size() == 0) {

                result = valueResult;
            } else if (serviceCallRequest.getRequestedPlatform().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getService().equals(MethodCallerTestMethods.class.getCanonicalName())
                && serviceCallRequest.getServiceMethod().equals("exceptionFunction")
                && serviceCallRequest.getServiceProperties() == null
                && serviceCallRequest.getParameterList().size() == 0) {

                result = exceptionResult;
            }
            return result;
        }

    }

    /**
     * Test object to call back.
     * 
     * @author Doreen Seider
     */
    private class DummyObject implements DummyInterface, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> getInterface() {
            return DummyInterface.class;
        }

        @Override
        public Object makePeng() {
            return null;
        }

        @Override
        public Object makePuff(String string) {
            return null;
        }

    }

    /**
     * Dummy interface.
     * 
     * @author Doreen Seider
     */
    private interface DummyInterface extends CallbackObject {

        Object makePuff(String string);

        Object makePeng();
    }

    /**
     * Test implementation of the {@link PlatformService}.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformService extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return LOCAL_NODE_ID;
        }

    }

    /**
     * Test {@link CallbackService} implementation.
     * 
     * @author Doreen Seider
     */
    private static class DummyCallbackService implements CallbackService, Serializable {

        private static final long serialVersionUID = 3384460645457699538L;

        private final String id = "id";

        @Override
        public String addCallbackObject(Object callBackObject, NodeIdentifier nodeId) {
            return null;
        }

        @Override
        public Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters)
            throws CommunicationException {
            return null;
        }

        @Override
        public CallbackProxy createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, NodeIdentifier proxyHome) {
            if (objectIdentifier == id) {
                return new CallbackProxy() {

                    private static final long serialVersionUID = -212249805288118195L;

                    @Override
                    public String getObjectIdentifier() {
                        return id;
                    }

                    @Override
                    public NodeIdentifier getHomePlatform() {
                        return LOCAL_NODE_ID;
                    }
                };
            }
            return null;
        }

        @Override
        public Object getCallbackObject(String objectIdentifier) {
            return new Serializable() {

                private static final long serialVersionUID = -5137415926803074469L;
            };
        }

        @Override
        public String getCallbackObjectIdentifier(Object callbackObject) {
            return id;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {}

    }
}
