/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link CallbackServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CallbackServiceImplTest {

    private CallbackServiceImpl service;

    private DummyObject callbackObject = new DummyObject();

    private final NodeIdentifier piLocal = NodeIdentifierFactory.fromHostAndNumberString("localhost:1");

    private final NodeIdentifier piRemote = NodeIdentifierFactory.fromHostAndNumberString("remotehost:1");

    /** Set up. */
    @Before
    public void setUp() {
        service = new CallbackServiceImpl();
    }

    /**
     * Test.
     * 
     * @throws RemoteOperationException if an error occurs.
     **/
    @Test
    public void testRemainingMethods() throws RemoteOperationException {
        String id = service.addCallbackObject(callbackObject, piRemote);
        assertNotNull(id);
        assertEquals(id, service.addCallbackObject(callbackObject, piRemote));
        assertTrue(id != service.addCallbackObject(new String(), piRemote));

        assertEquals(id, service.getCallbackObjectIdentifier(callbackObject));
        assertNull(service.getCallbackObjectIdentifier(new Object()));

        assertEquals(callbackObject, service.getCallbackObject(id));
        assertNull(service.getCallbackObject("unknown"));

        service.setTTL(id, new Long(5));
        service.setTTL("unknown2", new Long(2));

        assertEquals("some callback method called", service.callback(id, "someCallbackMethod", new ArrayList<Serializable>()));

        try {
            service.callback(id, "unknownMethod", new ArrayList<Serializable>());
            fail(StringUtils.format("Method 'unknownMethod' is not defined for  %s", DummyObject.class.getSimpleName()));
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }

        try {
            service.callback(id, "someMethod", new ArrayList<Serializable>());
            fail(StringUtils
                .format("Method 'someMethod' of %s is not allowed to be called from remote.", DummyObject.class.getSimpleName()));
        } catch (RemoteOperationException e) {
            assertTrue(true);
        }

        callbackObject = null;
        System.gc();
        assertNull(service.getCallbackObject(id));

        service.activate(EasyMock.createNiceMock(BundleContext.class));
        service.deactivate(EasyMock.createNiceMock(BundleContext.class));
    }

    /**
     * Test.
     * 
     * @throws RemoteOperationException (expected)
     **/
    @Test(expected = RemoteOperationException.class)
    public void testForFailure() throws RemoteOperationException {
        service.callback("id", "toString", new ArrayList<Serializable>());
    }

    /** Test. */
    @Test
    public void testCreateProxy() {
        service.bindPlatformService(new DummyPlatformService());

        String id = UUID.randomUUID().toString();
        Object proxy = service.createCallbackProxy(new DummyObject(), id, piRemote);

        assertTrue(proxy != null);
        assertTrue(proxy instanceof DummyInterface);
        assertTrue(proxy instanceof CallbackProxy);
        assertEquals(piLocal, ((CallbackProxy) proxy).getHomePlatform());
        assertEquals(id, ((CallbackProxy) proxy).getObjectIdentifier());
        assertEquals("some method called", ((DummyInterface) proxy).someMethod());
    }

    /** Test. */
    @Test
    public void testCreateProxyForClassWithCustomSuperclass() {
        service.bindPlatformService(new DummyPlatformService());

        String id = UUID.randomUUID().toString();
        Object proxy = service.createCallbackProxy(new DummyLevel2Object(), id, piRemote);

        assertTrue(proxy != null);
        assertTrue(proxy instanceof DummyInterface);
        assertTrue(proxy instanceof CallbackProxy);
        assertEquals(piLocal, ((CallbackProxy) proxy).getHomePlatform());
        assertEquals(id, ((CallbackProxy) proxy).getObjectIdentifier());
        assertEquals("some method called", ((DummyInterface) proxy).someMethod());
    }

    /**
     * Test implementation of the {@link PlatformService}.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformService extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return piLocal;
        }
    }

}
