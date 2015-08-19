/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.update.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdateService}.
 * 
 * @author Doreen Seider
 */
public class PersistentComponentDescriptionUpdateServiceImpl implements PersistentComponentDescriptionUpdateService {

    private Map<String, PersistentComponentDescriptionUpdater> updaters = Collections
        .synchronizedMap(new HashMap<String, PersistentComponentDescriptionUpdater>());
    
    private Map<Boolean, Map<String, Integer>> versionsToUpdate = new HashMap<Boolean, Map<String, Integer>>();
    
    public PersistentComponentDescriptionUpdateServiceImpl() {
        versionsToUpdate.put(true, Collections.synchronizedMap(new HashMap<String, Integer>()));
        versionsToUpdate.put(false, Collections.synchronizedMap(new HashMap<String, Integer>()));
    }
    
    @AllowRemoteAccess
    @Override
    public int getFormatVersionsAffectedByUpdate(List<PersistentComponentDescription> descriptions, Boolean silent) {
        int updateAvailable = PersistentDescriptionFormatVersion.NONE;

        if (descriptions != null) {
            for (PersistentComponentDescription description : descriptions) {
                PersistentComponentDescriptionUpdater updater = findFirstMatchingUpdater(description.getComponentIdentifier());
                if (versionsToUpdate.get(silent).containsKey(getVersionMapIdentifier(description))) {
                    updateAvailable = updateAvailable | versionsToUpdate.get(silent).get(getVersionMapIdentifier(description));
                } else if (updater != null) {
                    int update = updater.getFormatVersionsAffectedByUpdate(description.getComponentVersion(), silent);
                    updateAvailable = updateAvailable | update;

                    versionsToUpdate.get(silent).put(getVersionMapIdentifier(description), update);
                }
            }
        }
        return updateAvailable;
    }
    
    private String getVersionMapIdentifier(PersistentComponentDescription description) {
        return description.getComponentIdentifier() + description.getComponentVersion();
    }
    
    private boolean isUpdateNeeded(PersistentComponentDescription description,
        boolean silent, int formatVersion, PersistentComponentDescriptionUpdater updater) {
        if (!versionsToUpdate.get(silent).containsKey(getVersionMapIdentifier(description))) {
            versionsToUpdate.get(silent).put(getVersionMapIdentifier(description),
                updater.getFormatVersionsAffectedByUpdate(description.getComponentVersion(), silent));
        }
        return (versionsToUpdate.get(silent).get(getVersionMapIdentifier(description)) & formatVersion) == formatVersion;
    }
    
    private PersistentComponentDescriptionUpdater findFirstMatchingUpdater(String compIdentifier) {
        // iterate over all entries should be fine, because the amount of updaters shouldn't be more than 10 (07/2013)
        for (String key : updaters.keySet()) {
            if (compIdentifier.matches(key)) {
                return updaters.get(key);
            }
        }
        
        return null;
    }

    @AllowRemoteAccess
    @Override
    public List<PersistentComponentDescription> performComponentDescriptionUpdates(Integer formatVersion,
        List<PersistentComponentDescription> descriptions, Boolean silent) throws IOException {

        List<PersistentComponentDescription> updatedDescriptions = new ArrayList<PersistentComponentDescription>();

        for (PersistentComponentDescription description : descriptions) {
            PersistentComponentDescriptionUpdater updater = findFirstMatchingUpdater(description.getComponentIdentifier());
            if (updater != null && isUpdateNeeded(description, silent, formatVersion, updater)) {
                updatedDescriptions.add(updater.performComponentDescriptionUpdate(formatVersion, description, silent));
            } else {
                updatedDescriptions.add(description);
            }
        }

        return updatedDescriptions;
    }

    protected void addPersistentComponentDescriptionUpdater(PersistentComponentDescriptionUpdater updater) {
        for (String identifier : updater.getComponentIdentifiersAffectedByUpdate()) {
            updaters.put(identifier, updater);
        }
    }

    protected void removePersistentComponentDescriptionUpdater(PersistentComponentDescriptionUpdater updater) {
        updaters.remove(updater.getComponentIdentifiersAffectedByUpdate());
    }
}
