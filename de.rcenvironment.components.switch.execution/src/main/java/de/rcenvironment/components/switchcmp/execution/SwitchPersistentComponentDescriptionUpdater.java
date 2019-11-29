/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * 
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 *
 * @author David Scholz
 * @author Hendrik Abbenhaus
 */
public class SwitchPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V1_1 = "1.1";

    private static final String STATIC_INPUTS = "staticInputs";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String REQUIRED_IF_CONNCECTED = "RequiredIfConnected";

    private static final String NOT_REQUIRED = "NotRequired";
    
    private static final String REQUIRED = "Required";

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return SwitchComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V1_1) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }

        return versionsToUpdate;

    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {

        if (silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE
                && description.getComponentVersion().compareTo(V1_1) < 0) {
                description = updateToComponentVersion11(description);
            }
        }

        return description;
    }

    private PersistentComponentDescription updateToComponentVersion11(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        JsonNode staticInputs = node.get(STATIC_INPUTS);

        if (staticInputs != null) {
            for (JsonNode endpoint : staticInputs) {
                ObjectNode metaData = (ObjectNode) endpoint.get("metadata");
                if (metaData != null){
                    metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT, REQUIRED);
                }
            }
        }
        
        JsonNode dynamicInputs = node.get(DYNAMIC_INPUTS);

        if (dynamicInputs != null) {
            for (JsonNode endpoint : dynamicInputs) {
                ObjectNode metaData = (ObjectNode) endpoint.get("metadata");
                String currentConstraint = metaData.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).textValue();
                if (currentConstraint.equals(NOT_REQUIRED)){
                    metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT, REQUIRED_IF_CONNCECTED);
                }
            }
        }

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V1_1);
        return description;
    }
}
