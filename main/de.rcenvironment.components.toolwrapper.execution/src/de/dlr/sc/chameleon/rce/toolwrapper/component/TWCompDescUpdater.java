/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.dlr.sc.chameleon.rce.toolwrapper.component;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;

import de.dlr.sc.chameleon.rce.toolwrapper.common.CpacsComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.cpacs.utils.common.components.PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Markus Kunde
 * @author Doreen Seider
 */
// Note: Changed to a somewhat cryptic name as with the old name, the resulting .class file caused
// an excessive path length - see https://www.sistec.dlr.de/mantis/view.php?id=7459 - misc_ro
public class TWCompDescUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private final String currentVersion = V3_1;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return CpacsComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        if (!silent) {
            if (persistentComponentDescriptionVersion == null
                || persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if (persistentComponentDescriptionVersion != null
                && persistentComponentDescriptionVersion.compareTo(currentVersion) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
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
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.addStaticInput(description, "CPACS");
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.addStaticOutputCPACS(description);

        // if ConfigValue consumeCPACS==true; CPACS StaticInputs = required
        // else StaticInput CPACS=initialized
        // Delete ConfigValue consumeCPACS
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.updateConsumeCPACSFlag(description);

        // Sets all dynamic incoming channels usage to "optional."
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.updateDynamicInputsOptional(description);

        // if ConfigValue consumeDirectory==true; Directory DynamicInputs = required
        // else DynamicInput Directory=initialized
        // Delete ConfigValue consumeDirectory
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.updateConsumeDirectoryFlag(description);

        // Update ChannelID of Directory channel
        description = PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.updateDirectoryChannelId("dynamicInputs", description);
        description =
            PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree.updateDirectoryChannelId("dynamicOutputs", description);

        description.setComponentVersion(V3_0);

        return description;
    }

}
