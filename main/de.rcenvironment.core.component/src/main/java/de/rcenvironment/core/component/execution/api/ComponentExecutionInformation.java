/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.communication.common.NodeIdentifier;


/**
 * Component specific extension of {@link ExecutionInformation}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionInformation extends ExecutionInformation {

    /**
     * @return identifier of the component
     */
    String getComponentIdentifier();
    
    /**
     * @return name of workflow instance
     */
    String getWorkflowInstanceName();
    
    /**
     * @return execution identifier of workflow instance
     */
    String getWorkflowExecutionIdentifier();
    
    /**
     * @return get {@link NodeIdentifier} of workflow instance
     */
    NodeIdentifier getWorkflowNodeId();
}
