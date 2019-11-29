/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * ComboBox editing for selecting target platform for each component.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Brigitte Boden
 */
final class TargetNodeEditingSupport extends EditingSupport {

    private static final int ERROR = -1;

    private final NodeIdentifierConfigurationHelper nodeIdConfigHelper;

    private final int column;

    private final ColumnViewer viewer;

    private final LogicalNodeId localNode;

    private Map<WorkflowNodeIdentifier, List<String>> values = new HashMap<WorkflowNodeIdentifier, List<String>>();

    /**
     * Backing {@link InstanceNodeSessionId}s sorted as the are displayed in the combo box.
     */
    private Map<WorkflowNodeIdentifier, List<LogicalNodeId>> nodes = new HashMap<WorkflowNodeIdentifier, List<LogicalNodeId>>();

    private DistributedComponentKnowledge compKnowledge;

    private final ServiceRegistryAccess serviceRegistryAccess;

    private Map<WorkflowNodeIdentifier, Map<LogicalNodeId, Integer>> matchingNodeInfo =
        new HashMap<WorkflowNodeIdentifier, Map<LogicalNodeId, Integer>>();

    private Map<WorkflowNode, Boolean> hasVersionErrorMap = new HashMap<WorkflowNode, Boolean>();

    private Map<WorkflowNode, ComponentDescription> initialComponentDescriptions = new HashMap<WorkflowNode, ComponentDescription>();

    TargetNodeEditingSupport(final NodeIdentifierConfigurationHelper nodeIdConfigHelper, final LogicalNodeId localNode,
        final ColumnViewer viewer, final int column) {
        super(viewer);
        this.nodeIdConfigHelper = nodeIdConfigHelper;
        this.column = column;
        this.viewer = viewer;
        this.localNode = localNode;

        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentSnapshot();
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
        ComboBoxCellEditor editor = new ComboBoxCellEditor(((TableViewer) viewer).getTable(),
            getValues((WorkflowNode) element).toArray(new String[] {}));
        return editor;
    }

    protected boolean isNodeExactMatchRegardingComponentVersion(WorkflowNode node) {
        return isNodeExactMatchRegardingComponentVersion(node, node.getComponentDescription().getNode());
    }

    protected boolean isNodeExactMatchRegardingInitialComponentVersion(WorkflowNode node) {
        return isNodeExactMatchRegardingComponentVersion(node, initialComponentDescriptions.get(node).getNode());
    }

    protected boolean isNodeExactMatchRegardingComponentVersion(WorkflowNode wfNode, LogicalNodeId node) {
        if (node == null) {
            node = localNode;
        }
        return matchingNodeInfo.get(wfNode.getIdentifierAsObject()).containsKey(node) && matchingNodeInfo
            .get(wfNode.getIdentifierAsObject()).get(node).equals(ComponentUtils.EQUAL_COMPONENT_VERSION);
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

        LogicalNodeId node = ((WorkflowNode) element).getComponentDescription().getNode();
        if (isNodeExactMatchRegardingComponentVersion((WorkflowNode) element)) {
            if (node.equals(localNode)) {
                result = 0;
            } else {
                result = nodes.get(((WorkflowNode) element).getIdentifierAsObject()).indexOf(node);
            }
        } else {
            for (LogicalNodeId p : nodes.get(((WorkflowNode) element).getIdentifierAsObject())) {
                // if (isNodeExactMatchRegardingComponentVersion((WorkflowNode) element, p)) {
                // result = nodes.get(((WorkflowNode) element).getIdentifier()).indexOf(p);
                if (node.equals(p)) {
                    result = nodes.get(((WorkflowNode) element).getIdentifierAsObject()).indexOf(p);
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
            LogicalNodeId logicalNodeId = nodes.get(((WorkflowNode) element).getIdentifierAsObject()).get(intValue);
            if (logicalNodeId == null) {
                logicalNodeId = localNode;
            }
            if (logicalNodeId.isSameInstanceNodeAs(localNode)) {
                installation = ComponentUtils.getComponentInstallationForNode(
                    ((WorkflowNode) element).getComponentDescription().getIdentifier(),
                    compKnowledge.getAllLocalInstallations(), logicalNodeId);
            } else {
                installation = ComponentUtils.getComponentInstallationForNode(
                    ((WorkflowNode) element).getComponentDescription().getIdentifier(),
                    compKnowledge.getKnownSharedInstallationsOnNode(
                        logicalNodeId, false),
                    logicalNodeId);
            }
            if (installation != null) {
                ((WorkflowNode) element).getComponentDescription()
                    .setComponentInstallationAndUpdateConfiguration(installation);
            }
        }
    }

    /**
     * Sets a remote instance (random in list) for the WorkflowNode.
     */
    public int setRemoteValue(WorkflowNode node) {
        ComponentInstallation installation = null;
        int comboindex = ERROR;

        List<LogicalNodeId> nodeIdentifierList = nodes.get(node.getIdentifierAsObject());
        List<Integer> remoteIndexList = new ArrayList<Integer>();

        for (int i = 0; i < nodeIdentifierList.size(); i++) {

            if (nodes.get(node.getIdentifierAsObject()).get(i) != null) {

                if (!(nodes.get(node.getIdentifierAsObject()).get(i).equals(localNode))) {

                    if (isNodeExactMatchRegardingComponentVersion(node, nodes.get(node.getIdentifierAsObject()).get(i))
                        && isNodeExactMatchRegardingInitialComponentVersion(node)) {

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
                compKnowledge.getKnownSharedInstallationsOnNode(nodes.get(node.getIdentifierAsObject()).get(randomIndex), false),
                nodes.get(node.getIdentifierAsObject()).get(randomIndex));
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

        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentSnapshot();

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
            if (!nodes.containsKey((wfNode.getIdentifierAsObject()))) {
                nodes.put(wfNode.getIdentifierAsObject(), new ArrayList<LogicalNodeId>());
            }
            nodes.get(wfNode.getIdentifierAsObject()).clear();
        }

        // Nodes don't get refreshed at the moment when the selected node has a version
        // error.
        if (!hasVersionError) {
            matchingNodeInfo.put(wfNode.getIdentifierAsObject(),
                nodeIdConfigHelper.getTargetPlatformsForComponent(wfNode.getComponentDescription()));

        } else {
            matchingNodeInfo.put(wfNode.getIdentifierAsObject(),
                nodeIdConfigHelper.getTargetPlatformsForComponent(initialComponentDescriptions.get(wfNode)));

        }
        List<LogicalNodeId> sortedTargetNodes = new ArrayList<LogicalNodeId>(
            matchingNodeInfo.get(wfNode.getIdentifierAsObject()).keySet());

        nodeIdConfigHelper.sortNodes(sortedTargetNodes);

        List<String> nodeValues = new ArrayList<String>(nodes.size());
        // add the *local* option as the topmost one, if the local node supports the
        // component
        if (sortedTargetNodes.contains(localNode)) {
            nodeValues.add(enhanceNodeNameWithVersionInformation(wfNode,
                localNode.getAssociatedDisplayName() + " " + Messages.localPlatformSelectionTitle, localNode));
            nodes.get(wfNode.getIdentifierAsObject()).add(null);
        }
        // add all supporting nodes to the list of choices
        for (LogicalNodeId n : sortedTargetNodes) {
            if (n == null) {
                LogFactory.getLog(getClass()).error("NULL NodeIdentifier!");
                continue;
            }
            String nodeName = n.getAssociatedDisplayName();
            if (!n.equals(localNode)) {
                nodeValues.add(enhanceNodeNameWithVersionInformation(wfNode, nodeName, n));
                nodes.get(wfNode.getIdentifierAsObject()).add(n);
            }
        }

        values.put(wfNode.getIdentifierAsObject(), nodeValues);

        return nodeValues;
    }

    private String enhanceNodeNameWithVersionInformation(WorkflowNode wfNode, String nodeName, LogicalNodeId node) {
        if (matchingNodeInfo.get(wfNode.getIdentifierAsObject()).get(node).equals(ComponentUtils.LOWER_COMPONENT_VERSION)) {
            nodeName = nodeName + " " + Messages.older;
        } else if (matchingNodeInfo.get(wfNode.getIdentifierAsObject()).get(node)
            .equals(ComponentUtils.GREATER_COMPONENT_VERSION)) {
            nodeName = nodeName + " " + Messages.newer;
        }
        return nodeName;
    }

    public Map<WorkflowNode, Boolean> getHasVersionErrorMap() {
        return hasVersionErrorMap;
    }

}
