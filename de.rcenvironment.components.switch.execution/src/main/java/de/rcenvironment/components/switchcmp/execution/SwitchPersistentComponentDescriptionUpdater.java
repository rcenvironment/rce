/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.execution;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.switchcmp.common.SwitchComponentConstants;
import de.rcenvironment.components.switchcmp.common.SwitchCondition;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * 
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 *
 * @author David Scholz
 * @author Hendrik Abbenhaus
 * @author Kathrin Schaffert
 */
public class SwitchPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V1_1 = "1.1";

    private static final String V2_0 = "2.0";

    private static final String CURRENT_VERSION = V2_0;

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return SwitchComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
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
        } else {
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE
                && description.getComponentVersion().compareTo(V2_0) < 0) {
                description = updateToComponentVersion20(description);
            }
        }

        return description;
    }

    private PersistentComponentDescription updateToComponentVersion20(PersistentComponentDescription description)
        throws IOException {

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        // update endpoints
        JsonNode staticInput = node.get(PersistentComponentDescriptionConstants.STATIC_INPUTS);
        JsonNode staticOutputs = node.get(PersistentComponentDescriptionConstants.STATIC_OUTPUTS);

        String inputName = null;
        ObjectNode metaData = null;
        if (staticInput != null) {

            JsonNode endpoint = staticInput.get(0);
            inputName = endpoint.get("name").asText();
            String dataType = endpoint.get(PersistentComponentDescriptionConstants.DATATYPE).asText();
            JsonNode identifier = endpoint.get(PersistentComponentDescriptionConstants.IDENTIFIER);
            metaData = (ObjectNode) endpoint.get(PersistentComponentDescriptionConstants.METADATA);
            if (metaData == null) {
                metaData = JsonNodeFactory.instance.objectNode();
                metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                    EndpointDefinition.InputExecutionContraint.Required.name());
                metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                    EndpointDefinition.InputDatumHandling.Queue.name());
            }

            description =
                PersistentComponentDescriptionUpdaterUtils.addDynamicInput(description, inputName, dataType, metaData, identifier,
                    "dataToInput");
        }

        if (staticOutputs != null && inputName != null) {
            for (JsonNode endpoint : staticOutputs) {
                String name = endpoint.get("name").asText();
                if (name.equals("True")) {
                    String outputName = inputName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_CONDITION + " 1";
                    String dataType = endpoint.get(PersistentComponentDescriptionConstants.DATATYPE).asText();
                    JsonNode identifier = endpoint.get(PersistentComponentDescriptionConstants.IDENTIFIER);
                    description =
                        PersistentComponentDescriptionUpdaterUtils.addDynamicOutput(description, outputName, dataType, metaData, identifier,
                            "dataToOutput");
                }
                if (name.equals("False")) {
                    String outputName = inputName + SwitchComponentConstants.OUTPUT_VARIABLE_SUFFIX_NO_MATCH;
                    String dataType = endpoint.get(PersistentComponentDescriptionConstants.DATATYPE).asText();
                    JsonNode identifier = endpoint.get(PersistentComponentDescriptionConstants.IDENTIFIER);
                    description =
                        PersistentComponentDescriptionUpdaterUtils.addDynamicOutput(description, outputName, dataType, metaData, identifier,
                            "dataToOutput");
                }
            }
        }

        node = mapper.readTree(description.getComponentDescriptionAsString());
        ((ObjectNode) node).remove(PersistentComponentDescriptionConstants.STATIC_INPUTS);
        ((ObjectNode) node).remove(PersistentComponentDescriptionConstants.STATIC_OUTPUTS);

        // update config
        JsonNode config = node.get(PersistentComponentDescriptionConstants.CONFIGURATION);
        if (config != null) {

            ((ObjectNode) config).set("closeOutputsOnNoMatch", TextNode.valueOf(config.get("closeOutputsOnFalse").asText()));
            ((ObjectNode) config).remove("closeOutputsOnFalse");
            ((ObjectNode) config).set("closeOutputsOnConditionNumber", TextNode.valueOf(config.get("closeOutputsOnTrue").asText()));
            ((ObjectNode) config).remove("closeOutputsOnTrue");
            ((ObjectNode) config).set("selectedCondition", TextNode.valueOf("0"));
            ((ObjectNode) config).set("writeOutputKey", TextNode.valueOf("false"));

            ArrayList<SwitchCondition> arr = new ArrayList<>();
            SwitchCondition switchCondition = new SwitchCondition(1, config.get("conditionKey").asText());
            arr.add(switchCondition);
            String conditionString = mapper.writeValueAsString(arr);
            ((ObjectNode) config).set("conditionKey", TextNode.valueOf(conditionString));
        }

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V2_0);
        return description;
    }

    private PersistentComponentDescription updateToComponentVersion11(PersistentComponentDescription description)
        throws IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        JsonNode staticInputs = node.get(PersistentComponentDescriptionConstants.STATIC_INPUTS);

        if (staticInputs != null) {
            for (JsonNode endpoint : staticInputs) {
                ObjectNode metaData = (ObjectNode) endpoint.get(PersistentComponentDescriptionConstants.METADATA);
                if (metaData != null) {
                    metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                        EndpointDefinition.InputExecutionContraint.Required.name());
                }
            }
        }

        JsonNode dynamicInputs = node.get(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS);

        if (dynamicInputs != null) {
            for (JsonNode endpoint : dynamicInputs) {
                ObjectNode metaData = (ObjectNode) endpoint.get(PersistentComponentDescriptionConstants.METADATA);
                String currentConstraint = metaData.get(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT).textValue();
                if (currentConstraint.equals(EndpointDefinition.InputExecutionContraint.NotRequired.name())) {
                    metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                        EndpointDefinition.InputExecutionContraint.RequiredIfConnected.name());
                }
            }
        }

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V1_1);
        return description;
    }
}
