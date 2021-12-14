/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.MessageBox;

import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.gui.communication.views.internal.AnchorPoints;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.SimpleNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.SelfRenderingNetworkViewNode;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionNode;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionType;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Contributes the subtree showing the list of current {@link ConnectionSetup}s.
 * 
 * @author Robert Mischke
 * @author Kathrin Schaffert (#16977)
 * @author Jan Flink
 */
public class ConnectionSetupsListContributor extends NetworkViewContributorBase {

    private static final AnchorPoints PARENT_ANCHOR = AnchorPoints.MAIN_NETWORK_SECTION_PARENT_NODE;

    /**
     * Tree wrapper for a {@link ConnectionSetup}.
     * 
     * @author Robert Mischke
     * @author Kathrin Schaffert (#16977 added case SHOW_CONFIGURATION_SNIPPET)
     */
    private final class ConnectionSetupNode implements SelfRenderingNetworkViewNode, StandardUserNodeActionNode {

        private static final int INT_1000 = 1000;
        private final ConnectionSetup connectionSetup;

        ConnectionSetupNode(ConnectionSetup connectionSetup) {
            // consistency check
            if (connectionSetup == null) {
                throw new NullPointerException();
            }
            this.connectionSetup = connectionSetup;
        }

        @Override
        public NetworkViewContributor getContributor() {
            return ConnectionSetupsListContributor.this;
        }

        @Override
        public String getText() {
            String subState = "";
            ConnectionSetupState connectionState = connectionSetup.getState();
            DisconnectReason disconnectReason = connectionSetup.getDisconnectReason();
            if ((connectionState == ConnectionSetupState.DISCONNECTED || connectionState == ConnectionSetupState.DISCONNECTING)
                && (disconnectReason != null)) {
                subState = ": " + disconnectReason.getDisplayText();
            }
            return StringUtils.format("%s (%s%s)", connectionSetup.getDisplayName(), connectionState.getDisplayText(), subState);
        }

        @Override
        public Image getImage() {
            if (connectionSetup.getState() == ConnectionSetupState.CONNECTED) {
                return connectedImage;
            } else {
                return disconnectedImage;
            }
        }

        @Override
        public boolean getHasChildren() {
            return false;
        }

        @Override
        public boolean isActionApplicable(StandardUserNodeActionType actionType) {
            final ConnectionSetupState state = connectionSetup.getState();
            switch (actionType) {
            case START:
                return state.isReasonableToAllowStart();
            case STOP:
                return state.isReasonableToAllowStop();
            case EDIT:
            case DELETE:
                return state == ConnectionSetupState.DISCONNECTED;
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
                connectionSetup.signalStartIntent();
                break;
            case STOP:
                connectionSetup.signalStopIntent();
                break;
            case EDIT:
                performEdit();
                break;
            case DELETE:
                performDelete();
                break;
            case SHOW_CONFIGURATION_SNIPPET:
                performShowConfigurationSnippet();
                break;
            default:
                break;
            }
        }

        private void performEdit() {
            String connectionName = connectionSetup.getDisplayName();
            String networkContactString = connectionSetup.getNetworkContactPointString();
            EditNetworkConnectionDialog dialog =
                new EditNetworkConnectionDialog(treeViewer.getTree().getShell(), connectionName, networkContactString);
            if (dialog.open() == Window.OK) {
                String newConnectionName = dialog.getConnectionName();
                boolean newConnectImmediately = dialog.getConnectImmediately();
                NetworkContactPoint ncp = dialog.getParsedNetworkContactPoint();

                connectionSetupService.disposeConnectionSetup(connectionSetup);

                if (ncp != null) {
                    ConnectionSetup newNetworkConnection = connectionSetupService.createConnectionSetup(
                        ncp, newConnectionName, true);
                    if (newNetworkConnection != null) {
                        if (newConnectImmediately) {
                            newNetworkConnection.signalStartIntent();
                        }
                    } else {
                        display.asyncExec(() -> {
                            MessageBox errorDialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                            errorDialog.setMessage(StringUtils.format("SSH connection with host '%s' and port '%d' already exists.",
                                ncp.getHost(), ncp.getPort()));
                            errorDialog.open();
                        });
                    }
                }
            }
        }

        private void performDelete() {
            String connectionName = connectionSetup.getDisplayName();
            String networkContactString = connectionSetup.getNetworkContactPointString();

            MessageBox dialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
            dialog.setText("Delete Connection");
            dialog.setMessage("Do you really want to delete connection \"" + connectionName
                + "\" to contact point \"" + networkContactString + "\" ?");
            if (dialog.open() == SWT.OK) {
                connectionSetupService.disposeConnectionSetup(connectionSetup);
            }
        }

        private void performShowConfigurationSnippet() {
            display.asyncExec(() -> {
                ConfigurationSnippetDialog showConfigurationSnippetDialog =
                    new ConfigurationSnippetDialog(treeViewer.getTree().getShell(), "network", "connections",
                        connectionSetup.getDisplayName(), getConfigurationEntries());
                showConfigurationSnippetDialog.open();
            });
        }

        private Map<String, Object> getConfigurationEntries() {

            Map<String, Object> configurationEntries = new LinkedHashMap<>();

            configurationEntries.put("host", connectionSetup.getContactPointHost());
            configurationEntries.put("port", connectionSetup.getContactPointPort());
            configurationEntries.put("connectOnStartup", connectionSetup.getConnectOnStartup());
            configurationEntries.put("autoRetryInitialDelay", connectionSetup.getAutoRetryInitialDelayMsec() / INT_1000);
            configurationEntries.put("autoRetryMaximumDelay", connectionSetup.getAutoRetryMaximumDelayMsec() / INT_1000);
            configurationEntries.put("autoRetryDelayMultiplier", connectionSetup.getAutoRetryDelayMultiplier());

            return configurationEntries;
        }
    }

    private static final int ROOT_PRIORITY = 20;

    private final Image connectedImage;

    private final Image disconnectedImage;

    private SelfRenderingNetworkViewNode rootNode;

    private final WeakHashMap<ConnectionSetup, ConnectionSetupNode> wrapperMap = new WeakHashMap<>();

    private ConnectionSetupService connectionSetupService;

    public ConnectionSetupsListContributor() {
        connectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/connect.png")).createImage(); //$NON-NLS-1$
        disconnectedImage = ImageDescriptor.createFromURL(
            getClass().getResource("/resources/icons/disconnect.png")).createImage(); //$NON-NLS-1$

        connectionSetupService = ServiceRegistry.createAccessFor(this).getService(ConnectionSetupService.class);
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
        boolean hasConnections = currentModel.connectionSetups != null && !currentModel.connectionSetups.isEmpty();
        rootNode = new SimpleNetworkViewNode("Connections", disconnectedImage, this, hasConnections);
        return new Object[] { rootNode };
    }

    @Override
    public int getInstanceDataElementsPriority() {
        return 0; // disabled
    }

    @Override
    public Object[] getChildrenForNetworkInstanceNode(NetworkGraphNodeWithContext parentNode) {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean hasChildren(Object parentNode) {
        // all nodes are self-rendering
        throw newUnexpectedCallException();
    }

    @Override
    public Object[] getChildren(Object node) {
        if (node != rootNode) {
            throw newUnexpectedCallException();
        }
        if (currentModel.connectionSetups == null) {
            return EMPTY_ARRAY;
        }
        // get and sort actual setups
        final ConnectionSetup[] setups = currentModel.connectionSetups.toArray(new ConnectionSetup[currentModel.connectionSetups.size()]);
        Arrays.sort(setups, (ConnectionSetup o1, ConnectionSetup o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
        
        // wrap into node class
        final ConnectionSetupNode[] nodes = new ConnectionSetupNode[setups.length];
        int pos = 0;
        for (ConnectionSetup setup : setups) {
            // TODO check: use wrapperMap as cache here?
            ConnectionSetupNode wrapper = new ConnectionSetupNode(setup);
            wrapperMap.put(setup, wrapper);
            nodes[pos++] = wrapper;
        }
        // return wrappers
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
        // self-created (not shared) images, so dispose them
        connectedImage.dispose();
        disconnectedImage.dispose();
    }

    /**
     * A custom method as we don't strictly separate the main UI code from the contributors.
     */
    public void showAddConnectionDialog() {
        AddNetworkConnectionDialog dialog = new AddNetworkConnectionDialog(treeViewer.getTree().getShell());

        if (dialog.open() == Window.OK) {
            String connectionName = dialog.getConnectionName();
            boolean connectImmediately = dialog.getConnectImmediately();
            NetworkContactPoint ncp = dialog.getParsedNetworkContactPoint();

            //
            if (connectionSetupService.connectionAlreadyExists(ncp)) {
                display.asyncExec(() -> {
                    MessageBox errorDialog = new MessageBox(treeViewer.getTree().getShell(), SWT.ICON_ERROR | SWT.OK);
                    errorDialog.setMessage(
                        StringUtils.format("Connection with host %s and port %d already exists.", ncp.getHost(), ncp.getPort()));
                    errorDialog.open();
                });
                return;
            }
            //

            if (ncp != null) {
                if (connectionName.isEmpty()) {
                    connectionName = ncp.getHost() + ":" + ncp.getPort();
                }
                ConnectionSetup cs = connectionSetupService.createConnectionSetup(ncp, connectionName, true);
                if (connectImmediately) {
                    cs.signalStartIntent();
                }
            }
        }

    }

    /**
     * Returns the wrapper used to represent the given {@link ConnectionSetup} in the GUI tree.
     * 
     * @param setup the {@link ConnectionSetup} to look for
     * @return the wrapper tree element
     */
    public Object getTreeNodeForSetup(ConnectionSetup setup) {
        return wrapperMap.get(setup);
    }

    /**
     * @return the tree element to refresh when the list of {@link ConnectionSetup}s changes
     */
    public Object getFullRefreshRootElement() {
        // For some reason, refreshing the root element does not work.
        return PARENT_ANCHOR;
    }

    /**
     * @return the tree element to expand when the list of {@link ConnectionSetup}s changes
     */
    public Object getRootElementToExpand() {
        return rootNode;
    }
}
