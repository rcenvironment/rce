/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.MessageChannel;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;

/**
 * Service interface for methods that are usually required by the receiving end of a network
 * connection. Depending on the transport implementation, these methods may be provided as remote
 * services.
 * 
 * @author Robert Mischke
 */
public class DefaultRawMessageChannelEndpointHandler implements RawMessageChannelEndpointHandler {

    @Override
    public InitialNodeInformation exchangeNodeInformation(InitialNodeInformation nodeInformation) {
        return null;
    }

    @Override
    public void onRemoteInitiatedChannelEstablished(MessageChannel connection, ServerContactPoint serverContactPoint) {}

    @Override
    public void onInboundChannelClosing(String idOfInboundChannel) {}

    @Override
    public NetworkResponse onRawRequestReceived(NetworkRequest request, NodeIdentifier sourceId) {
        return null;
    }
}
