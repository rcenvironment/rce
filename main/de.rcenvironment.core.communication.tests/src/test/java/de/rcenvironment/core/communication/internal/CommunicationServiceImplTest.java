/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.model.internal.NetworkGraphImpl;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.LinkStateRoutingProtocolManager;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.utils.common.ServiceUtils;

/**
 * Test cases for the {@link CommunicationServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adaptations)
 */
public class CommunicationServiceImplTest {

    private static final int SAFETY_NET_TEST_TIMEOUT = 10000;

    private CommunicationServiceImpl communicationService;

    private BundleContext contextMock;

    private final NodeIdentifier pi1 = NodeIdentifierFactory.fromHostAndNumberString("localhost:0");

    private final NodeIdentifier pi2 = NodeIdentifierFactory.fromHostAndNumberString("remoteHost:0");

    private final NodeIdentifier pi3 = NodeIdentifierFactory.fromHostAndNumberString("notReachable:0");

    private final Object serviceInstance = new Object();

    private final Class<?> iface = Serializable.class;

    private final Map<String, String> serviceProperties = new HashMap<String, String>();

    /** Setup. */
    @Before
    public void setUp() {
        serviceProperties.put("piti", "platsch");
        communicationService = new CommunicationServiceImpl();
        communicationService.bindServiceProxyFactory(new CustomServiceProxyFactoryMock());

        DummyPlatformServiceLocal platformServiceMock = new DummyPlatformServiceLocal();
        communicationService.bindPlatformService(platformServiceMock);
        communicationService.bindCommunicationManagementService(EasyMock.createMock(CommunicationManagementService.class));

        NetworkRoutingService routingServiceMock = new CustomNetworkRoutingServiceMock(platformServiceMock.getLocalNodeId());
        communicationService.bindNetworkRoutingService(routingServiceMock);

        contextMock = EasyMock.createNiceMock(BundleContext.class);
        ServiceReference ifaceReferenceMock = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(contextMock.getServiceReference(iface.getName())).andReturn(ifaceReferenceMock).anyTimes();
        ServiceReference[] referencesDummy = new ServiceReference[2];
        referencesDummy[0] = ifaceReferenceMock;
        try {
            EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
                ServiceUtils.constructFilter(serviceProperties))).andReturn(referencesDummy).anyTimes();
        } catch (InvalidSyntaxException e) {
            fail();
        }
        EasyMock.expect(contextMock.getService(ifaceReferenceMock)).andReturn(serviceInstance).anyTimes();
        ServiceReference platformReferenceMock = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(contextMock.getServiceReference(PlatformService.class.getName())).andReturn(platformReferenceMock).anyTimes();
        EasyMock.expect(contextMock.getService(platformReferenceMock)).andReturn(new DummyPlatformServiceLocal()).anyTimes();

        EasyMock.replay(contextMock);

        communicationService.activate();
    }

    /** Test. */
    @Test(timeout = SAFETY_NET_TEST_TIMEOUT)
    public void testGetPlatforms() {
        // TODO >=3.0.0: add new test for communicationService.getAvailableNodes()? - misc_ro
    }

    /**
     * Test.
     * 
     * @throws Exception if an error occur.
     **/
    @Test
    public void testGetService() throws Exception {

        Object service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

        service = communicationService.getService(iface, pi2, contextMock);
        assertEquals(serviceInstance, service);

        service = communicationService.getService(iface, null, contextMock);
        assertEquals(serviceInstance, service);

        service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

        service = communicationService.getService(iface, pi2, contextMock);
        assertEquals(serviceInstance, service);

        service = communicationService.getService(iface, null, contextMock);
        assertEquals(serviceInstance, service);

    }

    /** Test. */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceRefArrayIsEmpty() {

        EasyMock.reset(contextMock);
        ServiceReference[] referencesDummy = new ServiceReference[0];
        try {
            EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
                ServiceUtils.constructFilter(serviceProperties))).andReturn(referencesDummy).anyTimes();
        } catch (InvalidSyntaxException e) {
            fail();
        }
        EasyMock.replay(contextMock);

        Object service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

    }

    /** Test. */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfNoServiceAvailable() {

        EasyMock.reset(contextMock);
        try {
            EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
                ServiceUtils.constructFilter(serviceProperties))).andReturn(null).anyTimes();
        } catch (InvalidSyntaxException e) {
            fail();
        }
        EasyMock.replay(contextMock);

        Object service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

    }

    /** Test. */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceRefIsNull() {

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getServiceReference(iface.getName())).andReturn(null).anyTimes();
        EasyMock.replay(contextMock);

        Object service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

    }

    /** Test. */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceIsNull() {

        EasyMock.reset(contextMock);
        ServiceReference referenceMock = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(contextMock.getServiceReference(iface.getName())).andReturn(referenceMock).anyTimes();
        EasyMock.replay(contextMock);

        Object service = communicationService.getService(iface, pi1, contextMock);
        assertEquals(serviceInstance, service);

    }

    /**
     * Dummy Remote Service Handler.
     * 
     * @author Doreen Seider
     */
    @SuppressWarnings("serial")
    private class CustomServiceProxyFactoryMock implements ServiceProxyFactory {

        @SuppressWarnings("unchecked")
        private <T> T createNullService(final Class<T> clazz) {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
                        throw new UndeclaredThrowableException(new RuntimeException("Service not available"));
                    }
                });
        }

        @Override
        public Object createServiceProxy(NodeIdentifier nodeId, Class<?> serviceIface, Class<?>[] ifaces) {
            Object service = null;
            if (nodeId.equals(pi1) && serviceIface == PlatformService.class) {
                service = new DummyPlatformServiceLocal();
            } else if (nodeId.equals(pi2) && serviceIface == PlatformService.class) {
                service = new DummyPlatformServiceRemote();
            } else if (nodeId.equals(pi3) && serviceIface == PlatformService.class) {
                service = createNullService(PlatformService.class);
            } else if (nodeId.equals(pi1) && serviceIface == CommunicationService.class) {
                service = new DummyCommunicationService();
            } else if (nodeId.equals(pi2) && serviceIface == CommunicationService.class) {
                service = new DummyBrokenCommunicationService();
            } else if (nodeId.equals(pi3) && serviceIface == CommunicationService.class) {
                service = createNullService(CommunicationService.class);
            } else if (nodeId.equals(pi2) && serviceIface == iface) {
                service = serviceInstance;
            }
            return service;
        }

    }

    /**
     * Custom mock implementation of {@link NetworkRoutingService}.
     * 
     * TODO add default stub and subclass it
     * 
     * @author Robert Mischke
     */
    private class CustomNetworkRoutingServiceMock implements NetworkRoutingService {

        private NetworkGraphImpl networkGraph;

        public CustomNetworkRoutingServiceMock(NodeIdentifier ownNodeId) {
            networkGraph = new NetworkGraphImpl(ownNodeId);
        }

        @Override
        public NetworkGraph getRawNetworkGraph() {
            return networkGraph;
        }

        @Override
        public NetworkGraph getReachableNetworkGraph() {
            return networkGraph;
        }

        @Override
        public String getFormattedNetworkInformation(String type) {
            return null;
        }

        @Override
        public LinkStateRoutingProtocolManager getProtocolManager() {
            return null;
        }
    }

    /**
     * Dummy local platform service.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformServiceLocal extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return pi1;
        }

        @Override
        public boolean isLocalNode(NodeIdentifier nodeId) {
            if (nodeId == pi1) {
                return true;
            }
            return false;
        }

    }

    /**
     * Dummy local platform service.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformServiceLocal2 extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return pi2;
        }

        @Override
        public boolean isLocalNode(NodeIdentifier nodeId) {
            if (nodeId == pi2) {
                return true;
            }
            return false;
        }

    }

    /**
     * Dummy remote platform service.
     * 
     * @author Doreen Seider
     */
    private class DummyPlatformServiceRemote extends PlatformServiceDefaultStub {

        @Override
        public NodeIdentifier getLocalNodeId() {
            return pi2;
        }

        @Override
        public boolean isLocalNode(NodeIdentifier nodeId) {
            return false;
        }

    }

    /**
     * Dummy implementation of {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyCommunicationService extends CommunicationServiceDefaultStub {

    }

    /**
     * Dummy implementation of {@link CommunicationService}.
     * 
     * @author Doreen Seider
     */
    private class DummyBrokenCommunicationService extends CommunicationServiceDefaultStub {

    }
}
