/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.spi;

import java.util.Collection;

import de.rcenvironment.core.communication.nodeproperties.NodeProperty;

/**
 * A listener for low-level changes to the known set of distributed node properties. Node properties with a value of "null" are passed on
 * as-is, without special treatment.
 * 
 * @author Robert Mischke
 */
public interface RawNodePropertiesChangeListener {

    /**
     * Reports a metadata update.
     * 
     * @param newProperties the set of entries that were added or updated; these may refer both to the local or remote nodes
     */
    void onRawNodePropertiesAddedOrModified(Collection<? extends NodeProperty> newProperties);
}
