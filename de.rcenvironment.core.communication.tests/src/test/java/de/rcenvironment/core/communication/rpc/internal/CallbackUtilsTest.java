/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link CallbackUtils}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class CallbackUtilsTest {

    private final InstanceNodeSessionId instanceIdLocal = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("local");

    private final InstanceNodeSessionId instanceIdRemote = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("remote");

    private final DummyCallbackService callbackService = new DummyCallbackService();

    private final DummyCallbackProxyService callbackProxyService = new DummyCallbackProxyService();

    private final String id1 = "id1";

    private final String id2 = "id2";

    private final String id3 = "id3";

    private final DummyObject object1 = new DummyObject();

    private final DummyObject object2 = new DummyObject();

    private final Object proxy11 = new DummyProxy(id1);

    private final Object proxy12 = new DummyProxy(id1);

    private final Object proxy2 = new DummyProxy(id2);

    private final Object proxy3 = new DummyProxy(id3);

    /** Test. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testHandlingCallbackObjects() {
        Object o = new Object();
        assertEquals(o, CallbackUtils.handleCallbackObject(o, instanceIdRemote, callbackService));

        assertEquals(proxy11, CallbackUtils.handleCallbackObject(object1, instanceIdRemote, callbackService));
        assertEquals(proxy2, CallbackUtils.handleCallbackObject(object2, instanceIdRemote, callbackService));

        List objects = new ArrayList<Serializable>();
        List object = new ArrayList<Serializable>();
        object.add(o);
        object.add(object1);
        object.add(object2);
        objects.add(object);

        List newObjects = (List) CallbackUtils.handleCallbackObject(objects, instanceIdRemote, callbackService);

        assertEquals(1, newObjects.size());
        List newObject = (List) newObjects.get(0);
        assertEquals(3, newObject.size());
        assertTrue(newObject.contains(o));
        assertTrue(newObject.contains(proxy11));
        assertTrue(newObject.contains(proxy2));
    }

    /** Test. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testHandlingCallbackProxyObjects() {
        Object o = new Object();
        assertEquals(o, CallbackUtils.handleCallbackProxy(o, callbackService, callbackProxyService));

        assertEquals(proxy12, CallbackUtils.handleCallbackProxy(proxy11, callbackService, callbackProxyService));
        assertEquals(object2, CallbackUtils.handleCallbackProxy(proxy2, callbackService, callbackProxyService));
        assertEquals(proxy3, CallbackUtils.handleCallbackProxy(proxy3, callbackService, callbackProxyService));

        List proxies = new ArrayList<Serializable>();
        List proxy = new ArrayList<Serializable>();
        proxy.add(o);
        proxy.add(proxy11);
        proxy.add(proxy2);
        proxy.add(proxy3);
        proxies.add(proxy);

        List newProxies = (List) CallbackUtils.handleCallbackProxy(proxies, callbackService, callbackProxyService);

        assertEquals(1, newProxies.size());
        List newProxy = (List) newProxies.get(0);
        assertEquals(4, newProxy.size());
        assertTrue(newProxy.contains(o));
        assertTrue(newProxy.contains(object2));
        assertTrue(newProxy.contains(proxy12));
        assertTrue(newProxy.contains(proxy3));
    }

    /**
     * Test {@link CallbackService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackService implements CallbackService {

        @Override
        public String addCallbackObject(Object callBackObject, InstanceNodeSessionId nodeId) {
            if (callBackObject == object1 && nodeId.equals(instanceIdRemote)) {
                return id1;
            }
            return null;
        }

        @Override
        public Object callback(String objectIdentifier, String methodName, List<? extends Serializable> parameters)
            throws RemoteOperationException {
            return null;
        }

        @Override
        public Object createCallbackProxy(CallbackObject callbackObject, String objectIdentifier, InstanceNodeSessionId proxyHome) {
            if (callbackObject == object1 && objectIdentifier == id1 && proxyHome.equals(instanceIdRemote)) {
                return proxy11;
            } else if (callbackObject == object2 && objectIdentifier == id2 && proxyHome.equals(instanceIdRemote)) {
                return proxy2;
            }
            return null;
        }

        @Override
        public Object getCallbackObject(String objectIdentifier) {
            if (objectIdentifier == id2) {
                return object2;
            }
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {

        }

        @Override
        public String getCallbackObjectIdentifier(Object callbackObject) {
            if (callbackObject == object2) {
                return id2;
            }
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
            if (objectIdentifier == id1) {
                return proxy12;
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
    private class DummyObject implements DummyInterface {

        private static final long serialVersionUID = 1L;

        @Override
        public String method() {
            return "method called";
        }

        @CallbackMethod
        @Override
        public void callbackMethod() {
            throw new RuntimeException("callbackMethod called");
        }

        @Override
        public Class<? extends Serializable> getInterface() {
            return DummyInterface.class;
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
        public String method() {
            return "method called";
        }

        @CallbackMethod
        @Override
        public void callbackMethod() {
            throw new RuntimeException("callbackMethod called");
        }

        @Override
        public InstanceNodeSessionId getHomePlatform() {
            return instanceIdLocal;
        }

        @Override
        public String getObjectIdentifier() {
            return id;
        }

        @Override
        public Class<? extends Serializable> getInterface() {
            return DummyInterface.class;
        }
    }

    /**
     * Test interface used for test callback object.
     * 
     * @author Doreen Seider
     */
    private interface DummyInterface extends CallbackObject {

        String method();

        void callbackMethod();
    }
}
