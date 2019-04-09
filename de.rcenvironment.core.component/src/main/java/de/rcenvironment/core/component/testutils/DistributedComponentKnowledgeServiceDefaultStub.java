/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.Collection;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;

/**
 * Default mock for {@link DistributedComponentKnowledgeService}.
 * 
 * @author Doreen Seider
 */
public class DistributedComponentKnowledgeServiceDefaultStub implements DistributedComponentKnowledgeService {

    @Override
    public void updateLocalComponentInstallations(Collection<DistributedComponentEntry> allInstallations, boolean publicationEnabled) {}

    @Override
    public DistributedComponentKnowledge getCurrentSnapshot() {
        return new DistributedComponentKnowledgeDefaultStub();
    }

}
