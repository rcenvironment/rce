/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.sql.execution;

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jackson.JsonParseException;

import de.rcenvironment.components.sql.common.SqlComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 */
public class SQLPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private final String currentVersion = "3.0";

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return ArrayUtils.addAll(ArrayUtils.addAll(SqlComponentConstants.COMPONENT_IDS_COMMAND, SqlComponentConstants.COMPONENT_IDS_READER),
            SqlComponentConstants.COMPONENT_IDS_WRITER);
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        if (!silent
            && (persistentComponentDescriptionVersion == null
            || persistentComponentDescriptionVersion.compareTo(currentVersion) < 0)) {
            return PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
        }
        return PersistentDescriptionFormatVersion.NONE;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent)
        throws IOException {
        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                return secondUpdate(description);
            }
        }
        return description;
    }

    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription secondUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs", "default", description);
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "default", description);

        description.setComponentVersion(currentVersion);
        return description;
    }
}
