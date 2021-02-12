/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import java.util.Collection;

import de.rcenvironment.core.communication.nodeproperties.NodePropertiesService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;

/**
 * A service that distributes public sets of {@link ComponentInstallation}s between nodes using the {@link NodePropertiesService}.
 * 
 * TODO expand description - misc_ro
 * 
 * @author Robert Mischke
 */
public interface DistributedComponentKnowledgeService {

    /**
     * Sets or updates the collection of {@link ComponentInstallation}s - local ones and those that should be made available to all
     * reachable nodes.
     * 
     * @param allInstallations the set of all local {@link ComponentInstallation}s; this information is not distributed to other nodes
     * @param publicationEnabled whether any remote publication should be done; usually false during startup and shutdown
     */
    void updateLocalComponentInstallations(Collection<DistributedComponentEntry> allInstallations,
        boolean publicationEnabled);

    /**
     * Gets the last snapshot of the known {@link DistributedComponentKnowledge}. This snapshot is equivalent to the last state that a
     * subscribed {@link DistributedComponentKnowledgeListener} would have received.
     * 
     * @return the current {@link DistributedComponentKnowledge}
     */
    DistributedComponentKnowledge getCurrentSnapshot(); // TODO rename
}
