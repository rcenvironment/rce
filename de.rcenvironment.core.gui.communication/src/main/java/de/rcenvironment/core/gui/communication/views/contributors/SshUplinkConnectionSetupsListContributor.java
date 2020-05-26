/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.util.Collection;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.MessageBox;

import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListener;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionListenerAdapter;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionService;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
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
 */
public class SshUplinkConnectionSetupsListContributor extends NetworkViewContributorBase {

    private static final AnchorPoints PARENT_ANCHOR = AnchorPoints.SSH_UPLINK_SECTION_PARENT_NODE;

    /**
     * Tree wrapper {@link SshSessionConfiguration}.
     * 
     * @author Brigitte Boden
     */
    private final class SshUplinkConnectionSetupNode implements SelfRenderingNetworkViewNode, StandardUserNodeActionNode {

        private final String connectionId;

        private final SshUplinkConnectionSetup sshConnectionSetup;

        SshUplinkConnectionSetupNode(String connectionId, SshUplinkConnectionSetup setup) {
            super();
            this.connectionId = connectionId;
            this.sshConnectionSetup = setup;
            wrapperMap.put(setup, this);
        }

        @Override
        public NetworkViewContributor getContributor() {
            return SshUplinkConnectionSetupsListContributor.this;
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
            default:
                return false;
            }
        }

        @Override
        public void performAction(StandardUserNodeActionType actionType) {
            switch (actionType) {
            case START:
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (sshConnectionSetup.getUsePassphrase()) {
                            String passphrase = sshUplinkConnectionService.retrieveUplinkConnectionPassword(connectionId);
                            if (passphrase == null) {
                                // If no stored passphrase is found, show dialog to the user
                                EnterPassphraseDialog dialog = new EnterPassphraseDialog(treeViewer.getTree().getShell());
                                if (dialog.open() == Window.OK) {
                                    passphrase = dialog.getPassphrase();
                                    sshUplinkConnectionService.setAuthPhraseForSshConnection(connectionId, dialog.getPassphrase(),
                                        dialog.getStorePassphrase());
                                }
                            }
                            final String passphraseToConnect = passphrase;
                            ConcurrencyUtils.getAsyncTaskService().execute("Connect SSH Uplink session.", new Runnable() {

                                @Override
                                public void run() {
                                    sshUplinkConnectionService.connectSession(connectionId, passphraseToConnect);
                                }

                            });

                        } else {
                            ConcurrencyUtils.getAsyncTaskService().execute("Connect SSH Uplink session.", new Runnable() {

                                @Override
                                public void run() {
                                    sshUplinkConnectionService.connectSession(connectionId);
                                }
                            });
                        }
                    }
                });
                break;
            case STOP:
                ConcurrencyUtils.getAsyncTaskService().execute("Disconnect SSH Uplink Connection.", new Runnable() {

                    @Override
                    public void run() {
                        sshUplinkConnectionService.disconnectSession(connectionId);
                    }

                });
                break;
            case EDIT:
                performEdit();
                break;
            case DELETE:
                ConcurrencyUtils.getAsyncTaskService().execute("Dispose SSH Uplink Connection.", new Runnable() {

                    @Override
                    public void run() {
                        sshUplinkConnectionService.disposeConnection(connectionId);
                    }

                });
                break;
            default:
                break;
            }
        }

        private void performEdit() {
            String storedPassphrase = null;
            if (sshConnectionSetup.getUsePassphrase()) {
                // Retrieve stored storedPassphrase, if one exists
                storedPassphrase = sshUplinkConnectionService.retrieveUplinkConnectionPassword(sshConnectionSetup.getId());

            }
            EditSshUplinkConnectionDialog dialog =
                new EditSshUplinkConnectionDialog(treeViewer.getTree().getShell(), sshConnectionSetup.getDisplayName(),
                    sshConnectionSetup.getHost(), sshConnectionSetup.getPort(), sshConnectionSetup.getQualifier(),
                    sshConnectionSetup.getUsername(),
                    sshConnectionSetup.getKeyfileLocation(), sshConnectionSetup.getUsePassphrase(),
                    storedPassphrase != null,
                    sshConnectionSetup.getConnectOnStartUp(),
                    sshConnectionSetup.getAutoRetry(), sshConnectionSetup.isGateway());

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
                final String qualifier = dialog.getQualifier();
                final boolean isGateway = dialog.isGateway();
                ConcurrencyUtils.getAsyncTaskService().execute("Edit SSH Connection.", new Runnable() {

                    @Override
                    public void run() {
                        sshUplinkConnectionService
                            .editSshUplinkConnection(new SshConnectionContext(id, connectionName, qualifier, host, port,
                                username, keyfileLocation, usePassphrase, connectImmediately, autoRetry, isGateway));
                        sshUplinkConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                        if (connectImmediately) {
                            sshUplinkConnectionService.connectSession(id, passphrase);
                        }
                    }
                });
            }
        }

        @Override
        public String getText() {
            String status = "connected";
            if (!sshUplinkConnectionService.isConnected(connectionId)) {
                if (sshUplinkConnectionService.isWaitingForRetry(connectionId)) {
                    status = "disconnected, waiting for retry";
                } else {
                    status = "disconnected";
                }
            }
            return StringUtils.format("%s (%s)", sshConnectionSetup.getDisplayName(), status);
        }

        @Override
        public Image getImage() {
            if (sshUplinkConnectionService.isConnected(connectionId)) {
                return connectedImage;
            }
            return disconnectedImage;
        }

        @Override
        public boolean getHasChildren() {
            return false;
        }
    }

    private static final int ROOT_PRIORITY = 30;

    private final Log log = LogFactory.getLog(getClass());

    private SelfRenderingNetworkViewNode rootNode;

    private final Image connectedImage;

    private final Image disconnectedImage;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private SshUplinkConnectionService sshUplinkConnectionService;

    private Collection<SshUplinkConnectionSetup> sshUplinkConnectionSetups;

    private final WeakHashMap<SshUplinkConnectionSetup, SshUplinkConnectionSetupNode> wrapperMap = new WeakHashMap<>();

    private NetworkViewCallback callback;

    public SshUplinkConnectionSetupsListContributor(NetworkViewCallback callback) {
        super();
        connectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connectUplink.png")).createImage(); //$NON-NLS-1$
        disconnectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnectUplink.png")).createImage(); //$NON-NLS-1$

        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        sshUplinkConnectionService = serviceRegistryAccess.getService(SshUplinkConnectionService.class);

        sshUplinkConnectionSetups = sshUplinkConnectionService.getAllSshConnectionSetups();

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
        boolean hasChildren = sshUplinkConnectionSetups != null && !sshUplinkConnectionSetups.isEmpty();
        rootNode =
            new SimpleNetworkViewNode("Uplink Connections", disconnectedImage, this,
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
        AddSshUplinkConnectionDialog dialog = new AddSshUplinkConnectionDialog(treeViewer.getTree().getShell());

        if (dialog.open() == Window.OK) {
            final String connectionName = dialog.getConnectionName();
            final boolean connectImmediately = dialog.getAutoRetry();
            final boolean autoRetry = dialog.getConnectImmediately();
            final String host = dialog.getHost();
            final int port = dialog.getPort();
            final String qualifier = dialog.getQualifier();
            final String username = dialog.getUsername();
            final String passphrase = dialog.getPassphrase();
            final boolean storePassphrase = dialog.shouldStorePassPhrase();
            final String keyfileLocation = dialog.getKeyfileLocation();
            final boolean usePassphrase = dialog.getUsePassphrase();
            final boolean isGateway = dialog.isGateway();

            //
            SshConnectionContext contextUplinkSSH = new SshConnectionContext(null, connectionName, qualifier, host, port,
                username, keyfileLocation, usePassphrase, connectImmediately, autoRetry, isGateway);

            if (sshUplinkConnectionService.sshUplinkConnectionAlreadyExists(contextUplinkSSH)) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        MessageBox errorDialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                        errorDialog.setMessage(StringUtils.format("SSH Uplink connection with host %s and port %d already exists.",
                            host, port));
                        errorDialog.open();
                    }
                });
                return;
            }
            //

            ConcurrencyUtils.getAsyncTaskService().execute("Create new SSH Uplink Connection.", new Runnable() {

                @Override
                public void run() {
                    String id =
                        sshUplinkConnectionService
                            .addSshUplinkConnection(contextUplinkSSH);
                    sshUplinkConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                    if (connectImmediately) {
                        sshUplinkConnectionService.connectSession(id, passphrase);
                    }
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
        if (sshUplinkConnectionSetups.isEmpty()) {
            return EMPTY_ARRAY;
        }
        final SshUplinkConnectionSetupNode[] nodes = new SshUplinkConnectionSetupNode[sshUplinkConnectionSetups.size()];
        int pos = 0;
        for (SshUplinkConnectionSetup setup : sshUplinkConnectionSetups) {
            SshUplinkConnectionSetupNode setupNode = new SshUplinkConnectionSetupNode(setup.getId(), setup);
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
        SshUplinkConnectionListener listener = new SshUplinkConnectionListenerAdapter() {

            @Override
            public void onCollectionChanged(final Collection<SshUplinkConnectionSetup> connections) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        sshUplinkConnectionSetups = connections;
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        treeViewer.refresh(PARENT_ANCHOR, false);
                        treeViewer.setExpandedState(rootNode, true);
                    }
                });
            }

            @Override
            public void onConnected(final SshUplinkConnectionSetup setup) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                        treeViewer.setExpandedState(node, true);
                        callback.onStateChanged(node);
                    }
                });
            }

            @Override
            public void onConnectionAttemptFailed(final SshUplinkConnectionSetup setup, final String reason,
                boolean firstConsecutiveFailure, boolean willAutoRetry) {
                // Show popup message only for first consecutive failure, not for every retry.
                if (firstConsecutiveFailure) {
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            MessageBox dialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                            String retryMessage = "\n\nWill not try to reconnect.";
                            if (willAutoRetry) {
                                retryMessage = "\n\nWill automatically try to reconnect.";
                            }
                            dialog
                                .setMessage(StringUtils.format("Uplink connection attempt to host %s on port %s failed:\n%s%s",
                                    setup.getHost(),
                                    setup.getPort(), reason, retryMessage));
                            dialog.open();
                        }
                    });
                }
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                        callback.onStateChanged(node);
                    }
                });
            }

            @Override
            public void onConnectionClosed(final SshUplinkConnectionSetup setup, boolean willAutoRetry) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                        callback.onStateChanged(node);
                    }
                });
            }

        };

        sshUplinkConnectionService.addListener(listener);
    }

    private SshUplinkConnectionSetupNode getSetupNodeForSetup(SshUplinkConnectionSetup setup) {
        return wrapperMap.get(setup);

    }

}
