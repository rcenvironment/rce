/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Collection;

import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;

/**
 * A service that distributes public sets of {@link ComponentInstallation}s between nodes using the
 * {@link NodePropertiesService}.
 * 
 * TODO expand description - misc_ro
 * 
 * @author Robert Mischke
 */
public interface DistributedComponentKnowledgeService {

    /**
     * Sets or updates the collection of {@link ComponentInstallation}s - local ones and those that
     * should be made available to all reachable nodes.
     * 
     * @param allInstallations the set of all local {@link ComponentInstallation}s; this information
     *        is not distributed to other nodes
     * @param installationsToPublish the set of {@link ComponentInstallation}s to publish to all
     *        reachable nodes
     */
    void setLocalComponentInstallations(Collection<ComponentInstallation> allInstallations,
        Collection<ComponentInstallation> installationsToPublish);

    /**
     * Gets the last snapshot of the known {@link DistributedComponentKnowledge}. This snapshot is
     * equivalent to the last state that a subscribed {@link DistributedComponentKnowledgeListener} would
     * have received.
     * 
     * @return the current {@link DistributedComponentKnowledge}
     */
    DistributedComponentKnowledge getCurrentComponentKnowledge();
}
