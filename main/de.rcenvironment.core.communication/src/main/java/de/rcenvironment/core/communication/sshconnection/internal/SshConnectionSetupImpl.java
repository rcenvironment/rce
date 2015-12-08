/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;
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
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;

/**
 * Default implementaion of @link {@link SshConnectionSetup}.
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

    private boolean storePassphrase;

    private Log log = LogFactory.getLog(getClass());

    public SshConnectionSetupImpl(String id, String displayName, String host, int port, String userName, boolean storePassphrase,
        boolean connectOnStartUp, SshConnectionListener listener) {
        config = SshSessionConfigurationFactory.createSshSessionConfigurationWithAuthPhrase(host, port, userName, null);
        this.id = id;
        this.connectOnStartup = connectOnStartUp;
        this.listener = listener;
        this.displayName = displayName;
        this.storePassphrase = storePassphrase;
        listener.onCreated(this);
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
            listener.onConnectionClosed(this, false);
            log.warn(StringUtils.format("SSH session lost: host %s, port %s", config.getDestinationHost(),
                config.getPort()));
        }
        return result;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Session connect(String passphrase) {

        if (passphrase == null) {
            log.warn(StringUtils.format("Connecting SSH session failed because of missing passphrase: host %s, port %s.",
                config.getDestinationHost(),
                config.getPort()));
            return null;
        }

        Logger logger = JschSessionFactory.createDelegateLogger(LogFactory.getLog(getClass()));
        try {
            session =
                JschSessionFactory.setupSession(config.getDestinationHost(), config.getPort(), config.getSshAuthUser(), null, passphrase,
                    logger);
        } catch (JSchException | SshParameterException e) {
            log.warn(StringUtils.format("Connecting SSH session failed: host %s, port %s: %s", config.getDestinationHost(),
                config.getPort(), e.toString()));
            // Filter typical reasons to produce better error messages.
            String reason = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof ConnectException) {
                reason = "The remote instance could not be reached. Probably the hostname or port is wrong.";
            } else if (cause != null && cause instanceof UnknownHostException) {
                reason = "No host with this name could be found.";
            } else if (reason.equals("Auth fail")) {
                reason = "Authentication failed. Probably the username or passphrase is wrong.";
            }
            listener.onConnectionAttemptFailed(this, reason, true, false);
            return null;
        }

        // Check if remote RCE instance has a compatible version
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
        String remoteRCEVersion;

        try {
            rceExecutor.start("ra protocol-version");
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                rceExecutor.waitForTermination();
                remoteRCEVersion = IOUtils.toString(stdoutStream).trim();
            }
        } catch (IOException | InterruptedException e1) {
            log.warn(StringUtils.format(
                "Connecting SSH session failed: Could not retreive version of RCE instance on host %s, port %s: %s",
                config.getDestinationHost(),
                config.getPort(), e1.toString()));
            session.disconnect();
            session = null;
            String reason = "The RCE version of the remote instance could not be retreived. Possibly it is not an RCE instance.";
            listener.onConnectionAttemptFailed(this, reason, true, false);
            return null;
        }
        if (!remoteRCEVersion.equals(SshConnectionConstants.REQUIRED_PROTOCOL_VERSION)) {
            log.warn(StringUtils.format("Connecting SSH session failed: RCE instance on host %s, port %s has an incompatible version.",
                config.getDestinationHost(),
                config.getPort()));
            session.disconnect();
            session = null;
            String reason = "The remote RCE instance has an incompatible version.";
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
    public boolean getStorePassphrase() {
        return storePassphrase;
    }

}
