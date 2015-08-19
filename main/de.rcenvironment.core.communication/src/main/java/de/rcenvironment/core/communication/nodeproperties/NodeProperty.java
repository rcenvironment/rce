/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties;

import de.rcenvironment.core.communication.nodeproperties.internal.NodePropertyImpl;

/**
 * Minimal interface to access a single distributed node property.
 * 
 * IMPORTANT: Two {@link NodePropertyImpl} instances are considered "equal" if their composite key is the same, ie they define the same
 * property of the same node. This is intended to support common set operations, but has the side effect that "equals" cannot be used to
 * check for equal sequence numbers and/or values.
 * 
 * @author Robert Mischke
 */
public interface NodeProperty {

    /**
     * @return the id string of the node that published this property
     */
    String getNodeIdString();

    /**
     * @return the property key
     */
    String getKey();

    /**
     * @return the property value
     */
    String getValue();

    /**
     * @return the sequential number of this property update, as set by the publisher; note that simultaneous updates of different
     *         properties may share the same sequence number
     */
    long getSequenceNo();
}
