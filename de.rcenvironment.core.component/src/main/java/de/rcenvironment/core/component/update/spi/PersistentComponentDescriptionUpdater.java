/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.spi;

import java.io.IOException;

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;


/**
 * Must be implemented by all components which need to update its persistent JSON descriptions in workflow files.
 *
 * @author Doreen Seider
 * 
 * Note: See note in {@link RemotablePersistentComponentDescriptionUpdateService}. --seid_do
 */
public interface PersistentComponentDescriptionUpdater {

    /**
     * @return component identifier this updater has updates for
     */
    String[] getComponentIdentifiersAffectedByUpdate();
    
    /**
     * @param persistentComponentDescriptionVersion version found
     * @param silent if dialog shouldn't pop up 
     * @return logically concatenated {@link PersistentDescriptionFormatVersion} an update must be performed for
     */
    int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent);
    
    /**
     * Performs the actual update.
     * @param formatVersion {@link PersistentDescriptionFormatVersion} the update must be performed for
     * @param description persistent descriptions of affected component
     * @param silent if dialog shouldn't pop up 
     * @return updated persistent descriptions
     * @throws IOException on parsing errors
     */
    PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException;
    
}
