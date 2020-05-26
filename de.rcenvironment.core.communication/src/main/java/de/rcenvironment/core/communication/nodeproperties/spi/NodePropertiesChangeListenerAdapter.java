/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.spi;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;

/**
 * Empty/default implementation of {@link NodePropertiesChangeListener} so listeners only need to implement the methods that they actually
 * use.
 * 
 * @author Robert Mischke
 */
public class NodePropertiesChangeListenerAdapter implements NodePropertiesChangeListener {

    @Override
    public void onReachableNodePropertiesChanged(Collection<? extends NodeProperty> addedProperties,
        Collection<? extends NodeProperty> updatedProperties, Collection<? extends NodeProperty> removedProperties) {}

    @Override
    public void onNodePropertyMapsOfNodesChanged(Map<InstanceNodeSessionId, Map<String, String>> updatedPropertyMaps) {}
}
