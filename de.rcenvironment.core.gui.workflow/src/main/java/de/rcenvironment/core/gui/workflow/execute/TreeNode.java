/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.CCombo;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Helper class to provide content for the TreeViewer.
 * 
 * Used in {@link TreeContentProvider}. This class represents a Node in the tree.
 * 
 * @author Goekhan Guerkan
 */

public class TreeNode {

    private String name;

    private List<TreeNode> childrenNodes;

    private boolean isChildElement;

    private WorkflowNode node;

    private TreeNode fatherNode;

    private CCombo combo;

    /**
     * Constructor for TreeNode.
     * 
     * @param componentName Name of the node (this will be shown in the tree).
     * @param isChildElement True if node is a child, false if rootNode.
     */
    public TreeNode(String componentName, boolean isChildElement) {
        this.name = componentName;
        this.childrenNodes = new ArrayList<TreeNode>();
        this.isChildElement = isChildElement;

    }

    public TreeNode(String componentName, boolean isChildElement, WorkflowNode node, TreeNode fatherNode) {
        this(componentName, isChildElement);
        this.node = node;
        this.fatherNode = fatherNode;
    }

    /**
     * Adds a new TreeChildNode(WorkflowNode) to the TreeNode.
     * 
     * @param treeNode the node to add.
     */
    public void addChildNode(TreeNode treeNode) {

        childrenNodes.add(treeNode);
    }

    public String getComponentName() {
        return name;
    }

    public void setComponentName(String componentName) {
        this.name = componentName;
    }

    public List<TreeNode> getChildrenNodes() {
        return childrenNodes;
    }

    public boolean isChildElement() {
        return isChildElement;
    }

    public WorkflowNode getWorkflowNode() {
        return node;
    }

    public TreeNode getFatherNode() {
        return fatherNode;
    }

    public CCombo getCombo() {
        return combo;
    }

    public void setCombo(CCombo combo) {
        this.combo = combo;
    }

}
