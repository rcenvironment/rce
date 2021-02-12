/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.tests.integration;

import org.junit.After;
import org.junit.Before;

import de.rcenvironment.core.communication.uplink.client.session.impl.LocalServiceUplinkConnectionImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkEndpointServiceImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionServiceImpl;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * An Uplink test using a "virtual" {@link LocalServiceUplinkConnectionImpl} to test the common code as generically as possible.
 * 
 * @author Robert Mischke
 */
public class LocalServiceUplinkConnectionImplTest extends AbstractUplinkConnectionTest {

    /**
     * Common setup.
     * 
     * @throws Exception on unexpected failure
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();

        // TODO refactor; move into base class
        ServerSideUplinkSessionServiceImpl mockServerSideUplinkSessionService = new ServerSideUplinkSessionServiceImpl();
        mockServerSideUplinkSessionService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());

        final ServerSideUplinkEndpointServiceImpl mockServerSideUplinkEndpointService = new ServerSideUplinkEndpointServiceImpl();
        mockServerSideUplinkEndpointService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());
        mockServerSideUplinkSessionService.bindServerSideUplinkEndpointService(mockServerSideUplinkEndpointService);

        // wrap into UplinkConnection
        uplinkConnection =
            new LocalServiceUplinkConnectionImpl(ConcurrencyUtils.getAsyncTaskService(), mockServerSideUplinkSessionService);
    }

    /**
     * Common tear-down.
     * 
     * @throws Exception on unexpected failure
     */
    @After
    public void tearDown() {
        uplinkConnection.close();
    }

}
