/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.LOCAL_PLATFORM;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REMOTE_CONTACT;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REMOTE_PLATFORM;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.REQUEST;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RESULT;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.RETURN_VALUE;
import static de.rcenvironment.core.communication.testutils.CommunicationTestHelper.SERVICE_CONTACT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.legacy.internal.NetworkContact;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;

/**
 * Unit test for the {@link OSGiServiceCallHandlerImpl}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
public class OSGiServiceCallHandlerImplTest extends TestCase {

    private static final String CALLBACK_TEST_METHOD = "callbackTest";

    private static OSGiServiceCallHandlerImpl callHandler;

    private final String objectID1 = "id1";

    private final String objectID2 = "id2";

    private final String objectID3 = "id3";

    private final CallbackProxy proxy = new DummyProxy(objectID1);

    private final CallbackObject object = new DummyObject();

    public static OSGiServiceCallHandlerImpl getCallHandler() {
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

        ServiceReference factoryRefMock = EasyMock.createNiceMock(ServiceReference.class);

        ServiceCallSenderFactory factoryMock = EasyMock.createNiceMock(ServiceCallSenderFactory.class);
        EasyMock.expect(factoryMock.createServiceCallSender(SERVICE_CONTACT))
            .andReturn(senderMock).anyTimes();
        EasyMock.expect(factoryMock.createServiceCallSender(REMOTE_CONTACT))
            .andReturn(new ServiceCallSenderDummy()).anyTimes();
        EasyMock.replay(factoryMock);

        ServiceReference testServiceRef = EasyMock.createNiceMock(ServiceReference.class);

        MethodCallerTestMethods testmethods = new MethodCallerTestMethodsImpl();

        BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);

        EasyMock.expect(contextMock.getBundles()).andReturn(new Bundle[] { bundleMock }).anyTimes();

        EasyMock.expect(contextMock.getAllServiceReferences(EasyMock.eq(ServiceCallSenderFactory.class.getName()),
            EasyMock.eq("(" + ServiceCallSenderFactory.PROTOCOL + "=de.rcenvironment.rce.communication.rmi)")))
            .andReturn(new ServiceReference[] { factoryRefMock }).anyTimes();
        EasyMock.expect(contextMock.getAllServiceReferences(REQUEST.getService(), REQUEST.getServiceProperties()))
            .andReturn(new ServiceReference[] { testServiceRef }).anyTimes();

        EasyMock.expect(contextMock.getService(factoryRefMock)).andReturn(factoryMock).anyTimes();
        EasyMock.expect(contextMock.getService(testServiceRef)).andReturn(testmethods).anyTimes();
        EasyMock.replay(contextMock);

        callHandler = new OSGiServiceCallHandlerImpl();
        callHandler.bindPlatformService(new DummyPlatformService());
        callHandler.bindCallbackService(new DummyCallbackService());
        callHandler.bindCallbackProxyService(new DummyCallbackProxyService());
        callHandler.activate(contextMock);
    }

    /**
     * Test normal call.
     * 
     * @throws Exception if the test fails.
     */
    public void testLocalCall() throws Exception {

        ServiceCallResult result = callHandler.handle(REQUEST);
        assertEquals(result.getReturnValue(), RETURN_VALUE);

        // equal call request to cover cache functionality
        result = callHandler.handle(REQUEST);
        assertEquals(result.getReturnValue(), RETURN_VALUE);

        List<Serializable> params = new ArrayList<>();
        params.add(new DummyProxy(objectID1));

        ServiceCallRequest callbackRequest = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM,
            MethodCallerTestMethods.class.getCanonicalName(), null, CALLBACK_TEST_METHOD, params);

        assertNotNull(callHandler.handle(callbackRequest));

        params = new ArrayList<Serializable>();
        params.add(new DummyProxy(objectID2));

        callbackRequest = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM,
            MethodCallerTestMethods.class.getCanonicalName(), null, CALLBACK_TEST_METHOD, params);

        assertNotNull(callHandler.handle(callbackRequest));

        params = new ArrayList<Serializable>();
        params.add(new DummyProxy(objectID3));

        callbackRequest = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM,
            MethodCallerTestMethods.class.getCanonicalName(), null, CALLBACK_TEST_METHOD, params);

        assertNotNull(callHandler.handle(callbackRequest));

        params = new ArrayList<Serializable>();
        List<Serializable> param = new ArrayList<>();
        param.add(new DummyProxy(objectID1));
        param.add(new DummyProxy(objectID2));
        param.add(new DummyProxy(objectID3));
        params.add((Serializable) param); // ArrayList

        callbackRequest = new ServiceCallRequest(LOCAL_PLATFORM, REMOTE_PLATFORM,
            MethodCallerTestMethods.class.getCanonicalName(), null, CALLBACK_TEST_METHOD, params);

        assertNotNull(callHandler.handle(callbackRequest));
    }

    /**
     * Test implementation of {@link CallbackService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackService implements CallbackService {

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
        public Object getCallbackObject(String objectIdentifier) {
            if (objectIdentifier.equals(objectID2)) {
                return object;
            }
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {}

        @Override
        public Object createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, NodeIdentifier proxyHome) {
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

        public DummyProxy(String id) {
            this.id = id;
        }

        @Override
        public void method() {}

        @Override
        public NodeIdentifier getHomePlatform() {
            return LOCAL_PLATFORM;
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
        public boolean isLocalNode(NodeIdentifier nodeId) {
            if (nodeId.equals(LOCAL_PLATFORM)) {
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
        public ServiceCallResult send(ServiceCallRequest serviceCallRequest) throws CommunicationException {
            return OSGiServiceCallHandlerImplTest.getCallHandler().handle(serviceCallRequest);
        }

        @Override
        public void initialize(NetworkContact contact) throws CommunicationException {}

    }
}
