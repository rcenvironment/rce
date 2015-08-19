/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.util.List;


/**
 * Is responsible for persistent component descriptions updates.
 *
 * @author Doreen Seider
 */
public interface PersistentComponentDescriptionUpdateService {

    /**
     * @param silent if dialog shouldn't pop up 
     * @param descriptions {@link PersistentComponentDescription}s to check
     * @return logically concatenated {@link PersistentDescriptionFormatVersion} an update must be performed
     *         for
     */
    // Boolean (instead of boolean) to enable remote access
    int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent);
    
    /**
     * Performs updates for all given {@link PersistentComponentDescription}s (if needed).
     * @param formatVersion {@link PersistentDescriptionFormatVersion} the update must be performed for
     * @param descriptions given {@link PersistentComponentDescription}s to possibly update
     * @param silent if dialog shouldn't pop up 
     * @return updated {@link PersistentComponentDescription}s
     * @throws IOException on parsing errors
     */
    // Boolean and Integer (instead of boolean and int) to enable remote access
    List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
        List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException;
    
}
