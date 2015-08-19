/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * This class represents the information that is exchanged between nodes when a new
 * {@link MessageChannel} is established.
 * 
 * @author Robert Mischke
 */
public interface InitialNodeInformation extends Serializable {

    /**
     * @return the unique node identifier string
     */
    String getNodeIdString();

    /**
     * Convenience method that returns the node id as a {@link NodeIdentifier}. This method may or
     * may not return the same object on repeated calls.
     * 
     * @return the wrapped unique node identifier
     */
    NodeIdentifier getNodeId();

    /**
     * @return the assigned name for this node
     */
    String getDisplayName();

    /**
     * @return true, if this node is marked as a "workflow host"; this flag singals acceptance to be
     *         used as a workflow controller, and also affects default access control parameters
     *         (for example, whether remote log access is allowed)
     * 
     *         TODO refer to central glossary?
     * 
     *         TODO replace with distributed metadata?
     */
    boolean getIsWorkflowHost();

    /**
     * @return the software version of the node; usually, the software version string in
     *         "a.b.c[.qualifier]" form
     */
    String getSoftwareVersion();

    /**
     * @return the node's preferred/"native" network protocol version; usually in "a.b" form,
     *         although future extensions may change this pattern
     */
    String getNativeProtocolVersion();

    /**
     * @return the description text to use in log output
     */
    String getLogDescription();
}
