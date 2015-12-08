/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.communication.views;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
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
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListener;
import de.rcenvironment.core.communication.nodeproperties.spi.NodePropertiesChangeListenerAdapter;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListenerAdapter;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.gui.communication.views.contributors.ConnectionSetupsListContributor;
import de.rcenvironment.core.gui.communication.views.contributors.InstanceComponentsInfoContributor;
import de.rcenvironment.core.gui.communication.views.contributors.MonitoringDataContributor;
import de.rcenvironment.core.gui.communication.views.contributors.SshConnectionSetupsListContributor;
import de.rcenvironment.core.gui.communication.views.internal.AnchorPoints;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext;
import de.rcenvironment.core.gui.communication.views.model.NetworkGraphNodeWithContext.Context;
import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionNode;
import de.rcenvironment.core.gui.communication.views.spi.StandardUserNodeActionType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataPollingManager;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshot;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * A view that shows a tree of all known network nodes and connections.
 * 
 * @author Sascha Zur
 * @author Robert Mischke
 * @author Oliver Seebach
 * @author David Scholz
 */
public class NetworkView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.rcenvironment.core.gui.communication.views.NetworkView";

    private static final ImageDescriptor ADD =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/newConnection.png"));

    private static final ImageDescriptor ADDSSH =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/newSshConnection.png"));

    private static final ImageDescriptor EDIT =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/edit16.gif"));

    private static final ImageDescriptor START =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/run_enabled.gif"));

    private static final ImageDescriptor STOP =
        ImageDescriptor.createFromURL(NetworkView.class.getResource("/resources/icons/cancel_enabled.gif"));

    private static final ImageDescriptor COPY = ImageManager.getInstance().getImageDescriptor(StandardImages.COPY_16);

    private static final ImageDescriptor DELETE = ImageManager.getInstance().getImageDescriptor(StandardImages.DELETE_16);

    private static final String TAB = "\t";

    private Display display;

    private TreeViewer viewer;

    private Action toggleNodeIdsVisibleAction;

    private Action toggleRawNodePropertiesVisibleAction;

    private Action addNetworkConnectionAction;

    private Action addSSHConnectionAction;

    private Action copyToClipBoardAction;

    private Action editAction;

    private Action deleteAction;

    private Action startAction;

    private Action stopAction;

    private NetworkViewContentProvider contentProvider;

    private NetworkViewLabelProvider labelProvider;

    private boolean rawPropertiesVisible;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private NetworkViewModel model;

    private SystemMonitoringDataPollingManager manager = new SystemMonitoringDataPollingManager();

    private ConnectionSetupsListContributor networkConnectionsContributor;

    private SshConnectionSetupsListContributor sshConnectionsContributor;

    private List<NetworkViewContributor> allContributors;

    public NetworkView() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

        labelProvider = new NetworkViewLabelProvider();
        labelProvider.setNodeIdsVisible(false);
        contentProvider = initializeContentProvider();
        contentProvider.setRawPropertiesVisible(false);

        createActions();
    }

    /** A {@link KeyListener} to react on pressedkey's. */
    private final class NetworkViewKeyListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent event) {
            if (event.stateMask == SWT.CTRL) {
                if (event.keyCode == 'c') {
                    copyToClipBoardAction.run();
                }
            } else if (event.keyCode == SWT.DEL) {
                deleteAction.run();
            }
        }
    }

    private void createActions() {
        // add "show nodeIds" option
        // TODO add icon
        toggleNodeIdsVisibleAction = new Action("Show Node Identifiers", SWT.TOGGLE) {

            @Override
            public void run() {
                labelProvider.setNodeIdsVisible(this.isChecked());
                // refresh all labels
                viewer.refresh(AnchorPoints.INSTANCES_PARENT_NODE, true);
            }
        };
        // add "show raw node properties" option
        // TODO add icon
        toggleRawNodePropertiesVisibleAction = new Action("Show Raw Node Properties", SWT.TOGGLE) {

            @Override
            public void run() {
                rawPropertiesVisible = this.isChecked();
                contentProvider.setRawPropertiesVisible(rawPropertiesVisible);
                viewer.refresh(AnchorPoints.INSTANCES_PARENT_NODE, false);
            }
        };
        // copies the selected raw node properties to the clipboard
        copyToClipBoardAction = new Action("Copy to Clipboard" + TAB + "Ctrl+C", COPY) {

            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                NetworkGraphNodeWithContext selectionObject = (NetworkGraphNodeWithContext) selection.getFirstElement();
                if (selectionObject.getContext() == Context.RAW_NODE_PROPERTY) {
                    // single raw node selected
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(
                        labelProvider.getText(selectionObject)), null);
                } else if (selectionObject.getContext() == Context.RAW_NODE_PROPERTIES_FOLDER) {
                    // all raw nodes folder selected
                    String allRawNodeProperties = "RAW NODE PROPERTIES: \n\n";
                    Map<NodeIdentifier, Map<String, String>> nodeProperties = model.getNodeProperties();
                    for (NodeIdentifier identifier : nodeProperties.keySet()) {
                        if (identifier.getAssociatedDisplayName() == selectionObject.getDisplayNameOfNode()) {
                            for (String nodeProperty : nodeProperties.get(identifier).keySet()) {
                                allRawNodeProperties += nodeProperty + ": " + nodeProperties.get(identifier).get(nodeProperty) + "\n";
                            }
                        }
                    }
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(allRawNodeProperties), null);
                }
            }
        };

        // add "add network connection" option
        addNetworkConnectionAction = new Action("Add Network Connection...", ADD) {

            @Override
            public void run() {
                networkConnectionsContributor.showAddConnectionDialog();
            }
        };

        // add "add network connection" option
        addSSHConnectionAction = new Action("Add SSH Remote Access Connection...", ADDSSH) {

            @Override
            public void run() {
                sshConnectionsContributor.showAddConnectionDialog();
            }
        };

        // add "edit network connection" option
        editAction = new Action("Edit Network Connection...", EDIT) {

            @Override
            public void run() {
                executeStandardUserNodeAction(StandardUserNodeActionType.EDIT);
            }
        };

        // add "delete network connection" option
        deleteAction = new Action("Delete Network Connection..." + TAB + "Delete", DELETE) {

            @Override
            public void run() {
                executeStandardUserNodeAction(StandardUserNodeActionType.DELETE);
            }
        };
        deleteAction.setAccelerator(SWT.DEL);

        // add "start network connection" option
        startAction = new Action("Start/Connect", START) {

            @Override
            public void run() {
                executeStandardUserNodeAction(StandardUserNodeActionType.START);
            }
        };

        // add "stop network connection" option
        stopAction = new Action("Stop/Disconnect", STOP) {

            @Override
            public void run() {
                executeStandardUserNodeAction(StandardUserNodeActionType.STOP);
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
        model =
            new NetworkViewModel(initialGraph, null, initialConnectionSetups, new HashMap<NodeIdentifier, Map<String, String>>(),
                new ConcurrentHashMap<NodeIdentifier, SystemMonitoringDataSnapshot>());

        for (NetworkViewContributor contributor : allContributors) {
            contributor.setCurrentModel(model);
            contributor.setTreeViewer(viewer);
        }

        viewer.setInput(model);
        viewer.expandToLevel(3);
        viewer.getControl().addKeyListener(new NetworkViewKeyListener());
        // Update context menu on open
        viewer.getTree().addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent event) {
                updatePossibleActionsForSelection(getSelectedTreeNode());
            }
        });

        viewer.getTree().addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent event) {}

            @Override
            public void mouseDown(MouseEvent event) {}

            @Override
            public void mouseDoubleClick(MouseEvent event) {
                if (editAction.isEnabled()) {
                    editAction.run();

                }
            }
        });

        hookContextMenu();
        addToolbarActions();
        registerChangeListeners();
        registerSelectionListeners();

    }

    @Override
    public void dispose() {
        manager.cancelAllPollingTasks();
        serviceRegistryAccess.dispose();
        for (NetworkViewContributor contributor : allContributors) {
            contributor.dispose();
        }
        super.dispose();
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    private NetworkViewContentProvider initializeContentProvider() {

        allContributors = defineContributors();
        List<NetworkViewContributor> rootContributors = new ArrayList<>();
        List<NetworkViewContributor> instanceDataContributors = new ArrayList<>();

        for (NetworkViewContributor contributor : allContributors) {
            if (contributor.getRootElementsPriority() != 0) {
                rootContributors.add(contributor);
            }
            if (contributor.getInstanceDataElementsPriority() != 0) {
                instanceDataContributors.add(contributor);
            }
        }
        Collections.sort(rootContributors, new Comparator<NetworkViewContributor>() {

            @Override
            public int compare(NetworkViewContributor o1, NetworkViewContributor o2) {
                return Integer.compare(o1.getRootElementsPriority(), o2.getRootElementsPriority());
            }
        });
        Collections.sort(instanceDataContributors, new Comparator<NetworkViewContributor>() {

            @Override
            public int compare(NetworkViewContributor o1, NetworkViewContributor o2) {
                return Integer.compare(o1.getInstanceDataElementsPriority(), o2.getInstanceDataElementsPriority());
            }
        });

        return new NetworkViewContentProvider(rootContributors, instanceDataContributors);
    }

    private List<NetworkViewContributor> defineContributors() {
        // add contributors here; their ordering does not matter as long as the priority values are unique
        List<NetworkViewContributor> result = new ArrayList<>();

        // note: saving contributors in a field is usually not required, but we don't go for a full-blown plugin structure here - misc_ro
        networkConnectionsContributor = new ConnectionSetupsListContributor();
        sshConnectionsContributor = new SshConnectionSetupsListContributor();

        result.add(networkConnectionsContributor);
        result.add(new MonitoringDataContributor());
        result.add(new InstanceComponentsInfoContributor());
        result.add(sshConnectionsContributor);

        return result;
    }

    private void registerSelectionListeners() {

        disableAllActions();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {

                Object node = getSelectedTreeNode();
                if (node != null) {
                    updatePossibleActionsForSelection(node);
                } else {
                    disableAllActions();
                }
            }
        });
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
                        viewer.refresh(AnchorPoints.INSTANCES_PARENT_NODE, false);
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
                        viewer.refresh(AnchorPoints.INSTANCES_PARENT_NODE);
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
                        viewer.refresh(AnchorPoints.INSTANCES_PARENT_NODE);
                    }
                });
            }
        });
        // TODO move this listener into the contributor for better separation
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
                        viewer.refresh(networkConnectionsContributor.getFullRefreshRootElement(), false);
                        viewer.setExpandedState(networkConnectionsContributor.getRootElementToExpand(), true);
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
                        Object node = networkConnectionsContributor.getTreeNodeForSetup(setup);
                        if (node != null) {
                            updatePossibleActionsForSelection(node);
                            viewer.update(node, null);
                        }
                    }
                });
            }
        });
    }

    private Object getSelectedTreeNode() {
        ISelection rawSelection = viewer.getSelection();
        if (rawSelection != null && rawSelection instanceof TreeSelection) {
            TreeSelection selection = ((TreeSelection) rawSelection);
            return selection.getFirstElement();
        }
        return null;
    }

    private void updatePossibleActionsForSelection(Object node) {

        if (node == null) {
            disableAllActions();
            return;
        }

        if (node instanceof StandardUserNodeActionNode) {
            StandardUserNodeActionNode typedNode = (StandardUserNodeActionNode) node;
            startAction.setEnabled(typedNode.isActionApplicable(StandardUserNodeActionType.START));
            stopAction.setEnabled(typedNode.isActionApplicable(StandardUserNodeActionType.STOP));
            editAction.setEnabled(typedNode.isActionApplicable(StandardUserNodeActionType.EDIT));
            deleteAction.setEnabled(typedNode.isActionApplicable(StandardUserNodeActionType.DELETE));
            copyToClipBoardAction.setEnabled(typedNode.isActionApplicable(StandardUserNodeActionType.COPY_TO_CLIPBOARD));
        } else if (node instanceof NetworkGraphNodeWithContext) {
            // TODO special handling; migrate
            disableAllActions();
            NetworkGraphNodeWithContext typedNode = (NetworkGraphNodeWithContext) node;
            if (typedNode.getContext() == Context.RAW_NODE_PROPERTY || typedNode.getContext() == Context.RAW_NODE_PROPERTIES_FOLDER) {
                copyToClipBoardAction.setEnabled(true);
            }
        } else {
            disableAllActions();
        }
    }

    private void disableAllActions() {
        editAction.setEnabled(false);
        deleteAction.setEnabled(false);
        startAction.setEnabled(false);
        stopAction.setEnabled(false);
        copyToClipBoardAction.setEnabled(false);
    }

    private void executeStandardUserNodeAction(StandardUserNodeActionType actionType) {
        Object node = getSelectedTreeNode();
        if (node instanceof StandardUserNodeActionNode) {
            StandardUserNodeActionNode typedNode = (StandardUserNodeActionNode) node;
            if (typedNode.isActionApplicable(actionType)) {
                // execute
                typedNode.performAction(actionType);
            } else {
                // the action was enabled when it shouldn't be anymore, so update all action states - misc_ro
                updatePossibleActionsForSelection(typedNode);
            }
        }
    }

    private void addToolbarActions() {

        // add toolbar actions (right top of view)
        final IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        toolBarManager.add(addNetworkConnectionAction);
        toolBarManager.add(addSSHConnectionAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(startAction);
        toolBarManager.add(stopAction);
        toolBarManager.add(editAction);
        toolBarManager.add(deleteAction);
    }

    private void hookContextMenu() {

        // submenu
        final MenuManager subMenuManager = new MenuManager("Advanced");
        subMenuManager.add(toggleNodeIdsVisibleAction);
        subMenuManager.add(toggleRawNodePropertiesVisibleAction);

        final MenuManager menuManager = new MenuManager();
        menuManager.add(addNetworkConnectionAction);
        menuManager.add(addSSHConnectionAction);
        menuManager.add(new Separator());
        menuManager.add(startAction);
        menuManager.add(stopAction);
        menuManager.add(editAction);
        menuManager.add(deleteAction);
        menuManager.add(new Separator());
        menuManager.add(copyToClipBoardAction);
        menuManager.add(new Separator());
        menuManager.add(subMenuManager); // "Advanced"
        menuManager.updateAll(true);
        final Menu menu = menuManager.createContextMenu(viewer.getTree());
        viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);
        getSite().setSelectionProvider(viewer);
    }

}
