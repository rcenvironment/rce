/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * An interface to access mutable information that has been gathered about a node.
 * 
 * @author Robert Mischke
 */
public interface SharedNodeInformationHolder {

    /**
     * @return the display name associated with the node, or null if none is available/known
     */
    String getDisplayName();
}
