/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.editor.pages;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeNode;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputDatumHandling;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition.InputExecutionContraint;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.ComponentNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.EndpointMappingNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingNode;
import de.rcenvironment.core.gui.integration.workflowintegration.editor.mappingtreenodes.MappingType;
import de.rcenvironment.core.workflow.execution.function.EndpointAdapter;

/**
 * Content Provider for {@link MappingPage}'s TreeViewer.
 *
 * @author Kathrin Schaffert
 * @author Jan Flink
 */
public class MappingTreeContentProvider implements ITreeContentProvider {

    private TreeNode root = new TreeNode("root");

    private String originMappings;

    public MappingTreeContentProvider() {
        super();
    }

    @Override
    public Object[] getChildren(Object parent) {

        // Hide components that does not have configurable properties or not connected end points.
        if (parent.equals(root) && root.hasChildren()) {
            return Arrays.stream(root.getChildren()).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast)
                .filter(ComponentNode::hasChildren).toArray();
        }

        if (parent instanceof TreeNode && ((TreeNode) parent).hasChildren()) {
            return ((TreeNode) parent).getChildren();
        }
        return new TreeNode[0];
    }

    @Override
    public Object[] getElements(Object arg0) {
        return getChildren(arg0);
    }

    @Override
    public Object getParent(Object object) {
        if (object instanceof MappingNode) {
            return ((MappingNode) object).getParent();
        }
        if (object instanceof ComponentNode) {
            return getRoot();
        }
        return false;
    }

    @Override
    public boolean hasChildren(Object object) {
        if (object instanceof TreeNode) {
            final TreeNode node = (TreeNode) object;
            return node.hasChildren();
        }
        return false;
    }

    public TreeNode getRoot() {
        return root;
    }

    private List<ComponentNode> getComponentNodes() {
        return Arrays.stream(getChildren(getRoot())).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast)
            .collect(Collectors.toList());
    }

    private List<MappingNode> getAllMappingNodes() {
        return getComponentNodes().stream().flatMap(componentNode -> Arrays.stream(componentNode.getChildren()))
            .map(MappingNode.class::cast).collect(Collectors.toList());
    }

    private List<MappingNode> getMappingNodesOfComponent(ComponentNode componentNode) {
        return Arrays.stream(getChildren(componentNode)).filter(MappingNode.class::isInstance).map(MappingNode.class::cast)
            .collect(Collectors.toList());
    }

    protected List<String> getMappedNamesOfOtherCheckedNodes(MappingNode node) {
        return getAllMappingNodes().stream().filter(node.getClass()::isInstance).filter(n -> !n.equals(node)).filter(MappingNode::isChecked)
            .map(MappingNode::getExternalName)
            .map(String::toLowerCase)
            .collect(Collectors.toList());
    }

    protected boolean hasInvalidMappedNames() {
        return !getAllMappingNodes().stream().allMatch(MappingNode::isNameValid);
    }

    protected void updateValidationOfOtherNodes(MappingNode node) {
        List<MappingNode> sameTypeNodes = getAllMappingNodes().stream().filter(node.getClass()::isInstance).collect(Collectors.toList());
        sameTypeNodes.stream()
            .forEach(
                n -> n.setNameValid(!n.isChecked() || !getMappedNamesOfOtherCheckedNodes(n).contains(n.getExternalName().toLowerCase())));
    }

    public void addInput(ComponentNode componentNode, EndpointDescription input) {
        if (!componentNode.hasChildNode(input.getName(), MappingType.INPUT)) {
            InputDatumHandling handling = null;
            InputExecutionContraint constraint = null;

            if (input.getMetaData().containsKey(ComponentConstants.METADATAKEY_INPUT_USAGE)) {
                // migration code: usage (required, initial, optional) -> consuming vs.
                // immutable
                // and required vs. required if connected
                handling = InputDatumHandling.Single;
                constraint = InputExecutionContraint.Required;
            } else {
                if (input.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING) != null) {
                    handling =
                        InputDatumHandling.valueOf(input.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING));
                } else {
                    handling = input.getEndpointDefinition().getDefaultInputDatumHandling();
                }
                if (input.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                    constraint = InputExecutionContraint
                        .valueOf(input.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
                } else {
                    constraint = input.getEndpointDefinition().getDefaultInputExecutionConstraint();
                }
            }
            componentNode.addChildNode(
                MappingNode.createInputMappingNode(componentNode, input.getName(), input.getDataType(), handling, constraint));
        }

    }

    private ComponentNode getComponentNodeOrGenerate(String componentName, WorkflowNodeIdentifier workflowNodeIdentifier) {
        Optional<ComponentNode> node = getComponentNode(workflowNodeIdentifier);
        if (node.isPresent()) {
            return node.get();
        }
        ComponentNode newNode = new ComponentNode(root, workflowNodeIdentifier, componentName);
        addComponentNode(newNode);
        return newNode;
    }

    private Optional<ComponentNode> getComponentNode(WorkflowNodeIdentifier workflowNodeIdentifier) {
        return getComponentNodes().stream().filter(n -> n.getWorkflowNodeIdentifier().equals(workflowNodeIdentifier)).findFirst();
    }

    private void addComponentNode(ComponentNode newNode) {
        List<TreeNode> children =
            Arrays.stream(getChildren(getRoot())).filter(TreeNode.class::isInstance).map(TreeNode.class::cast).collect(Collectors.toList());
        children.add(newNode);
        root.setChildren(children.stream().toArray(TreeNode[]::new));
    }

    public void removeChildNode(ComponentNode componentNode, String nodeName, MappingType mappingType) {
        if (componentNode.hasChildNode(nodeName, mappingType)) {
            componentNode.removeChildNode(nodeName, mappingType);
        }

    }

    public void addOutput(ComponentNode componentNode, EndpointDescription output) {
        if (!componentNode.hasChildNode(output.getName(), MappingType.OUTPUT)) {
            componentNode.addChildNode(
                MappingNode.createOutputMappingNode(componentNode, output.getName(), output.getDataType()));
        }
    }

    public void removeComponenteNode(WorkflowNodeIdentifier workflowNodeIdentifier) {
        Optional<ComponentNode> node = getComponentNode(workflowNodeIdentifier);
        if (node.isPresent()) {
            removeComponenteNode(node.get());
        }
    }

    private void removeComponenteNode(ComponentNode componentNode) {
        List<TreeNode> children =
            Arrays.stream(root.getChildren()).filter(TreeNode.class::isInstance).map(TreeNode.class::cast).collect(Collectors.toList());
        children.remove(componentNode);
        root.setChildren(children.stream().toArray(TreeNode[]::new));
    }

    private List<MappingNode> getCheckedMappingNodes() {
        return getAllMappingNodes().stream().filter(MappingNode::isChecked).collect(Collectors.toList());

    }

    public List<EndpointAdapter> getEndpointAdapters() {
        return getCheckedMappingNodes().stream().filter(EndpointMappingNode.class::isInstance).map(EndpointMappingNode.class::cast)
            .map(EndpointMappingNode::getEndpointAdapter).collect(Collectors.toList());
    }

    public void restoreCheckedMappingNodes(List<EndpointAdapter> endpointAdapters) {
        endpointAdapters.stream().forEach(adapter -> {
            Optional<ComponentNode> componentNode = getComponentNode(new WorkflowNodeIdentifier(adapter.getWorkflowNodeIdentifier()));
            if (!componentNode.isPresent()) {
                return;
            }
            Optional<EndpointMappingNode> mappingNode = getMappingNodesOfComponent(componentNode.get()).stream()
                .filter(EndpointMappingNode.class::isInstance).map(EndpointMappingNode.class::cast)
                .filter(node -> node.getEndpointAdapter().equals(adapter)).findFirst();
            if (mappingNode.isPresent()) {
                mappingNode.get().setChecked(true);
                mappingNode.get().setExternalName(adapter.getExternalName());
            }
        });
        originMappings = getMappingsAsString();
    }


    protected void updateContent(WorkflowDescription workflow) {
        workflow.getWorkflowNodes().stream().filter(n -> !n.isEnabled()).forEach(n -> {
            workflow.removeWorkflowNodeAndRelatedConnections(n);
            removeComponenteNode(n.getIdentifierAsObject());
        });
        for (WorkflowNode node : workflow.getWorkflowNodes()) {
            ComponentNode componentNode = getComponentNodeOrGenerate(node.getName(), node.getIdentifierAsObject());
            for (EndpointDescription input : node.getInputDescriptionsManager().getEndpointDescriptions()) {
                if (!input.isConnected()) {
                    addInput(componentNode, input);
                } else {
                    removeChildNode(componentNode, input.getName(), MappingType.INPUT);
                }
            }
            for (EndpointDescription output : node.getOutputDescriptionsManager().getEndpointDescriptions()) {
                if (!output.isConnected()) {
                    addOutput(componentNode, output);
                } else {
                    removeChildNode(componentNode, output.getName(), MappingType.OUTPUT);
                }
            }
        }
    }

    public boolean hasChanges() {
        String currentMappings = getMappingsAsString();
        return !currentMappings.equals(originMappings);
    }

    private String getMappingsAsString() {
        return getCheckedMappingNodes().stream().sorted().map(MappingNode::toString).collect(Collectors.joining());
    }

}
