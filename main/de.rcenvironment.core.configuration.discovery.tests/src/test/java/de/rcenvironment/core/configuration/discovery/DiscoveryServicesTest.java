/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.discovery;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryBootstrapService;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryClientSetup;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryConfiguration;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryServerSetup;
import de.rcenvironment.core.configuration.discovery.bootstrap.DiscoveryClientSetup.ServerEntry;
import de.rcenvironment.core.configuration.discovery.client.DiscoveryClientService;
import de.rcenvironment.core.configuration.discovery.internal.DiscoveryBootstrapServiceImpl;
import de.rcenvironment.core.configuration.discovery.internal.DiscoveryClientServiceImpl;
import de.rcenvironment.core.configuration.discovery.internal.DiscoveryServerManagementServiceImpl;
import de.rcenvironment.core.configuration.discovery.internal.RemoteDiscoveryService;
import de.rcenvironment.core.configuration.discovery.server.DiscoveryServerManagementService;
import de.rcenvironment.core.jetty.JettyService;

/**
 * Tests for the three exported discovery services (bootstrap, client and server management).
 * 
 * @author Robert Mischke
 * 
 */
public class DiscoveryServicesTest {

    private static final String SIMULATED_SERVER_ADDRESS = "5.5.5.5";

    private static final String UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE =
        "Discovery property map contains unexpected number of elements";

    private static final int SOAP_TEST_PORT = 9999;

    private static final String EXPECTED_SOAP_SERVICE_URI = "http://" + SIMULATED_SERVER_ADDRESS + ":" + SOAP_TEST_PORT + "/Discovery";

    private DiscoveryServerManagementService serverManagementService;

    private DiscoveryClientService clientService;

    private DiscoveryBootstrapService bootstrapService;

    private JettyService jettyServiceMock;

    /**
     * DiscoveryServerManagementServiceImpl subclass that provides public constructor injection.
     * 
     * @author Robert Mischke
     * 
     */
    private final class MockInjectedDiscoveryServerManagementService extends DiscoveryServerManagementServiceImpl {

        private MockInjectedDiscoveryServerManagementService(JettyService jettyServiceMock) {
            bindJettyService(jettyServiceMock);
        }

    }

    /**
     * DiscoveryClientServiceImpl subclass that provides public constructor injection.
     * 
     * @author Robert Mischke
     * 
     */
    private final class MockInjectedDiscoveryClientService extends DiscoveryClientServiceImpl {

        private MockInjectedDiscoveryClientService(JettyService jettyServiceMock) {
            bindJettyService(jettyServiceMock);
        }

    }

    /**
     * DiscoveryBootstrapServiceImpl subclass that provides public constructor injection.
     * 
     * @author Robert Mischke
     * 
     */
    private final class MockInjectedDiscoveryBootstrapServiceImpl extends DiscoveryBootstrapServiceImpl {

        private MockInjectedDiscoveryBootstrapServiceImpl(DiscoveryServerManagementService serverService,
            DiscoveryClientService clientService) {
            bindDiscoveryServerManagementService(serverService);
            bindDiscoveryClientService(clientService);
        }

    }

    /**
     * Sets up mocked subclasses of all discovery services and stores them into fields.
     */
    @Before
    public void setupMockInjectedServices() {
        jettyServiceMock = createMock(JettyService.class);
        serverManagementService = new MockInjectedDiscoveryServerManagementService(jettyServiceMock);
        clientService = new MockInjectedDiscoveryClientService(jettyServiceMock);
        bootstrapService = new MockInjectedDiscoveryBootstrapServiceImpl(serverManagementService, clientService);
    }

    /**
     * Tests the empty default configuration.
     */
    @Test
    public void testDefaultConfiguration() {
        // test with empty configuration
        DiscoveryConfiguration configuration = new DiscoveryConfiguration();

        // expected: the jetty service should not be used at all
        replay(jettyServiceMock);

        // test
        Map<String, String> properties = bootstrapService.initializeDiscovery(configuration);
        Assert.assertNotNull(properties);
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE, 0, properties.size());
    }

    /**
     * Tests whether the server part of the configuration triggers the correct Jetty service
     * deployment.
     */
    @Test
    public void testServerSetup() {
        // setup server-only configuration
        DiscoveryConfiguration configuration = new DiscoveryConfiguration();
        DiscoveryServerSetup serverSetup = new DiscoveryServerSetup();
        serverSetup.setBindAddress(SIMULATED_SERVER_ADDRESS);
        serverSetup.setPort(SOAP_TEST_PORT);
        configuration.setRunDiscoveryServer(serverSetup);

        // expected: a RemoteDiscoveryService is deployed
        // TODO add class matching when EasyMock is upgraded
        jettyServiceMock.deployWebService(anyObject(), eq(EXPECTED_SOAP_SERVICE_URI));
        replay(jettyServiceMock);

        // test
        Map<String, String> properties = bootstrapService.initializeDiscovery(configuration);
        Assert.assertNotNull(properties);
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE, 0, properties.size());
    }

    /**
     * Tests whether the client part of the configuration triggers the creation of the correct Jetty
     * service stub, and whether the external client address "obtained" from this stub is properly
     * mapped to the returned discovery properties.
     */
    @Test
    public void testClientSetupAndClientAddressReturn() {
        // setup client-only configuration
        DiscoveryConfiguration configuration = new DiscoveryConfiguration();
        DiscoveryClientSetup clientSetup = new DiscoveryClientSetup();
        List<ServerEntry> servers = new ArrayList<ServerEntry>();
        servers.add(new ServerEntry(SIMULATED_SERVER_ADDRESS, SOAP_TEST_PORT));
        clientSetup.setServers(servers);
        configuration.setUseDiscovery(clientSetup);

        // expected: a RemoteDiscoveryService stub is requested
        // TODO add class matching when EasyMock is upgraded
        RemoteDiscoveryService clientStubMock = createMock(RemoteDiscoveryService.class);
        expect(jettyServiceMock.createWebServiceClient((Class<?>) anyObject(), eq(EXPECTED_SOAP_SERVICE_URI))).andReturn(clientStubMock);
        expect(clientStubMock.getReflectedCallerAddress()).andReturn("4.4.4.4");
        replay(jettyServiceMock, clientStubMock);

        Map<String, String> properties = bootstrapService.initializeDiscovery(configuration);
        Assert.assertNotNull(properties);
        Assert.assertEquals("Simulated client address not found in discovery property map", "\"4.4.4.4\"",
            properties.get(DiscoveryBootstrapService.QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY));
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE, 1, properties.size());
    }

    /**
     * Tests behaviour on remote call failure with no fallback properties set.
     */
    @Test
    public void testRemoteCallFailureWithoutFallbackProperties() {
        // set up normal client use of discovery
        DiscoveryConfiguration configuration = new DiscoveryConfiguration();
        DiscoveryClientSetup clientSetup = new DiscoveryClientSetup();
        List<ServerEntry> servers = new ArrayList<ServerEntry>();
        servers.add(new ServerEntry(SIMULATED_SERVER_ADDRESS, SOAP_TEST_PORT));
        clientSetup.setServers(servers);
        configuration.setUseDiscovery(clientSetup);

        // simulate normal call behaviour, but throw an exception on simulated remote call
        RemoteDiscoveryService clientStubMock = createMock(RemoteDiscoveryService.class);
        expect(jettyServiceMock.createWebServiceClient((Class<?>) anyObject(), eq(EXPECTED_SOAP_SERVICE_URI))).andReturn(clientStubMock);
        expect(clientStubMock.getReflectedCallerAddress()).andThrow(new RuntimeException("Mock Error: Server not found!"));
        replay(jettyServiceMock, clientStubMock);

        // test: the final reflected address should be undefined (empty discovery properties)
        Map<String, String> properties = bootstrapService.initializeDiscovery(configuration);
        Assert.assertNotNull(properties);
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE, 0, properties.size());
    }

    /**
     * Tests behaviour on remote call failure with fallback properties.
     */
    @Test
    public void testRemoteCallFailureWithFallbackProperties() {
        // set up normal client use of discovery
        DiscoveryConfiguration configuration = new DiscoveryConfiguration();
        DiscoveryClientSetup clientSetup = new DiscoveryClientSetup();
        List<ServerEntry> servers = new ArrayList<ServerEntry>();
        servers.add(new ServerEntry(SIMULATED_SERVER_ADDRESS, SOAP_TEST_PORT));
        clientSetup.setServers(servers);
        // set up fallback properties
        Map<String, String> fallbackProperties = new HashMap<String, String>();
        fallbackProperties.put(DiscoveryBootstrapService.QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY, "\"1.2.4.2\"");
        clientSetup.setFallbackProperties(fallbackProperties);
        configuration.setUseDiscovery(clientSetup);

        // simulate normal call behaviour, but throw an exception on simulated remote call
        RemoteDiscoveryService clientStubMock = createMock(RemoteDiscoveryService.class);
        expect(jettyServiceMock.createWebServiceClient((Class<?>) anyObject(), eq(EXPECTED_SOAP_SERVICE_URI))).andReturn(clientStubMock);
        expect(clientStubMock.getReflectedCallerAddress()).andThrow(new RuntimeException("Mock Error: Server not found!"));
        replay(jettyServiceMock, clientStubMock);

        // test: the final reflected address should be the fallback value
        Map<String, String> properties = bootstrapService.initializeDiscovery(configuration);
        Assert.assertNotNull(properties);
        Assert.assertEquals("Simulated client address does not match fallback value", "\"1.2.4.2\"",
            properties.get(DiscoveryBootstrapService.QUOTED_REFLECTED_CLIENT_ADDRESS_PROPERTY));
        Assert.assertEquals(UNEXPECTED_NUMBER_OF_PROPERTY_ELEMENTS_MESSAGE, 1, properties.size());
    }

}
