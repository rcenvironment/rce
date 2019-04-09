/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Encapsulates the identity of an executing or finished workflow or component. Introduced to replace manual handling of identifier/location
 * pairs.
 *
 * @author Robert Mischke
 */
public interface ExecutionHandle extends Serializable {

    /**
     * @return this workflow or component execution's abstract identifier; expected to be unique on the hosting node, and with near-certain
     *         probability also globally unique
     */
    String getIdentifier();

    /**
     * @return the node id of this workflow or component execution's controller
     */
    LogicalNodeId getLocation();
}
