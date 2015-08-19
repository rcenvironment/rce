/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.transport.activemq;

import de.rcenvironment.core.communication.testutils.TestConfigurationImpl;
import de.rcenvironment.core.communication.transport.activemq.testutils.ActiveMQNetworkContactPointGenerator;
import de.rcenvironment.core.communication.transport.spi.DefaultMessageChannelIdFactoryImpl;

/**
 * Test configuration for ActiveMQ tests.
 * 
 * @author Robert Mischke
 */
public class ActiveMQTestConfiguration extends TestConfigurationImpl {

    private static final int DEFAULT_TRAFFIC_WAIT_TIMEOUT_MSEC = 1250;

    private static final int DEFAULT_NETWORK_SILENCE_WAIT_MSEC = 1500;

    private static final int DEFAULT_NETWORK_SILENCE_WAIT_TIMEOUT_MSEC = 30000;

    public ActiveMQTestConfiguration() {
        setTransportProvider(new ActiveMQTransportProvider(new DefaultMessageChannelIdFactoryImpl()));
        setContactPointGenerator(new ActiveMQNetworkContactPointGenerator("localhost"));
        setDefaultTrafficWaitTimeout(DEFAULT_TRAFFIC_WAIT_TIMEOUT_MSEC);
        setDefaultNetworkSilenceWait(DEFAULT_NETWORK_SILENCE_WAIT_MSEC);
        setDefaultNetworkSilenceWaitTimeout(DEFAULT_NETWORK_SILENCE_WAIT_TIMEOUT_MSEC);
    }
}
