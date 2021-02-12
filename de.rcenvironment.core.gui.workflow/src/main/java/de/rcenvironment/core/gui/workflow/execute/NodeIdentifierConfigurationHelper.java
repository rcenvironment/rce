/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Helper class providing utility functions aiding in the configuration of a workflow description and execution.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 */
public final class NodeIdentifierConfigurationHelper {

    /**
     * Compares NodeIdentifier instances by their name.
     * 
     * @author Christian Weiss
     */
    private static final class NodeIdentifierNameComparator implements Comparator<LogicalNodeId> {

        private static final int LT = -1;

        private static final int GT = 1;

        @Override
        public int compare(LogicalNodeId o1, LogicalNodeId o2) {
            int result;
            if (o1 == null && o2 == null) {
                result = 0;
            } else if (o1 == null) {
                result = LT;
            } else if (o2 == null) {
                result = GT;
            } else {
                result = o1.getLogicalNodeIdString().compareTo(o2.getLogicalNodeIdString());
            }
            return result;
        }
    }

    /** Comparator to sort NodeIdentifier instances by their name. */
    private static final NodeIdentifierNameComparator NODE_IDENTIFIER_COMPARATOR = new NodeIdentifierNameComparator();

    private Collection<DistributedComponentEntry> installations;

    private ServiceRegistryAccess serviceRegistryAccess;

    private DistributedComponentKnowledge compKnowledge;

    public NodeIdentifierConfigurationHelper() {
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentSnapshot();
        installations = compKnowledge.getAllInstallations();
    }

    /**
     * @return platforms
     */
    public List<LogicalNodeId> getWorkflowControllerNodesSortedByName() {
        List<LogicalNodeId> result = new ArrayList<LogicalNodeId>(serviceRegistryAccess.getService(WorkflowHostService.class)
            .getLogicalWorkflowHostNodes());
        Collections.sort(result, NODE_IDENTIFIER_COMPARATOR);
        return result;
    }

    /**
     * Returns a list of platforms the component is installed on, sorted by their name.
     * 
     * @param compDesc Description of the component.
     * @return List of platform the component is installed on.
     */
    // TODO 9.0.0: replace with LogicalNodeSessionId?
    public Map<LogicalNodeId, Integer> getTargetPlatformsForComponent(ComponentDescription compDesc) {

        return ComponentUtils.getNodesForComponent(installations, compDesc);

    }

    /**
     * @param nodes nodes to sort
     * @return sorted list of nodes
     */
    public List<LogicalNodeId> sortNodes(List<LogicalNodeId> nodes) {
        Collections.sort(nodes, NODE_IDENTIFIER_COMPARATOR);
        return nodes;
    }

    /**
     * 
     * Refresh installations when connection is reestablished or lost.
     * 
     */
    public synchronized void refreshInstallations() {

        compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentSnapshot();
        installations = compKnowledge.getAllInstallations();

    }
}
