/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.communication.sshconnection.SshConnectionConstants;
import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListener;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListenerAdapter;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.configuration.SecurePreferencesFactory;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Default implementation of {@link SshConnectionService}.
 *
 * @author Brigitte Boden
 */
public class SshConnectionServiceImpl implements SshConnectionService {

    private static final String NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED = "No SSH connection with id %s configured.";

    private final Map<String, SshConnectionSetup> connectionSetups;

    private final Log log = LogFactory.getLog(getClass());

    private final AsyncOrderedCallbackManager<SshConnectionListener> callbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);

    private NodeConfigurationService configurationService;

    public SshConnectionServiceImpl() {
        connectionSetups = new HashMap<String, SshConnectionSetup>();
    }

    @Override
    public Session getAvtiveSshSession(String connectionId) {
        final SshConnectionSetup setup = connectionSetups.get(connectionId);
        if (setup == null) {
            log.warn(StringUtils.format(NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED, connectionId));
            return null;
        }
        return setup.getSession();
    }

    @Override
    public String addSshConnection(String displayName, String destinationHost, int port, String sshAuthUser, String keyfileLocation,
        boolean usePassphrase, boolean connectImmediately) {
        String connectionId = UUID.randomUUID().toString();

        SshConnectionListener listenerAdapter = defineListenerForSSHConnectionSetup();

        final SshConnectionSetupImpl newSetup;
        newSetup = new SshConnectionSetupImpl(connectionId, displayName, destinationHost, port, sshAuthUser,
            keyfileLocation, usePassphrase, false, connectImmediately, listenerAdapter);

        if (newSetup != null) {
            synchronized (connectionSetups) {
                connectionSetups.put(connectionId, newSetup);
                final Collection<SshConnectionSetup> snapshot = Collections.unmodifiableCollection(connectionSetups.values());
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onCollectionChanged(snapshot);
                    }
                });
            }
        }
        return connectionId;
    }

    private SshConnectionListener defineListenerForSSHConnectionSetup() {
        SshConnectionListener listenerAdapter = new SshConnectionListenerAdapter() {

            @Override
            public void onConnectionAttemptFailed(final SshConnectionSetup setup, final String reason,
                final boolean firstConsecutiveFailure, final boolean willAutoRetry) {
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onConnectionAttemptFailed(setup, reason, firstConsecutiveFailure, willAutoRetry);
                    }
                });
            }

            @Override
            public void onConnectionClosed(final SshConnectionSetup setup, final boolean willAutoRetry) {
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onConnectionClosed(setup, willAutoRetry);
                    }
                });
            }

            @Override
            public void onConnected(final SshConnectionSetup setup) {
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onConnected(setup);
                    }
                });
            }

            @Override
            public void onCreated(final SshConnectionSetup setup) {
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onCreated(setup);
                    }
                });
            }

        };
        return listenerAdapter;
    }

    @Override
    public boolean isConnected(String connectionId) {
        return connectionSetups.get(connectionId).isConnected();
    }

    @Override
    public Session connectSession(String connectionId) {

        String passphrase = "";
        if (connectionSetups.get(connectionId).getUsePassphrase()) {
            // Retreive passphrase from secure store.
            passphrase = retreiveSshConnectionPassword(connectionId);
        }

        return connectSession(connectionId, passphrase);
    }

    @Override
    public Session connectSession(String connectionId, String passphrase) {
        final SshConnectionSetup sshConnectionSetup = connectionSetups.get(connectionId);
        if (sshConnectionSetup == null) {
            log.warn(StringUtils.format(NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED, connectionId));
            return null;
        }
        return sshConnectionSetup.connect(passphrase);
    }

    @Override
    public void disconnectSession(String connectionId) {
        final SshConnectionSetup sshConnectionSetup = connectionSetups.get(connectionId);
        if (sshConnectionSetup == null) {
            log.warn(StringUtils.format(NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED, connectionId));
            return;
        }
        sshConnectionSetup.disconnect();
    }

    @Override
    public void disposeConnection(String connectionId) {
        final SshConnectionSetup setup = connectionSetups.get(connectionId);
        if (setup == null) {
            log.warn(StringUtils.format(NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED, connectionId));
            return;
        }
        if (setup.isConnected()) {
            setup.disconnect();
        }
        synchronized (connectionSetups) {
            connectionSetups.remove(connectionId);
            final Collection<SshConnectionSetup> snapshot = Collections.unmodifiableCollection(connectionSetups.values());
            callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                @Override
                public void performCallback(SshConnectionListener listener) {
                    listener.onDisposed(setup);
                    listener.onCollectionChanged(snapshot);
                }
            });
        }
    }

    @Override
    public SshConnectionSetup getConnectionSetup(String connnectionId) {
        return connectionSetups.get(connnectionId);
    }

    @Override
    public Collection<SshConnectionSetup> getAllSshConnectionSetups() {
        return Collections.unmodifiableCollection(connectionSetups.values());
    }

    @Override
    public Map<String, SshConnectionSetup> getAllActiveSshConnectionSetups() {
        Map<String, SshConnectionSetup> activeConnections = new HashMap<String, SshConnectionSetup>();
        for (SshConnectionSetup connection : connectionSetups.values()) {
            if (connection.isConnected()) {
                activeConnections.put(connection.getId(), connection);
            }
        }
        return activeConnections;
    }

    /**
     * Adds a {@link SshConnectionListener}.
     * 
     * @param listener The listener.
     */
    public void addListener(SshConnectionListener listener) {
        callbackManager.addListener(listener);
    }

    /**
     * Removes a {@link SshConnectionListener}.
     * 
     * @param listener The listener.
     */
    public void removeListener(SshConnectionListener listener) {
        callbackManager.removeListener(listener);
    }

    @Override
    public void editSshConnection(SshConnectionContext context) {

        SshConnectionListener listenerAdapter = defineListenerForSSHConnectionSetup();

        final SshConnectionSetupImpl newSetup;
        newSetup =
            new SshConnectionSetupImpl(context.getId(), context.getDisplayName(), context.getDestinationHost(),
                context.getPort(), context.getSshAuthUser(),
                context.getKeyfileLocation(), context.isUsePassphrase(), false, context.isConnectImmediately(), listenerAdapter);

        if (newSetup != null) {
            synchronized (connectionSetups) {
                connectionSetups.put(context.getId(), newSetup);
                final Collection<SshConnectionSetup> snapshot = Collections.unmodifiableCollection(connectionSetups.values());
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onCollectionChanged(snapshot);
                    }
                });
            }
        }
    }

    @Override
    public Collection<String> getAllActiveSshConnectionSetupIds() {
        return getAllActiveSshConnectionSetups().keySet();
    }

    /**
     * OSGi-DS lifecycle method.
     */
    public void activate() {
        ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

            @Override
            @TaskDescription("Client-Side Remote Access: Add pre-configured SSH connections")
            public void run() {
                addInitialSshConfigs(configurationService.getInitialSSHConnectionConfigs());
            }
        });
    }

    private void addInitialSshConfigs(List<InitialSshConnectionConfig> configs) {
        for (InitialSshConnectionConfig config : configs) {
            SshConnectionSetup setup =
                new SshConnectionSetupImpl(config.getId(), config.getDisplayName(), config.getHost(), config.getPort(), config.getUser(),
                    config.getKeyFileLocation(), config.getUsePassphrase(), false, false, defineListenerForSSHConnectionSetup());
            connectionSetups.put(config.getId(), setup);
        }
    }

    /**
     * OSGI bind method.
     * 
     * @param service The service to bind.
     */
    public void bindNodeConfigurationService(NodeConfigurationService service) {
        this.configurationService = service;
    }

    private void storeSshConnectionPassword(String connectionId, String password) {

        try {
            ISecurePreferences prefs = SecurePreferencesFactory.getSecurePreferencesStore();
            ISecurePreferences node = prefs.node(SshConnectionConstants.SSH_CONNECTIONS_PASSWORDS_NODE);
            node.put(connectionId, password, true);
        } catch (IOException | StorageException e) {
            log.error("Could not store password: " + e);
        }
    }

    private void removeSshConnectionPassword(String connectionId) {

        try {
            ISecurePreferences prefs = SecurePreferencesFactory.getSecurePreferencesStore();
            ISecurePreferences node = prefs.node(SshConnectionConstants.SSH_CONNECTIONS_PASSWORDS_NODE);
            node.remove(connectionId);
        } catch (IOException e) {
            log.error("Could not remove password: " + e);
        }
    }

    @Override
    public String retreiveSshConnectionPassword(String connectionId) {
        String passphrase = null;
        try {
            ISecurePreferences prefs = SecurePreferencesFactory.getSecurePreferencesStore();
            ISecurePreferences node = prefs.node(SshConnectionConstants.SSH_CONNECTIONS_PASSWORDS_NODE);
            passphrase = node.get(connectionId, null);
        } catch (IOException | StorageException e) {
            log.error("Could not retreive password: " + e);
            return null;
        }
        return passphrase;
    }

    @Override
    public void setAuthPhraseForSshConnection(String id, String sshAuthPassPhrase, boolean storePassphrase) {
        SshConnectionListener listenerAdapter = defineListenerForSSHConnectionSetup();

        final SshConnectionSetup oldSetup = connectionSetups.get(id);
        final SshConnectionSetupImpl newSetup;
        newSetup =
            new SshConnectionSetupImpl(id, oldSetup.getDisplayName(), oldSetup.getHost(), oldSetup.getPort(), oldSetup.getUsername(),
                oldSetup.getKeyfileLocation(), oldSetup.getUsePassphrase(), storePassphrase, oldSetup.getConnectOnStartUp(),
                listenerAdapter);

        if (newSetup != null) {
            synchronized (connectionSetups) {
                connectionSetups.put(id, newSetup);
                final Collection<SshConnectionSetup> snapshot = Collections.unmodifiableCollection(connectionSetups.values());
                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onCollectionChanged(snapshot);
                    }
                });
            }
            if (storePassphrase) {
                storeSshConnectionPassword(id, sshAuthPassPhrase);
            } else if (oldSetup.getStorePassphrase()) {
                // Remove old stored password, if one exists.
                removeSshConnectionPassword(id);
            }
        }
    }

}
