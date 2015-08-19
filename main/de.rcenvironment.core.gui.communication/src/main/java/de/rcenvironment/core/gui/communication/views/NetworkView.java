/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListener;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupListenerAdapter;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * A view that shows a tree of all known network nodes and connections.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Oliver Seebach
 */
public class NetworkView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.core.gui.communication.views.NetworkView";

    private static final ImageDescriptor ADD =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/newConnection.png"));

    private static final ImageDescriptor EDIT =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/edit16.gif"));

    private static final ImageDescriptor START =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/run_enabled.gif"));

    private static final ImageDescriptor STOP =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/cancel_enabled.gif"));

    private static final ImageDescriptor DELETE = ImageManager.getInstance().getImageDescriptor(StandardImages.DELETE_16);

    private Display display;

    private TreeViewer viewer;

    private Action toggleNodeIdsVisibleAction;

    private Action toggleRawNodePropertiesVisibleAction;

    private Action addNetworkConnectionAction;

    private Action editNetworkConnectionAction;

    private Action deleteNetworkConnectionAction;

    private Action startNetworkConnectionAction;

    private Action stopNetworkConnectionAction;

    private NetworkViewContentProvider contentProvider;

    private NetworkViewLabelProvider labelProvider;

    private boolean rawPropertiesVisible;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private ConnectionSetupService connectionSetupService;

    private final Log log = LogFactory.getLog(getClass());

    private NetworkViewModel model;

    public NetworkView() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

        labelProvider = new NetworkViewLabelProvider();
        labelProvider.setNodeIdsVisible(false);
        contentProvider = new NetworkViewContentProvider();
        contentProvider.setRawPropertiesVisible(false);

        connectionSetupService = serviceRegistryAccess.getService(ConnectionSetupService.class);

        createActions();

    }

    private void createActions() {
        // add "show nodeIds" option
        // TODO add icon
        toggleNodeIdsVisibleAction = new Action("Show Node Identifiers", SWT.TOGGLE) {

            @Override
            public void run() {
                labelProvider.setNodeIdsVisible(this.isChecked());
                // refresh all labels
                viewer.refresh(NetworkViewContentProvider.NETWORK_ROOT_NODE, true);
            }
        };
        // add "show raw node properties" option
        // TODO add icon
        toggleRawNodePropertiesVisibleAction = new Action("Show Raw Node Properties", SWT.TOGGLE) {

            @Override
            public void run() {
                rawPropertiesVisible = this.isChecked();
                contentProvider.setRawPropertiesVisible(rawPropertiesVisible);
                viewer.refresh(NetworkViewContentProvider.NETWORK_ROOT_NODE, false);
            }
        };

        // add "add network connection" option
        addNetworkConnectionAction = new Action("Add Network Connection...", ADD) {

            @Override
            public void run() {

                AddNetworkConnectionDialog dialog = new AddNetworkConnectionDialog(viewer.getTree().getShell());

                if (dialog.open() == Window.OK) {

                    String connectionName = dialog.getConnectionName();
                    boolean connectImmediately = dialog.getConnectImmediately();
                    NetworkContactPoint ncp = dialog.getParsedNetworkContactPoint();

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
        };

        // add "edit network connection" option
        editNetworkConnectionAction = new Action("Edit Network Connection...", EDIT) {

            @Override
            public void run() {

                ConnectionSetup setup = getSelectedSetup();

                if (setup != null) {
                    String connectionName = setup.getDisplayName();
                    String networkContactString = setup.getNetworkContactPointString();
                    EditNetworkConnectionDialog dialog =
                        new EditNetworkConnectionDialog(viewer.getTree().getShell(), connectionName, networkContactString);
                    if (dialog.open() == Window.OK) {

                        String newConnectionName = dialog.getConnectionName();
                        boolean newConnectImmediately = dialog.getConnectImmediately();
                        NetworkContactPoint ncp = dialog.getParsedNetworkContactPoint();

                        connectionSetupService.disposeConnectionSetup(setup);

                        if (ncp != null) {
                            ConnectionSetup newNetworkConnection = connectionSetupService.createConnectionSetup(
                                ncp, newConnectionName, true);
                            if (newConnectImmediately) {
                                newNetworkConnection.signalStartIntent();
                            }
                        }
                    }
                }
            }
        };

        // add "delete network connection" option
        deleteNetworkConnectionAction = new Action("Delete Network Connection...", DELETE) {

            @Override
            public void run() {
                ConnectionSetup setup = getSelectedSetup();

                if (setup != null) {
                    String connectionName = setup.getDisplayName();
                    String networkContactString = setup.getNetworkContactPointString();

                    MessageBox dialog = new MessageBox(viewer.getTree().getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
                    dialog.setText("Delete Connection");
                    dialog.setMessage("Do you really want to delete connection \"" + connectionName
                        + "\" to contact point \"" + networkContactString + "\" ?");

                    if (dialog.open() == SWT.OK) {
                        connectionSetupService.disposeConnectionSetup(setup);
                    }
                }
            }
        };

        // add "start network connection" option
        startNetworkConnectionAction = new Action("Start/Connect", START) {

            @Override
            public void run() {
                ConnectionSetup setup = getSelectedSetup();
                if (setup != null) {
                    setup.signalStartIntent();
                }
            }
        };

        // add "stop network connection" option
        stopNetworkConnectionAction = new Action("Stop/Disconnect", STOP) {

            @Override
            public void run() {
                ConnectionSetup setup = getSelectedSetup();
                if (setup != null) {
                    setup.signalStopIntent();
                }
            }
        };
    }

    @Override
    public void createPartControl(Composite parent) {
        // store display reference for asyncExec calls
        display = parent.getShell().getDisplay();

        viewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(labelProvider);
        // viewer.setSorter(new NetworkViewSorter());

        NetworkGraph initialGraph = serviceRegistryAccess.getService(NetworkRoutingService.class).getReachableNetworkGraph();
        Collection<ConnectionSetup> initialConnectionSetups =
            serviceRegistryAccess.getService(ConnectionSetupService.class).getAllConnectionSetups();
        model = new NetworkViewModel(initialGraph, null, initialConnectionSetups, new HashMap<NodeIdentifier, Map<String, String>>());

        viewer.setInput(model);
        viewer.expandToLevel(2);

        // Update context menu on open
        viewer.getTree().addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent event) {
                enablePossibleActionsForSelection(getSelectedSetup());
            }
        });

        hookContextMenu();
        addToolbarActions();
        registerChangeListeners();
        registerSelectionListeners();

    }

    @Override
    public void dispose() {
        serviceRegistryAccess.dispose();
        super.dispose();
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private void registerSelectionListeners() {

        disableAllActions();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {

                ConnectionSetup setup = getSelectedSetup();
                if (setup != null) {
                    enablePossibleActionsForSelection(setup);
                } else {
                    disableAllActions();
                }
            }
        });
    }

    private void disableAllActions() {
        editNetworkConnectionAction.setEnabled(false);
        deleteNetworkConnectionAction.setEnabled(false);
        startNetworkConnectionAction.setEnabled(false);
        stopNetworkConnectionAction.setEnabled(false);
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     * 
     * @param display
     */
    private void registerChangeListeners() {
        serviceRegistryAccess.registerService(NetworkTopologyChangeListener.class, new NetworkTopologyChangeListenerAdapter() {

            @Override
            public void onReachableNetworkChanged(final NetworkGraph networkGraph) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        model.networkGraph = networkGraph;
                        model.updateGraphWithProperties();
                        if (viewer.getControl().isDisposed()) {
                            return;
                        }
                        // update the tree, but no need to update existing labels
                        viewer.refresh(NetworkViewContentProvider.NETWORK_ROOT_NODE, false);
                    }
                });
            }

        });
        serviceRegistryAccess.registerService(NodePropertiesChangeListener.class, new NodePropertiesChangeListenerAdapter() {

            @Override
            public void onNodePropertyMapsOfNodesChanged(final Map<NodeIdentifier, Map<String, String>> updatedPropertyMaps) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        model.nodeProperties.putAll(updatedPropertyMaps); // inner maps are
                                                                          // immutable
                        model.updateGraphWithProperties();
                        if (viewer.getControl().isDisposed()) {
                            return;
                        }
                        viewer.refresh(NetworkViewContentProvider.NETWORK_ROOT_NODE);
                    }
                });
            }
        });
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class, new DistributedComponentKnowledgeListener() {

            @Override
            public void onDistributedComponentKnowledgeChanged(final DistributedComponentKnowledge newKnowledge) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        model.componentKnowledge = newKnowledge;
                        if (viewer.getControl().isDisposed()) {
                            return;
                        }
                        viewer.refresh(NetworkViewContentProvider.NETWORK_ROOT_NODE);
                    }
                });
            }
        });
        serviceRegistryAccess.registerService(ConnectionSetupListener.class, new ConnectionSetupListenerAdapter() {

            @Override
            public void onCollectionChanged(final Collection<ConnectionSetup> setups) {
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        model.connectionSetups = setups;
                        if (viewer.getControl().isDisposed()) {
                            return;
                        }
                        // no need to update all labels
                        viewer.refresh(NetworkViewContentProvider.CONNECTIONS_ROOT_NODE, false);
                        for (TreeItem it : viewer.getTree().getItems()) {
                            if (it.getText().equals("Connections")) {
                                it.setExpanded(true);
                                viewer.refresh();
                                if (it.getItemCount() > 0) {
                                    viewer.getTree().setSelection(it.getItem(0));
                                    viewer.setSelection(viewer.getSelection());
                                }
                            }
                        }

                    }
                });
            }

            @Override
            public void onStateChanged(final ConnectionSetup setup, final ConnectionSetupState oldState, ConnectionSetupState newState) {
                // trigger GUI refresh
                display.asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        if (viewer.getControl().isDisposed()) {
                            return;
                        }
                        // only update this specific label
                        enablePossibleActionsForSelection(setup);
                        viewer.update(setup, null);
                    }
                });
            }
        });
    }

    private void enablePossibleActionsForSelection(ConnectionSetup setup) {
        if (setup != null) {
            ConnectionSetupState state = setup.getState();
            startNetworkConnectionAction.setEnabled(state.isReasonableToAllowStart());
            stopNetworkConnectionAction.setEnabled(state.isReasonableToAllowStop());
            boolean isDisconnected = state == ConnectionSetupState.DISCONNECTED;
            editNetworkConnectionAction.setEnabled(isDisconnected);
            deleteNetworkConnectionAction.setEnabled(isDisconnected);
        } else {
            disableAllActions();
        }
    }

    private void addToolbarActions() {

        // add toolbar actions (right top of view)
        getViewSite().getActionBars().getToolBarManager().add(addNetworkConnectionAction);
        getViewSite().getActionBars().getToolBarManager().add(startNetworkConnectionAction);
        getViewSite().getActionBars().getToolBarManager().add(stopNetworkConnectionAction);
        getViewSite().getActionBars().getToolBarManager().add(editNetworkConnectionAction);
        getViewSite().getActionBars().getToolBarManager().add(deleteNetworkConnectionAction);
    }

    private void hookContextMenu() {

        // submenu
        MenuManager subMenuManager = new MenuManager("Advanced");
        subMenuManager.add(toggleNodeIdsVisibleAction);
        subMenuManager.add(toggleRawNodePropertiesVisibleAction);

        MenuManager menuManager = new MenuManager();
        menuManager.add(addNetworkConnectionAction);
        menuManager.add(new Separator());
        menuManager.add(startNetworkConnectionAction);
        menuManager.add(stopNetworkConnectionAction);
        menuManager.add(editNetworkConnectionAction);
        menuManager.add(deleteNetworkConnectionAction);
        menuManager.add(new Separator());
        menuManager.add(subMenuManager);
        Menu menu = menuManager.createContextMenu(viewer.getTree());
        viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);
        getSite().setSelectionProvider(viewer);
    }

    private ConnectionSetup getSelectedSetup() {
        ConnectionSetup setup = null;
        if (viewer.getSelection() != null) {
            if (viewer.getSelection() instanceof TreeSelection) {
                TreeSelection selection = ((TreeSelection) viewer.getSelection());
                if (selection.getFirstElement() instanceof ConnectionSetup) {
                    setup = (ConnectionSetup) selection.getFirstElement();
                }
            }
        }
        return setup;
    }
}
