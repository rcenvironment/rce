/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.api.SimpleCommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Helper class providing utility functions aiding in the configuration of a workflow description
 * and execution.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 */
public final class WorkflowExecutionConfigurationHelper {

    /**
     * Compares NodeIdentifier instances by their name.
     *
     * @author Christian Weiss
     */
    private static final class NodeIdentifierNameComparator implements Comparator<NodeIdentifier> {

        private static final int LT = -1;

        private static final int GT = 1;

        @Override
        public int compare(NodeIdentifier o1, NodeIdentifier o2) {
            int result;
            if (o1 == null && o2 == null) {
                result = 0;
            } else if (o1 == null) {
                result = LT;
            } else if (o2 == null) {
                result = GT;
            } else {
                result = o1.toString().compareTo(o2.toString());
            }
            return result;
        }
    }

    /** Comparator to sort NodeIdentifier instances by their name. */
    private static final NodeIdentifierNameComparator NODE_IDENTIFIER_COMPARATOR = new NodeIdentifierNameComparator();

    private final SimpleCommunicationService scs;

    private final Collection<ComponentInstallation> installations;

    private ServiceRegistryAccess serviceRegistryAccess;
    
    public WorkflowExecutionConfigurationHelper(SimpleCommunicationService scs) {
        this.scs = scs;
        
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        DistributedComponentKnowledge compKnowledge = serviceRegistryAccess.getService(DistributedComponentKnowledgeService.class)
            .getCurrentComponentKnowledge();
        installations =  compKnowledge.getAllInstallations();
    }
    

    /**
     * @return platforms
     */
    public List<NodeIdentifier> getWorkflowControllerNodesSortedByName() {
        List<NodeIdentifier> result = new ArrayList<NodeIdentifier>(serviceRegistryAccess.getService(WorkflowHostService.class)
            .getWorkflowHostNodes());
        Collections.sort(result, NODE_IDENTIFIER_COMPARATOR);
        return result;
    }

    /**
     * Returns a list of platforms the component is installed on, sorted by their name.
     * 
     * @param compDesc Description of the component.
     * @return List of platform the component is installed on.
     */
    public Map<NodeIdentifier, Integer> getTargetPlatformsForComponent(ComponentDescription compDesc) {
        return ComponentUtils.getNodesForComponent(installations, compDesc);
    }
    
    /**
     * @param nodes nodes to sort
     * @return sorted list of nodes
     */
    public List<NodeIdentifier> sortNodes(List<NodeIdentifier> nodes) {
        Collections.sort(nodes, NODE_IDENTIFIER_COMPARATOR);
        return nodes;
    }
    
}
