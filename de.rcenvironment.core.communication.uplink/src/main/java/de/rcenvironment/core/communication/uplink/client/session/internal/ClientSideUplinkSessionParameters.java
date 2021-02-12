/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.internal;

import java.util.Map;

/**
 * A simple holder for parameters for initiating up a client-side Uplink session.
 *
 * @author Robert Mischke
 */
public class ClientSideUplinkSessionParameters {

    private final String announcedDisplayName;

    private final String qualifier;

    private String clientVersionInfo;

    private final Map<String, String> customHandshakeData;

    /**
     * @param announcedDisplayName the display name to represent this connection/session to other clients, e.g. in network overviews or log
     *        messages
     * @param qualifier the "client id", a string to identify a connection in case several connections use the same SSH account
     * @param clientVersionInfo an optional client version value to send as part of the client-to-server handshake, e.g.
     *        "rce/10.2.0.<qualifier>"
     * @param customHandshakeData handshake field overrides; typically used for testing
     * 
     */
    public ClientSideUplinkSessionParameters(String announcedDisplayName, String qualifier, String clientVersionInfo,
        Map<String, String> customHandshakeData) {
        this.announcedDisplayName = announcedDisplayName;
        this.qualifier = qualifier;
        this.clientVersionInfo = clientVersionInfo;
        this.customHandshakeData = customHandshakeData;
    }

    public String getAnnouncedDisplayName() {
        return announcedDisplayName;
    }

    public String getSessionQualifier() {
        return qualifier;
    }

    public String getClientVersionInfo() {
        return clientVersionInfo;
    }

    public Map<String, String> getCustomHandshakeData() {
        return customHandshakeData;
    }
}
