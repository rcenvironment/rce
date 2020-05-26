/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.relay.internal;

import java.io.InputStream;
import java.io.OutputStream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
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
    public ServerSideUplinkSession createServerSideSession(String clientInformationString, String loginAccountName, InputStream inputStream,
        OutputStream outputStream) {
        return new ServerSideUplinkSessionImpl(clientInformationString, loginAccountName, inputStream, outputStream,
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
