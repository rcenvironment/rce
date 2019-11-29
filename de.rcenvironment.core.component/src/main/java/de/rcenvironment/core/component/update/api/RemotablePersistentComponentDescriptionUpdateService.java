/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.util.List;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;


/**
 * Is responsible for persistent component descriptions updates.
 *
 * @author Doreen Seider
 * 
 * Note: To check whether updates are available for a component in a workflow, update
 * {@link RemotablePersistentComponentDescriptionUpdateService}s are requested. I'm not happy with this active polling approach as
 * information are affected that are not likely to change very often in the network. Therefore, I would consider to make use of the
 * node properties: https://mantis.sc.dlr.de/view.php?id=9548 --seid_do
 */
@RemotableService
public interface RemotablePersistentComponentDescriptionUpdateService {

    /**
     * @param silent if dialog shouldn't pop up 
     * @param descriptions {@link PersistentComponentDescription}s to check
     * @return logically concatenated {@link PersistentDescriptionFormatVersion} an update must be performed
     *         for
     * @throws RemoteOperationException if called from remote and remote method call failed
     */
    // Boolean (instead of boolean) to enable remote access
    int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent)
        throws RemoteOperationException;
    
    /**
     * Performs updates for all given {@link PersistentComponentDescription}s (if needed).
     * @param formatVersion {@link PersistentDescriptionFormatVersion} the update must be performed for
     * @param descriptions given {@link PersistentComponentDescription}s to possibly update
     * @param silent if dialog shouldn't pop up 
     * @return updated {@link PersistentComponentDescription}s
     * @throws IOException on parsing errors
     * @throws RemoteOperationException if called from remote and remote method call failed
     */
    // Boolean and Integer (instead of boolean and int) to enable remote access
    List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
        List<PersistentComponentDescription> descriptions, Boolean silent)  throws RemoteOperationException, IOException;
    
}
