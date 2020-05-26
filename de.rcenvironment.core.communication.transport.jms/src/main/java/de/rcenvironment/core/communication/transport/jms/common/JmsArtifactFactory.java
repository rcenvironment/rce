/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.ConnectionFactory;

import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.model.NetworkContactPoint;

/**
 * Abstract factory to provide objects instances that depend on a specific JMS implementation.
 * 
 * @author Robert Mischke
 */
public interface JmsArtifactFactory {

    /**
     * Creates a new {@link JmsBroker} for the given {@link ServerContactPoint}. The broker is not
     * automatically started; call {@link JmsBroker#start()} for this.
     * 
     * @param scp the {@link ServerContactPoint} to attach to (which usually translates to listening
     *        to a network port)
     * @param remoteInitiatedConnectionFactory the connection factory for "inverse" or "passive"
     *        connections that are created after remote nodes have made their "active" connections
     * @return the new broker instance
     */
    JmsBroker createBroker(ServerContactPoint scp, RemoteInitiatedMessageChannelFactory remoteInitiatedConnectionFactory);

    /**
     * Creates a JMS connection factory towards the given {@link NetworkContactPoint}.
     * 
     * @param ncp the {@link NetworkContactPoint} describing the destination
     * @return the JMS {@link ConnectionFactory}
     */
    ConnectionFactory createConnectionFactory(NetworkContactPoint ncp);

}
