/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_LOGICAL_NODE_SESSION_ID;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REMOTE_CONTACT;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REQUEST;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RESULT;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RETURN_VALUE;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.SERVICE_CONTACT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.communication.testutils.CommunicationTestHelper;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import junit.framework.TestCase;

/**
 * Unit test for {@link ServiceCallHandlerServiceImpl} with an OSGi service resolver.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class OSGiServiceCallHandlerImplTest extends TestCase {

    private static final String CALLBACK_TEST_METHOD = "callbackTest";

    private static ServiceCallHandlerServiceImpl callHandler;

    private final String objectID1 = "id1";

    private final String objectID2 = "id2";

    private final String objectID3 = "id3";

    private final CallbackProxy proxy = new DummyProxy(objectID1);

    private final CallbackObject object = new DummyObject();

    public static ServiceCallHandlerServiceImpl getCallHandler() {
        return callHandler;
    }

    @Override
    public void setUp() throws Exception {

        Bundle bundleMock = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundleMock.getSymbolicName()).andReturn("de.rcenvironment.rce.communication.rmi").anyTimes();
        EasyMock.replay(bundleMock);

        ServiceCallSender senderMock = EasyMock.createNiceMock(ServiceCallSender.class);
        EasyMock.expect(senderMock.send(REQUEST)).andReturn(RESULT).anyTimes();
        EasyMock.replay(senderMock);

        ServiceReference<?> factoryRefMock = EasyMock.createNiceMock(ServiceReference.class);

        ServiceCallSenderFactory factoryMock = EasyMock.createNiceMock(ServiceCallSenderFactory.class);
        EasyMock.expect(factoryMock.createServiceCallSender(SERVICE_CONTACT))
            .andReturn(senderMock).anyTimes();
        EasyMock.expect(factoryMock.createServiceCallSender(REMOTE_CONTACT))
            .andReturn(new ServiceCallSenderDummy()).anyTimes();
        EasyMock.replay(factoryMock);

        ServiceReference<?> testServiceRef = EasyMock.createNiceMock(ServiceReference.class);

        MethodCallTestInterface testmethods = new MethodCallTestInterfaceImpl();

        BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);

        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] { bundleMock }).anyTimes();

        EasyMock.expect(contextMock.getServiceReferences(EasyMock.eq(ServiceCallSenderFactory.class.getName()),
            EasyMock.eq((String) null))).andReturn(new ServiceReference[] { factoryRefMock }).anyTimes();
        EasyMock.expect(contextMock.getServiceReferences(REQUEST.getServiceName(), null))
            .andReturn(new ServiceReference[] { testServiceRef }).anyTimes();

        contextMock.getService(factoryRefMock);
        EasyMock.expectLastCall().andReturn(factoryMock).anyTimes();
        contextMock.getService(testServiceRef);
        EasyMock.expectLastCall().andReturn(testmethods).anyTimes();
        EasyMock.replay(contextMock);

        callHandler = new ServiceCallHandlerServiceImpl();
        callHandler.bindPlatformService(new DummyPlatformService());
        callHandler.bindCallbackService(new DummyCallbackService());
        callHandler.bindCallbackProxyService(new DummyCallbackProxyService());
        OSGiLocalServiceResolver serviceResolver = new OSGiLocalServiceResolver();
        serviceResolver.activate(contextMock);
        callHandler.bindLocalServiceResolver(serviceResolver);
    }

    /**
     * Test normal call.
     * 
     * @throws Exception if the test fails.
     */
    public void testLocalCall() throws Exception {

        ServiceCallResult result = callHandler.dispatchToLocalService(REQUEST);
        assertEquals(RETURN_VALUE, result.getReturnValue());

        // equal call request to cover cache functionality
        result = callHandler.dispatchToLocalService(REQUEST);
        assertEquals(result.getReturnValue(), RETURN_VALUE);

        List<Serializable> params = new ArrayList<>();
        params.add(new DummyProxy(objectID1));

        ServiceCallRequest callbackRequest =
            new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, CommunicationTestHelper.REMOTE_LOGICAL_NODE_SESSION_ID,
                MethodCallTestInterface.class.getCanonicalName(), CALLBACK_TEST_METHOD, params, null);

        assertNotNull(callHandler.dispatchToLocalService(callbackRequest));

        params = new ArrayList<Serializable>();
        params.add(new DummyProxy(objectID2));

        callbackRequest = new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, CommunicationTestHelper.REMOTE_LOGICAL_NODE_SESSION_ID,
            MethodCallTestInterface.class.getCanonicalName(), CALLBACK_TEST_METHOD, params, null);

        assertNotNull(callHandler.dispatchToLocalService(callbackRequest));

        params = new ArrayList<Serializable>();
        params.add(new DummyProxy(objectID3));

        callbackRequest = new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, CommunicationTestHelper.REMOTE_LOGICAL_NODE_SESSION_ID,
            MethodCallTestInterface.class.getCanonicalName(), CALLBACK_TEST_METHOD, params, null);

        assertNotNull(callHandler.dispatchToLocalService(callbackRequest));

        params = new ArrayList<Serializable>();
        List<Serializable> param = new ArrayList<>();
        param.add(new DummyProxy(objectID1));
        param.add(new DummyProxy(objectID2));
        param.add(new DummyProxy(objectID3));
        params.add((Serializable) param); // ArrayList

        callbackRequest = new ServiceCallRequest(LOCAL_LOGICAL_NODE_SESSION_ID, CommunicationTestHelper.REMOTE_LOGICAL_NODE_SESSION_ID,
            MethodCallTestInterface.class.getCanonicalName(), CALLBACK_TEST_METHOD, params, null);

        assertNotNull(callHandler.dispatchToLocalService(callbackRequest));
    }

    /**
     * Test implementation of {@link CallbackService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackService implements CallbackService {

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
        public Object getCallbackObject(String objectIdentifier) {
            if (objectIdentifier.equals(objectID2)) {
                return object;
            }
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {}

        @Override
        public Object createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, InstanceNodeSessionId proxyHome) {
            return null;
        }

        @Override
        public String getCallbackObjectIdentifier(Object callbackObject) {
            return null;
        }

    }

    /**
     * Test {@link CallbackProxyService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackProxyService implements CallbackProxyService {

        @Override
        public void addCallbackProxy(CallbackProxy callBackProxy) {

        }

        @Override
        public Object getCallbackProxy(String objectIdentifier) {
            if (objectIdentifier.equals(objectID3)) {
                return proxy;
            }
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {

        }

    }

    /**
     * Test callback object.
     * 
     * @author Doreen Seider
     */
    private class DummyProxy implements DummyInterface, CallbackProxy {

        private static final long serialVersionUID = 1L;

        private String id;

        DummyProxy(String id) {
            this.id = id;
        }

        @Override
        public void method() {}

        @Override
        public InstanceNodeSessionId getHomePlatform() {
            return CommunicationTestHelper.LOCAL_INSTANCE_SESSION_ID;
        }

        @Override
        public String getObjectIdentifier() {
            return id;
        }

        @Override
        public Class<?> getInterface() {
            return DummyInterface.class;
        }
    }

    /**
     * Test callback object.
     * 
     * @author Doreen Seider
     */
    private class DummyObject implements DummyInterface {

        private static final long serialVersionUID = 1L;

        @Override
        public void method() {}

        @Override
        public Class<?> getInterface() {
            return DummyInterface.class;
        }

    }

    /**
     * Test interface used for test callback object.
     * 
     * @author Doreen Seider
     */
    private interface DummyInterface extends CallbackObject {

        void method();
    }

    /**
     * Test {@link PlatformService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformService extends PlatformServiceDefaultStub {

        @Override
        public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
            if (nodeId.equals(LOCAL_LOGICAL_NODE_SESSION_ID)) {
                return true;
            }
            return false;
        }

    }

    /**
     * 
     * Dummy {@link ServiceCallSender} implementation.
     * 
     * @author Doreen Seider
     */
    private class ServiceCallSenderDummy implements ServiceCallSender {

        @Override
        public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws RemoteOperationException {
            try {
                return OSGiServiceCallHandlerImplTest.getCallHandler().dispatchToLocalService(serviceCallRequest);
            } catch (InternalMessagingException e) {
                return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest, "Exception in mock handler", e);
            }
        }

        @Override
        public void initialize(NetworkContact contact) throws CommunicationException {}

    }
}
