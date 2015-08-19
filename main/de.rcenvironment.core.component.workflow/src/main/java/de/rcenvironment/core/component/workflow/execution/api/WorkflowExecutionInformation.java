/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;


/**
 * Workflow specific extension of {@link ExecutionInformation}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionInformation extends ExecutionInformation {

    /**
     * @return time the execution was started
     */
    long getStartTime();

    /**
     * @return the underlying {@link WorkflowDescription}
     */
    WorkflowDescription getWorkflowDescription();
    
    /**
     * @param identifier execution identifier of the component instance to get information for
     * @return {@link ComponentExecutionInformation} for the component instance
     */
    ComponentExecutionInformation getComponentExecutionInformation(String identifier);
    
    /**
     * @return {@link NodeIdentifier} of the instance the execution was started from
     */
    NodeIdentifier getNodeIdStartedExecution();
    
    /**
     * @return additional information optionally provided at workflow start
     */
    String getAdditionalInformationProvidedAtStart();
    
}
