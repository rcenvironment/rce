/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;


/**
 * Provides utility methods for loading workflow files.
 * 
 * @author Heinrich Wendel
 * @author Christian Weiss
 * @author Doreen Seider
 */
public final class WorkflowDescriptionValidator {

    private static final Log LOGGER = LogFactory.getLog(WorkflowDescriptionValidator.class.getName());
    
    private static DistributedComponentKnowledgeService componentKnowledgeService = ServiceUtils
        .createFailingServiceProxy(DistributedComponentKnowledgeService.class);
    
    private static WorkflowHostService workflowHostService = ServiceUtils.createFailingServiceProxy(WorkflowHostService.class);
    
    private static PlatformService platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);
    
    @Deprecated
    public WorkflowDescriptionValidator() {}
    
    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        componentKnowledgeService = newInstance;
    }
    
    protected void bindWorkflowHostService(WorkflowHostService workflowHostServiceBound) {
        workflowHostService = workflowHostServiceBound;
    }

    protected void bindPlatformService(PlatformService platformServiceBound) {
        platformService = platformServiceBound;
    }

    
    private static String getInvalidNodeOrNull(WorkflowDescription wd, User user){
        // FIXME: error messages

        boolean valid = true;
        NodeIdentifier ni = null;
        
        if (wd != null) {

            ni = wd.getControllerNode();
            // controller platform valid
            if (ni != null && !workflowHostService.getWorkflowHostNodesAndSelf().contains(ni)) {
                LOGGER.warn("workflow not valid: node " + ni + " not available");
                valid = false;
            }
            
            DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
            
            // component noded valid
            for (WorkflowNode node : wd.getWorkflowNodes()) {
                if (node.getComponentDescription().getNode() == null) {
                    ni = platformService.getLocalNodeId();
                } else {
                    ni = node.getComponentDescription().getNode();
                }
                if (!ComponentUtils.hasComponent(compKnowledge.getAllInstallations(), node.getComponentDescription().getIdentifier(), ni)) {
                    LOGGER.error(StringUtils.format("Workflow description is invalid. Component '%s' not installed on node %s",
                        node.getName(), ni.getAssociatedDisplayName()));
                    valid = false;
                    break;
                }
            }
        } else {
            LOGGER.warn("Workflow description is invalid, becuase it is null");
            valid = false;
        }

        // If the result is invalid, the missing node is returned.
        // Otherwise null is returned.
        if (!valid && ni != null){
            return ni.getAssociatedDisplayName();
        } else {
            return null;
        }
    }
    
    /**
     * Returns if the current description is valid so that it can be run.
     * 
     * @param wd WorkflowDescription to run.
     * @param user acting user 
     * @return <code>true</code> if it is valid, otherwise <code>false</code>
     */
    public static boolean isWorkflowDescriptionValid(WorkflowDescription wd, User user) {
        return getInvalidNodeOrNull(wd, user) == null;
    }
    
    /**
     * Returns a node that is searched but not to be found or null.
     * This is required to show in an ErrorDialog which Node is not reachable anymore.
     * 
     * @param wd WorkflowDescription to run.
     * @param user acting user
     * @return the unreachable node or <code>null</code>
     */
    public static String findUnreachableNode(WorkflowDescription wd, User user){
        return getInvalidNodeOrNull(wd, user);
    }

}
