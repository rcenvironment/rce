/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.Collection;
import java.util.HashSet;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Default mock for {@link DistributedComponentKnowledge}.
 * 
 * @author Doreen Seider
 */
public class DistributedComponentKnowledgeDefaultStub implements DistributedComponentKnowledge {

    @Override
    public Collection<ComponentInstallation> getPublishedInstallationsOnNode(ResolvableNodeId nodeId) {
        return new HashSet<>();
    }

    @Override
    public Collection<ComponentInstallation> getAllPublishedInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<ComponentInstallation> getLocalInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<ComponentInstallation> getAllInstallations() {
        return new HashSet<>();
    }

}
