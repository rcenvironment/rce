/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSessionEventHandler;
import de.rcenvironment.core.communication.uplink.client.session.api.LocalUplinkSessionService;
import de.rcenvironment.core.utils.common.StreamConnectionEndpoint;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Default {@link LocalUplinkSessionService} implementation.
 *
 * @author Robert Mischke
 */
@Component
public class LocalUplinkSessionServiceImpl implements LocalUplinkSessionService {

    private ConcurrencyUtilsFactory concurrencyUtilsFactory;

    @Override
    public ClientSideUplinkSession createSession(StreamConnectionEndpoint connectionEndpoint,
        ClientSideUplinkSessionParameters sessionParameters, ClientSideUplinkSessionEventHandler eventHandler) {
        return new ClientSideUplinkSessionImpl(connectionEndpoint, sessionParameters, eventHandler,
            concurrencyUtilsFactory);
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
