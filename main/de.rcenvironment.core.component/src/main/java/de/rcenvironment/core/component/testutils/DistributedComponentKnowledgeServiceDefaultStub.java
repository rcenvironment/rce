/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import java.util.Collection;

import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Default mock for {@link DistributedComponentKnowledgeService}.
 * 
 * @author Doreen Seider
 */
public class DistributedComponentKnowledgeServiceDefaultStub implements DistributedComponentKnowledgeService {

    @Override
    public void setLocalComponentInstallations(Collection<ComponentInstallation> allInstallations,
        Collection<ComponentInstallation> installationsToPublish) {}

    @Override
    public DistributedComponentKnowledge getCurrentComponentKnowledge() {
        return new DistributedComponentKnowledgeDefaultStub();
    }

}
