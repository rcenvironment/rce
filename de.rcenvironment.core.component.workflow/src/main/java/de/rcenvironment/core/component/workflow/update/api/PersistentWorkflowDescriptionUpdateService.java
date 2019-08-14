/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.update.api;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;

import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;


/**
 * Is responsible for persistent workflow description updates. 
 *
 * @author Doreen Seider
 * 
 * Note: See note in {@link RemotablePersistentComponentDescriptionUpdateService}. --seid_do
 */
public interface PersistentWorkflowDescriptionUpdateService {

    /**
     * @param description PersistentWorkflowDescription to check
     * @param silent if no dialog should pop up
     * @return <code>true</code> if update is available, otherwise <code>false</code>
     */
    boolean isUpdateForWorkflowDescriptionAvailable(PersistentWorkflowDescription description, boolean silent);
    
    /**
     * Performs the actual update.
     * @param description persistent workflow description
     * @return updated persistent descriptions
     * @throws IOException on parsing errors
     */
    PersistentWorkflowDescription performWorkflowDescriptionUpdate(PersistentWorkflowDescription description)
        throws IOException;
    
    /**
     * Creates a {@link PersistentWorkflowDescription} object.
     * @param persistentWorkflowDescription persistent workflow description in JSON format
     * @return {@link PersistentWorkflowDescription} object created on base of given JSON string
     * @throws JsonParseException if given description is corrupt
     * @throws IOException if given description is corrupt
     */
    PersistentWorkflowDescription createPersistentWorkflowDescription(String persistentWorkflowDescription)
        throws JsonParseException, IOException;

}
