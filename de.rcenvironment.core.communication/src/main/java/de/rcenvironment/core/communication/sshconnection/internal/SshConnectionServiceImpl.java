/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.communication.sshconnection.SshConnectionConstants;
import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListener;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListenerAdapter;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.communication.sshconnection.impl.SshConnectionSetupImpl;
import de.rcenvironment.core.configuration.SecureStorageImportService;
import de.rcenvironment.core.configuration.SecureStorageSection;
import de.rcenvironment.core.configuration.SecureStorageService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadGuard;

/**
 * Default implementation of {@link SshConnectionService}.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */

@Component
public class SshConnectionServiceImpl implements SshConnectionService {

    private static final String NO_SSH_CONNECTION_WITH_ID_S_CONFIGURED = "No SSH connection with id %s configured.";

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final Map<String, SshConnectionSetup> connectionSetups;

    private final Log log = LogFactory.getLog(getClass());

    private final AsyncOrderedCallbackManager<SshConnectionListener> callbackManager =
        ConcurrencyUtils.getFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER);

    @Reference
    private NodeConfigurationService configurationService;

    @Reference
    private SecureStorageService secureStorageService;

    @Reference
    private SecureStorageImportService secureStorageImportService;

    private SecureStorageSection secureStorageSection;

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

    /**
     * @param context the {@link SshConnectionContext}.
     * @return true iff a connection with the same host and port already exists
     */
    public boolean sshConnectionAlreadyExists(SshConnectionContext context) {
        for (String s : connectionSetups.keySet()) {
            if (connectionSetups.get(s).getHost().equals(context.getDestinationHost())
                && connectionSetups.get(s).getPort() == context.getPort()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String addSshConnection(SshConnectionContext context) {

        String connectionId = UUID.randomUUID().toString();

        SshConnectionListener listenerAdapter = defineListenerForSSHConnectionSetup();

        final SshConnectionSetupImpl newSetup;
        newSetup = new SshConnectionSetupImpl(connectionId, context.getDisplayName(), context.getDestinationHost(),
            context.getPort(), context.getSshAuthUser(), context.getKeyfileLocation(), context.isUsePassphrase(),
            context.isConnectImmediately(), context.isAutoRetry(), listenerAdapter);

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

                if (willAutoRetry) {
                    scheduleAutoRetry(setup);
                }

                callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                    @Override
                    public void performCallback(SshConnectionListener listener) {
                        listener.onConnectionAttemptFailed(setup, reason, firstConsecutiveFailure, willAutoRetry);
                    }
                });
            }

            @Override
            public void onConnectionClosed(final SshConnectionSetup setup, final boolean willAutoRetry) {

                if (willAutoRetry) {
                    scheduleAutoRetry(setup);
                }

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

    private void scheduleAutoRetry(final SshConnectionSetup setup) {
        log.debug(StringUtils.format("Scheduling auto-retry of connection %s in %d msec", setup.getDisplayName(),
            SshConnectionConstants.DELAY_BEFORE_RETRY));
        threadPool.scheduleAfterDelay("Communication Layer: SshConnectionService auto-reconnect timer", () -> {
            if (setup.isWaitingForRetry()) {
                connectSession(setup.getId());
            }
        },
            SshConnectionConstants.DELAY_BEFORE_RETRY);
        setup.setWaitingForRetry(true);
    }

    @Override
    public boolean isConnected(String connectionId) {
        return connectionSetups.get(connectionId).isConnected();
    }

    @Override
    public boolean isWaitingForRetry(String connectionId) {
        return connectionSetups.get(connectionId).isWaitingForRetry();
    }

    @Override
    public Session connectSession(String connectionId) {
        ThreadGuard.checkForForbiddenThread();

        String passphrase = "";
        if (connectionSetups.get(connectionId).getUsePassphrase()) {
            // Retreive passphrase from secure storage.
            passphrase = retrieveSshConnectionPassword(connectionId);
        }

        return connectSession(connectionId, passphrase);
    }

    @Override
    public Session connectSession(String connectionId, String passphrase) {
        ThreadGuard.checkForForbiddenThread();
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
        if (sshConnectionSetup.isConnected()) {
            sshConnectionSetup.disconnect();
        } else if (sshConnectionSetup.isWaitingForRetry()) {
            sshConnectionSetup.setWaitingForRetry(false);
            callbackManager.enqueueCallback(new AsyncCallback<SshConnectionListener>() {

                @Override
                public void performCallback(SshConnectionListener listener) {
                    listener.onConnectionClosed(sshConnectionSetup, false);
                }
            });
        }
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
        } else if (setup.isWaitingForRetry()) {
            setup.setWaitingForRetry(false);
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
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "removeListener") // injected by OSGi
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
                context.getPort(), context.getSshAuthUser(), context.getKeyfileLocation(), context.isUsePassphrase(),
                context.isConnectImmediately(),
                context.isAutoRetry(), listenerAdapter);

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
    @Activate
    public void activate() {

        // perform any file-based password imports
        final File importFilesDir =
            configurationService.getStandardImportDirectory(SshConnectionConstants.PASSWORD_FILE_IMPORT_SUBDIRECTORY);
        try {
            secureStorageImportService.processImportDirectory(importFilesDir,
                SshConnectionConstants.SSH_CONNECTIONS_PASSWORDS_NODE, null, null, true, true);
        } catch (OperationFailureException e) {
            log.warn(
                "Error while attempting to import SSH Remote Access connection passwords from " + importFilesDir + ": " + e.getMessage());
        }

        try {
            secureStorageSection = secureStorageService.getSecureStorageSection(SshConnectionConstants.SSH_CONNECTIONS_PASSWORDS_NODE);
        } catch (IOException e) {
            // TODO decide: how to handle this case?
            log.error("Failed to initialize secure storage");
        }

        ConcurrencyUtils.getAsyncTaskService().execute("Client-Side Remote Access: Add pre-configured SSH connections",
            () -> addAndConnectInitialSshConfigs(configurationService.getInitialSSHConnectionConfigs()));
    }

    private void addAndConnectInitialSshConfigs(List<InitialSshConnectionConfig> configs) {
        ThreadGuard.checkForForbiddenThread();
        for (InitialSshConnectionConfig config : configs) {
            SshConnectionSetup setup =
                new SshConnectionSetupImpl(config.getId(), config.getDisplayName(), config.getHost(), config.getPort(), config.getUser(),
                    config.getKeyFileLocation(), config.getUsePassphrase(), config.getConnectOnStartup(), config.getAutoRetry(),
                    defineListenerForSSHConnectionSetup());
            connectionSetups.put(config.getId(), setup);
            if (config.getConnectOnStartup()) {
                if (config.getUsePassphrase()) {
                    // log.error("Could not connect connection " + config.getDisplayName() + " on startup, it requires a passphrase.");
                    setup.connect(retrieveSshConnectionPassword(setup.getId()));
                } else {
                    setup.connect("");
                }
            }
        }
    }

    private void storeSshConnectionPassword(String connectionId, String password) {

        try {
            secureStorageSection.store(connectionId, password);
        } catch (OperationFailureException e) {
            log.error("Could not store password: " + e);
        }
    }

    private void removeSshConnectionPasswordIfExists(String connectionId) {

        try {
            secureStorageSection.delete(connectionId);
        } catch (OperationFailureException e) {
            log.error("Could not remove password: " + e);
        }
    }

    @Override
    public String retrieveSshConnectionPassword(String connectionId) {
        String passphrase = null;
        try {
            passphrase = secureStorageSection.read(connectionId, null);
        } catch (OperationFailureException e) {
            log.error("Could not retrieve password: " + e);
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
                oldSetup.getKeyfileLocation(), oldSetup.getUsePassphrase(), oldSetup.getConnectOnStartUp(), oldSetup.getAutoRetry(),
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
            } else {
                // Remove old stored password, if one exists.
                removeSshConnectionPasswordIfExists(id);
            }
        }
    }

}
