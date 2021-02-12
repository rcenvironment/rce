/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.joiner.execution;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.joiner.common.JoinerComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class JoinerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String DATATYPE = "datatype";

    private static final String CONFIGURATION = "configuration";

    private static final String MERGED = "Merged";

    private static final String IDENTIFIER = "identifier";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String NAME = "name";

    private static final String DYNAMIC_ADD_INPUTS = "addInput";

    private static final String V1_0 = "1.0";

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";
    
    private static final String V3_3 = "3.3";

    private final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return JoinerComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (silent && (persistentComponentDescriptionVersion == null
            || persistentComponentDescriptionVersion.compareTo(V1_0) < 0)) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        } else if (silent && persistentComponentDescriptionVersion.compareTo(V3_3) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        } else if (!silent && persistentComponentDescriptionVersion != null) {
            if (persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if (persistentComponentDescriptionVersion.compareTo(V3_2) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
            }
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent) throws IOException {
        if (silent && formatVersion == PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE) {
            return updateToV1(description);
        } else if (!silent && formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
            return updateToV3(description);
        } else if (!silent && formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
            switch (description.getComponentVersion()) {
            case V3_0:
                description = updateFromV3ToV31(description);
            case V3_1:
                description = updateFromV31ToV32(description);
            case V3_2:
                description = updateFromV32ToV33(description);
                break;
            default:
                break;
            }
        } else if (silent && formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
            switch (description.getComponentVersion()) {
            case V3_2:
                description = updateFromV32ToV33(description);
                break;
            default:
                break;
            }
        }
        return description;
    }

    private PersistentComponentDescription updateFromV32ToV33(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        ObjectNode node = (ObjectNode) mapper.readTree(description.getComponentDescriptionAsString());
        
        String dataType;
        String inputCount;
        if (node.has(DYNAMIC_INPUTS)) {
            ArrayNode dynInputs = (ArrayNode) node.get(DYNAMIC_INPUTS);
            ObjectNode firstInput = (ObjectNode) dynInputs.get(0);
            dataType = firstInput.get(DATATYPE).textValue();
            inputCount = String.valueOf(dynInputs.size());
        } else {
            dataType = "Float";
            inputCount = "0";
        }
        ObjectNode configuration;
        if (node.has(CONFIGURATION)) {            
            configuration = (ObjectNode) node.get(CONFIGURATION);
            configuration.removeAll();
        } else {
            configuration = node.putObject(CONFIGURATION);
        }
        configuration.put(JoinerComponentConstants.INPUT_COUNT, inputCount);
        configuration.put(JoinerComponentConstants.DATATYPE, dataType);                        

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V3_3);
        return description;
    }
    
    
    private PersistentComponentDescription updateFromV31ToV32(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);
        TextNode nameNode = (TextNode) rootNode.get(NAME);
        String nodeName = nameNode.textValue();
        if (nodeName.contains("Merger")) {
            nodeName = nodeName.replaceAll("Merger", "Joiner");
            ((ObjectNode) rootNode).set(NAME, TextNode.valueOf(nodeName));
        }

        final String joinedString = "Joined";

        ArrayNode staticOutputs = (ArrayNode) rootNode.get("staticOutputs");
        ObjectNode staticOutput = (ObjectNode) staticOutputs.get(0);
        staticOutput.set(NAME, TextNode.valueOf(joinedString));

        JsonNode dynInputs = rootNode.get("dynamicInputs");
        if (dynInputs != null) {
            for (JsonNode dynInput : dynInputs) {
                ((ObjectNode) dynInput).set("epIdentifier", TextNode.valueOf(JoinerComponentConstants.DYNAMIC_INPUT_ID));
            }
        }

        ObjectNode configuration = (ObjectNode) rootNode.get(CONFIGURATION);
        if (configuration != null) {
            String number = configuration.get(MERGED).textValue();
            configuration.remove(MERGED);
            configuration.set(joinedString, TextNode.valueOf(number));
        } else {
            configuration = mapper.createObjectNode();
            configuration.set(joinedString, TextNode.valueOf("2"));
            ((ObjectNode) rootNode).set(CONFIGURATION, configuration);
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V3_2);
        jsonParser.close();
        return description;
    }

    private PersistentComponentDescription updateFromV3ToV31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);
        JsonNode staticInputs = rootNode.get("staticInputs");
        if (staticInputs != null) {
            for (JsonNode staticInput : staticInputs) {
                String name = staticInput.get(NAME).textValue();
                String number = name.substring(name.length() - 3);
                for (JsonNode dynInput : rootNode.get(DYNAMIC_INPUTS)) {
                    if (dynInput.get(NAME).textValue().endsWith(number)) {
                        ((ObjectNode) dynInput).set(IDENTIFIER, staticInput.get(IDENTIFIER));
                    }
                }
            }
        }
        ((ObjectNode) rootNode).remove("staticInputs");

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V3_1);
        jsonParser.close();
        return description;
    }

    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription updateToV3(PersistentComponentDescription description) throws JsonParseException, IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier(DYNAMIC_INPUTS,
                JoinerComponentConstants.DYNAMIC_INPUT_ID, description);

        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);

        ArrayNode staticOutputs = JsonNodeFactory.instance.arrayNode();
        ObjectNode output = JsonNodeFactory.instance.objectNode();
        output.set(NAME, TextNode.valueOf(MERGED));
        output.set(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
        output.set(DATATYPE, TextNode.valueOf(rootNode.get(DYNAMIC_INPUTS).get(0).get(DATATYPE).textValue()));

        staticOutputs.add(output);
        ((ObjectNode) rootNode).set("staticOutputs", staticOutputs);
        ((ObjectNode) rootNode).remove("dynamicOutputs");
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V3_0);
        jsonParser.close();
        return description;
    }

    private PersistentComponentDescription updateToV1(PersistentComponentDescription description)
        throws IOException, JsonParseException,

        JsonProcessingException, JsonGenerationException, JsonMappingException {

        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);
        ArrayNode dynamicInputsNode = (ArrayNode) rootNode.findPath(DYNAMIC_ADD_INPUTS);
        ArrayNode newDynamicInputsNode = JsonNodeFactory.instance.arrayNode();

        for (JsonNode endpoint : dynamicInputsNode) {
            String newString = "Input " + endpoint.textValue()
                .substring(endpoint.textValue().indexOf("_") + 1);
            newDynamicInputsNode.add(TextNode.valueOf(newString));
        }
        ((ObjectNode) rootNode).remove(DYNAMIC_ADD_INPUTS);
        ((ObjectNode) rootNode).set(DYNAMIC_ADD_INPUTS, newDynamicInputsNode);
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V1_0);
        jsonParser.close();
        return description;
    }
}
