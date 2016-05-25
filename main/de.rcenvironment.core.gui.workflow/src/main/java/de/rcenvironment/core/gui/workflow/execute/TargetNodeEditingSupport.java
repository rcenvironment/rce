/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import java.util.Random;

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

    private static final int ERROR = -1;

    private final NodeIdentifierConfigurationHelper nodeIdConfigHelper;

    private final int column;

    private final ColumnViewer viewer;

    private final NodeIdentifier localNode;

    private Map<String, List<String>> values = new HashMap<String, List<String>>();

    /** Backing {@link NodeIdentifier}s sorted as the are displayed in the combo box. */
    private Map<String, List<NodeIdentifier>> nodes = new HashMap<String, List<NodeIdentifier>>();

    private DistributedComponentKnowledge compKnowledge;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private Map<String, Map<NodeIdentifier, Integer>> matchingNodeInfo = new HashMap<String, Map<NodeIdentifier, Integer>>();

    private Map<WorkflowNode, Boolean> hasVersionErrorMap = new HashMap<WorkflowNode, Boolean>();

    private Map<WorkflowNode, ComponentDescription> initialComponentDescriptions = new HashMap<WorkflowNode, ComponentDescription>();

    TargetNodeEditingSupport(final NodeIdentifierConfigurationHelper nodeIdConfigHelper, final NodeIdentifier localNode,
        final ColumnViewer viewer, final int column) {
        super(viewer);
        this.nodeIdConfigHelper = nodeIdConfigHelper;
        this.column = column;
        this.viewer = viewer;
        this.localNode = localNode;

        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
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
     * The returned value is preferably the one specified in the {@link ComponentDescription}, but if this platform is not able to execute
     * this type of node, another valid platform is chosen (which would be indicated by a return value of 'true' through
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
                // if (isNodeExactMatchRegardingComponentVersion((WorkflowNode) element, p)) {
                // result = nodes.get(((WorkflowNode) element).getIdentifier()).indexOf(p);
                if (node.equals(p)) {
                    result = nodes.get(((WorkflowNode) element).getIdentifier()).indexOf(p);
                }

                break;
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
     * Sets a remote instance (random in list) for the WorkflowNode.
     */
    public int setRemoteValue(WorkflowNode node) {
        ComponentInstallation installation = null;
        int comboindex = ERROR;

        List<NodeIdentifier> nodeIdentifierList = nodes.get(node.getIdentifier());
        List<Integer> remoteIndexList = new ArrayList<Integer>();

        for (int i = 0; i < nodeIdentifierList.size(); i++) {

            if (nodes.get(node.getIdentifier()).get(i) != null) {

                if (!(nodes.get(node.getIdentifier()).get(i).equals(localNode))) {

                    if (isNodeExactMatchRegardingComponentVersion(node, nodes
                        .get(node.getIdentifier()).get(i)) && isNodeExactMatchRegardingComponentVersion(node)) {

                        remoteIndexList.add(i);
                    }
                }
            }
        }

        if (!remoteIndexList.isEmpty()) {

            Random random = new Random();
            int randomIndex = remoteIndexList.get(random.nextInt(remoteIndexList.size()));

            installation = ComponentUtils.getComponentInstallationForNode(
                node.getComponentDescription().getIdentifier(),
                compKnowledge.getPublishedInstallationsOnNode(
                    nodes.get(node.getIdentifier()).get(randomIndex)),
                nodes.get(node.getIdentifier()).get(randomIndex));
            comboindex = randomIndex;

            setValue(node, randomIndex);
        }
        if (installation != null) {
            node.getComponentDescription().setComponentInstallationAndUpdateConfiguration(installation);
        }

        return comboindex;
    }

    /**
     * Builds and returns the list of available platforms for the given {@link WorkflowNode}.
     * 
     * @param wfNode The {@link WorkflowNode}.
     * @return The list of strings representing the available platforms for the given {@link WorkflowNode}.
     */
    protected List<String> getValues(WorkflowNode wfNode) {

        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class).getCurrentComponentKnowledge();

        if (!wfNode.isInit()) {
            initialComponentDescriptions.put(wfNode, wfNode.getComponentDescription().clone());
            wfNode.setInit(true);
        }

        boolean hasVersionError = false;
        if (hasVersionErrorMap.containsKey(wfNode)) {
            if (hasVersionErrorMap.get(wfNode).equals(true)) {
                // return values.get(wfNode.getIdentifier());
                hasVersionError = true;
            }
        }
        synchronized (nodes) {
            if (!nodes.containsKey((wfNode.getIdentifier()))) {
                nodes.put(wfNode.getIdentifier(), new ArrayList<NodeIdentifier>());
            }
            nodes.get(wfNode.getIdentifier()).clear();
        }

        // Nodes don't get refreshed at the moment when the selected node has a version error.
        if (!hasVersionError) {
            matchingNodeInfo.put(wfNode.getIdentifier(),
                nodeIdConfigHelper.getTargetPlatformsForComponent(wfNode.getComponentDescription()));

        } else {
            matchingNodeInfo.put(wfNode.getIdentifier(),
                nodeIdConfigHelper.getTargetPlatformsForComponent(initialComponentDescriptions.get(wfNode)));

        }
        List<NodeIdentifier> sortedTargetNodes = new ArrayList<NodeIdentifier>(matchingNodeInfo.get(wfNode.getIdentifier()).keySet());

        nodeIdConfigHelper.sortNodes(sortedTargetNodes);

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

        values.put(wfNode.getIdentifier(), nodeValues);

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

    public Map<WorkflowNode, Boolean> getHasVersionErrorMap() {
        return hasVersionErrorMap;
    }

}
