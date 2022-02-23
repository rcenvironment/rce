/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.evaluationmemory.execution;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Doreen Seider
 */
public class EvalulationMemoryPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String STATIC_INPUTS = "staticInputs";

    private static final String V1_0 = "1.0";

    private static final String V2 = "2";

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { EvaluationMemoryComponentConstants.COMPONENT_ID };
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V2) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) { // called after "silent" was called
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V1_0:
                    description = updatefrom10To2(description);
                    break;
                default:
                }
            }
        }
        return description;
    }
    
    private PersistentComponentDescription updatefrom10To2(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        if (node.has(STATIC_INPUTS)) {
            ArrayNode endpointsJsonNode = (ArrayNode) node.get(STATIC_INPUTS);
            Iterator<JsonNode> elements = endpointsJsonNode.elements();
            while (elements.hasNext()) {
                ObjectNode endpointJsonNode = (ObjectNode) elements.next();
                if (endpointJsonNode.get("name").textValue().equals("Loop done")) {
                    elements.remove();
                    break;
                }
            }
        }
        description = new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
        description.setComponentVersion(V2);
        return description;
    }
    
}
