/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;

import de.rcenvironment.core.communication.model.InitialNodeInformation;

/**
 * Simple holder class for the information that is exchanged in the initial handshake with a remote
 * node.
 * 
 * @author Robert Mischke
 */
public class HandshakeInformation {

    private String protocolVersionString;

    private InitialNodeInformation initialNodeInformation;

    private String channelId;

    public String getProtocolVersionString() {
        return protocolVersionString;
    }

    public void setProtocolVersionString(String protocolVersionString) {
        this.protocolVersionString = protocolVersionString;
    }

    public InitialNodeInformation getInitialNodeInformation() {
        return initialNodeInformation;
    }

    public void setInitialNodeInformation(InitialNodeInformation initialNodeInformation) {
        this.initialNodeInformation = initialNodeInformation;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * @param expectedProtocolVersion the expected version string
     * @return true if the contained version string is equal to the provided string; the check is
     *         null-safe
     */
    public boolean matchesVersion(String expectedProtocolVersion) {
        return (protocolVersionString != null) && protocolVersionString.equals(expectedProtocolVersion);
    }

}
