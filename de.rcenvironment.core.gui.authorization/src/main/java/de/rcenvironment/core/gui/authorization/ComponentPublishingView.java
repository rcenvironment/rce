/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.api.DefaultAuthorizationObjects;
import de.rcenvironment.core.component.authorization.api.ComponentAuthorizationSelector;
import de.rcenvironment.core.component.authorization.api.NamedComponentAuthorizationSelector;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.management.api.PermissionMatrixChangeListener;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * View to publish components by associating them with a group.
 *
 * @author Oliver Seebach
 * @author Jan Flink
 */
public class ComponentPublishingView extends ViewPart {

    private static final String FILTER_COMPONENTS_MESSAGE = "Filter components...";

    private static final String FILTER_GROUPS_MESSAGE = "Filter groups...";

    private AuthorizationGroupContentProvider authorizationGroupContentProvider;

    private AuthorizationComponentContentProvider authorizationComponentContentProvider;

    private final ServiceRegistryPublisherAccess serviceRegistryAccess;

    private final AuthorizationService authorizationService;

    private final DefaultAuthorizationObjects defaultAuthorizationObjects;

    private final LocalComponentRegistrationService localComponentRegistrationService;

    private TreeViewer groupsTreeViewer;

    private GroupsTreeViewerComparator groupsTreeViewerComparator = new GroupsTreeViewerComparator();

    private TreeViewer componentsTreeViewer;

    private ComponentsTreeViewerComparator componentsTreeViewerComparator = new ComponentsTreeViewerComparator();

    private TreeSortAction sortAscendingName;

    private TreeSortAction sortDescendingName;

    private ExpandAllAction expandAll;

    private CollapseAllAction collapseAll;

    private SelectionProviderAction groupsAssignComponentsAction;

    private Action groupsAssignGroupsAction;

    private AssignComponentsAction componentsAssignComponentsAction;

    private AssignGroupsAction componentsAssignGroupsAction;

    private Action manageGroupAction;

    private IAction importGroupKeyAction;

    private IAction groupsExportGroupKeyAction;

    private IAction componentsExportGroupKeyAction;

    private ToolbarFilterWidget toolbarFilter;

    private ShowAuthorizationGroupIdAction groupsShowIdAction;

    private ShowAuthorizationGroupIdAction componentsShowIdAction;

    /**
     * Action for group to component assignment.
     *
     * @author Jan Flink
     */
    private final class AssignGroupsAction extends SelectionProviderAction {

        private AssignGroupsAction(ISelectionProvider provider) {
            super(provider, "Assign Groups...");
            setEnabled(false);
        }

        @Override
        public void run() {
            IStructuredSelection selection = getStructuredSelection();
            // Get mapping of component and publishing state
            Map<AuthorizationAccessGroup, Boolean> groupToStateMapping = new HashMap<>();
            List<AuthorizationAccessGroup> groups = localComponentRegistrationService
                .listAvailableAuthorizationAccessGroups();

            if (selection.isEmpty() || !(selection.getFirstElement() instanceof NamedComponentAuthorizationSelector)) {
                return;
            }

            NamedComponentAuthorizationSelector selectedComponentObject = (NamedComponentAuthorizationSelector) selection.getFirstElement();

            // TODO at some point we will have to decide how to handle existing permissions
            // with no current matching local components - display them or not? --misc_ro
            AuthorizationPermissionSet authorizationPermissionSet = localComponentRegistrationService
                .getComponentPermissionSet(selectedComponentObject, true);
            for (AuthorizationAccessGroup group : groups) {
                // prevent public group from being added.
                if (!authorizationService.isPublicAccessGroup(group)) {
                    groupToStateMapping.put(group, authorizationPermissionSet.includesAccessGroup(group));
                }
            }

            Shell shell = Display.getDefault().getActiveShell();
            AssignGroupsDialog assignGroupsDialog = new AssignGroupsDialog(shell, groupToStateMapping,
                authorizationPermissionSet, selectedComponentObject.getDisplayName());
            if (assignGroupsDialog.open() == 0) {
                PublishingType type = assignGroupsDialog.getType();
                final AuthorizationPermissionSet newPermissionSet;
                if (type == PublishingType.CUSTOM) {
                    List<AuthorizationAccessGroup> newAuthorizationGroups = new ArrayList<>();
                    // New Group To State Mapping to handle
                    for (AuthorizationAccessGroup group : groups) {
                        if (groupToStateMapping.keySet().contains(group) && groupToStateMapping.get(group)) {
                            newAuthorizationGroups.add(group);
                        }
                    }
                    newPermissionSet = authorizationService.buildPermissionSet(newAuthorizationGroups);
                } else if (type == PublishingType.LOCAL) {
                    newPermissionSet = defaultAuthorizationObjects.permissionSetLocalOnly();
                } else if (type == PublishingType.PUBLIC) {
                    newPermissionSet = defaultAuthorizationObjects.permissionSetPublicInLocalNetwork();
                } else {
                    throw new IllegalStateException();
                }
                localComponentRegistrationService.setComponentPermissions(selectedComponentObject, newPermissionSet);
                componentsTreeViewer.setExpandedState(selectedComponentObject, true);
            }
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof NamedComponentAuthorizationSelector) {
                setEnabled(true);
                return;
            }
            setEnabled(false);
        }
    }

    /**
     * Action for components to group assignment.
     *
     * @author Jan Flink
     */
    private final class AssignComponentsAction extends SelectionProviderAction {

        private AssignComponentsAction(ISelectionProvider provider) {
            super(provider, "Assign Components...");
            setEnabled(false);
        }

        @Override
        public void run() {
            IStructuredSelection selection = getStructuredSelection();

            // Get mapping of component and publishing state
            Map<NamedComponentAuthorizationSelector, Boolean> componentToStateMapping = new TreeMap<>();

            if (selection.isEmpty() || !(selection.getFirstElement() instanceof AuthorizationAccessGroup)) {
                return;
            }

            AuthorizationAccessGroup selectedGroupObject = (AuthorizationAccessGroup) selection.getFirstElement();

            List<NamedComponentAuthorizationSelector> components = localComponentRegistrationService
                .listAuthorizationSelectorsForRemotableComponentsIncludingOrphans();
            for (NamedComponentAuthorizationSelector component : components) {
                final AuthorizationPermissionSet permissionSet =
                    localComponentRegistrationService.getComponentPermissionSet(component, true);
                // We do not want to display tools that are in the "public" group unless the user is currently editing the "public" group
                if (!permissionSet.isPublic() || authorizationService.isPublicAccessGroup(selectedGroupObject)) {
                    componentToStateMapping.put(component, permissionSet.includesAccessGroup(selectedGroupObject));
                }
            }

            // Actual dialog showing
            final Display display = Display.getDefault();
            Shell shell = display.getActiveShell();
            AssignComponentsDialog assignComponentsDialog =
                new AssignComponentsDialog(shell, authorizationService, componentToStateMapping,
                    selectedGroupObject.getDisplayName());
            int id = assignComponentsDialog.open();
            if (id == 0) {
                Map<NamedComponentAuthorizationSelector, Boolean> resultingComponentToStateMapping = assignComponentsDialog
                    .getComponentToStateMapping();
                for (Entry<NamedComponentAuthorizationSelector, Boolean> e : resultingComponentToStateMapping.entrySet()) {
                    final NamedComponentAuthorizationSelector componentSelector = e.getKey();
                    localComponentRegistrationService.setComponentPermissionState(componentSelector, selectedGroupObject,
                        e.getValue());
                }
                groupsTreeViewer.setExpandedState(selectedGroupObject, true);
            }
        }

        @Override
        public void selectionChanged(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();
            if (selection.size() == 1 && obj instanceof AuthorizationAccessGroup) {
                setEnabled(true);
                return;
            }
            setEnabled(false);
        }
    }

    /**
     * An {@link Action} that collapses all nodes.
     * 
     * @author Oliver Seebach
     */
    private class CollapseAllAction extends Action {

        @Override
        public void run() {
            if (componentsTreeViewer.getControl().isVisible()) {
                componentsTreeViewer.collapseAll();
            }
            if (groupsTreeViewer.getControl().isVisible()) {
                groupsTreeViewer.collapseAll();
            }
        }
    }

    /**
     * An {@link Action} that expands all nodes.
     * 
     * @author Oliver Seebach
     */
    private class ExpandAllAction extends Action {

        @Override
        public void run() {
            if (componentsTreeViewer.getControl().isVisible()) {
                componentsTreeViewer.expandAll();
            }
            if (groupsTreeViewer.getControl().isVisible()) {
                groupsTreeViewer.expandAll();
            }
        }
    }

    /**
     * Doubleclick Listener for trees.
     *
     * @author Jan Flink
     */
    private final class TreeDoubleClickListener implements IDoubleClickListener {

        @Override
        public void doubleClick(DoubleClickEvent event) {
            if (!(event.getViewer() instanceof TreeViewer)) {
                return;
            }
            TreeViewer sourceViewer = (TreeViewer) event.getViewer();
            IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
            Object selectedNode = thisSelection.getFirstElement();
            if (selectedNode instanceof AuthorizationAccessGroup) {
                new AssignComponentsAction(sourceViewer).run();
            }
            if (selectedNode instanceof NamedComponentAuthorizationSelector) {
                new AssignGroupsAction(sourceViewer).run();
            }
        }
    }

    public ComponentPublishingView() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);

        localComponentRegistrationService = serviceRegistryAccess.getService(LocalComponentRegistrationService.class);
        authorizationService = serviceRegistryAccess.getService(AuthorizationService.class);

        // cache for convenient access
        defaultAuthorizationObjects = authorizationService.getDefaultAuthorizationObjects();

    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

        initComponentTreeContentProvider();
        initGroupTreeContentProvider();
    }

    @Override
    public void dispose() {
        serviceRegistryAccess.dispose();
        super.dispose();
    }

    private void initGroupTreeContentProvider() {
        authorizationGroupContentProvider = new AuthorizationGroupContentProvider();
    }

    private void initComponentTreeContentProvider() {
        authorizationComponentContentProvider = new AuthorizationComponentContentProvider();
    }

    private void registerChangeListeners() {
        serviceRegistryAccess.registerService(PermissionMatrixChangeListener.class,
            // We do not transform this anonymous inner class into a lambda as registerService only requires an Object as its second
            // argument. Turning this PermissionMatrixChangeListener into a lambda would cause us to lose the information about which
            // class is instantiated in order to implement the listener
            new PermissionMatrixChangeListener() {

                @Override
                public void onPermissionMatrixChanged(boolean accessGroupsChanged,
                    boolean componentSelectorsChanged, boolean assignmentsChanged) {
                    Display.getDefault().asyncExec(() -> {
                        if (groupsTreeViewer.getControl().isDisposed()
                            || componentsTreeViewer.getControl().isDisposed()) {
                            return;
                        }

                        groupsTreeViewer.refresh();
                        componentsTreeViewer.refresh();
                    });

                }
            });
    }

    /**
     * Comparator for groups tree sorting.
     *
     * @author Oliver Seebach
     */
    class GroupsTreeViewerComparator extends ViewerComparator {

        private int direction = AuthorizationConstants.ASCENDING;

        public void setDirection(int direction) {
            this.direction = direction;
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (e1 instanceof AuthorizationAccessGroup && e2 instanceof AuthorizationAccessGroup) {
                AuthorizationAccessGroup group1 = (AuthorizationAccessGroup) e1;
                AuthorizationAccessGroup group2 = (AuthorizationAccessGroup) e2;
                int returncode = group1.compareToIgnoreCase(group2);
                if (direction == AuthorizationConstants.DESCENDING) {
                    returncode = -returncode;
                }
                return returncode;
            }
            if (e1 instanceof NamedComponentAuthorizationSelector && e2 instanceof NamedComponentAuthorizationSelector) {
                NamedComponentAuthorizationSelector component1 = (NamedComponentAuthorizationSelector) e1;
                NamedComponentAuthorizationSelector component2 = (NamedComponentAuthorizationSelector) e2;
                return component1.compareToIgnoreCase(component2);
            }

            return super.compare(viewer, e1, e2);
        }

        public int getDirection() {
            return direction;
        }
    }

    /**
     * Comparator for components tree sorting.
     *
     * @author Oliver Seebach
     * @author Jan Flink
     */
    class ComponentsTreeViewerComparator extends ViewerComparator {

        private int direction = AuthorizationConstants.ASCENDING;

        public void setDirection(int direction) {
            this.direction = direction;
        }

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (e1 instanceof AuthorizationAccessGroup && e2 instanceof AuthorizationAccessGroup) {
                AuthorizationAccessGroup group1 = (AuthorizationAccessGroup) e1;
                AuthorizationAccessGroup group2 = (AuthorizationAccessGroup) e2;
                return group1.compareToIgnoreCase(group2);
            }
            if (e1 instanceof NamedComponentAuthorizationSelector && e2 instanceof NamedComponentAuthorizationSelector) {
                NamedComponentAuthorizationSelector component1 = (NamedComponentAuthorizationSelector) e1;
                NamedComponentAuthorizationSelector component2 = (NamedComponentAuthorizationSelector) e2;
                int returncode = component1.compareToIgnoreCase(component2);
                if (direction == AuthorizationConstants.DESCENDING) {
                    returncode = -returncode;
                }
                return returncode;
            }
            return super.compare(viewer, e1, e2);
        }

        public int getDirection() {
            return direction;
        }

    }

    @Override
    public void createPartControl(Composite parent) {
        ScrolledComposite scrollComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scrollComposite.setMinSize(AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_WIDTH,
            AuthorizationConstants.SCROLL_COMPOSITE_MINIMUM_HEIGHT);
        scrollComposite.setExpandHorizontal(true);
        scrollComposite.setExpandVertical(true);

        Composite scrollContent = new Composite(scrollComposite, SWT.FILL);
        GridLayout contentLayout = new GridLayout(2, false);
        scrollContent.setLayout(contentLayout);

        CTabFolder arrangementTabFolder = new CTabFolder(scrollContent, SWT.BORDER | SWT.NO_FOCUS);
        GridData arrangementTabFolderGridData = new GridData(GridData.FILL_BOTH);
        arrangementTabFolder.setLayoutData(arrangementTabFolderGridData);
        arrangementTabFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setSortActionsChecked();
                setViewerFilterAssignment();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // The firing of the widgetDefaultSelected event is highly platform- and widget-dependent. Since the widgetSelected event
                // suffices for our purposes in this listener, we leave this method empty
            }
        });
        arrangementTabFolder.setSimple(false);


        final ToolBar toolbar = new ToolBar(arrangementTabFolder, SWT.HORIZONTAL | SWT.FLAT);
        arrangementTabFolder.setTopRight(toolbar);
        scrollComposite.setContent(scrollContent);

        // TAB BY GROUPS
        CTabItem arrangeByGroupTabItem = new CTabItem(arrangementTabFolder, SWT.NONE);
        arrangeByGroupTabItem.setText("Component Assignment");

        Composite authorizationByGroupComposite = new Composite(arrangementTabFolder, SWT.NONE);
        GridData authorizationGroupGridData = new GridData(GridData.FILL_VERTICAL);
        GridLayout authorizationGroupLayout = new GridLayout(2, false);
        authorizationByGroupComposite.setLayout(authorizationGroupLayout);
        authorizationByGroupComposite.setLayoutData(authorizationGroupGridData);

        groupsTreeViewer = new TreeViewer(authorizationByGroupComposite, SWT.SINGLE | SWT.BORDER | SWT.NO_FOCUS);
        GridData groupsTreeViewerGridData = new GridData(GridData.FILL_BOTH);
        groupsTreeViewerGridData.verticalSpan = 3;
        groupsTreeViewer.getTree().setLayoutData(groupsTreeViewerGridData);

        groupsTreeViewer.setContentProvider(authorizationGroupContentProvider);
        groupsTreeViewer.setLabelProvider(new AuthorizationLabelProvider());
        groupsTreeViewer.setInput(new String[0]); // empty initialisation
        groupsTreeViewer.addDoubleClickListener(new TreeDoubleClickListener());
        groupsTreeViewer.setComparator(groupsTreeViewerComparator);

        groupsAssignComponentsAction = new AssignComponentsAction(groupsTreeViewer);
        groupsAssignGroupsAction = new AssignGroupsAction(groupsTreeViewer);
        groupsExportGroupKeyAction = new ExportGroupKeyAction(groupsTreeViewer, authorizationService);
        groupsShowIdAction = new ShowAuthorizationGroupIdAction(groupsTreeViewer);

        ActionContributionItem groupsAssignComponentsContribution = new ActionContributionItem(groupsAssignComponentsAction);
        groupsAssignComponentsContribution.fill(authorizationByGroupComposite);
        Button groupsAssignComponentsButton = (Button) groupsAssignComponentsContribution.getWidget();
        GridData groupsAssignComponentsButtonGridData = new GridData();
        groupsAssignComponentsButtonGridData.verticalAlignment = SWT.TOP;
        groupsAssignComponentsButtonGridData.horizontalAlignment = SWT.FILL;
        groupsAssignComponentsButton.setLayoutData(groupsAssignComponentsButtonGridData);

        ActionContributionItem groupsAssignGroupsContribution = new ActionContributionItem(groupsAssignGroupsAction);
        groupsAssignGroupsContribution.fill(authorizationByGroupComposite);
        Button groupsAssignGroupsButton = (Button) groupsAssignGroupsContribution.getWidget();
        GridData groupsAssignGroupsButtonGridData = new GridData();
        groupsAssignGroupsButtonGridData.verticalAlignment = SWT.TOP;
        groupsAssignGroupsButtonGridData.horizontalAlignment = SWT.FILL;
        groupsAssignGroupsButton.setLayoutData(groupsAssignGroupsButtonGridData);

        // TAB BY COMPONENTS
        CTabItem arrangeByComponentsTabItem = new CTabItem(arrangementTabFolder, SWT.NONE);
        arrangeByComponentsTabItem.setText("Group Assignment");

        Composite authorizationByComponentComposite = new Composite(arrangementTabFolder, SWT.NULL);
        GridData authorizationComponentGridData = new GridData(GridData.FILL_VERTICAL);
        GridLayout authorizationComponentLayout = new GridLayout(2, false);
        authorizationByComponentComposite.setLayout(authorizationComponentLayout);
        authorizationByComponentComposite.setLayoutData(authorizationComponentGridData);

        componentsTreeViewer = new TreeViewer(authorizationByComponentComposite, SWT.SINGLE | SWT.BORDER | SWT.NO_FOCUS);
        GridData componentsTreeViewerGridData = new GridData(GridData.FILL_BOTH);
        componentsTreeViewerGridData.verticalSpan = 3;
        componentsTreeViewer.getTree().setLayoutData(componentsTreeViewerGridData);

        componentsTreeViewer.setContentProvider(authorizationComponentContentProvider);
        componentsTreeViewer.setLabelProvider(new AuthorizationLabelProvider());
        componentsTreeViewer.setInput(new String[0]); // empty initialisation
        componentsTreeViewer.addDoubleClickListener(new TreeDoubleClickListener());
        componentsTreeViewer.setComparator(componentsTreeViewerComparator);

        componentsAssignGroupsAction = new AssignGroupsAction(componentsTreeViewer);
        componentsAssignComponentsAction = new AssignComponentsAction(componentsTreeViewer);
        componentsExportGroupKeyAction = new ExportGroupKeyAction(componentsTreeViewer, authorizationService);
        componentsShowIdAction = new ShowAuthorizationGroupIdAction(componentsTreeViewer);

        ActionContributionItem componentsAssignComponentsContribution = new ActionContributionItem(componentsAssignComponentsAction);
        componentsAssignComponentsContribution.fill(authorizationByComponentComposite);
        Button componentsAssignComponentsButton = (Button) componentsAssignComponentsContribution.getWidget();
        GridData componentsAssignComponentsButtonGridData = new GridData();
        componentsAssignComponentsButtonGridData.verticalAlignment = SWT.TOP;
        componentsAssignComponentsButtonGridData.horizontalAlignment = SWT.FILL;
        componentsAssignComponentsButton.setLayoutData(componentsAssignComponentsButtonGridData);

        ActionContributionItem componentsAssignGroupsContribution = new ActionContributionItem(componentsAssignGroupsAction);
        componentsAssignGroupsContribution.fill(authorizationByComponentComposite);
        Button componentsAssignGroupsButton = (Button) componentsAssignGroupsContribution.getWidget();
        GridData componentsAssignGroupsButtonGridData = new GridData();
        componentsAssignGroupsButtonGridData.verticalAlignment = SWT.TOP;
        componentsAssignGroupsButtonGridData.horizontalAlignment = SWT.FILL;
        componentsAssignGroupsButton.setLayoutData(componentsAssignGroupsButtonGridData);

        makeActions();

        GridData manageGroupsButtonGridData = new GridData();
        manageGroupsButtonGridData.verticalAlignment = SWT.BOTTOM;
        ActionContributionItem groupsManageGroupsContribution = new ActionContributionItem(manageGroupAction);
        groupsManageGroupsContribution.fill(authorizationByGroupComposite);
        Button groupsManageGroupsButton = (Button) groupsManageGroupsContribution.getWidget();
        groupsManageGroupsButton.setLayoutData(manageGroupsButtonGridData);

        ActionContributionItem componentsManageGroupsContribution = new ActionContributionItem(manageGroupAction);
        componentsManageGroupsContribution.fill(authorizationByComponentComposite);
        Button componentsManageGroupsButton = (Button) componentsManageGroupsContribution.getWidget();
        componentsManageGroupsButton.setLayoutData(manageGroupsButtonGridData);


        // add items' controls
        arrangeByGroupTabItem.setControl(authorizationByGroupComposite);
        arrangeByComponentsTabItem.setControl(authorizationByComponentComposite);

        // default selection
        arrangementTabFolder.setSelection(arrangeByGroupTabItem);

        fillLocalToolBar(toolbar);
        hookContextMenus();
        registerChangeListeners();

    }

    protected void setViewerFilterAssignment() {
        if (groupsTreeViewer.getTree().isVisible()) {
            toolbarFilter.setViewer(groupsTreeViewer);
            toolbarFilter.setMessage(FILTER_GROUPS_MESSAGE);
            groupsTreeViewer.resetFilters();
            groupsTreeViewer.getTree().setFocus();
            Tree tree = groupsTreeViewer.getTree();
            if (tree.getItemCount() > 0 && tree.getSelectionCount() == 0) {
                tree.setSelection(tree.getItem(0));
                tree.notifyListeners(SWT.Selection, new Event());
            }
        } else if (componentsTreeViewer.getTree().isVisible()) {
            toolbarFilter.setViewer(componentsTreeViewer);
            toolbarFilter.setMessage(FILTER_COMPONENTS_MESSAGE);
            componentsTreeViewer.resetFilters();
            Tree tree = componentsTreeViewer.getTree();
            if (tree.getItemCount() > 0 && tree.getSelectionCount() == 0) {
                tree.setSelection(tree.getItem(0));
                tree.notifyListeners(SWT.Selection, new Event());
            }
            componentsTreeViewer.getTree().setFocus();
        }
    }

    private void makeActions() {
        sortAscendingName = new TreeSortAction(AuthorizationConstants.ASCENDING);
        sortAscendingName.setImageDescriptor(AuthorizationConstants.SORT_ASC);
        sortAscendingName.setText("Sort alphabetically ascending");
        sortDescendingName = new TreeSortAction(AuthorizationConstants.DESCENDING);
        sortDescendingName.setImageDescriptor(AuthorizationConstants.SORT_DESC);
        sortDescendingName.setText("Sort alphabetically descending");
        expandAll = new ExpandAllAction();
        expandAll.setImageDescriptor(AuthorizationConstants.EXPAND_ALL_ICON);
        expandAll.setText("Expand all");
        collapseAll = new CollapseAllAction();
        collapseAll.setImageDescriptor(AuthorizationConstants.COLLAPSE_ALL_ICON);
        collapseAll.setText("Collapse all");
        manageGroupAction = new ManageAuthorizationGroupsAction();
        importGroupKeyAction = new ImportGroupKeyAction(authorizationService);
    }

    private void fillLocalToolBar(ToolBar toolbar) {
        toolbarFilter = new ToolbarFilterWidget(groupsTreeViewer, FILTER_GROUPS_MESSAGE);
        toolbarFilter.fill(toolbar, 0);
        new ActionContributionItem(sortAscendingName).fill(toolbar, 1);
        new ActionContributionItem(sortDescendingName).fill(toolbar, 2);
        new ActionContributionItem(expandAll).fill(toolbar, 3);
        new ActionContributionItem(collapseAll).fill(toolbar, 4);
    }

    private void hookContextMenus() {
        MenuManager groupsMenuManager = new MenuManager();
        groupsMenuManager.setRemoveAllWhenShown(true);
        groupsMenuManager.addMenuListener(this::fillGroupsContextMenu);

        Menu groupsTreeMenu = groupsMenuManager.createContextMenu(groupsTreeViewer.getControl());
        groupsTreeViewer.getControl().setMenu(groupsTreeMenu);
        getSite().registerContextMenu(groupsMenuManager, groupsTreeViewer);

        MenuManager componentsMenuManager = new MenuManager();
        componentsMenuManager.setRemoveAllWhenShown(true);
        componentsMenuManager.addMenuListener(this::fillComponentsContextMenu);
        Menu componentsTreeMenu = componentsMenuManager.createContextMenu(componentsTreeViewer.getControl());
        componentsTreeViewer.getControl().setMenu(componentsTreeMenu);
        getSite().registerContextMenu(componentsMenuManager, componentsTreeViewer);
    }

    protected void fillComponentsContextMenu(IMenuManager manager) {
        fillContextMenu(manager);
        manager.add(componentsShowIdAction);
        manager.add(new Separator());
        manager.add(componentsAssignComponentsAction);
        manager.add(componentsAssignGroupsAction);
        manager.add(new Separator());
        manager.add(importGroupKeyAction);
        manager.add(componentsExportGroupKeyAction);
    }

    protected void fillGroupsContextMenu(IMenuManager manager) {
        fillContextMenu(manager);
        manager.add(groupsShowIdAction);
        manager.add(new Separator());
        manager.add(groupsAssignComponentsAction);
        manager.add(groupsAssignGroupsAction);
        manager.add(new Separator());
        manager.add(importGroupKeyAction);
        manager.add(groupsExportGroupKeyAction);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(sortAscendingName);
        manager.add(sortDescendingName);
        manager.add(expandAll);
        manager.add(collapseAll);
        manager.add(new Separator());
        manager.add(manageGroupAction);
    }

    @Override
    public void setFocus() {
        groupsTreeViewer.getTree().setFocus();
    }

    /**
     * Tree sorting action.
     *
     * @author Jan Flink
     */
    private final class TreeSortAction extends Action {

        private int direction;

        protected TreeSortAction(int direction) {
            super();
            this.direction = direction;
            setChecked(componentsTreeViewerComparator.getDirection() == direction);
        }

        @Override
        public void run() {
            if (componentsTreeViewer.getControl().isVisible()) {
                componentsTreeViewerComparator.setDirection(direction);
                componentsTreeViewer.refresh();
            }
            if (groupsTreeViewer.getControl().isVisible()) {
                groupsTreeViewerComparator.setDirection(direction);
                groupsTreeViewer.refresh();
            }
            setSortActionsChecked();
        }

    }

    private void setSortActionsChecked() {
        if (componentsTreeViewer.getControl().isVisible()) {
            sortAscendingName.setChecked(componentsTreeViewerComparator.getDirection() == AuthorizationConstants.ASCENDING);
            sortDescendingName.setChecked(componentsTreeViewerComparator.getDirection() == AuthorizationConstants.DESCENDING);
        }
        if (groupsTreeViewer.getControl().isVisible()) {
            sortAscendingName.setChecked(groupsTreeViewerComparator.getDirection() == AuthorizationConstants.ASCENDING);
            sortDescendingName.setChecked(groupsTreeViewerComparator.getDirection() == AuthorizationConstants.DESCENDING);
        }

    }
    // BY GROUP HELPER CLASSES

    /**
     * Content Provider for the group tree.
     *
     * @author Oliver Seebach
     * @author Robert Mischke
     */
    private class AuthorizationGroupContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getChildren(Object groupObject) {
            if (!(groupObject instanceof AuthorizationAccessGroup)) {
                return new Object[0];
            }
            AuthorizationAccessGroup group = (AuthorizationAccessGroup) groupObject;
            return localComponentRegistrationService.listAuthorizationSelectorsForAccessGroup(group, true).toArray();
        }

        @Override
        public Object[] getElements(Object arg0) {
            List<AuthorizationAccessGroup> authorizationGroups = new ArrayList<>(
                authorizationService.listAccessibleGroups(true));
            Collections.sort(authorizationGroups);
            return authorizationGroups.toArray();
        }

        @Override
        public Object getParent(Object arg0) {
            return null;
        }

        @Override
        public boolean hasChildren(Object groupObject) {
            if (!(groupObject instanceof AuthorizationAccessGroup)) {
                return false;
            }
            AuthorizationAccessGroup group = (AuthorizationAccessGroup) groupObject;
            return !localComponentRegistrationService.listAuthorizationSelectorsForAccessGroup(group, true).isEmpty();
        }

    }


    /**
     * Content Provider for the component tree.
     *
     * @author Oliver Seebach
     * @author Robert Mischke
     */
    private class AuthorizationComponentContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getChildren(Object o) {
            if (!(o instanceof ComponentAuthorizationSelector)) {
                return new Object[0];
            }
            ComponentAuthorizationSelector selector = (ComponentAuthorizationSelector) o;
            final Collection<AuthorizationAccessGroup> accessGroups = localComponentRegistrationService
                .getComponentPermissionSet(selector, true).getAccessGroups();
            return accessGroups.toArray();
        }

        @Override
        public Object[] getElements(Object arg0) {
            List<NamedComponentAuthorizationSelector> components = localComponentRegistrationService
                .listAuthorizationSelectorsForRemotableComponents();
            return components.toArray();
        }

        @Override
        public Object getParent(Object arg0) {
            return null;
        }

        @Override
        public boolean hasChildren(Object o) {
            if (!(o instanceof ComponentAuthorizationSelector)) {
                return false;
            }
            ComponentAuthorizationSelector selector = (ComponentAuthorizationSelector) o;
            return !localComponentRegistrationService.getComponentPermissionSet(selector, true).isLocalOnly();
        }

    }
}
