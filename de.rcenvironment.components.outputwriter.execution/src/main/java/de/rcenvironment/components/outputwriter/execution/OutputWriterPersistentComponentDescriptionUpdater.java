/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Doreen Seider
 * @author Kathrin Schaffert (Update 2.0 > 2.1)
 */
public class OutputWriterPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V1_1 = "1.1";

    private static final String V2_0 = "2.0";

    private static final String V2_1 = "2.1";

    private static final String CURRENT_VERSION = V2_1;

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { OutputWriterComponentConstants.COMPONENT_ID };
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int update = PersistentDescriptionFormatVersion.NONE;
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
            update = update | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return update;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent && formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
            if (description.getComponentVersion().compareTo(V1_1) < 0) {
                description = updateFromV10ToV11(description);
            }
            if (description.getComponentVersion().compareTo(V2_0) < 0) {
                description = updateFromV11ToV20(description); // NOSONAR because Sonar does not understand this code pattern
            }
            if (description.getComponentVersion().compareTo(V2_1) < 0) {
                description = updateFromV11ToV21(description);
            }
        }
        return description;
    }

    private PersistentComponentDescription updateFromV10ToV11(PersistentComponentDescription description)
        throws IOException {

        final String affectedConfigFieldName = "OWWritePath";

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configurationsNode = (ObjectNode) node.get(WorkflowDescriptionPersistenceHandler.CONFIGURATION);
        if (configurationsNode != null && configurationsNode.has(affectedConfigFieldName)) {
            configurationsNode.remove(affectedConfigFieldName);
            configurationsNode.put(affectedConfigFieldName, "${targetRootFolder}");
        }

        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V1_1);

        return description;
    }

    private PersistentComponentDescription updateFromV11ToV20(PersistentComponentDescription description) {

        // No update required, just update version
        description.setComponentVersion(V2_0);

        return description;
    }

    private PersistentComponentDescription updateFromV11ToV21(PersistentComponentDescription description)
        throws IOException {

        final String affectedConfigFieldName = "OverwriteFilesAndDirs";

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configurationsNode = (ObjectNode) node.get(WorkflowDescriptionPersistenceHandler.CONFIGURATION);
        if (configurationsNode != null) {
            configurationsNode.put(affectedConfigFieldName, "false");
        }

        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V2_1);

        return description;
    }

}
