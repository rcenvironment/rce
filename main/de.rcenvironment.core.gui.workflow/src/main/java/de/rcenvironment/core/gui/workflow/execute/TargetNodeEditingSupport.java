/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * ComboBox editing for selecting target platform for each component.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 */
final class TargetNodeEditingSupport extends EditingSupport {

    private final WorkflowExecutionConfigurationHelper helper;

    private final int column;

    private final ColumnViewer viewer;

    private final NodeIdentifier localNode;

    /** Backing {@link NodeIdentifier}s sorted as the are displayed in the combo box. */
    private final Map<String, List<NodeIdentifier>> nodes = new HashMap<String, List<NodeIdentifier>>();

    private final DistributedComponentKnowledge compKnowledge;

    private final Map<String, Map<NodeIdentifier, Integer>> matchingNodeInfo = new HashMap<String, Map<NodeIdentifier, Integer>>();

    public TargetNodeEditingSupport(final WorkflowExecutionConfigurationHelper helper, final NodeIdentifier localNode,
        final ColumnViewer viewer, final int column) {
        super(viewer);
        this.helper = helper;
        this.column = column;
        this.viewer = viewer;
        this.localNode = localNode;

        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class).getCurrentComponentKnowledge();
    }

    @Override
    protected boolean canEdit(Object arg0) {
        return column == 1;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        if (!(element instanceof WorkflowNode)) {
            return null;
        }
        ComboBoxCellEditor editor = new ComboBoxCellEditor(((TableViewer) viewer).getTable(), getValues((WorkflowNode) element)
            .toArray(new String[] {}));
        return editor;
    }

    protected boolean isNodeExactMatchRegardingComponentVersion(WorkflowNode node) {
        return isNodeExactMatchRegardingComponentVersion(node, node.getComponentDescription().getNode());
    }

    protected boolean isNodeExactMatchRegardingComponentVersion(WorkflowNode wfNode, NodeIdentifier node) {
        if (node == null) {
            node = localNode;
        }
        return matchingNodeInfo.get(wfNode.getIdentifier()).containsKey(node)
            && matchingNodeInfo.get(wfNode.getIdentifier()).get(node).equals(ComponentUtils.EQUAL_COMPONENT_VERSION);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * The returned value is preferably the one specified in the {@link ComponentDescription}, but
     * if this platform is not able to execute this type of node, another valid platform is chosen
     * (which would be indicated by a return value of 'true' through
     * {@link #isValueSuggestion(WorkflowNode)}).
     * </p>
     * 
     * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
     */
    @Override
    protected Object getValue(Object element) {

        Integer result = null;

        if (!(element instanceof WorkflowNode) || column != 1) {
            return result;
        }

        NodeIdentifier node = ((WorkflowNode) element).getComponentDescription().getNode();
        if (isNodeExactMatchRegardingComponentVersion((WorkflowNode) element)) {
            if (node.equals(localNode)) {
                result = 0;
            } else {
                result = nodes.get(((WorkflowNode) element).getIdentifier()).indexOf(node);
            }
        } else {
            for (NodeIdentifier p : nodes.get(((WorkflowNode) element).getIdentifier())) {
                if (isNodeExactMatchRegardingComponentVersion((WorkflowNode) element, p)) {
                    result = nodes.get(((WorkflowNode) element).getIdentifier()).indexOf(p);
                    break;
                }
            }
        }
        if (result == null && !nodes.isEmpty()) {
            result = 0;
        }
        return result;
    }

    @Override
    protected void setValue(Object element, Object value) {
        int intValue = ((Integer) value).intValue();
        if (intValue >= 0) {
            ComponentInstallation installation;
            if (nodes.get(((WorkflowNode) element).getIdentifier()).get(intValue) == null
                || nodes.get(((WorkflowNode) element).getIdentifier()).get(intValue).equals(localNode)) {
                installation = ComponentUtils.getComponentInstallationForNode(
                    ((WorkflowNode) element).getComponentDescription().getIdentifier(),
                    compKnowledge.getLocalInstallations(), localNode);
            } else {
                installation = ComponentUtils.getComponentInstallationForNode(
                    ((WorkflowNode) element).getComponentDescription().getIdentifier(),
                    compKnowledge.getPublishedInstallationsOnNode(
                        nodes.get(((WorkflowNode) element).getIdentifier()).get(intValue)),
                    nodes.get(((WorkflowNode) element).getIdentifier()).get(intValue));
            }
            if (installation != null) {
                ((WorkflowNode) element).getComponentDescription().setComponentInstallationAndUpdateConfiguration(installation);
            }
        }
    }

    /**
     * Builds and returns the list of available platforms for the given {@link WorkflowNode}.
     * 
     * @param wfNode The {@link WorkflowNode}.
     * @return The list of strings representing the available platforms for the given
     *         {@link WorkflowNode}.
     */
    protected List<String> getValues(WorkflowNode wfNode) {
        synchronized (nodes) {
            if (!nodes.containsKey((wfNode.getIdentifier()))) {
                nodes.put(wfNode.getIdentifier(), new ArrayList<NodeIdentifier>());
            }
            nodes.get(wfNode.getIdentifier()).clear();
        }
        matchingNodeInfo.put(wfNode.getIdentifier(), helper.getTargetPlatformsForComponent(wfNode.getComponentDescription()));

        List<NodeIdentifier> sortedTargetNodes = new ArrayList<NodeIdentifier>(matchingNodeInfo.get(wfNode.getIdentifier()).keySet());

        helper.sortNodes(sortedTargetNodes);

        List<String> nodeValues = new ArrayList<String>(nodes.size());
        // add the *local* option as the topmost one, if the local node supports the component
        if (sortedTargetNodes.contains(localNode)) {
            nodeValues.add(enhanceNodeNameWithVersionInformation(wfNode, localNode.getAssociatedDisplayName() + " "
                + Messages.localPlatformSelectionTitle, localNode));
            nodes.get(wfNode.getIdentifier()).add(null);
        }
        // add all supporting nodes to the list of choices
        for (NodeIdentifier n : sortedTargetNodes) {
            if (n == null) {
                LogFactory.getLog(getClass()).error("NULL NodeIdentifier!");
                continue;
            }
            String nodeName = n.getAssociatedDisplayName();
            if (!n.equals(localNode)) {
                nodeValues.add(enhanceNodeNameWithVersionInformation(wfNode, nodeName, n));
                nodes.get(wfNode.getIdentifier()).add(n);
            }
        }

        return nodeValues;
    }

    private String enhanceNodeNameWithVersionInformation(WorkflowNode wfNode, String nodeName, NodeIdentifier node) {
        if (matchingNodeInfo.get(wfNode.getIdentifier()).get(node).equals(ComponentUtils.LOWER_COMPONENT_VERSION)) {
            nodeName = nodeName + " " + Messages.older;
        } else if (matchingNodeInfo.get(wfNode.getIdentifier()).get(node).equals(ComponentUtils.GREATER_COMPONENT_VERSION)) {
            nodeName = nodeName + " " + Messages.newer;
        }
        return nodeName;
    }

}
