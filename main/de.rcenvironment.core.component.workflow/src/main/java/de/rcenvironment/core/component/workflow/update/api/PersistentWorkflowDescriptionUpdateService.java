/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.update.api;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;

import de.rcenvironment.core.authentication.User;


/**
 * Is responsible for persistent workflow description updates. 
 *
 * @author Doreen Seider
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
     * @param user user representation
     * @return {@link PersistentWorkflowDescription} object created on base of given JSON string
     * @throws JsonParseException if given description is corrupt
     * @throws IOException if given description is corrupt
     */
    PersistentWorkflowDescription createPersistentWorkflowDescription(String persistentWorkflowDescription, User user)
        throws JsonParseException, IOException;

}
