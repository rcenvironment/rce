/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

/**
 * An interface providing a way to get extended node information for a node id.
 * 
 * @author Robert Mischke
 */
public interface NodeInformationRegistry {

    /**
     * Returns a {@link NodeInformation} instance that can be queried for the information gathered
     * about the specified node.
     * 
     * @param id the id of the node
     * @return the {@link NodeInformation} interface for this node
     */
    NodeInformation getNodeInformation(String id);

}
