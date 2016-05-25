/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;


/**
 * Represents a runnable component installation on an RCE node that fulfills the semantic behaviour
 * of an abstract {@link ComponentRevision}.
 * 
 * In the user interface, {@link ComponentInstallation}s are selected when running a workflow to
 * define where the individual workflow component nodes (which define a {@link ComponentRevision})
 * should be executed.
 * 
 * @author Robert Mischke
 */
public interface ComponentInstallation extends Comparable<ComponentInstallation>, Cloneable {

    /**
     * @return the node id string that defines this installation's location
     */
    String getNodeId();

    /**
     * @return the {@link ComponentRevision} that this {@link ComponentInstallation} fulfills
     */
    ComponentRevision getComponentRevision();

    /**
     * @return a string identifying this installation; only required to be unique per node
     */
    String getInstallationId();
    
    /**
     * @return the number of maximum parallel instances, or <code>null</code> if unlimited
     */
    Integer getMaximumCountOfParallelInstances();
    
    /**
     * @return <code>true</code> if {@link ComponentInstallation} is published (is made available for remote nodes), otherwise
     *         <code>false</code>
     */
    boolean getIsPublished();

}
