/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.management.CommunicationManagementService;
import de.rcenvironment.core.communication.model.internal.NetworkGraphImpl;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.LinkStateRoutingProtocolManager;
import de.rcenvironment.core.communication.rpc.api.RemotableCallbackService;
import de.rcenvironment.core.communication.rpc.internal.OSGiLocalServiceResolver;
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

    private final InstanceNodeSessionId instanceSessionIdLocal =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("local");

    private final InstanceNodeSessionId instanceSessionIdRemote =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("remote");

    private final InstanceNodeSessionId instanceSessionIdNotReachable =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("notReachable");

    // arbitrary test service interface; must be annotated with @RemotableService to pass the service publication tests
    private final Class<?> iface = RemotableCallbackService.class;

    private final RemotableCallbackService serviceInstance = EasyMock.createNiceMock(RemotableCallbackService.class);

    private final Map<String, String> serviceProperties = new HashMap<String, String>();

    private LiveNetworkIdResolutionServiceImpl idResolutionService;

    /**
     * Setup.
     * 
     * @throws InvalidSyntaxException on uncaught exceptions
     */
    @Before
    public void setUp() throws InvalidSyntaxException {

        serviceProperties.put("piti", "platsch");
        communicationService = new CommunicationServiceImpl();
        communicationService.bindServiceProxyFactory(new CustomServiceProxyFactoryMock());

        DummyPlatformServiceLocal platformServiceMock = new DummyPlatformServiceLocal();
        communicationService.bindPlatformService(platformServiceMock);
        communicationService.bindCommunicationManagementService(EasyMock.createMock(CommunicationManagementService.class));

        NetworkRoutingService routingServiceMock = new CustomNetworkRoutingServiceMock(platformServiceMock.getLocalInstanceNodeSessionId());
        communicationService.bindNetworkRoutingService(routingServiceMock);

        idResolutionService = new LiveNetworkIdResolutionServiceImpl();
        communicationService.bindLiveNetworkIdResolutionService(idResolutionService);

        contextMock = EasyMock.createNiceMock(BundleContext.class);
        ServiceReference<?> ifaceReferenceMock = EasyMock.createNiceMock(ServiceReference.class);
        contextMock.getServiceReference(iface.getName());
        EasyMock.expectLastCall().andReturn(ifaceReferenceMock).anyTimes();

        // for OSGiLocalServiceResolver compatibility, which uses this call pattern
        EasyMock.expect(contextMock.getServiceReferences(iface.getName(), null)).andReturn(new ServiceReference[] { ifaceReferenceMock })
            .anyTimes();

        ServiceReference<?>[] referencesDummy = new ServiceReference[2];
        referencesDummy[0] = ifaceReferenceMock;
        EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
            ServiceUtils.constructFilter(serviceProperties))).andReturn(referencesDummy).anyTimes();

        contextMock.getService(ifaceReferenceMock);
        EasyMock.expectLastCall().andReturn(serviceInstance).anyTimes();
        ServiceReference<?> platformReferenceMock = EasyMock.createNiceMock(ServiceReference.class);
        contextMock.getServiceReference(PlatformService.class.getName());
        EasyMock.expectLastCall().andReturn(platformReferenceMock).anyTimes();
        contextMock.getService(platformReferenceMock);
        EasyMock.expectLastCall().andReturn(new DummyPlatformServiceLocal()).anyTimes();

        EasyMock.replay(contextMock, serviceInstance);

        OSGiLocalServiceResolver localServiceResolverAdapter = new OSGiLocalServiceResolver(2); // reduce retry count to 2
        localServiceResolverAdapter.activate(contextMock);

        communicationService.bindLocalServiceResolver(localServiceResolverAdapter);

        LiveNetworkIdResolutionServiceImpl liveNetworkIdResolver = new LiveNetworkIdResolutionServiceImpl();
        communicationService.bindLiveNetworkIdResolutionService(liveNetworkIdResolver);

        communicationService.activate();

        // register the ids "observed" in the network; this must be done after activate() as the local id is reserved there
        liveNetworkIdResolver.registerInstanceNodeSessionId(instanceSessionIdLocal);
        liveNetworkIdResolver.registerInstanceNodeSessionId(instanceSessionIdRemote);
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

        Object service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
        // TODO disabled for now, as local services are proxied now as well, but the proxy is created internally - misc_ro
        // assertEquals(serviceInstance, service);

        // register the remote session id as known/seen on the routing level
        idResolutionService.registerInstanceNodeSessionId(instanceSessionIdRemote);

        service = communicationService.getRemotableService(iface, instanceSessionIdRemote);
        assertEquals(serviceInstance, service);

        try {
            communicationService.getRemotableService(iface, (ResolvableNodeId) null);
            fail("Exception expected on null node id");
        } catch (RuntimeException e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // TODO review: does it make sense to run this twice?

        service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
        // see above
        // assertEquals(serviceInstance, service);

        service = communicationService.getRemotableService(iface, instanceSessionIdRemote);
        assertEquals(serviceInstance, service);

        try {
            communicationService.getRemotableService(iface, (ResolvableNodeId) null);
            fail("Exception expected on null node id");
        } catch (RuntimeException e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

    }

    /**
     * Test.
     * 
     * @throws IdentifierException not expected
     */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceRefArrayIsEmpty() throws IdentifierException {

        EasyMock.reset(contextMock);
        ServiceReference<?>[] referencesDummy = new ServiceReference[0];
        try {
            EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
                ServiceUtils.constructFilter(serviceProperties))).andReturn(referencesDummy).anyTimes();
        } catch (InvalidSyntaxException e) {
            fail();
        }
        EasyMock.replay(contextMock);

        Object service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
        assertEquals(serviceInstance, service);

    }

    /**
     * Test.
     * 
     * @throws IdentifierException not expected
     */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfNoServiceAvailable() throws IdentifierException {

        EasyMock.reset(contextMock);
        try {
            EasyMock.expect(contextMock.getServiceReferences(iface.getName(),
                ServiceUtils.constructFilter(serviceProperties))).andReturn(null).anyTimes();
        } catch (InvalidSyntaxException e) {
            fail();
        }
        EasyMock.replay(contextMock);

        Object service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
        assertEquals(serviceInstance, service);

    }

    /**
     * Test.
     * 
     * @throws IdentifierException not expected
     */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceRefIsNull() throws IdentifierException {

        EasyMock.reset(contextMock);
        EasyMock.expect(contextMock.getServiceReference(iface.getName())).andReturn(null).anyTimes();
        EasyMock.replay(contextMock);

        Object service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
        assertEquals(serviceInstance, service);

    }

    /**
     * Test.
     * 
     * @throws IdentifierException not expected
     */
    @Test(expected = IllegalStateException.class)
    public void testGetServiceIfServiceIsNull() throws IdentifierException {

        EasyMock.reset(contextMock);
        ServiceReference<?> referenceMock = EasyMock.createNiceMock(ServiceReference.class);
        contextMock.getServiceReference(iface.getName());
        EasyMock.expectLastCall().andReturn(referenceMock).anyTimes();
        EasyMock.replay(contextMock);

        Object service = communicationService.getRemotableService(iface, instanceSessionIdLocal);
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
        public Object createServiceProxy(ResolvableNodeId rawNodeId, Class<?> serviceIface, Class<?>[] ifaces,
            ReliableRPCStreamHandle reliableRPCStreamHandle) {
            Object service = null;
            LogicalNodeSessionId nodeId;
            try {
                nodeId = idResolutionService.resolveToLogicalNodeSessionId(rawNodeId);
            } catch (IdentifierException e) {
                throw NodeIdentifierUtils.wrapIdentifierException(e); // should not happen in test environment
            }
            if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdLocal) && serviceIface == PlatformService.class) {
                service = new DummyPlatformServiceLocal();
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdRemote) && serviceIface == PlatformService.class) {
                service = new DummyPlatformServiceRemote();
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdNotReachable) && serviceIface == PlatformService.class) {
                service = createNullService(PlatformService.class);
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdLocal) && serviceIface == CommunicationService.class) {
                service = new DummyCommunicationService();
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdRemote) && serviceIface == CommunicationService.class) {
                service = new DummyBrokenCommunicationService();
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdNotReachable) && serviceIface == CommunicationService.class) {
                service = createNullService(CommunicationService.class);
            } else if (nodeId.isSameInstanceNodeSessionAs(instanceSessionIdRemote) && serviceIface == iface) {
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

        CustomNetworkRoutingServiceMock(InstanceNodeSessionId ownNodeId) {
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
        public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
            return instanceSessionIdLocal;
        }

        @Override
        public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
            if (nodeId == instanceSessionIdLocal) {
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
        public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
            return instanceSessionIdRemote;
        }

        @Override
        public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
            if (nodeId == instanceSessionIdRemote) {
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
        public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
            return instanceSessionIdRemote;
        }

        @Override
        public boolean matchesLocalInstance(ResolvableNodeId nodeId) {
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
