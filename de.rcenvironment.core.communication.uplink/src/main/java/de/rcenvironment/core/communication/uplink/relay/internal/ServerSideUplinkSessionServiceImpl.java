/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Default {@link ServerSideUplinkEndpointService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class ServerSideUplinkSessionServiceImpl implements ServerSideUplinkSessionService {

    private ServerSideUplinkEndpointService serverSideUplinkEndpointService;

    private ConcurrencyUtilsFactory concurrencyUtilsFactory;

    
    @Override
    public ServerSideUplinkSession createServerSideSession(StreamConnectionEndpoint connectionEndpoint, String loginAccountName,
        String clientInformationString) {
        return new ServerSideUplinkSessionImpl(connectionEndpoint, loginAccountName, clientInformationString,
            serverSideUplinkEndpointService, concurrencyUtilsFactory);
    }

    /**
     * OSGi-DS service injector.
     * 
     * @param newInstance the service instance
     */
    @Reference
    public void bindServerSideUplinkEndpointService(ServerSideUplinkEndpointService newInstance) {
        this.serverSideUplinkEndpointService = newInstance;
    }

    /**
     * OSGi-DS injection method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindConcurrencyUtilsFactory(ConcurrencyUtilsFactory newInstance) {
        this.concurrencyUtilsFactory = newInstance;
    }

}
