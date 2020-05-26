/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.impl;

import java.net.ConnectException;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.sshconnection.SshConnectionConstants;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListener;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadGuard;

/**
 * Default implementation of @link {@link SshConnectionSetup}.
 *
 * @author Brigitte Boden
 */
public class SshConnectionSetupImpl implements SshConnectionSetup {

    private SshSessionConfiguration config;

    private String id;

    private String displayName;

    private Session session;

    private SshConnectionListener listener;

    private boolean connectOnStartup;

    private boolean autoRetry;

    private boolean usePassphrase;

    private int consecutiveConnectionFailures;

    private volatile boolean waitingForRetry;

    private Log log = LogFactory.getLog(getClass());

    public SshConnectionSetupImpl(String id, String displayName, String host, int port, String userName, String keyFileLocation,
        boolean usePassphrase, boolean connectOnStartUp, boolean autoRetry, SshConnectionListener listener) {
        if (keyFileLocation == null || keyFileLocation.isEmpty()) {
            config = SshSessionConfigurationFactory.createSshSessionConfigurationWithAuthPhrase(host, port, userName, null);
        } else {
            config = SshSessionConfigurationFactory.createSshSessionConfigurationWithKeyFileLocation(host, port, userName, keyFileLocation);
        }
        this.id = id;
        this.connectOnStartup = connectOnStartUp;
        this.autoRetry = autoRetry;
        this.listener = listener;
        this.displayName = displayName;
        this.usePassphrase = usePassphrase;
        listener.onCreated(this);
        this.consecutiveConnectionFailures = 0;
        this.waitingForRetry = false;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup#getHost()
     */
    @Override
    public String getHost() {
        return config.getDestinationHost();
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup#getPort()
     */
    @Override
    public int getPort() {
        return config.getPort();
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup#getUsername()
     */
    @Override
    public String getUsername() {
        return config.getSshAuthUser();
    }

    @Override
    public String getKeyfileLocation() {
        return config.getSshKeyFileLocation();
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup#isConnected()
     */
    @Override
    public boolean isConnected() {
        if (session == null) {
            return false;
        }
        boolean result = session.isConnected();
        // If the session is not null, but also not connected, it has been lost, which should be communicated by the listener.
        if (!result) {
            session = null;
            listener.onConnectionClosed(this, autoRetry);
            String retryMessage = "";
            if (autoRetry) {
                retryMessage = " Will try to auto-reconnect.";
            }
            log.warn(StringUtils.format("SSH session lost: host %s, port %s%s", config.getDestinationHost(),
                config.getPort(), retryMessage));
        }
        return result;
    }

    @Override
    public boolean isWaitingForRetry() {
        return waitingForRetry;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Session connect(String passphrase) {
        ThreadGuard.checkForForbiddenThread();

        if (config.getSshKeyFileLocation() == null && passphrase == null) {
            log.warn(StringUtils.format("Connecting SSH session failed because no key file and no passphrase is given: host %s, port %s.",
                config.getDestinationHost(), config.getPort()));
            String error = "No key file or passphrase could be found. Probable cause: "
                + "This was an automatic reconnection attempt and the passphrase is not stored.";
            listener.onConnectionAttemptFailed(this, error, true, false);
            return null;
        }

        Logger logger = JschSessionFactory.createDelegateLogger(LogFactory.getLog(getClass()));
        try {
            session =
                JschSessionFactory.setupSession(config.getDestinationHost(), config.getPort(), config.getSshAuthUser(),
                    config.getSshKeyFileLocation(), passphrase, logger);
            waitingForRetry = false;
            consecutiveConnectionFailures = 0;
        } catch (JSchException | SshParameterException e) {
            log.warn(StringUtils.format("Connecting SSH session failed: host %s, port %s: %s", config.getDestinationHost(),
                config.getPort(), e.toString()));
            // Filter typical reasons to produce better error messages.
            String reason = e.getMessage();
            Throwable cause = e.getCause();
            // Reconnect only makes sense if some network problem occured, not if the credentials are wrong.
            boolean shouldTryToReconnect = autoRetry;
            if (cause != null && cause instanceof ConnectException) {
                reason = "The remote instance could not be reached. Probably the hostname or port is wrong.";
            } else if (cause != null && cause instanceof UnknownHostException) {
                reason = "No host with this name could be found.";
            } else if (reason.equals("Auth fail")) {
                reason =
                    "Authentication failed. Probably the username or passphrase is wrong, the wrong key file was used or the account is "
                        + "not enabled on the remote host.";
                shouldTryToReconnect = false;
            } else if (reason.equals("USERAUTH fail")) {
                reason = "Authentication failed. The wrong passphrase for the key file " + config.getSshKeyFileLocation() + " was used.";
                shouldTryToReconnect = false;
            } else if (reason.startsWith("invalid privatekey")) {
                reason = "Authentication failed. An invalid private key was used.";
                shouldTryToReconnect = false;
            }
            if (shouldTryToReconnect) {
                consecutiveConnectionFailures++;
            }
            listener.onConnectionAttemptFailed(this, reason, (consecutiveConnectionFailures <= 1), shouldTryToReconnect);

            return null;
        }
        // Check if remote RCE instance has a compatible version
        String remoteRCEVersion = null;
        // Extract RCE version information from server banner
        for (String versionToken : session.getServerVersion().split(" ")) {
            if (versionToken.startsWith("RemoteAccess/")) {
                remoteRCEVersion = versionToken.split("/")[1];
            }
        }

        if (remoteRCEVersion == null) {
            log.warn(StringUtils.format(
                "Connecting SSH session failed: Could not retreive version of RCE instance on host %s, port %s from server banner.",
                config.getDestinationHost(),
                config.getPort()));
            session.disconnect();
            session = null;
            String reason = "The RCE version of the remote instance could not be retrieved. Possibly it is not an RCE instance.";
            listener.onConnectionAttemptFailed(this, reason, true, false);
            return null;
        }
        if (!remoteRCEVersion.contains(SshConnectionConstants.REQUIRED_PROTOCOL_VERSION)) {
            log.warn(StringUtils
                .format(
                    "Connecting SSH session failed: Either, the RCE instance on host %s, port %s has an incompatible version, "
                        + "or the user %s does not have the required permissions to run remote access tools and workflows. "
                        + "(Detected server version information: %s, required version: %s)",
                    config.getDestinationHost(), config.getPort(), config.getSshAuthUser(),
                    remoteRCEVersion, SshConnectionConstants.REQUIRED_PROTOCOL_VERSION));
            session.disconnect();
            session = null;
            String reason = StringUtils.format(
                "Either, the remote RCE instance has an incompatible version, or the user %s "
                    + "does not have the required permissions to run remote access tools and workflows. "
                    + "\n\n(Detected server version information: %s, required version: %s)",
                config.getSshAuthUser(),
                remoteRCEVersion, SshConnectionConstants.REQUIRED_PROTOCOL_VERSION);
            listener.onConnectionAttemptFailed(this, reason, true, false);
            return null;
        }

        listener.onConnected(this);
        return session;
    }

    @Override
    public void disconnect() {
        session.disconnect();
        listener.onConnectionClosed(this, false);
        session = null;
        this.consecutiveConnectionFailures = 0;
        this.waitingForRetry = false;
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
}
