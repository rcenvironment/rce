/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.viewers.TreeNode;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;

/**
 * 
 * {@link MappingTreeNode}} extension representing components of the integrated workflow.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class ComponentNode extends TreeNode implements Comparable<ComponentNode> {
    
    private List<MappingNode> childNodes = new ArrayList<>();

    private String componentName;

    private WorkflowNodeIdentifier workflowNodeIdentifier;

    public ComponentNode(TreeNode root, WorkflowNodeIdentifier workflowNodeIdentifier, String componentName) {
        super(root);
        this.workflowNodeIdentifier = workflowNodeIdentifier;
        this.componentName = componentName;

        this.setParent(root);
    }

    public String getComponentName() {
        return componentName;
    }

    public WorkflowNodeIdentifier getWorkflowNodeIdentifier() {
        return workflowNodeIdentifier;
    }
    
    @Override
    public TreeNode[] getChildren() {
        return childNodes.stream().toArray(MappingNode[]::new);
    }

    @Override
    public boolean hasChildren() {
        return !childNodes.isEmpty();
    }

    public void addChildNode(MappingNode node) {
        childNodes.add(node);
    }

    public boolean hasChildNode(String name, MappingType mappingType) {
        return childNodes.stream().anyMatch(n -> n.getInternalName().equals(name) && n.getMappingType() == mappingType);
    }

    public void removeChildNode(String inputName, MappingType mappingType) {
        Optional<MappingNode> node =
            childNodes.stream().filter(n -> n.getInternalName().equals(inputName) && n.getMappingType() == mappingType).findFirst();
        if (node.isPresent()) {
            childNodes.remove(node.get());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((componentName == null) ? 0 : componentName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ComponentNode other = (ComponentNode) obj;
        if (componentName == null) {
            if (other.componentName != null) {
                return false;
            }
        } else if (!componentName.equals(other.componentName)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ComponentNode o) {
        return getComponentName().compareToIgnoreCase(o.getComponentName());
    }
}
