/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.api;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Is responsible for persistent workflow description updates.
 * 
 * @author Doreen Seider
 */
public class SimplePersistentWorkflowDescriptionUpdateService implements PersistentWorkflowDescriptionUpdateService {

    private PersistentWorkflowDescriptionUpdateService updateService;

    public SimplePersistentWorkflowDescriptionUpdateService() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        updateService = serviceRegistryAccess.getService(PersistentWorkflowDescriptionUpdateService.class);
    }
    
    @Override
    public boolean isUpdateForWorkflowDescriptionAvailable(PersistentWorkflowDescription description, boolean silent) {
        return updateService.isUpdateForWorkflowDescriptionAvailable(description, silent);
    }

    @Override
    public PersistentWorkflowDescription performWorkflowDescriptionUpdate(PersistentWorkflowDescription description) throws IOException {
        return updateService.performWorkflowDescriptionUpdate(description);
    }

    @Override
    public PersistentWorkflowDescription createPersistentWorkflowDescription(String persistentWorkflowDescription, User user)
        throws JsonParseException, IOException {
        return updateService.createPersistentWorkflowDescription(persistentWorkflowDescription, user);
    }

}
