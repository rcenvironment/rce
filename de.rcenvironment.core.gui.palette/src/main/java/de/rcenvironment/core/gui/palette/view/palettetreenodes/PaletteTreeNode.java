/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.gef.Tool;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.gui.palette.PaletteViewConstants;
import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.PaletteViewContentProvider;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Tree Node for the Palette View Tree.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public abstract class PaletteTreeNode extends TreeNode implements Comparable<PaletteTreeNode> {

    private String nodeName;

    private Optional<String> shortKey = Optional.empty();

    private Image icon;

    protected PaletteTreeNode(TreeNode parent, String nodeName) {
        super(nodeName);
        this.nodeName = nodeName;
        setParent(parent);
    }

    public static RootNode createRootNode(PaletteViewContentProvider contentProvider) {
        return new RootNode(contentProvider);
    }

    public static PaletteTreeNode createGroupNode(PaletteTreeNode parent, String nodeName) {
        return new GroupNode(parent, nodeName);
    }

    public static AccessibleComponentNode createToolNode(PaletteTreeNode parent, DistributedComponentEntry toolEntry,
        ToolIdentification toolIdentification) {
        return new AccessibleComponentNode(parent, toolEntry, toolIdentification);
    }

    public static OfflineComponentNode createOfflineComponentNode(PaletteTreeNode parent, ToolIdentification toolIdentification) {
        return new OfflineComponentNode(parent, toolIdentification);
    }

    public static CreationToolNode createBasicToolNode(PaletteTreeNode parent, String nodeName, Tool tool,
        Image icon) {
        return new CreationToolNode(parent, nodeName, tool, icon);
    }

    public String getQualifiedGroupName() {
        return getPath(nodeName, this);
    }

    private String getPath(String path, PaletteTreeNode node) {
        TreeNode parent = node.getParent();
        if (!(parent instanceof PaletteTreeNode) || node.getPaletteParent().isRoot()) {
            return path;
        }
        return getPath(((PaletteTreeNode) parent).getNodeName(), (PaletteTreeNode) parent)
            .concat(StringUtils.format("%s%s", PaletteViewConstants.GROUP_STRING_SEPERATOR, node.getNodeName()));
    }

    public abstract void handleDoubleclick(PaletteView paletteView);

    public abstract void handleWidgetSelected(WorkflowEditor editor);

    public abstract void handleEditEvent();

    public abstract boolean canHandleEditEvent();

    public abstract Optional<String> getHelpContextID();

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public boolean isRoot() {
        return this instanceof RootNode;
    }

    public boolean isGroup() {
        return this instanceof GroupNode;
    }

    public boolean isAccessibleComponent() {
        return this instanceof AccessibleComponentNode;
    }

    public boolean isOfflineComponent() {
        return this instanceof OfflineComponentNode;
    }

    public boolean isCreationTool() {
        return this instanceof CreationToolNode;
    }

    public GroupNode getGroupNode() {
        return (GroupNode) this;
    }

    public AccessibleComponentNode getAccessibleComponentNode() {
        return (AccessibleComponentNode) this;
    }

    public OfflineComponentNode getOfflineComponentNode() {
        return (OfflineComponentNode) this;
    }

    public CreationToolNode getCreationToolNode() {
        return (CreationToolNode) this;
    }

    public RootNode getRootNode() {
        return (RootNode) this;
    }

    public void addChild(PaletteTreeNode child) {
        List<TreeNode> children;
        if (this.getChildren() != null) {
            children = new ArrayList<>(Arrays.asList(this.getChildren()));
        } else {
            children = new ArrayList<>();
        }
        if (!children.contains(child)) {
            children.add(child);
        }
        this.setChildren(children.toArray(new PaletteTreeNode[children.size()]));
    }

    public void removeChild(TreeNode child) {
        if (this.hasChildren()) {
            ArrayList<TreeNode> children = new ArrayList<>(Arrays.asList(this.getChildren()));
            children.remove(child);
            this.setChildren(children.toArray(new PaletteTreeNode[children.size()]));
        }
    }

    public List<GroupNode> getAllSubGroups() {
        return getAllSubGroups(this);
    }

    public List<GroupNode> getAllSubGroups(PaletteTreeNode parent) {
        List<GroupNode> nodeList = new ArrayList<>();
        if (parent.getChildren() == null) {
            return nodeList;
        }
        Arrays.stream(parent.getChildren()).filter(GroupNode.class::isInstance).map(GroupNode.class::cast)
            .filter(PaletteTreeNode::isGroup).forEach(child -> {
                nodeList.addAll(getAllSubGroups(child));
                nodeList.add(child);
            });
        
        return nodeList;
    }

    public List<PaletteTreeNode> getSubGroups() {
        List<PaletteTreeNode> nodeList = new ArrayList<>();
        if (this.getChildren() == null) {
            return nodeList;
        }
        for (TreeNode child : this.getChildren()) {
            PaletteTreeNode node = (PaletteTreeNode) child;
            if (node.isGroup()) {
                nodeList.add(node);
            }
        }
        return nodeList;
    }

    public abstract boolean isCustomized();
    

    public Optional<Image> getIcon() {
        return Optional.ofNullable(this.icon);
    }

    public String getDisplayName() {
        return this.nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Optional<String> getShortKey() {
        return shortKey;
    }

    public void setShortKey(String shortKey) {
        this.shortKey = Optional.of(shortKey);
    }

    public RootNode getPaletteRoot() {
        if (isRoot()) {
            return getRootNode();
        } else {
            return getPaletteParent().getPaletteRoot();
        }
    }

    public PaletteViewContentProvider getContentProvider() {
        return getPaletteRoot().getContentProvider();
    }

    @Override
    public int hashCode() {
        return getQualifiedGroupName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PaletteTreeNode other = (PaletteTreeNode) obj;
        if (this.getNodeName() == null) {
            if (other.getNodeName() != null) {
                return false;
            }
        } else if (!this.getQualifiedGroupName().equals(other.getQualifiedGroupName())) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(PaletteTreeNode o) {
        return getQualifiedGroupName().compareToIgnoreCase(o.getQualifiedGroupName());
    }

    public PaletteTreeNode getPaletteParent() {
        TreeNode node = super.getParent();
        if (node instanceof PaletteTreeNode) {
            return (PaletteTreeNode) node;
        }
        throw new ClassCastException("All tree nodes in the palette tree are expected to be instance of type PaletteTreeNode.");
    }


}
