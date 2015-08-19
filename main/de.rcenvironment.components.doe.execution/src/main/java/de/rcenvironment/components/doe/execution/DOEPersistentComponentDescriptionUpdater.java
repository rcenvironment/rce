/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.execution;

import java.io.IOException;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 */
public class DOEPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V3_1 = "3.1";

    private final String currentVersion = V3_1;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { DOEConstants.COMPONENT_ID };
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_1) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
       
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE
                && description.getComponentVersion().compareTo(V3_1) < 0) {
                description = updateToVersion31(description);
            }
        }
        return description;
    }

    private PersistentComponentDescription updateToVersion31(PersistentComponentDescription description) {
        description.setComponentVersion(currentVersion);
        return description;
    }

}
