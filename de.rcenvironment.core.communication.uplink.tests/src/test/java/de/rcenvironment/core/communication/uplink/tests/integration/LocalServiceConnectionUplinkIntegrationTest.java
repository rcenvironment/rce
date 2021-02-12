/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.tests.integration;

import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.client.session.impl.LocalServiceUplinkConnectionImpl;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * A subclass implementation of {@link AbstractUplinkIntegrationTest} using {@link LocalServiceUplinkConnectionImpl}s.
 *
 * @author Robert Mischke
 */
public class LocalServiceConnectionUplinkIntegrationTest extends AbstractUplinkIntegrationTest {

    @Override
    protected LocalServiceUplinkConnectionImpl setUpClientConnection() {

        // the mock UplinkConnection to the service
        LocalServiceUplinkConnectionImpl uplinkConnection =
            new LocalServiceUplinkConnectionImpl(ConcurrencyUtils.getAsyncTaskService(), getMockServerSideUplinkSessionService());
        return uplinkConnection;
    }

    @Override
    protected void simulateClientSideEOF(UplinkConnection connection) {
        ((LocalServiceUplinkConnectionImpl) connection).simulateClientSideEOF();
    }

}
