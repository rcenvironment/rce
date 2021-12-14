/*
 * Copyright 2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.gui.palette.ComponentKnowledgeUpdateListener;
import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.ToolGroupAssignment;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.AccessibleComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.ComponentNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.GroupNode;
import de.rcenvironment.core.gui.palette.view.palettetreenodes.PaletteTreeNode;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Content Provider for Palette view's TreeViewer.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class PaletteViewContentProvider implements ITreeContentProvider {

    private PaletteView paletteView;

    private PaletteTreeNode rootNode;

    private ToolGroupAssignment assignment;

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    private LogicalNodeId localNode;

    private Set<DistributedComponentEntry> currentToolInstallations;

    public PaletteViewContentProvider(PaletteView paletteView) {
        this.paletteView = paletteView;
        this.assignment = new ToolGroupAssignment();

        this.serviceRegistryAccess = getServiceRegistryPublisherAccess();

        DistributedComponentKnowledgeService distributedComponentKnowledgeService = serviceRegistryAccess
            .getService(DistributedComponentKnowledgeService.class);

        this.localNode = getLocalNodeId();

        this.currentToolInstallations = getCurrentToolInstallations(distributedComponentKnowledgeService);

        registerChangeListeners();
    }

    protected Set<DistributedComponentEntry> getCurrentToolInstallations(
        DistributedComponentKnowledgeService distributedComponentKnowledgeService) {
        return new HashSet<>(ComponentUtils.eliminateComponentInterfaceDuplicates(
            distributedComponentKnowledgeService.getCurrentSnapshot().getAllInstallations(), localNode));
    }

    protected LogicalNodeId getLocalNodeId() {
        return ServiceRegistry.createAccessFor(this).getService(PlatformService.class).getLocalDefaultLogicalNodeId();
    }

    protected Set<DistributedComponentEntry> getCurrentToolInstallations() {
        return currentToolInstallations;
    }

    // For testing purposes.
    protected void setCurrentToolInstallations(Set<DistributedComponentEntry> currentToolInstallations) {
        this.currentToolInstallations = currentToolInstallations;
    }

    private LogicalNodeId getLocalNode() {
        return localNode;
    }

    public ToolGroupAssignment getAssignment() {
        return assignment;
    }

    protected void setAssignment(ToolGroupAssignment assignment) {
        this.assignment = assignment;
    }

    public PaletteView getPaletteView() {
        return paletteView;
    }

    /**
     * Registers an event listener for network changes as an OSGi service (whiteboard pattern).
     */
    protected void registerChangeListeners() {
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class, new ComponentKnowledgeUpdateListener(this));
    }

    @Override
    public Object[] getChildren(Object parent) {

        if (!(parent instanceof PaletteTreeNode) || parent instanceof AccessibleComponentNode) {
            return new PaletteTreeNode[0];
        }

        final PaletteTreeNode node = (PaletteTreeNode) parent;

        if (!node.hasChildren()) {
            return node.getChildren();
        }
        PaletteTreeNode[] children = (PaletteTreeNode[]) node.getChildren();
        children = Arrays.stream(children)
            .filter(child -> !child.isOfflineComponent())
            .map(PaletteTreeNode.class::cast).toArray(PaletteTreeNode[]::new);

        // hide empty groups
        ArrayList<PaletteTreeNode> childList = new ArrayList<>();
        for (PaletteTreeNode child : children) {
            if (child.isGroup()) {
                boolean addToList = false;
                addToList =
                    containsAnyToolNodes(child, true) || (getPaletteView().isShowEmptyGroups() && child.getGroupNode().isCustomGroup());
                if (!addToList) {
                    continue;
                }
            }
            childList.add(child);
        }
        return childList.toArray();
    }


    @Override
    public Object[] getElements(Object object) {
        return getChildren(object);
    }

    @Override
    public Object getParent(Object object) {
        if (object instanceof PaletteTreeNode) {
            return ((PaletteTreeNode) object).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object object) {
        if (object instanceof PaletteTreeNode && ((PaletteTreeNode) object).isGroup()) {
            PaletteTreeNode node = (PaletteTreeNode) object;
            if (node.getChildren() == null) {
                return false;
            }
            return Arrays.stream(node.getChildren()).anyMatch(GroupNode.class::isInstance) || containsAnyToolNodes(node, true);
        }
        return false;
    }

    protected void setRootNode(PaletteTreeNode root) {
        this.rootNode = root;
    }

    private Optional<PaletteTreeNode> getDefaultGroup(ToolIdentification toolIdentification) {
        for (DistributedComponentEntry entry : getCurrentToolInstallations()) {
            String toolID = entry.getComponentInterface().getIdentifierAndVersion();
            if (!toolIdentification.getToolID().equals(toolID)) {
                continue;
            }
            String groupPath = entry.getComponentInstallation().getComponentInterface().getGroupName();
            if (!groupPath.equals(toolIdentification.getType().getTopLevelGroupName())) {
                groupPath = toolIdentification.getType().getTopLevelGroupName() + PaletteViewConstants.GROUP_STRING_SEPERATOR + groupPath;
            }
            return Optional.of(getOrCreateGroupNode(assignment.createPathArray(groupPath)));
        }
        return Optional.empty();
    }

    public void resetGroup(ComponentNode node) {
        if (node == null) {
            return;
        }
        assignment.getCustomizedAssignments().remove(node.getToolIdentification());
        PaletteTreeNode parent = node.getPaletteParent();
        parent.removeChild(node);
        Optional<PaletteTreeNode> optional = getDefaultGroup(node.getToolIdentification());
        if (optional.isPresent()) {
            PaletteTreeNode defaultGroup = optional.get();
            defaultGroup.addChild(node);
            node.setParent(defaultGroup);
        }
    }

    private AccessibleComponentNode createToolNode(PaletteTreeNode parent, DistributedComponentEntry toolEntry,
        ToolIdentification toolIdentification) {
        AccessibleComponentNode nodeToAdd = PaletteTreeNode.createToolNode(parent, toolEntry, toolIdentification);
        nodeToAdd.setParent(parent);
        return nodeToAdd;
    }

    public boolean toolIsPresent(String toolName) {
        return getCurrentToolInstallations().stream().map(DistributedComponentEntry::getDisplayName).anyMatch(toolName::equals);
    }

    public void updateGroup(AccessibleComponentNode node, PaletteTreeNode group) {

        if (node.getParent().equals(group)) {
            return;
        }

        ToolIdentification identification = ToolIdentification.createToolIdentification(node.getComponentEntry());
        assignment.getCustomizedAssignments().remove(identification);
        assignment.getCustomizedAssignments().put(identification, assignment.createPathArray(group.getQualifiedGroupName()));
        PaletteTreeNode parent = node.getPaletteParent();
        parent.removeChild(node);
        group.addChild(node);
        node.setParent(group);
    }

    protected List<GroupNode> getAllGroupNodes() {
        return getRootNode().getAllSubGroups();
    }

    public PaletteTreeNode getRootNode() {
        return rootNode;
    }

    public PaletteTreeNode getOrCreateGroupNode(String[] pathArray) {
        Optional<PaletteTreeNode> optional = getExistingGroupNode(pathArray);
        if (optional.isPresent()) {
            return optional.get();
        }
        if (pathArray.length > 1) {
            PaletteTreeNode parent = getOrCreateGroupNode(Arrays.copyOfRange(pathArray, 0, pathArray.length - 1));

            String nodeName = pathArray[pathArray.length - 1];
            PaletteTreeNode newGroupNode = PaletteTreeNode.createGroupNode(parent, nodeName);
            parent.addChild(newGroupNode);
            return newGroupNode;
        } else {
            PaletteTreeNode root = getRootNode();
            PaletteTreeNode newGroupNode = PaletteTreeNode.createGroupNode(root, pathArray[0]);
            root.addChild(newGroupNode);
            return newGroupNode;
        }
    }

    public PaletteTreeNode createGroupNode(PaletteTreeNode parent, String groupName) {
        PaletteTreeNode node = PaletteTreeNode.createGroupNode(parent, groupName);
        parent.addChild(node);
        return node;
    }

    private Optional<PaletteTreeNode> getExistingGroupNode(String[] pathArray) {
        List<GroupNode> currentGroups = getAllGroupNodes();
        for (PaletteTreeNode node : currentGroups) {
            if (node.getQualifiedGroupName().equals(assignment.createQualifiedGroupName(pathArray))) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private void addTool(ToolIdentification toolIdentification, DistributedComponentEntry entry) {
        String[] pathArray;
        if (assignment.getCustomizedAssignments().containsKey(toolIdentification)) {
            pathArray = assignment.getCustomizedAssignments().get(toolIdentification);
        } else {
            String groupPath = entry.getComponentInstallation().getComponentInterface().getGroupName();
            if (groupPath == null) {
                groupPath = toolIdentification.getType().getTopLevelGroupName();
            } else if (!groupPath.equals(toolIdentification.getType().getTopLevelGroupName())) {
                groupPath = toolIdentification.getType().getTopLevelGroupName() + PaletteViewConstants.GROUP_STRING_SEPERATOR + groupPath;
            }
            pathArray = assignment.createPathArray(groupPath);
        }
        addChildToTree(entry, pathArray, toolIdentification);
    }

    private void removeChildFromTreeViewer(ToolIdentification identification, boolean refreshGroupNode) {
        List<ComponentNode> nodeList = getAllComponentNodes(getRootNode());
        nodeList.stream().filter(node -> node.getToolIdentification().equals(identification)).forEach(node -> {
            PaletteTreeNode parent = node.getPaletteParent();
            parent.removeChild(node);
            if (refreshGroupNode) {
                refreshPaletteTreeViewer(parent);
                removeEmptyToolIntegrationGroups(parent);
            }
        });
    }

    private void addChildToTree(DistributedComponentEntry entry, String[] pathArray,
        ToolIdentification toolIdentification) {
        PaletteTreeNode groupNode = getOrCreateGroupNode(pathArray);
        AccessibleComponentNode toolNode = createToolNode(groupNode, entry, toolIdentification);
        if (!getAllComponentNodes(getRootNode()).contains(toolNode)) {
            groupNode.addChild(toolNode);
        }
    }

    public List<ComponentNode> getAllComponentNodes(PaletteTreeNode node) {
        List<ComponentNode> nodeList = new ArrayList<>();
        if (node.getChildren() == null) {
            return nodeList;
        }
        for (TreeNode child : node.getChildren()) {
            if (child instanceof ComponentNode) {
                nodeList.add((ComponentNode) child);
            } else {
                nodeList.addAll(getAllComponentNodes((PaletteTreeNode) child));
            }
        }
        return nodeList;
    }

    public void refreshPaletteView(Collection<DistributedComponentEntry> newState) {

        Display.getDefault().asyncExec(() -> {

            List<DistributedComponentEntry> newToolInstallations =
                ComponentUtils.eliminateComponentInterfaceDuplicates(newState, getLocalNode());

            Set<DistributedComponentEntry> installationsToRemove = new HashSet<>();
            Set<DistributedComponentEntry> installationsToAdd = new HashSet<>();

            installationsToRemove.addAll(getCurrentToolInstallations());
            installationsToRemove.removeAll(newToolInstallations);

            installationsToAdd.addAll(newToolInstallations);
            installationsToAdd.removeAll(getCurrentToolInstallations());

            getCurrentToolInstallations().clear();
            getCurrentToolInstallations().addAll(newToolInstallations);

            Optional<WorkflowEditor> editor = paletteView.getWorkflowEditor();

            if (editor.isPresent() && !editor.get().getViewer().getEditDomain().getActiveTool().equals(paletteView.getSelectionTool())) {
                Object o = paletteView.getPaletteTreeViewer().getStructuredSelection().getFirstElement();
                if (o instanceof AccessibleComponentNode) {
                    AccessibleComponentNode node = (AccessibleComponentNode) o;
                    if (installationsToRemove.contains(node.getComponentEntry())) {
                        paletteView.selectSelectionToolNode();
                    }
                }
            }

            paletteView.getPaletteTreeViewer().getTree().setVisible(false);
            Object[] expandedElements = paletteView.getPaletteTreeViewer().getExpandedElements();
            updateTree(installationsToAdd, installationsToRemove);
            paletteView.getPaletteTreeViewer().refresh();
            paletteView.getPaletteTreeViewer().setExpandedElements(expandedElements);
            paletteView.getPaletteTreeViewer().refresh();
            paletteView.getPaletteTreeViewer().getTree().setVisible(true);

            // If organizeGroupsDialog is open, we need to update the TableViewer.
            if (paletteView.getOrganizeGroupsDialog() != null) {
                paletteView.getOrganizeGroupsDialog().refreshTree();
            }
        });
    }

    public void updateTree(Set<DistributedComponentEntry> toolInstallationsToAdd, Set<DistributedComponentEntry> toolsToRemove) {

        if (toolsToRemove != null) {
            for (DistributedComponentEntry entry : toolsToRemove) {
                ToolIdentification identification = ToolIdentification.createToolIdentification(entry);
                removeChildFromTreeViewer(identification, true);
            }
        }
        for (DistributedComponentEntry entry : toolInstallationsToAdd) {
            ToolIdentification toolIdentification = ToolIdentification.createToolIdentification(entry);
            if (getAssignment().getCustomizedAssignments().containsKey(toolIdentification)) {
                removeChildFromTreeViewer(toolIdentification, false);
            }
            addTool(toolIdentification, entry);
        }
        for (ToolIdentification toolIdentification : getAssignment().getCustomizedAssignments().keySet()) {
            if (!containsComponent(getRootNode(), toolIdentification)) {
                PaletteTreeNode node = getOrCreateGroupNode(getAssignment().getCustomizedAssignments().get(toolIdentification));
                node.addChild(PaletteTreeNode.createOfflineComponentNode(node, toolIdentification));
            }
        }
    }

    private boolean containsComponent(TreeNode node, ToolIdentification identification) {
        if (node instanceof ComponentNode) {
            ComponentNode componentNode = (ComponentNode) node;
            if (componentNode.getToolIdentification().equals(identification)) {
                return true;
            }
        }
        if (!node.hasChildren()) {
            return false;
        }
        for (TreeNode child : node.getChildren()) {
            if (containsComponent(child, identification)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnyToolNodes(PaletteTreeNode node, boolean exludeOfflineTools) {
        if (!node.isGroup()) {
            return (node.isAccessibleComponent()) || (node.isOfflineComponent() && !exludeOfflineTools);
        }
        PaletteTreeNode[] children = (PaletteTreeNode[]) node.getChildren();
        if (children == null) {
            return false;
        }
        boolean hasToolNodes = false;
        for (PaletteTreeNode child : children) {
            hasToolNodes = containsAnyToolNodes(child, exludeOfflineTools);
            if (hasToolNodes) {
                break;
            }
        }
        return hasToolNodes;
    }

    protected PaletteTreeNode[] getSuitableGroups(AccessibleComponentNode[] nodes, boolean hideOwnGroup) {

        List<GroupNode> groupList = getRootNode().getAllSubGroups();

        groupList.removeIf(group -> ToolType.STANDARD_COMPONENT.getTopLevelGroupName().equals(group.getQualifiedGroupName()));
        groupList.removeIf(group -> Arrays.asList(PaletteViewConstants.RCE_STANDARD_GROUPS).contains(group.getQualifiedGroupName()));

        List<ToolType> selectedToolTypes =
            Arrays.stream(nodes).map(AccessibleComponentNode::getType).distinct().collect(Collectors.toList());

        selectedToolTypes.stream().forEach(type -> groupList.removeIf(group -> !getTopLevelGroup(group).isCustomGroup()
            && !group.getQualifiedGroupName().startsWith(type.getTopLevelGroupName())));

        // Hide own group, if all selected nodes are currently member of this group.
        if (hideOwnGroup && Arrays.stream(nodes).map(AccessibleComponentNode::getType).sorted().distinct().toArray().length == 1) {
            groupList.removeIf(group -> group.equals(nodes[0].getParent()));
        }

        return groupList.stream().sorted().toArray(PaletteTreeNode[]::new);
    }

    private GroupNode getTopLevelGroup(PaletteTreeNode node) {
        if (node.getPaletteParent().equals(getRootNode())) {
            return (GroupNode) node;
        }
        return getTopLevelGroup(node.getPaletteParent());
    }

    public void deleteGroup(PaletteTreeNode node) {
        if (node.hasChildren()) {
            for (TreeNode child : node.getChildren()) {
                if (child instanceof PaletteTreeNode) {
                    deleteGroup((PaletteTreeNode) child);
                }
            }
        }
        PaletteTreeNode parent = node.getPaletteParent();
        parent.removeChild(node);
        refreshPaletteTreeViewer(parent);
        removeEmptyToolIntegrationGroups(parent);
    }

    public void refreshPaletteTreeViewer(PaletteTreeNode parent) {
        getPaletteView().getPaletteTreeViewer().refresh(parent);
    }

    public boolean isExpandable(GroupNode node) {
        if (node.getChildren() == null) {
            return false;
        }
        TreeNode[] children = node.getChildren();
        return Arrays.stream(children).filter(PaletteTreeNode.class::isInstance).map(PaletteTreeNode.class::cast)
            .anyMatch(child -> child.isGroup() || containsAnyToolNodes(child, true));
    }

    protected void addAllParentGroupNodes(PaletteTreeNode node, List<PaletteTreeNode> nodes) {
        if (node.getParent() == null) {
            return;
        }
        PaletteTreeNode parent = node.getPaletteParent();
        if (parent.isGroup()) {
            nodes.add(parent);
            addAllParentGroupNodes(parent, nodes);
        }
    }

    protected void setExpandedState(TreeNode[] nodes, boolean expanded) {
        if (nodes == null) {
            return;
        }
        Arrays.stream(nodes).filter(GroupNode.class::isInstance)
            .map(GroupNode.class::cast).forEach(node -> {
                setExpandedState(node, expanded);
                setExpandedState(node.getChildren(), expanded);
            });
    }

    public void setExpandedState(PaletteTreeNode node, boolean expanded) {
        getPaletteView().getPaletteTreeViewer().setExpandedState(node, expanded);
        getPaletteView().getPaletteTreeViewer().update(node, new String[] { IBasicPropertyConstants.P_IMAGE });
    }

    protected void removeEmptyToolIntegrationGroups(PaletteTreeNode node) {
        if (!node.hasChildren() && node.isGroup() && !node.getGroupNode().isCustomGroup()) {
            PaletteTreeNode parent = node.getPaletteParent();
            deleteGroup(node);
            removeEmptyToolIntegrationGroups(parent);
        }
    }

    @Override
    public void dispose() {
        serviceRegistryAccess.dispose();
    }

    protected ServiceRegistryPublisherAccess getServiceRegistryPublisherAccess() {
        return ServiceRegistry.createPublisherAccessFor(this);
    }

    public boolean getExpandedState(PaletteTreeNode node) {
        return getPaletteView().getPaletteTreeViewer().getExpandedState(node);
    }

}
