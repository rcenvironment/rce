/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.MessageBox;

import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListener;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionListenerAdapter;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.gui.communication.views.internal.AnchorPoints;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.SimpleNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionNode;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionType;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;

/**
 * Contributes the subtree showing the list of current SSH connections.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 * @author Kathrin Schaffert (#16977)
 * @author Jan Flink
 */
public class SshConnectionSetupsListContributor extends NetworkViewContributorBase {

    private static final AnchorPoints PARENT_ANCHOR = AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE;

    /**
     * Tree wrapper {@link SshSessionConfiguration}.
     * 
     * @author Brigitte Boden
     * @author Kathrin Schaffert (#16977 added case SHOW_CONFIGURATION_SNIPPET)
     */
    private final class SshConnectionSetupNode implements SelfRenderingNetworkViewNode, StandardUserNodeActionNode {

        private final String connectionId;

        private final SshConnectionSetup sshConnectionSetup;

        SshConnectionSetupNode(String connectionId, SshConnectionSetup setup) {
            super();
            this.connectionId = connectionId;
            this.sshConnectionSetup = setup;
            wrapperMap.put(setup, this);
        }

        @Override
        public NetworkViewContributor getContributor() {
            return SshConnectionSetupsListContributor.this;
        }

        @Override
        public boolean isActionApplicable(StandardUserNodeActionType actionType) {
            switch (actionType) {
            case START:
                return !sshConnectionSetup.isConnected();
            case STOP:
                return sshConnectionSetup.isConnected() || sshConnectionSetup.isWaitingForRetry();
            case EDIT:
                return !(sshConnectionSetup.isConnected() || sshConnectionSetup.isWaitingForRetry());
            case DELETE:
                return !(sshConnectionSetup.isConnected() || sshConnectionSetup.isWaitingForRetry());
            case SHOW_CONFIGURATION_SNIPPET:
                return true;
            default:
                return false;
            }
        }

        @Override
        public void performAction(StandardUserNodeActionType actionType) {
            switch (actionType) {
            case START:
                display.asyncExec(() -> {
                    if (sshConnectionSetup.getUsePassphrase()) {
                        String passphrase = sshConnectionService.retrieveSshConnectionPassword(connectionId);
                        if (passphrase == null || passphrase.isEmpty()) {
                            // If no stored passphrase is found, show dialog to the user
                            EnterPassphraseDialog dialog = new EnterPassphraseDialog(treeViewer.getTree().getShell());
                            if (dialog.open() == Window.OK) {
                                passphrase = dialog.getPassphrase();
                                sshConnectionService.setAuthPhraseForSshConnection(connectionId, dialog.getPassphrase(),
                                    dialog.getStorePassphrase());
                            }
                        }
                        final String passphraseToConnect = passphrase;
                        ConcurrencyUtils.getAsyncTaskService().execute("Connect SSH session.",
                            () -> sshConnectionService.connectSession(connectionId, passphraseToConnect));

                    } else {
                        ConcurrencyUtils.getAsyncTaskService().execute("Connect SSH session.",
                            () -> sshConnectionService.connectSession(connectionId));

                    }
                });
                break;
            case STOP:
                ConcurrencyUtils.getAsyncTaskService().execute("Disconnect SSH Connection.",
                    () -> sshConnectionService.disconnectSession(connectionId));

                break;
            case EDIT:
                performEdit();
                break;
            case DELETE:
                ConcurrencyUtils.getAsyncTaskService().execute("Dispose SSH Connection.",
                    () -> sshConnectionService.disposeConnection(connectionId));

                break;
            case SHOW_CONFIGURATION_SNIPPET:
                performShowConfigurationSnippet();
                break;
            default:
                break;
            }
        }

        private void performEdit() {
            String storedPassphrase = null;
            if (sshConnectionSetup.getUsePassphrase()) {
                // Retrieve stored passphrase, if it exists
                storedPassphrase = sshConnectionService.retrieveSshConnectionPassword(sshConnectionSetup.getId());

            }
            EditSshConnectionDialog dialog =
                new EditSshConnectionDialog(treeViewer.getTree().getShell(), sshConnectionSetup.getDisplayName(),
                    sshConnectionSetup.getHost(), sshConnectionSetup.getPort(), sshConnectionSetup.getUsername(),
                    sshConnectionSetup.getKeyfileLocation(), sshConnectionSetup.getUsePassphrase(),
                    storedPassphrase != null,
                    sshConnectionSetup.getConnectOnStartUp(),
                    sshConnectionSetup.getAutoRetry());
            if (storedPassphrase != null) {
                dialog.setPassphrase(storedPassphrase);
            }

            final String id = sshConnectionSetup.getId();

            if (dialog.open() == Window.OK) {
                final String connectionName = dialog.getConnectionName();
                final boolean connectImmediately = dialog.getConnectImmediately();
                final String host = dialog.getHost();
                final int port = dialog.getPort();
                final String username = dialog.getUsername();
                final String passphrase = dialog.getPassphrase();
                final boolean storePassphrase = dialog.shouldStorePassPhrase();
                final String keyfileLocation = dialog.getKeyfileLocation();
                final boolean usePassphrase = dialog.getUsePassphrase();
                final boolean autoRetry = dialog.getAutoRetry();
                ConcurrencyUtils.getAsyncTaskService().execute("Edit SSH Connection.", () -> {
                    sshConnectionService.editSshConnection(new SshConnectionContext(id, connectionName, "", host, port, username,
                        keyfileLocation, usePassphrase, connectImmediately, autoRetry, false));
                    sshConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                    if (connectImmediately) {
                        sshConnectionService.connectSession(id, passphrase);
                    }
                });
            }
        }

        private void performShowConfigurationSnippet() {
            display.asyncExec(() -> {
                ConfigurationSnippetDialog showConfigurationSnippetDialog =
                    new ConfigurationSnippetDialog(treeViewer.getTree().getShell(), "sshRemoteAccess", "sshConnections",
                        "MySSHConnectionID",
                        getConfigurationEntries());
                showConfigurationSnippetDialog.open();
            });
        }

        private Map<String, Object> getConfigurationEntries() {

            Map<String, Object> configurationEntries = new LinkedHashMap<>();

            configurationEntries.put("displayName", sshConnectionSetup.getDisplayName());
            configurationEntries.put("host", sshConnectionSetup.getHost());
            configurationEntries.put("port", sshConnectionSetup.getPort());
            configurationEntries.put("connectOnStartup", sshConnectionSetup.getConnectOnStartUp());
            configurationEntries.put("autoRetry", sshConnectionSetup.getAutoRetry());
            configurationEntries.put("loginName", sshConnectionSetup.getUsername());
            if (sshConnectionSetup.getKeyfileLocation() != null) {
                configurationEntries.put("keyfileLocation", sshConnectionSetup.getKeyfileLocation());
            }
            if (!sshConnectionSetup.getUsePassphrase()) {
                configurationEntries.put("noPassphrase", Boolean.TRUE);
            }

            return configurationEntries;
        }

        @Override
        public String getText() {
            String status = "connected";
            if (!sshConnectionService.isConnected(connectionId)) {
                if (sshConnectionService.isWaitingForRetry(connectionId)) {
                    status = "disconnected, waiting for retry";
                } else {
                    status = "disconnected";
                }
            }
            return StringUtils.format("%s (%s)", sshConnectionSetup.getDisplayName(), status);
        }

        @Override
        public Image getImage() {
            if (sshConnectionService.isConnected(connectionId)) {
                return connectedImage;
            }
            return disconnectedImage;
        }

        @Override
        public boolean getHasChildren() {
            return false;
        }
    }

    private static final int ROOT_PRIORITY = 40;

    private SelfRenderingNetworkViewNode rootNode;

    private final Image connectedImage;

    private final Image disconnectedImage;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private SshConnectionService sshConnectionService;

    private Collection<SshConnectionSetup> sshConnectionSetups;

    private final WeakHashMap<SshConnectionSetup, SshConnectionSetupNode> wrapperMap = new WeakHashMap<>();

    private NetworkViewCallback callback;

    public SshConnectionSetupsListContributor(NetworkViewCallback callback) {
        super();
        connectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connectSsh.png")).createImage(); //$NON-NLS-1$
        disconnectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnectSsh.png")).createImage(); //$NON-NLS-1$

        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        sshConnectionService = serviceRegistryAccess.getService(SshConnectionService.class);

        sshConnectionSetups = sshConnectionService.getAllSshConnectionSetups();

        registerListeners();

        this.callback = callback;
    }

    @Override
    public int getRootElementsPriority() {
        return ROOT_PRIORITY;
    }

    @Override
    public Object[] getTopLevelElements(Object parentNode) {
        if (parentNode != PARENT_ANCHOR) {
            return null;
        }
        boolean hasChildren = sshConnectionSetups != null && !sshConnectionSetups.isEmpty();
        rootNode =
            new SimpleNetworkViewNode("SSH Remote Access Connections", disconnectedImage, this,
                hasChildren);
        return new Object[] { rootNode };
    }

    @Override
    public int getInstanceDataElementsPriority() {
        return 0;
    }

    /**
     * A custom method as we don't strictly separate the main UI code from the contributors.
     */
    public void showAddConnectionDialog() {
        AddSshConnectionDialog dialog = new AddSshConnectionDialog(treeViewer.getTree().getShell());

        if (dialog.open() == Window.OK) {
            final String connectionName = dialog.getConnectionName();
            final boolean connectImmediately = dialog.getAutoRetry();
            final boolean autoRetry = dialog.getConnectImmediately();
            final String host = dialog.getHost();
            final int port = dialog.getPort();
            final String username = dialog.getUsername();
            final String passphrase = dialog.getPassphrase();
            final boolean storePassphrase = dialog.shouldStorePassPhrase();
            final String keyfileLocation = dialog.getKeyfileLocation();
            final boolean usePassphrase = dialog.getUsePassphrase();

            //
            SshConnectionContext contextSsh = new SshConnectionContext(null, connectionName, keyfileLocation, host, port, username,
                keyfileLocation, usePassphrase, connectImmediately, autoRetry, usePassphrase);

            if (sshConnectionService.sshConnectionAlreadyExists(contextSsh)) {
                display.asyncExec(() -> {
                    MessageBox errorDialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                    errorDialog.setMessage(StringUtils.format("SSH connection with host %s and port %d already exists.",
                        host, port));
                    errorDialog.open();
                });
                return;
            }
            //

            ConcurrencyUtils.getAsyncTaskService().execute("Create new SSH Connection.", () -> {
                // add contextSsh here instead of new SshConnectionContext
                String id = sshConnectionService.addSshConnection(contextSsh);
                sshConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                if (connectImmediately) {
                    sshConnectionService.connectSession(id, passphrase);
                }
            });
            //

        }
    }

    @Override
    public Object[] getChildrenForNetworkInstanceNode(NetworkGraphNodeWithContext instanceNode) {
        throw newUnexpectedCallException();
    }

    @Override
    public boolean hasChildren(Object parentNode) {
        throw newUnexpectedCallException();
    }

    @Override
    public Object[] getChildren(Object node) {
        if (node != rootNode) {
            throw newUnexpectedCallException();
        }
        if (sshConnectionSetups.isEmpty()) {
            return EMPTY_ARRAY;
        }
        final SshConnectionSetupNode[] nodes = new SshConnectionSetupNode[sshConnectionSetups.size()];
        int pos = 0;
        for (SshConnectionSetup setup : sshConnectionSetups) {
            SshConnectionSetupNode setupNode = new SshConnectionSetupNode(setup.getId(), setup);
            nodes[pos++] = setupNode;
        }

        return nodes;
    }

    @Override
    public Object getParent(Object node) {
        if (node == rootNode) {
            return PARENT_ANCHOR;
        } else {
            return rootNode;
        }
    }

    @Override
    public String getText(Object node) {
        throw newUnexpectedCallException();
    }

    @Override
    public Image getImage(Object node) {
        throw newUnexpectedCallException();
    }

    @Override
    public void dispose() {
        connectedImage.dispose();
        disconnectedImage.dispose();
        serviceRegistryAccess.dispose();
    }

    private void registerListeners() {
        SshConnectionListener listener = new SshConnectionListenerAdapter() {

            @Override
            public void onCollectionChanged(final Collection<SshConnectionSetup> connections) {
                display.asyncExec(() -> {
                    sshConnectionSetups = connections;
                    if (treeViewer.getControl().isDisposed()) {
                        return;
                    }
                    treeViewer.refresh(PARENT_ANCHOR, false);
                    treeViewer.setExpandedState(rootNode, true);
                });
            }

            @Override
            public void onConnected(final SshConnectionSetup setup) {
                display.asyncExec(() -> {
                    if (treeViewer.getControl().isDisposed()) {
                        return;
                    }
                    Object node = getSetupNodeForSetup(setup);
                    treeViewer.refresh(node);
                    treeViewer.setExpandedState(node, true);
                    callback.onStateChanged(node);
                });
            }

            @Override
            public void onConnectionAttemptFailed(final SshConnectionSetup setup, final String reason, boolean firstConsecutiveFailure,
                boolean willAutoRetry) {
                // Show popup message only for first consecutive failure, not for every retry.
                if (firstConsecutiveFailure) {
                    display.asyncExec(() -> {
                        MessageBox dialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                        String retryMessage = "\n\nWill not try to reconnect.";
                        if (willAutoRetry) {
                            retryMessage = "\n\nWill automatically try to reconnect.";
                        }
                        dialog
                            .setMessage(StringUtils.format("SSH connection attempt to host %s on port %s failed:\n%s%s",
                                setup.getHost(),
                                setup.getPort(), reason, retryMessage));
                        dialog.open();
                    });
                }
                display.asyncExec(() -> {
                    if (treeViewer.getControl().isDisposed()) {
                        return;
                    }
                    Object node = getSetupNodeForSetup(setup);
                    treeViewer.refresh(node);
                    callback.onStateChanged(node);
                });
            }

            @Override
            public void onConnectionClosed(final SshConnectionSetup setup, boolean willAutoRetry) {
                display.asyncExec(() -> {
                    if (treeViewer.getControl().isDisposed()) {
                        return;
                    }
                    Object node = getSetupNodeForSetup(setup);
                    treeViewer.refresh(node);
                    callback.onStateChanged(node);
                });
            }

        };

        serviceRegistryAccess.registerService(SshConnectionListener.class, listener);
    }

    private SshConnectionSetupNode getSetupNodeForSetup(SshConnectionSetup setup) {
        return wrapperMap.get(setup);

    }

}
