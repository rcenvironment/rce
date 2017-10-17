/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Collection;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Immutable holder for a consistent set of {@link ComponentInstallation} knowledge, representing which installations were published by
 * known nodes.
 * 
 * @author Robert Mischke
 */
public interface DistributedComponentKnowledge {

    /**
     * @param nodeId the node of interest
     * @return all {@link ComponentInstallation}s published by the given node; TODO clarify: add special treatment of local node or not?
     */
    Collection<ComponentInstallation> getPublishedInstallationsOnNode(ResolvableNodeId nodeId);

    /**
     * @return all {@link ComponentInstallation}s published by known and reachable nodes
     */
    Collection<ComponentInstallation> getAllPublishedInstallations();

    /**
     * @return unfiltered list of {@link ComponentInstallation}s on the local node
     */
    Collection<ComponentInstallation> getLocalInstallations();

    /**
     * Convenient method merging results from {@link DistributedComponentKnowledge#getAllPublishedInstallations()} and
     * {@link DistributedComponentKnowledge#getLocalInstallations()}.
     * 
     * @return all {@link ComponentInstallation}s published by known and reachable nodes and unfiltered list of
     *         {@link ComponentInstallation}s on the local node
     */
    Collection<ComponentInstallation> getAllInstallations();
}
