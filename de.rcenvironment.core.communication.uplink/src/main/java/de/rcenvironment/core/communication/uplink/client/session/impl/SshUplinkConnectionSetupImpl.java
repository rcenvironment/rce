/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListener;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;

/**
 * Default implementation of @link {@link SshUplinkConnectionSetup}.
 *
 * @author Brigitte Boden
 * @author Kathrin Schaffert (added method setDisplayName to fix #17306)
 * @author Dominik Schneider (added wantToReconnect for correct retry behavior)
 */

public class SshUplinkConnectionSetupImpl implements SshUplinkConnectionSetup {

    private SshSessionConfiguration config;

    private String id;

    private String displayName;

    private String qualifier;

    private ClientSideUplinkSession session;

    private boolean connectOnStartup;

    private boolean autoRetry;

    private boolean usePassphrase;

    private boolean isGateway;

    private AtomicBoolean wantToReconnect;

    private int consecutiveConnectionFailures;

    private volatile boolean waitingForRetry;

    private String destinationIdPrefix;

    private Log log = LogFactory.getLog(getClass());

    public SshUplinkConnectionSetupImpl(String id, String displayName, String qualifier, String host, int port, String userName,
        String keyFileLocation, boolean usePassphrase, boolean connectOnStartUp, boolean autoRetry, boolean isGateway,
        SshUplinkConnectionListener listener) {
        if (keyFileLocation == null || keyFileLocation.isEmpty()) {
            config = SshSessionConfigurationFactory.createSshSessionConfigurationWithAuthPhrase(host, port, userName, null);
        } else {
            config = SshSessionConfigurationFactory.createSshSessionConfigurationWithKeyFileLocation(host, port, userName, keyFileLocation);
        }
        this.id = id;
        this.connectOnStartup = connectOnStartUp;
        this.autoRetry = autoRetry;
        this.displayName = displayName;
        this.qualifier = qualifier;
        this.usePassphrase = usePassphrase;
        this.consecutiveConnectionFailures = 0;
        this.waitingForRetry = false;
        this.isGateway = isGateway;
        this.wantToReconnect = new AtomicBoolean(autoRetry);
        listener.onCreated(this);
    }

    @Override
    public String getHost() {
        return config.getDestinationHost();
    }

    @Override
    public int getPort() {
        return config.getPort();
    }

    @Override
    public String getUsername() {
        return config.getSshAuthUser();
    }

    @Override
    public String getKeyfileLocation() {
        return config.getSshKeyFileLocation();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isConnected() {
        return session != null && session.isActive();
    }

    @Override
    public boolean isWaitingForRetry() {
        return waitingForRetry;
    }

    @Override
    public ClientSideUplinkSession getSession() {
        return session;
    }

    @Override
    public void setSession(ClientSideUplinkSession session) {
        if (session != null && this.session != null) {
            log.warn("Attaching new Uplink session " + session.getLocalSessionId() + " before the previous session "
                + this.session.getLocalSessionId() + " was disposed");
        }

        this.session = session;
        this.wantToReconnect.set(autoRetry);
    }

    @Override
    public void disconnect() {
        this.wantToReconnect.set(false);
        session.initiateCleanShutdownIfRunning();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean getConnectOnStartUp() {
        return connectOnStartup;
    }

    @Override
    public boolean getUsePassphrase() {
        return usePassphrase;
    }

    @Override
    public boolean getAutoRetry() {
        return autoRetry;
    }

    @Override
    public void setWaitingForRetry(boolean waitingForRetry) {
        this.waitingForRetry = waitingForRetry;
        if (!waitingForRetry) {
            this.consecutiveConnectionFailures = 0;
        }
    }

    @Override
    public void resetConsecutiveConnectionFailures() {
        this.consecutiveConnectionFailures = 0;
    }

    @Override
    public void raiseConsecutiveConnectionFailures() {
        this.consecutiveConnectionFailures++;

    }

    @Override
    public int getConsecutiveConnectionFailures() {
        return consecutiveConnectionFailures;
    }

    @Override
    public String getDestinationIdPrefix() {
        return destinationIdPrefix;
    }

    @Override
    public void setDestinationIdPrefix(String destinationIdPrefix) {
        this.destinationIdPrefix = destinationIdPrefix;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public boolean isGateway() {
        return isGateway;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean wantToReconnect() {
        return this.wantToReconnect.get();
    }
}
