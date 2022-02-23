/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_LOGICAL_NODE_SESSION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.internal.LiveNetworkIdResolutionServiceImpl;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkRequestFactory;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.MessageRoutingService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link ServiceProxyFactoryImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (reworked for 2.5.0+; 8.0.0 id adaptations)
 */
public class ServiceProxyFactoryImplTest {

    private static final LogicalNodeSessionId LOCAL_NODE_ID = NodeIdentifierTestUtils
        .createTestLogicalNodeSessionIdWithDisplayName("mockLocalNode", true);

    private static final InstanceNodeSessionId LOCAL_INSTANCE_SESSION_ID = LOCAL_NODE_ID.convertToInstanceNodeSessionId();

    private final int sum = 3;

    private final ServiceCallResult addResult = ServiceCallResultFactory.wrapReturnValue(sum);

    private final ServiceCallResult callbackResult = ServiceCallResultFactory.wrapReturnValue(new Serializable() {

        private static final long serialVersionUID = -35724097511389572L;
    });

    private final String value = new String("value");

    private final ServiceCallResult valueResult = ServiceCallResultFactory.wrapReturnValue(value);

    private final IOException exception = new IOException("Simulated test exception");

    private final ServiceCallResult exceptionResult = ServiceCallResultFactory.wrapMethodException(exception);

    private ServiceProxyFactoryImpl serviceProxyFactory;

    private RemoteServiceCallSenderServiceImpl remoteServiceCallService;

    private LiveNetworkIdResolutionServiceImpl liveIdResolver;

    /**
     * Common setup.
     */
    @Before
    public void setUp() {
        // note that the routing service is bound in the individual tests - misc_ro
        remoteServiceCallService = new RemoteServiceCallSenderServiceImpl();

        liveIdResolver = new LiveNetworkIdResolutionServiceImpl();
        liveIdResolver.registerLocalInstanceNodeSessionId(LOCAL_INSTANCE_SESSION_ID);

        serviceProxyFactory = new ServiceProxyFactoryImpl();
        serviceProxyFactory.bindCallbackService(new DummyCallbackService());
        serviceProxyFactory.bindCallbackProxyService(EasyMock.createNiceMock(CallbackProxyService.class));
        serviceProxyFactory.bindPlatformService(new DummyPlatformService());
        serviceProxyFactory.bindRemoteServiceCallService(remoteServiceCallService);
        serviceProxyFactory.bindLiveNetworkIdResolutionService(liveIdResolver);
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

        Object proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        assertTrue(proxy instanceof MethodCallTestInterface);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class,
            new Class<?>[] { BundleActivator.class, Bundle.class }, null);
        assertTrue(proxy instanceof MethodCallTestInterface);
        assertTrue(proxy instanceof BundleActivator);
        assertTrue(proxy instanceof Bundle);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        assertTrue(proxy instanceof MethodCallTestInterface);

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        assertTrue(proxy instanceof MethodCallTestInterface);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("rumpel", "false");
        properties.put("pumpel", "true");

        proxy = serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        assertTrue(proxy instanceof MethodCallTestInterface);

    }

    /**
     * Tests an RPC call with an return value of type "int".
     * 
     * @throws RemoteOperationException not expected; would indicate a test error
     */
    @Test
    public void testNativeReturnValue() throws RemoteOperationException {
        MethodCallTestInterface proxy =
            (MethodCallTestInterface) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = Integer.valueOf(new MethodCallTestInterfaceImpl().add(1, 2));
        MessageRoutingService routingServiceMock =
            createSingleCallNetworkMock(ServiceCallResultFactory.wrapReturnValue(expectedReturnValue));
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        assertEquals(3, proxy.add(1, 2));

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests an RPC call with a String return value.
     * 
     * @throws RemoteOperationException not expected; would indicate a test error
     */
    @Test
    public void testStringReturnValue() throws RemoteOperationException {

        MethodCallTestInterface proxy =
            (MethodCallTestInterface) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = new MethodCallTestInterfaceImpl().getString();
        MessageRoutingService routingServiceMock =
            createSingleCallNetworkMock(ServiceCallResultFactory.wrapReturnValue(expectedReturnValue));
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        assertEquals(expectedReturnValue, proxy.getString());

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests that passing a callback object as a remote method parameter succeeds.
     * 
     * @throws RemoteOperationException not expected; would indicate a test error
     **/
    @Test
    public void testCallbackParameterDoesNotFail() throws RemoteOperationException {

        MethodCallTestInterface proxy =
            (MethodCallTestInterface) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);
        Serializable expectedReturnValue;

        // create a network mock that simulates the remote generation of the expected result
        expectedReturnValue = Integer.valueOf(3);
        MessageRoutingService routingServiceMock =
            createSingleCallNetworkMock(ServiceCallResultFactory.wrapReturnValue(expectedReturnValue));
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        // TODO check: test probably too weak
        // TODO add test for callback object return value, too?
        proxy.callbackTest(new DummyObject());

        EasyMock.verify(routingServiceMock);
    }

    /**
     * Tests that exceptions throw by the target method are correctly forwarded to the caller.
     * 
     * @throws RemoteOperationException not expected; would indicate a test error
     */
    @Test
    public void testRemoteExceptionThrowing() throws RemoteOperationException {

        MethodCallTestInterface proxy =
            (MethodCallTestInterface) serviceProxyFactory.createServiceProxy(LOCAL_NODE_ID, MethodCallTestInterface.class, null, null);

        // create a network mock that simulates the remote generation of the expected result
        MessageRoutingService routingServiceMock = createSingleCallNetworkMock(ServiceCallResultFactory.wrapMethodException(exception));
        remoteServiceCallService.bindMessageRoutingService(routingServiceMock);

        EasyMock.replay(routingServiceMock);

        try {
            proxy.ioExceptionThrower();
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
            serviceProxyFactory.createServiceProxy(null, MethodCallTestInterface.class, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            serviceProxyFactory.createServiceProxy(LOCAL_LOGICAL_NODE_SESSION_ID, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    private MessageRoutingService createSingleCallNetworkMock(final ServiceCallResult serviceCallResult) {
        // define expected calls
        MessageRoutingService routingServiceMock = EasyMock.createMock(MessageRoutingService.class);
        // a request is not available in this mock, so fake a request
        NetworkRequest request =
            NetworkRequestFactory
                .createNetworkRequest(null, ProtocolConstants.VALUE_MESSAGE_TYPE_RPC, LOCAL_INSTANCE_SESSION_ID,
                    LOCAL_INSTANCE_SESSION_ID);
        NetworkResponse networkResponseMock = NetworkResponseFactory.generateSuccessResponse(request,
            MessageUtils.serializeSafeObject(serviceCallResult));
        EasyMock.expect(routingServiceMock.performRoutedRequest(EasyMock.anyObject(byte[].class),
            EasyMock.eq(ProtocolConstants.VALUE_MESSAGE_TYPE_RPC), EasyMock.eq(LOCAL_INSTANCE_SESSION_ID)))
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
        public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws RemoteOperationException {
            ServiceCallResult result = null;
            if (serviceCallRequest.getTargetNodeId().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getServiceName().equals(MethodCallTestInterface.class.getCanonicalName())
                && serviceCallRequest.getMethodName().equals("add")
                && serviceCallRequest.getParameterList().get(0).equals(1)
                && serviceCallRequest.getParameterList().get(1).equals(2)
                && serviceCallRequest.getParameterList().size() == 2) {

                result = addResult;
            } else if (serviceCallRequest.getTargetNodeId().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getServiceName().equals(MethodCallTestInterface.class.getCanonicalName())
                && serviceCallRequest.getMethodName().equals("callbackTest")
                && (serviceCallRequest.getParameterList().get(0) instanceof CallbackProxy)
                && serviceCallRequest.getParameterList().size() == 1) {

                result = callbackResult;
            } else if (serviceCallRequest.getTargetNodeId().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getServiceName().equals(MethodCallTestInterface.class.getCanonicalName())
                && serviceCallRequest.getMethodName().equals("getValue")
                && serviceCallRequest.getParameterList().size() == 0) {

                result = valueResult;
            } else if (serviceCallRequest.getTargetNodeId().equals(LOCAL_NODE_ID)
                && serviceCallRequest.getServiceName().equals(MethodCallTestInterface.class.getCanonicalName())
                && serviceCallRequest.getMethodName().equals("exceptionFunction")
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
        public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
            return LOCAL_INSTANCE_SESSION_ID;
        }

        @Override
        public LogicalNodeSessionId getLocalDefaultLogicalNodeSessionId() {
            return LOCAL_LOGICAL_NODE_SESSION_ID;
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
        public String addCallbackObject(Object callBackObject, InstanceNodeSessionId nodeId) {
            return null;
        }

        @Override
        public Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters)
            throws RemoteOperationException {
            return null;
        }

        @Override
        public CallbackProxy createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, InstanceNodeSessionId proxyHome) {
            if (objectIdentifier == id) {
                return new CallbackProxy() {

                    private static final long serialVersionUID = -212249805288118195L;

                    @Override
                    public String getObjectIdentifier() {
                        return id;
                    }

                    @Override
                    public InstanceNodeSessionId getHomePlatform() {
                        return LOCAL_INSTANCE_SESSION_ID;
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
