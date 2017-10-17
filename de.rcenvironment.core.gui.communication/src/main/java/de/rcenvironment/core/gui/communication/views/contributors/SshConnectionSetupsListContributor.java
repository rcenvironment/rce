/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Contributes the subtree showing the list of current SSH connections.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public class SshConnectionSetupsListContributor extends NetworkViewContributorBase {

    private static final AnchorPoints PARENT_ANCHOR = AnchorPoints.SSH_REMOTE_ACCESS_SECTION_PARENT_NODE;

    /**
     * Tree wrapper {@link SshSessionConfiguration}.
     * 
     * @author Brigitte Boden
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
                return sshConnectionSetup.isConnected();
            case EDIT:
            case DELETE:
                return !sshConnectionSetup.isConnected();
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
                        if (sshConnectionSetup.getUsePassphrase() && !sshConnectionSetup.getStorePassphrase()) {
                            EnterPassphraseDialog dialog = new EnterPassphraseDialog(treeViewer.getTree().getShell());
                            if (dialog.open() == Window.OK) {
                                sshConnectionService.setAuthPhraseForSshConnection(connectionId, dialog.getPassphrase(),
                                    dialog.getStorePassphrase());
                                sshConnectionService.connectSession(connectionId, dialog.getPassphrase());
                            }
                        } else {
                            sshConnectionService.connectSession(connectionId);
                        }
                    }
                });
                break;
            case STOP:
                sshConnectionService.disconnectSession(connectionId);
                break;
            case EDIT:
                performEdit();
                break;
            case DELETE:
                sshConnectionService.disposeConnection(connectionId);
                break;
            default:
                break;
            }
        }

        private void performEdit() {
            EditSshConnectionDialog dialog =
                new EditSshConnectionDialog(treeViewer.getTree().getShell(), sshConnectionSetup.getDisplayName(),
                    sshConnectionSetup.getHost(), sshConnectionSetup.getPort(), sshConnectionSetup.getUsername(),
                    sshConnectionSetup.getKeyfileLocation(), sshConnectionSetup.getUsePassphrase(),
                    sshConnectionSetup.getStorePassphrase(),
                    sshConnectionSetup.getConnectOnStartUp());
            if (sshConnectionSetup.getStorePassphrase()) {
                // Retrieve stored passphrase
                String passphrase = sshConnectionService.retreiveSshConnectionPassword(sshConnectionSetup.getId());
                if (passphrase != null) {
                    dialog.setPassphrase(passphrase);
                }
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
                ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                    @TaskDescription("Edit SSH Connection.")
                    @Override
                    public void run() {
                        sshConnectionService.editSshConnection(new SshConnectionContext(id, connectionName, host, port, username,
                            keyfileLocation, usePassphrase, connectImmediately));
                        sshConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                        if (connectImmediately) {
                            sshConnectionService.connectSession(id, passphrase);
                        }
                    }
                });
            }
        }

        @Override
        public String getText() {
            String status = "connected";
            if (!sshConnectionService.isConnected(connectionId)) {
                status = "disconnected";
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

    private static final int ROOT_PRIORITY = 30;

    private final Log log = LogFactory.getLog(getClass());

    private SelfRenderingNetworkViewNode rootNode;

    private final Image connectedImage;

    private final Image disconnectedImage;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private SshConnectionService sshConnectionService;

    private Collection<SshConnectionSetup> sshConnectionSetups;

    private final WeakHashMap<SshConnectionSetup, SshConnectionSetupNode> wrapperMap = new WeakHashMap<>();

    public SshConnectionSetupsListContributor() {
        super();
        connectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connectSsh.png")).createImage(); //$NON-NLS-1$
        disconnectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnectSsh.png")).createImage(); //$NON-NLS-1$

        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        sshConnectionService = serviceRegistryAccess.getService(SshConnectionService.class);

        sshConnectionSetups = sshConnectionService.getAllSshConnectionSetups();

        registerListeners();
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
            final boolean connectImmediately = dialog.getConnectImmediately();
            final String host = dialog.getHost();
            final int port = dialog.getPort();
            final String username = dialog.getUsername();
            final String passphrase = dialog.getPassphrase();
            final boolean storePassphrase = dialog.shouldStorePassPhrase();
            final String keyfileLocation = dialog.getKeyfileLocation();
            final boolean usePassphrase = dialog.getUsePassphrase();

            ConcurrencyUtils.getAsyncTaskService().execute(new Runnable() {

                @TaskDescription("Create new SSH Connection.")
                @Override
                public void run() {
                    String id =
                        sshConnectionService.addSshConnection(connectionName, host, port, username, keyfileLocation, usePassphrase,
                            connectImmediately);
                    sshConnectionService.setAuthPhraseForSshConnection(id, passphrase, storePassphrase);
                    if (connectImmediately) {
                        sshConnectionService.connectSession(id, passphrase);
                    }
                }
            });
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
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        sshConnectionSetups = connections;
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        treeViewer.refresh(PARENT_ANCHOR, false);
                        treeViewer.setExpandedState(rootNode, true);
                    }
                });
            }

            @Override
            public void onConnected(final SshConnectionSetup setup) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                        treeViewer.setExpandedState(node, true);
                    }
                });
            }

            @Override
            public void onConnectionAttemptFailed(final SshConnectionSetup setup, final String reason, boolean firstConsecutiveFailure,
                boolean willAutoRetry) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        MessageBox dialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                        dialog
                            .setMessage(StringUtils.format("SSH connection attempt to host %s on port %s failed:\n%s",
                                setup.getHost(),
                                setup.getPort(), reason));
                        dialog.open();
                    }
                });
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                    }
                });
            }

            @Override
            public void onConnectionClosed(final SshConnectionSetup setup, boolean willAutoRetry) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (treeViewer.getControl().isDisposed()) {
                            return;
                        }
                        Object node = getSetupNodeForSetup(setup);
                        treeViewer.refresh(node);
                    }
                });
            }

        };

        serviceRegistryAccess.registerService(SshConnectionListener.class, listener);
    }

    private SshConnectionSetupNode getSetupNodeForSetup(SshConnectionSetup setup) {
        return wrapperMap.get(setup);

    }

}
