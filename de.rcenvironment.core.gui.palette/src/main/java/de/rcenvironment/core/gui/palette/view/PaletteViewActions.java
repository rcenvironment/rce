/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.integration.common.ShowDeactivateIntegrationHandler;
import de.rcenvironment.core.gui.integration.toolintegration.ShowIntegrationWizardHandler;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.dialogs.AddCustomGroupDialog;
import de.rcenvironment.core.gui.palette.view.dialogs.ComponentInformationDialog;
import de.rcenvironment.core.gui.palette.view.dialogs.EditCustomGroupDialog;
import de.rcenvironment.core.gui.palette.view.dialogs.ManageCustomGroupsAction;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.ComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.documentation.ToolIntegrationDocumentationGUIHelper;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Actions for the palette views tree viewer.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class PaletteViewActions {

    private final PaletteView paletteView;

    private final PaletteViewContentProvider contentProvider;

    private final Log log = LogFactory.getLog(getClass());

    private MenuManager subMenuManager;

    private Action organizeGroups;

    private Action sortAscending;

    private Action sortDescending;

    private Action collapseAll;

    private Action expandAll;

    private Action resetGroup;

    private Action integrateTool;

    private Action editToolIntegration;

    private Action editWorkflowIntegration;

    private Action deactivateIntegration;

    private Action deleteEmptyGroup;

    private MenuManager paletteViewMenuManager;

    private Action showEmptyGroups;

    private Action openComponentDocumentation;

    private Action exportComponentDocumentation;

    private Action editGroup;

    private Action addSubGroup;

    private Action addGroup;

    private Action showComponentInformation;

    public PaletteViewActions(PaletteView paletteView) {
        this.paletteView = paletteView;
        this.contentProvider = paletteView.getContentProvider();
        this.subMenuManager = new MenuManager("Move to Group...",
            ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_MOVE), "de.rcenvironment.palette.submenu");
    }

    protected void updateContextMenu(TreeItem[] treeItems) {

        PaletteTreeNode[] nodes = getPaletteTreeNodesFromTreeItems(treeItems);

        updateContextMenuActivation(nodes);
        updateContextMenuItems(nodes);

    }

    private void updateContextMenuItems(PaletteTreeNode[] nodes) {
        paletteViewMenuManager.removeAll();
        subMenuManager.removeAll();
        if (Arrays.stream(nodes).allMatch(PaletteTreeNode::isAccessibleComponent)) {
            updateSubMenuItems(nodes);
        }
        addActions(paletteViewMenuManager);
    }

    private void updateSubMenuItems(PaletteTreeNode[] nodes) {
        AccessibleComponentNode[] toolNodes = Arrays.stream(nodes).filter(AccessibleComponentNode.class::isInstance)
            .map(AccessibleComponentNode.class::cast).toArray(AccessibleComponentNode[]::new);
        PaletteTreeNode[] groups = contentProvider.getSuitableGroups(toolNodes, true);
        Arrays.stream(groups).filter(GroupNode.class::isInstance).map(GroupNode.class::cast)
            .forEach(group -> subMenuManager.add(new MoveToGroupAction(toolNodes, group)));
    }

    private void updateContextMenuActivation(PaletteTreeNode[] nodes) {

        boolean singleSelection = nodes.length == 1;
        resetGroup.setEnabled(false);
        editToolIntegration.setEnabled(singleSelection && nodes[0].canHandleEditEvent() && isIntegratedTool(nodes[0]));
        openComponentDocumentation.setEnabled(singleSelection && (isIntegratedTool(nodes[0]) || isIntegratedWorkflow(nodes[0])));
        exportComponentDocumentation.setEnabled(singleSelection && (isIntegratedTool(nodes[0]) || isIntegratedWorkflow(nodes[0])));
        editWorkflowIntegration.setEnabled(singleSelection && nodes[0].canHandleEditEvent() && isIntegratedWorkflow(nodes[0]));
        deleteEmptyGroup.setEnabled(singleSelection && isCustomGroup(nodes[0])
            && !contentProvider.containsAnyToolNodes(nodes[0], true));
        if (deleteEmptyGroup.isEnabled()) {
            if (nodes[0].getAllSubGroups().isEmpty()) {
                deleteEmptyGroup.setText("Delete Group");
            } else {
                deleteEmptyGroup.setText("Delete Group and Subgroups");
            }
        }
        editGroup.setEnabled(singleSelection && isCustomGroup(nodes[0]));
        addSubGroup.setEnabled(singleSelection && nodes[0].isGroup() && !isStandardComponentsTopLevelGroup(nodes[0]));
        showComponentInformation.setEnabled(singleSelection && nodes[0].isAccessibleComponent());
        if (Arrays.stream(nodes).allMatch(PaletteTreeNode::isAccessibleComponent)) {
            updateActivationForCustomizedTools(nodes);
        }
    }

    private boolean isCustomGroup(PaletteTreeNode node) {
        return node.isGroup() && node.getGroupNode().isCustomGroup();
    }

    private boolean isStandardComponentsTopLevelGroup(PaletteTreeNode node) {
        if (node.getPaletteParent().isRoot()) {
            return node.getNodeName().equals(ToolType.STANDARD_COMPONENT.getTopLevelGroupName());
        }
        return isStandardComponentsTopLevelGroup(node.getPaletteParent());
    }

    private boolean isIntegratedTool(PaletteTreeNode paletteTreeNode) {
        if (paletteTreeNode.isAccessibleComponent()) {
            return paletteTreeNode.getAccessibleComponentNode().getType().equals(ToolType.INTEGRATED_TOOL);
        }
        return false;
    }

    private boolean isIntegratedWorkflow(PaletteTreeNode paletteTreeNode) {
        if (paletteTreeNode.isAccessibleComponent()) {
            return paletteTreeNode.getAccessibleComponentNode().getType().equals(ToolType.INTEGRATED_WORKFLOW);
        }
        return false;
    }

    private void updateActivationForCustomizedTools(PaletteTreeNode[] nodes) {
        Set<String> nodeNames = Arrays.stream(nodes).map(PaletteTreeNode::getNodeName).collect(Collectors.toSet());
        Set<ToolIdentification> customizedGroups = contentProvider.getAssignment().getCustomizedAssignments().keySet();
        if (customizedGroups.stream().anyMatch(item -> nodeNames.contains(item.getToolName()))) {
            resetGroup.setEnabled(true);
        }
    }

    protected void hookContextMenu() {
        paletteViewMenuManager = new MenuManager();

        TreeViewer paletteTreeViewer = paletteView.getPaletteTreeViewer();
        Menu paletteTreeMenu = paletteViewMenuManager.createContextMenu(paletteTreeViewer.getControl());
        paletteTreeViewer.getControl().setMenu(paletteTreeMenu);
        paletteView.getSite().registerContextMenu(paletteViewMenuManager, paletteTreeViewer);
    }

    private void addActions(IMenuManager menuManager) {
        if (Arrays.stream(subMenuManager.getItems()).anyMatch(IContributionItem::isEnabled)) {
            menuManager.add(subMenuManager);
        }
        if (resetGroup.isEnabled()) {
            menuManager.add(resetGroup);
        }
        if (addSubGroup.isEnabled()) {
            menuManager.add(addSubGroup);
        }
        if (editGroup.isEnabled()) {
            menuManager.add(editGroup);
        }
        if (deleteEmptyGroup.isEnabled()) {
            menuManager.add(deleteEmptyGroup);
        }
        menuManager.add(new Separator());
        menuManager.add(addGroup);
        menuManager.add(organizeGroups);
        menuManager.add(showEmptyGroups);
        if (openComponentDocumentation.isEnabled()) {
            menuManager.add(new Separator());
            menuManager.add(openComponentDocumentation);
            menuManager.add(exportComponentDocumentation);
        }
        if (editToolIntegration.isEnabled()) {
            menuManager.add(editToolIntegration);
        }
        if (editWorkflowIntegration.isEnabled()) {
            menuManager.add(editWorkflowIntegration);
        }
        menuManager.add(new Separator());
        menuManager.add(integrateTool);
        menuManager.add(deactivateIntegration);

        if (showComponentInformation.isEnabled()) {
            menuManager.add(new Separator());
            menuManager.add(showComponentInformation);
        }
    }

    protected void addToolbarItems(IToolBarManager toolBarManager) {

        PaletteToolbarFilterWidget toolbarFilter = new PaletteToolbarFilterWidget(paletteView);

        toolBarManager.add(toolbarFilter);
        toolBarManager.add(new Separator("Palette sorting"));
        toolBarManager.add(sortAscending);
        toolBarManager.add(sortDescending);
        toolBarManager.add(new Separator("Palette collapse/expand"));
        toolBarManager.add(expandAll);
        toolBarManager.add(collapseAll);
    }

    private PaletteTreeNode[] getPaletteTreeNodesFromTreeItems(TreeItem[] selectedItems) {
        return Arrays.stream(selectedItems).map(TreeItem::getData)
            .filter(PaletteTreeNode.class::isInstance).toArray(PaletteTreeNode[]::new);
    }

    protected void createActions() {

        organizeGroups = new ManageCustomGroupsAction(paletteView);

        sortAscending = new TreeSortAction("Sort groups alphabetically ascending",
            ImageManager.getInstance().getImageDescriptor(StandardImages.SORT_ALPHA_ASC), PaletteTreeViewerComparator.SORT_BY_NAME_ASC);
        sortAscending.setChecked(true); // default when opening the view

        sortDescending =
            new TreeSortAction("Sort groups alphabetically descending",
                ImageManager.getInstance().getImageDescriptor(StandardImages.SORT_ALPHA_DESC),
                PaletteTreeViewerComparator.SORT_BY_NAME_DESC);
        sortDescending.setChecked(false);

        collapseAll = createCollapseAllAction();

        expandAll = createExpandAllAction();

        resetGroup = createResetGroupAction();

        addGroup = createAddGroupAction();

        addSubGroup = createAddSubGroupAction();

        editGroup = createEditGroupAction();

        integrateTool = createIntegrateToolAction();

        editToolIntegration = createEditToolIntegrationAction();

        editWorkflowIntegration = createEditWorkflowIntegrationAction();

        deactivateIntegration = createDeactivateIntegrationAction();

        openComponentDocumentation = createOpenComponentDocumentationAction();

        exportComponentDocumentation = createExportComponentDocumentationAction();

        deleteEmptyGroup = createDeleteEmptyGroupAction();

        showEmptyGroups = createShowEmptyGroupsAction();
        showEmptyGroups.setChecked(paletteView.isShowEmptyGroups());

        showComponentInformation = createShowComponentInformationAction();

    }

    private Action createShowComponentInformationAction() {
        return new Action("Show Component Information...", ImageManager.getInstance().getImageDescriptor(StandardImages.INFORMATION_16)) {

            @Override
            public void run() {
                Object node = paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData();
                if (node instanceof AccessibleComponentNode) {
                    ComponentInformationDialog dialog =
                        new ComponentInformationDialog(Display.getDefault().getActiveShell(), (AccessibleComponentNode) node);
                    dialog.open();
                }
            }
        };
    }

    private Action createShowEmptyGroupsAction() {
        return new Action("Show Empty Custom Groups", IAction.AS_CHECK_BOX) {

            @Override
            public void run() {
                paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
                paletteView.setShowEmptyGroups(showEmptyGroups.isChecked());
                paletteView.getPaletteTreeViewer().refresh();
                paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
                paletteView.getPaletteTreeViewer().getTree().setVisible(true);
            }
        };
    }

    private Action createDeleteEmptyGroupAction() {
        return new Action("Delete Group", ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_DELETE)) {

            @Override
            public void run() {

                PaletteTreeNode node = (PaletteTreeNode) (paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData());
                if (!contentProvider.containsAnyToolNodes(node, false)) {
                    paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                    String logNodeName = StringUtils.format("'%s'", node.getQualifiedGroupName());
                    if (node.hasChildren()) {
                        logNodeName = StringUtils.format("%s and subgroup(s) (%s)", logNodeName,
                            String.join(", ",
                                node.getAllSubGroups().stream().sorted().map(PaletteTreeNode::getQualifiedGroupName)
                                    .toArray(String[]::new)));
                    }
                    contentProvider.deleteGroup(node);
                    paletteView.getPaletteTreeViewer().refresh();
                    paletteView.getPaletteTreeViewer().getTree().setVisible(true);
                    paletteView.selectSelectionToolNode();
                    log.debug(StringUtils.format("Deleted group %s.", logNodeName));
                } else {
                    if (MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        "Selected group contains offline components",
                        "The selected group or one of its subgroups contains at least one component that is currently "
                            + "not available on the network and thus does not appear in the palette.\n"
                            + "Would you like to open the 'Manage Custom Groups' dialog to reset the group assignment?")) {
                        organizeGroups.run();
                    }
                }
            }
        };
    }

    private Action createOpenComponentDocumentationAction() {
        return new Action("Open Component Documentation") {

            @Override
            public void run() {
                Object data = paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData();
                if (data instanceof AccessibleComponentNode) {
                    AccessibleComponentNode node = (AccessibleComponentNode) data;
                    ToolIntegrationDocumentationGUIHelper.getInstance()
                        .showComponentDocumentation(node.getComponentEntry().getComponentInterface().getIdentifierAndVersion(), false);
                    paletteView.selectSelectionToolNode();
                }

            }
        };
    }

    private Action createExportComponentDocumentationAction() {
        return new Action("Export Component Documentation") {

            @Override
            public void run() {
                Object data = paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData();
                if (data instanceof AccessibleComponentNode) {
                    AccessibleComponentNode node = (AccessibleComponentNode) data;
                    ToolIntegrationDocumentationGUIHelper.getInstance()
                        .showComponentDocumentation(node.getComponentEntry().getComponentInterface().getIdentifierAndVersion(), true);
                    paletteView.selectSelectionToolNode();
                }

            }
        };
    }

    private Action createDeactivateIntegrationAction() {
        return new Action("Deactivate an Integration...",
            ImageManager.getInstance().getImageDescriptor(StandardImages.INTEGRATION_REMOVE)) {

            @Override
            public void run() {
                ShowDeactivateIntegrationHandler handler = new ShowDeactivateIntegrationHandler();
                try {
                    handler.execute(new ExecutionEvent());
                    paletteView.selectSelectionToolNode();
                } catch (ExecutionException e) {
                    log.error("Opening tool deactivation wizard failed", e);
                }
            }

        };
    }

    private Action createEditToolIntegrationAction() {
        return new Action("Edit selected Tool Integration...",
            ImageManager.getInstance().getImageDescriptor(StandardImages.INTEGRATION_EDIT)) {

            @Override
            public void run() {
                PaletteTreeNode node = (PaletteTreeNode) (paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData());
                node.handleEditEvent();
                paletteView.selectSelectionToolNode();
            }
        };
    }

    private Action createEditWorkflowIntegrationAction() {
        return new Action("Edit selected Workflow Integration...",
            ImageManager.getInstance().getImageDescriptor(StandardImages.WORKFLOW_INTEGRATION_EDIT)) {

            @Override
            public void run() {
                PaletteTreeNode node = (PaletteTreeNode) (paletteView.getPaletteTreeViewer().getTree().getSelection()[0].getData());
                node.handleEditEvent();
                paletteView.selectSelectionToolNode();
            }
        };
    }

    private Action createIntegrateToolAction() {
        return new Action("Integrate a Tool...", ImageManager.getInstance().getImageDescriptor(StandardImages.INTEGRATION_NEW)) {

            @Override
            public void run() {
                ShowIntegrationWizardHandler handler = new ShowIntegrationWizardHandler();
                try {
                    handler.execute(new ExecutionEvent());
                    paletteView.selectSelectionToolNode();
                } catch (ExecutionException e) {
                    log.error("Opening Tool Integration wizard failed", e);
                }
            }
        };
    }

    private Action createEditGroupAction() {
        return new Action("Edit Group..", ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_EDIT)) {

            @Override
            public void run() {
                TreeItem[] selectedItems = paletteView.getPaletteTreeViewer().getTree().getSelection();
                PaletteTreeNode[] nodes = getPaletteTreeNodesFromTreeItems(selectedItems);
                if (nodes.length > 1) {
                    return;
                }
                EditCustomGroupDialog editDialog =
                    new EditCustomGroupDialog(Display.getDefault().getActiveShell(), nodes[0], contentProvider);
                editDialog.open();
                if (editDialog.isGroupUpdated()) {
                    paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                    Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
                    paletteView.getPaletteTreeViewer().refresh();
                    paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
                    paletteView.getPaletteTreeViewer().getTree().setVisible(true);
                }
            }
        };
    }

    private Action createAddSubGroupAction() {
        return new Action("Add Subgroup...", ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_ADD_SUBGROUP)) {

            @Override
            public void run() {
                TreeItem[] selectedItems = paletteView.getPaletteTreeViewer().getTree().getSelection();
                PaletteTreeNode[] nodes = getPaletteTreeNodesFromTreeItems(selectedItems);
                if (nodes.length > 1) {
                    return;
                }
                AddCustomGroupDialog addDialog =
                    new AddCustomGroupDialog(Display.getDefault().getActiveShell(), contentProvider, nodes[0], true);
                addDialog.open();
                if (addDialog.isGroupAdded()) {
                    paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                    paletteView.getPaletteTreeViewer().refresh();
                    paletteView.expandToNode(nodes[0]);
                    paletteView.getPaletteTreeViewer().getTree().setVisible(true);
                }
            }

        };
    }

    private Action createAddGroupAction() {
        return new Action("Add Custom Group...", ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_ADD)) {

            @Override
            public void run() {
                TreeItem[] selectedItems = paletteView.getPaletteTreeViewer().getTree().getSelection();
                PaletteTreeNode[] nodes = getPaletteTreeNodesFromTreeItems(selectedItems);
                if (nodes.length > 1) {
                    return;
                }
                AddCustomGroupDialog addDialog =
                    new AddCustomGroupDialog(Display.getDefault().getActiveShell(), contentProvider,
                        contentProvider.getRootNode(), false);
                addDialog.open();
                if (addDialog.isGroupAdded()) {
                    paletteView.getPaletteTreeViewer().refresh();
                }
            }

        };
    }

    private Action createExpandAllAction() {
        return new Action("Expand all", ImageManager.getInstance().getImageDescriptor(StandardImages.EXPAND_ALL)) {

            @Override
            public void run() {
                paletteView.expandAll();
            }
        };
    }

    private Action createCollapseAllAction() {
        return new Action("Collapse all", ImageManager.getInstance().getImageDescriptor(StandardImages.COLLAPSE_ALL)) {

            @Override
            public void run() {
                paletteView.collapseAll();
            }
        };
    }

    private Action createResetGroupAction() {
        return new Action("Reset to Default Group", ImageManager.getInstance().getImageDescriptor(StandardImages.PALETTE_GROUPS_RESTORE)) {

            @Override
            public void run() {
                TreeItem[] selectedItems = paletteView.getPaletteTreeViewer().getTree().getSelection();
                PaletteTreeNode[] nodes = getPaletteTreeNodesFromTreeItems(selectedItems);
                paletteView.getPaletteTreeViewer().getTree().setVisible(false);
                Arrays.stream(nodes).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast)
                    .forEach(node -> {
                        Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
                        PaletteTreeNode oldParent = node.getPaletteParent();
                        contentProvider.resetGroup(node);
                        contentProvider.removeEmptyToolIntegrationGroups(oldParent);
                        paletteView.getPaletteTreeViewer().refresh();
                        paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
                        paletteView.expandToNode(node);
                    });
                paletteView.getPaletteTreeViewer().getTree().setVisible(true);
                paletteView.selectSelectionToolNode();
                paletteView.setFocus();
                log.debug(StringUtils.format("Group assignment of component(s) '%s' reset to default group(s).",
                    String.join(", ",
                        Arrays.stream(nodes).filter(AccessibleComponentNode.class::isInstance).map(AccessibleComponentNode.class::cast)
                            .sorted()
                            .map(node -> StringUtils.format("%s (%s)", node.getDisplayName(), node.getToolIdentification().getToolID()))
                            .toArray(String[]::new))));
            }

        };
    }

    private final class MoveToGroupAction extends Action {

        private final AccessibleComponentNode[] toolNodes;

        private final PaletteTreeNode group;

        private MoveToGroupAction(AccessibleComponentNode[] toolNodes, GroupNode group) {
            super(group.getQualifiedGroupName(), ImageDescriptor.createFromImage(group.getMenuIcon()));
            this.toolNodes = toolNodes;
            this.group = group;
        }

        @Override
        public void run() {
            paletteView.getPaletteTreeViewer().getTree().setVisible(false);
            Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
            Arrays.stream(toolNodes)
                .forEach(toolNode -> {
                    PaletteTreeNode oldParent = toolNode.getPaletteParent();
                    if (group.getQualifiedGroupName().equals(toolNode.getGroupPathPrefix() + toolNode.getPredefinedGroup())) {
                        contentProvider.resetGroup(toolNode);
                    } else {
                        contentProvider.updateGroup(toolNode, group);
                    }
                    contentProvider.removeEmptyToolIntegrationGroups(oldParent);
                });
            paletteView.getPaletteTreeViewer().refresh();
            paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
            paletteView.expandToNode(group);
            paletteView.getPaletteTreeViewer().getTree().setVisible(true);
            paletteView.selectSelectionToolNode();
            paletteView.setFocus();
            log.debug(StringUtils.format("Moved component(s) '%s' to group '%s'.",
                String.join(", ",
                    Arrays.stream(toolNodes).sorted()
                        .map(node -> StringUtils.format("%s (%s)", node.getDisplayName(), node.getToolIdentification().getToolID()))
                        .toArray(String[]::new)),
                group.getQualifiedGroupName()));
        }
    }

    private final class TreeSortAction extends Action {

        private int direction;

        protected TreeSortAction(String text, ImageDescriptor imageDescriptor, int direction) {
            super();
            this.setText(text);
            this.setImageDescriptor(imageDescriptor);
            this.direction = direction;
        }

        @Override
        public void run() {
            paletteView.getPaletteTreeViewer().getTree().setVisible(false);
            Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
            paletteView.getPaletteTreeViewerComparator().setDirection(direction);
            paletteView.getPaletteTreeViewer().refresh();
            paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
            paletteView.getPaletteTreeViewer().refresh();
            paletteView.getPaletteTreeViewer().getTree().setVisible(true);
            setSortActionsChecked();
        }
    }

    protected void setSortActionsChecked() {
        sortAscending
            .setChecked(paletteView.getPaletteTreeViewerComparator().getDirection() == PaletteTreeViewerComparator.SORT_BY_NAME_ASC);
        sortDescending
            .setChecked(paletteView.getPaletteTreeViewerComparator().getDirection() == PaletteTreeViewerComparator.SORT_BY_NAME_DESC);

    }

}
