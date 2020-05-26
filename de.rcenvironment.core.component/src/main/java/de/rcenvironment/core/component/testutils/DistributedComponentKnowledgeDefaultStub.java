/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.Collection;
import java.util.HashSet;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Default mock for {@link DistributedComponentKnowledge}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class DistributedComponentKnowledgeDefaultStub implements DistributedComponentKnowledge {

    @Override
    public Collection<DistributedComponentEntry> getKnownSharedInstallationsOnNode(ResolvableNodeId nodeId, boolean includeInaccessible) {
        return new HashSet<>();
    }

    @Override
    public Collection<DistributedComponentEntry> getKnownSharedInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<DistributedComponentEntry> getAllLocalInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<DistributedComponentEntry> getAllInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<DistributedComponentEntry> getSharedAccessInstallations() {
        return new HashSet<>();
    }

    @Override
    public Collection<DistributedComponentEntry> getLocalAccessInstallations() {
        return new HashSet<>();
    }

}
