/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Provides information about the execution of a workflow or component.
 * 
 * @author Doreen Seider
 */
public interface ExecutionContext extends Serializable {

    /**
     * @return identifier of the workflow/component executed
     */
    String getExecutionIdentifier();

    /**
     * @return name of the workflow/component executed
     */
    String getInstanceName();

    /**
     * @return host node of the workflow/component executed
     */
    NodeIdentifier getNodeId();

    /**
     * @return default storage node of the workflow/component executed
     */
    NodeIdentifier getDefaultStorageNodeId();
}
