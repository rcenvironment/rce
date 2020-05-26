/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Represents a runnable component installation on an RCE node that fulfills the semantic behaviour of an abstract {@link ComponentRevision}
 * .
 * 
 * In the user interface, {@link ComponentInstallation}s are selected when running a workflow to define where the individual workflow
 * component nodes (which define a {@link ComponentRevision}) should be executed.
 * 
 * @author Robert Mischke
 * 
 *         Note: The concept of {@link ComponentInstallation}, {@link ComponentRevision}, and {@link ComponentInterface} are introduced
 *         later on compared to {@link ComponentDescription}. The concept was not fully implemented yet. Implementation of
 *         {@link ComponentRevision} is missing at all. Also, the concept was not applied in a way that the benefits of this approach really
 *         come through. --seid_do
 */
public interface ComponentInstallation extends Comparable<ComponentInstallation>, Cloneable {

    /**
     * @return the string form of this installation's location, which is a {@link LogicalNodeId}
     * 
     * @see LogicalNodeId#getLogicalNodeIdString()
     */
    String getNodeId();

    /**
     * @return the node id that defines this installation's location
     */
    LogicalNodeId getNodeIdObject();

    /**
     * @return the {@link ComponentRevision} of this {@link ComponentInstallation}
     */
    ComponentRevision getComponentRevision();

    /**
     * @return the {@link ComponentInterface} of this {@link ComponentInstallation} (which is determined by its {@link ComponentRevision}).
     *         Calling this method is equivalent to calling <code>getComponentInterface()</code>.
     */
    ComponentInterface getComponentInterface();

    /**
     * @return a string identifying this installation; only required to be unique per node
     */
    String getInstallationId();

    /**
     * @return the number of maximum parallel instances, or <code>null</code> if unlimited
     */
    Integer getMaximumCountOfParallelInstances();

    /**
     * @return true iff the installation is a mapped component (i.e. representing a tool from an uplink connection)
     */
    boolean isMappedComponent();

}
