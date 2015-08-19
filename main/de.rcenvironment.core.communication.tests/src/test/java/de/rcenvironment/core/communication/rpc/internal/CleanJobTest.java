/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.internal.CleanJob.CleanRunnable;
import de.rcenvironment.core.communication.spi.CallbackObject;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;

/**
 * Test cases for {@link CleanJob}.
 * 
 * @author Doreen Seider
 */
public class CleanJobTest {

    private CleanJob job;

    private final BundleContext contextMock = EasyMock.createNiceMock(BundleContext.class);

    private final NodeIdentifier pi1 = NodeIdentifierFactory.fromHostAndNumberString("localhost:1");

    private final NodeIdentifier pi2 = NodeIdentifierFactory.fromHostAndNumberString("localhost:2");

    private final String id1 = "id1";

    private final String id2 = "id2";

    private Map<String, WeakReference<Object>> objects = new HashMap<String, WeakReference<Object>>();

    private Map<String, Long> ttls = new HashMap<String, Long>();

    private Map<String, NodeIdentifier> nodes = new HashMap<String, NodeIdentifier>();

    /** Set up. */
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        job = new CleanJob();
        job.activate(contextMock);
        job.bindCommunicationService(new DummyCommunicatioService());
    }

    /** Tests if scheduling and unscheduling clean job work without exceptions. */
    @Test
    public void testUnSchedule() {
        CleanJob.scheduleJob(CallbackService.class, objects, ttls, nodes);
        CleanJob.unscheduleJob(CallbackService.class);
    }

    /**
     * Test.
     **/
    @Test
    public void testExecuteForCallbackService() {
        testExecute(CallbackService.class);
    }

    /**
     * Test.
     **/
    @Test
    public void testExecuteForCallbackProxyService() {
        testExecute(CallbackProxyService.class);
    }

    private void testExecute(Class<?> iface) {

        CleanRunnable runnable = new CleanRunnable(iface, objects, ttls, nodes);
        runnable.run();

        Object o = new Object();
        objects.put(id1, new WeakReference<Object>(o));
        nodes.put(id1, pi1);
        ttls.put(id1, new Date(System.currentTimeMillis() + CleanJob.TTL_MSEC).getTime());

        assertEquals(1, objects.size());
        assertEquals(1, nodes.size());
        assertEquals(1, ttls.size());
        
        runnable = new CleanRunnable(CallbackService.class, objects, ttls, nodes);
        runnable.run();

        assertEquals(1, objects.size());
        assertEquals(1, nodes.size());
        assertEquals(1, ttls.size());
        
        nodes.put(id1, pi2);

        o = null;
        System.gc();

        runnable = new CleanRunnable(CallbackService.class, objects, ttls, nodes);
        runnable.run();

        assertEquals(0, objects.size());
        assertEquals(0, nodes.size());
        assertEquals(0, ttls.size());
        
        o = new Object();
        objects.put(id2, new WeakReference<Object>(o));
        nodes.put(id2, pi2);
        ttls.put(id2, new Date(System.currentTimeMillis() - CleanJob.TTL_MSEC).getTime());

        assertEquals(1, objects.size());
        assertEquals(1, nodes.size());
        assertEquals(1, ttls.size());
        
        runnable = new CleanRunnable(CallbackService.class, objects, ttls, nodes);
        runnable.run();
        
        assertEquals(0, objects.size());
        assertEquals(0, nodes.size());
        assertEquals(0, ttls.size());
    }
    
    /**
     * Test {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicatioService extends CommunicationServiceDefaultStub {

        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            Object service = null;
            if (iface == CallbackService.class && nodeId.equals(pi1) && bundleContext == contextMock) {
                service = new DummyCallbackService1();
            } else if (iface == CallbackService.class && nodeId.equals(pi2) && bundleContext == contextMock) {
                service = new DummyCallbackService2();
            } else if (iface == CallbackProxyService.class && nodeId.equals(pi1) && bundleContext == contextMock) {
                service = new DummyCallbackProxyService1();
            } else if (iface == CallbackProxyService.class && nodeId.equals(pi2) && bundleContext == contextMock) {
                service = new DummyCallbackProxyService2();
            }
            return service;
        }

    }

    /**
     * Test {@link CallbackService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackService1 implements CallbackService {

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
     * Test {@link CallbackService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackService2 implements CallbackService {

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
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {
            throw new RuntimeException("fail");

        }

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
    private class DummyCallbackProxyService1 implements CallbackProxyService {

        @Override
        public void addCallbackProxy(CallbackProxy callBackProxy) {}

        @Override
        public Object getCallbackProxy(String objectIdentifier) {
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {}

    }

    /**
     * Test {@link CallbackProxyService} implementation.
     * 
     * @author Doreen Seider
     */
    private class DummyCallbackProxyService2 implements CallbackProxyService {

        @Override
        public void addCallbackProxy(CallbackProxy callBackProxy) {}

        @Override
        public Object getCallbackProxy(String objectIdentifier) {
            return null;
        }

        @Override
        public void setTTL(String objectIdentifier, Long ttl) {
            throw new RuntimeException("fail");
        }

    }
}
