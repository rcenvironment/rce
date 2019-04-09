/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;

/**
 * Service interface for methods that are usually required by the receiving end of a network
 * connection. Depending on the transport implementation, these methods may be provided as remote
 * services.
 * 
 * @author Robert Mischke
 */
public class DefaultRawMessageChannelEndpointHandler implements MessageChannelEndpointHandler {

    @Override
    public InitialNodeInformation exchangeNodeInformation(InitialNodeInformation nodeInformation) {
        return null;
    }

    @Override
    public void onRemoteInitiatedChannelEstablished(MessageChannel connection, ServerContactPoint serverContactPoint) {}

    @Override
    public void onInboundChannelClosing(String idOfInboundChannel) {}

    @Override
    public NetworkResponse onRawRequestReceived(NetworkRequest request, String sourceId) {
        return null;
    }
}
