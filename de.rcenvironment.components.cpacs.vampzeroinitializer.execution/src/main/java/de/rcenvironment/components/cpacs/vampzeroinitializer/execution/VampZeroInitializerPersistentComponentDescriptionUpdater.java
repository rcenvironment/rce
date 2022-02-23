/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.cpacs.vampzeroinitializer.execution;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;

import de.rcenvironment.components.cpacs.vampzeroinitializer.common.VampZeroInitializerComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Markus Kunde
 * @author Doreen Seider
 */
public class VampZeroInitializerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private final String currentVersion = V3_1;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return VampZeroInitializerComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (!silent) {
            if (persistentComponentDescriptionVersion == null || persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
                versionsToUpdate |= PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if (persistentComponentDescriptionVersion != null && persistentComponentDescriptionVersion.compareTo(currentVersion) < 0) {
                versionsToUpdate |= PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
            }
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent)
        throws IOException {
        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                description = updateToV30(description);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                description = updateFromV30ToV31(description);
            }
        }
        return description;
    }
    
    private PersistentComponentDescription updateFromV30ToV31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateSchedulingInformation(description);
        description.setComponentVersion(V3_1);
        return description;
    }
    
    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription updateToV30(PersistentComponentDescription description) throws JsonParseException, IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs", "default", description);
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "default", description);

        
        // StaticOutput CPACS=FileReference, Static Input CPACS=FileReference
        description = PersistentComponentDescriptionUpdaterUtils.addStaticOutput(description,
            VampZeroInitializerComponentConstants.OUTPUT_NAME_CPACS);

        
        // Sets all dynamic incoming channels usage to "optional."
        description = PersistentComponentDescriptionUpdaterUtils.updateDynamicInputsOptional(description);

        description.setComponentVersion(currentVersion);
        
        return description;
    }
    
    
}
