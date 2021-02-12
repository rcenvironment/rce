/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;

/**
 * Provides information about an executing instance like a component or a workflow.
 *
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface ExecutionInformation extends Serializable {

    /**
     * @return identifier of the associates instance
     */
    String getExecutionIdentifier();

    /**
     * @return name of the associates instance
     */
    String getInstanceName();

    /**
     * @return {@link InstanceNodeSessionId} of host node
     */
    LogicalNodeId getNodeId();

    /**
     * @return {@link InstanceNodeSessionId} of the node which is the storage node for the execution
     */
    // TODO (p2) switched to NetworkDestination as it is needed by calling code, but is this actually appropriate here? - misc_ro
    NetworkDestination getStorageNetworkDestination();
}
