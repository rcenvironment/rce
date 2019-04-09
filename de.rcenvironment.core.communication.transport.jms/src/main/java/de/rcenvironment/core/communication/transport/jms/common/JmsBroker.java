/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.jms.common;

import javax.jms.Connection;

/**
 * Common interface for embedded JMS brokers that accept incoming connections and create matching
 * remote-initiated ("passive") connections for them. Currently, each broker provides exactly one
 * {@link ServerContactPoint}; this may be changed in the future.
 * 
 * @author Robert Mischke
 */
public interface JmsBroker {

    /**
     * Causes this broker to start up and begin accepting connections.
     * 
     * @throws Exception on startup exceptions
     */
    void start() throws Exception;

    /**
     * Causes this broker to stop accepting connections and shut down.
     */
    void stop();

    /**
     * @return an established JMS {@link Connection} that can be used to communicate with the
     *         embedded broker
     */
    Connection getLocalConnection();

}
