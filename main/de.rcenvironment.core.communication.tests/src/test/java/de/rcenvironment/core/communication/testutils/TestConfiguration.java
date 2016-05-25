/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;

/**
 * An interface defining specific test configurations for running the abstract test templates with.
 * 
 * @author Robert Mischke
 */
public interface TestConfiguration {

    /**
     * @return the {@link NetworkContactPointGenerator} instance to use
     */
    NetworkContactPointGenerator getContactPointGenerator();

    /**
     * @return the {@link NetworkTransportProvider} instance to use
     */
    NetworkTransportProvider getTransportProvider();

    /**
     * @return the default time to wait before the first traffic must have occured
     */
    int getDefaultTrafficWaitTimeout();

    /**
     * @return the default minimum time without observed traffic before it is considered
     *         "network silence"
     */
    int getDefaultNetworkSilenceWait();

    /**
     * @return the default maximum time to wait for "network silence"
     */
    int getDefaultNetworkSilenceWaitTimeout();

}
