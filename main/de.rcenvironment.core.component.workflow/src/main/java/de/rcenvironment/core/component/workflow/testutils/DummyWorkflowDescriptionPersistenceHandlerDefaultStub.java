/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.testutils;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;

/**
 * Default stub for {@link WorkflowDescriptionPersistenceHandler} that instantiates static services that are intended to be injected by OSGi
 * at runtime.
 * 
 * @author Doreen Seider
 */
public class DummyWorkflowDescriptionPersistenceHandlerDefaultStub extends WorkflowDescriptionPersistenceHandler {

    public DummyWorkflowDescriptionPersistenceHandlerDefaultStub(PlatformService platformService) {
        super();
        bindPlatformService(platformService);
    }

}
