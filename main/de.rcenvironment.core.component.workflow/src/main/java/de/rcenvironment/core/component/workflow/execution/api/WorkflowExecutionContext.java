/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Workflow-specific {@link ExecutionContext}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionContext extends ExecutionContext {

    /**
     * @return {@link WorkflowDescription} of the workflow executed
     */
    WorkflowDescription getWorkflowDescription();
    
    /**
     * @param wfNodeId workflow node id of the component within the {@link WorkflowDescription}
     * @return execution identifier of the component with the given workflow node id within the {@link WorkflowDescription}
     */
    String getCompExeIdByWfNodeId(String wfNodeId);
    
    /**
     * @return {@link NodeIdentifier} of the instance the execution was started from
     */
    NodeIdentifier getNodeIdStartedExecution();
    
    /**
     * @return additional information optionally provided at workflow start
     */
    String getAdditionalInformationProvidedAtStart();

}
