/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils;

import org.junit.Ignore;

import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;

/**
 * Default bean-style {@link TestConfiguration} implementation.
 * 
 * @author Robert Mischke
 */
@Ignore("configuration class; causes 'no runnable methods' warning unless ignored")
public class TestConfigurationImpl implements TestConfiguration {

    private NetworkContactPointGenerator contactPointGenerator;

    private NetworkTransportProvider transportProvider;

    private int defaultTrafficWaitTimeout;

    private int defaultNetworkSilenceWait;

    private int defaultNetworkSilenceWaitTimeout;

    @Override
    public NetworkContactPointGenerator getContactPointGenerator() {
        return contactPointGenerator;
    }

    public void setContactPointGenerator(NetworkContactPointGenerator contactPointGenerator) {
        this.contactPointGenerator = contactPointGenerator;
    }

    @Override
    public NetworkTransportProvider getTransportProvider() {
        return transportProvider;
    }

    public void setTransportProvider(NetworkTransportProvider transportProvider) {
        this.transportProvider = transportProvider;
    }

    @Override
    public int getDefaultTrafficWaitTimeout() {
        return defaultTrafficWaitTimeout;
    }

    public void setDefaultTrafficWaitTimeout(int defaultTrafficWaitTimeout) {
        this.defaultTrafficWaitTimeout = defaultTrafficWaitTimeout;
    }

    @Override
    public int getDefaultNetworkSilenceWait() {
        return defaultNetworkSilenceWait;
    }

    public void setDefaultNetworkSilenceWait(int defaultNetworkSilenceWait) {
        this.defaultNetworkSilenceWait = defaultNetworkSilenceWait;
    }

    @Override
    public int getDefaultNetworkSilenceWaitTimeout() {
        return defaultNetworkSilenceWaitTimeout;
    }

    public void setDefaultNetworkSilenceWaitTimeout(int defaultNetworkSilenceWaitTimeout) {
        this.defaultNetworkSilenceWaitTimeout = defaultNetworkSilenceWaitTimeout;
    }
}
