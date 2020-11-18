/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
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

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Utils for updater classes.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Markus Kunde
 * @author Kathrin Schaffert
 */
public final class PersistentComponentDescriptionUpdaterUtils {

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private PersistentComponentDescriptionUpdaterUtils() {}

    /**
     * @param direction if it is in or outputs (valid are "dynmaicInputs" and "dynamicOutputs")
     * @param identifier that shall be used
     * @param description of the component
     * @return updated description
     * @throws JsonParseException :
     * @throws IOException :
     */
    public static PersistentComponentDescription updateAllDynamicEndpointsToIdentifier(String direction, String identifier,
        PersistentComponentDescription description) throws JsonParseException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        JsonNode dynEndpoints = node.get(direction);
        if (dynEndpoints != null) {
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(PersistentComponentDescriptionConstants.EP_IDENTIFIER) == null
                    || endpoint.get(PersistentComponentDescriptionConstants.EP_IDENTIFIER).equals("null")
                    || endpoint.get(PersistentComponentDescriptionConstants.EP_IDENTIFIER).isNull()) {
                    ((ObjectNode) endpoint).remove(PersistentComponentDescriptionConstants.EP_IDENTIFIER);
                    ((ObjectNode) endpoint).set(PersistentComponentDescriptionConstants.EP_IDENTIFIER, TextNode.valueOf(identifier));
                }
            }
        }
        // Add Static endpoints
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Removes endpoints related to "outer loop done" approach. (RCE 7.1.2 -> 8.0.0).
     * 
     * @param description {@link PersistentComponentDescription} to update
     * @return updated {@link PersistentComponentDescription}
     * @throws JsonProcessingException on unexpected error
     * @throws IOException on unexpected error
     */
    public static PersistentComponentDescription removeOuterLoopDoneEndpoints(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        removeOuterLoopDoneEndpoint(node, PersistentComponentDescriptionConstants.STATIC_OUTPUTS,
            PersistentComponentDescriptionConstants.NAME, "Outer loop done");
        removeOuterLoopDoneEndpoint(node, PersistentComponentDescriptionConstants.DYNAMIC_INPUTS,
            PersistentComponentDescriptionConstants.EP_IDENTIFIER, "outerLoopDone");
        return new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }

    private static void removeOuterLoopDoneEndpoint(JsonNode node, String endpointGroup, String identifyingKey, String identifyingValue) {
        if (node.has(endpointGroup)) {
            ArrayNode endpointsJsonNode = (ArrayNode) node.get(endpointGroup);
            Iterator<JsonNode> elements = endpointsJsonNode.elements();
            while (elements.hasNext()) {
                ObjectNode endpointJsonNode = (ObjectNode) elements.next();
                if (endpointJsonNode.get(identifyingKey).textValue().equals(identifyingValue)) {
                    elements.remove();
                    break;
                }
            }
        }
    }

    /**
     * Removes endpoint character information from meta data. (RCE 7.1.2 -> 8.0.0).
     * 
     * @param description {@link PersistentComponentDescription} to update
     * @return updated {@link PersistentComponentDescription}
     * @throws JsonProcessingException on unexpected error
     * @throws IOException on unexpected error
     */
    public static PersistentComponentDescription removeEndpointCharacterInfoFromMetaData(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        removeEndpointCharacterInfoFromMetaData(node, PersistentComponentDescriptionConstants.STATIC_INPUTS);
        removeEndpointCharacterInfoFromMetaData(node, PersistentComponentDescriptionConstants.STATIC_OUTPUTS);
        removeEndpointCharacterInfoFromMetaData(node, PersistentComponentDescriptionConstants.DYNAMIC_INPUTS);
        removeEndpointCharacterInfoFromMetaData(node, PersistentComponentDescriptionConstants.DYNAMIC_OUTPUTS);
        return new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }

    private static void removeEndpointCharacterInfoFromMetaData(JsonNode node, String endpointGroup) {
        if (node.has(endpointGroup)) {
            ArrayNode endpointsJsonNode = (ArrayNode) node.get(endpointGroup);
            Iterator<JsonNode> elements = endpointsJsonNode.elements();
            while (elements.hasNext()) {
                JsonNode endpointJsonNode = elements.next();
                if (endpointJsonNode.has(PersistentComponentDescriptionConstants.METADATA)) {
                    JsonNode metaDataJsonNode = endpointJsonNode.get(PersistentComponentDescriptionConstants.METADATA);
                    Iterator<Entry<String, JsonNode>> metaDataFields = metaDataJsonNode.fields();
                    while (metaDataFields.hasNext()) {
                        Entry<String, JsonNode> nextMetaDataField = metaDataFields.next();
                        if (nextMetaDataField.getKey().equals("loopEndpointType_5e0ed1cd")) {
                            metaDataFields.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Reassigns new endpoint identifiers for certain entdpoints. (RCE 7.1.2 -> 8.0.0).
     * 
     * @param description {@link PersistentComponentDescription} to update
     * @param endpointGroup e.g. "staticInputs", "dynamicOutputs", etc.
     * @param epIdentifier identifier of the endpoint of request
     * @param epIdentifierToReplace new identifier of endpoint
     * @param epNameSuffix name suffix of endpoint of request
     * @return updated {@link PersistentComponentDescription}
     * @throws JsonProcessingException on unexpected error
     * @throws IOException on unexpected error
     */
    public static PersistentComponentDescription reassignEndpointIdentifiers(PersistentComponentDescription description,
        String endpointGroup, String epIdentifier, String epIdentifierToReplace, String epNameSuffix)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        if (node.has(endpointGroup)) {
            ArrayNode endpointsJsonNode = (ArrayNode) node.get(endpointGroup);
            Iterator<JsonNode> elements = endpointsJsonNode.elements();
            while (elements.hasNext()) {
                JsonNode endpointJsonNode = elements.next();
                if (endpointJsonNode.has(PersistentComponentDescriptionConstants.EP_IDENTIFIER)) {
                    if (endpointJsonNode.get(PersistentComponentDescriptionConstants.EP_IDENTIFIER).textValue().equals(epIdentifier)
                        && endpointJsonNode.get(PersistentComponentDescriptionConstants.NAME).textValue().endsWith(epNameSuffix)) {
                        ((ObjectNode) endpointJsonNode).put(PersistentComponentDescriptionConstants.EP_IDENTIFIER, epIdentifierToReplace);
                    }
                }
            }
        }
        return new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }

    /**
     * Updates configuration key for fault-tolerance (RCE 7.0.2 -> 7.1.0).
     * 
     * @param description {@link PersistentComponentDescription} to update
     * @return updated {@link PersistentComponentDescription}
     * @throws JsonProcessingException on unexpected error
     * @throws IOException on unexpected error
     */
    public static PersistentComponentDescription updateFaultToleranceOfLoopDriver(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode configJsonNode = node.get("configuration");
        if (configJsonNode instanceof ObjectNode) {
            ObjectNode configObjectNode = (ObjectNode) configJsonNode;
            String confgKeyFaultToleranceNAV = "loopFaultTolerance_5e0ed1cd";
            if (configObjectNode.has(confgKeyFaultToleranceNAV)) {
                String faultToleranceNAV = configObjectNode.get(confgKeyFaultToleranceNAV).textValue();
                configObjectNode.put(confgKeyFaultToleranceNAV, "Fail");
                configObjectNode.put("faultTolerance-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyMaxRerunBeforeFail = "loopRerunAndFail_5e0ed1cd";
            if (configObjectNode.has(configKeyMaxRerunBeforeFail)) {
                String faultToleranceNAV = configObjectNode.get(configKeyMaxRerunBeforeFail).textValue();
                configObjectNode.remove(configKeyMaxRerunBeforeFail);
                configObjectNode.put("maxRerunBeforeFail-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyMaxRerunBeforeDiscard = "loopRerunAndDiscard_5e0ed1cd";
            if (configObjectNode.has(configKeyMaxRerunBeforeDiscard)) {
                String faultToleranceNAV = configObjectNode.get(configKeyMaxRerunBeforeDiscard).textValue();
                configObjectNode.remove(configKeyMaxRerunBeforeDiscard);
                configObjectNode.put("maxRerunBeforeDiscard-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyFailLoopOnly = "failLoop_5e0ed1cd";
            if (configObjectNode.has(configKeyFailLoopOnly)) {
                String faultToleranceNAV = configObjectNode.get(configKeyFailLoopOnly).textValue();
                configObjectNode.remove(configKeyFailLoopOnly);
                configObjectNode.put("failLoopOnly-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyFinallyFailIfDiscarded = "finallyFail_5e0ed1cd";
            if (configObjectNode.has(configKeyFinallyFailIfDiscarded)) {
                String faultToleranceNAV = configObjectNode.get(configKeyFinallyFailIfDiscarded).textValue();
                configObjectNode.remove(configKeyFinallyFailIfDiscarded);
                configObjectNode.put("finallyFailIfDiscarded-NAV_5e0ed1cd", faultToleranceNAV);
            }
        }
        return new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }

    /**
     * Replaces the usage meta data entries with input handling and input execution constraints entries.
     * 
     * @param description {@link PersistentComponentDescription} of the component
     * @return {@link PersistentComponentDescription} with updated scheduling information
     * @throws JsonParseException on error
     * @throws IOException on error
     */
    public static PersistentComponentDescription updateSchedulingInformation(PersistentComponentDescription description)
        throws JsonParseException, IOException {

        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        JsonNode node = mapper.readTree(jsonParser);
        jsonParser.close();

        updateInputNode(node.get(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS));
        updateInputNode(node.get(PersistentComponentDescriptionConstants.STATIC_INPUTS));

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        return description;
    }

    /**
     * Updates the configuration key for "is nested loop".
     * 
     * @param description {@link PersistentComponentDescription} of the component
     * @return {@link PersistentComponentDescription} with updated scheduling information
     * @throws JsonParseException on error
     * @throws IOException on error
     */
    public static PersistentComponentDescription updateIsNestedLoop(PersistentComponentDescription description)
        throws JsonParseException, IOException {

        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        JsonNode node = mapper.readTree(jsonParser);
        jsonParser.close();

        String oldConfigKey = "isNestedLoop";
        ObjectNode configurationsNode = (ObjectNode) node.get(PersistentComponentDescriptionConstants.CONFIGURATION);
        if (configurationsNode != null && configurationsNode.has(oldConfigKey)) {
            boolean isNestedLoop = configurationsNode.get(oldConfigKey).asBoolean();
            configurationsNode.remove(oldConfigKey);
            configurationsNode.put(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, isNestedLoop);
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        return description;
    }

    private static void updateInputNode(JsonNode inputNode) {
        if (inputNode != null) {
            Iterator<JsonNode> nodeIterator = inputNode.elements();
            while (nodeIterator.hasNext()) {
                JsonNode dynInputNode = nodeIterator.next();
                ObjectNode jsonNode = (ObjectNode) dynInputNode.get(PersistentComponentDescriptionConstants.METADATA);
                JsonNode usageJsonNode = jsonNode.get(PersistentComponentDescriptionConstants.USAGE);
                if (usageJsonNode != null) {
                    String usage = usageJsonNode.textValue();
                    switch (usage) {
                    case PersistentComponentDescriptionConstants.REQUIRED:
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                            EndpointDefinition.InputDatumHandling.Single.name());
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.Required.name());
                        break;
                    case PersistentComponentDescriptionConstants.INITIAL:
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                            EndpointDefinition.InputDatumHandling.Constant.name());
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.Required.name());
                        break;
                    case "optional":
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                            EndpointDefinition.InputDatumHandling.Single.name());
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.NotRequired.name());
                        break;
                    default:
                        break;
                    }
                    jsonNode.remove(PersistentComponentDescriptionConstants.USAGE);
                }
            }
        }
    }

    /**
     * Adding staticInput "CPACS".
     * 
     * @param description of the component
     * @param inputName name of the static input to add
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addStaticInput(PersistentComponentDescription description, String inputName)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticInputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.STATIC_INPUTS);
        if (staticInputs == null) {
            staticInputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).set(PersistentComponentDescriptionConstants.STATIC_INPUTS, staticInputs);
        }
        ObjectNode staticCPACSIn = JsonNodeFactory.instance.objectNode();
        ObjectNode metaDataNode = JsonNodeFactory.instance.objectNode();
        metaDataNode.set(PersistentComponentDescriptionConstants.USAGE, TextNode.valueOf(PersistentComponentDescriptionConstants.INITIAL));
        staticCPACSIn.set(PersistentComponentDescriptionConstants.NAME, TextNode.valueOf(inputName));
        staticCPACSIn.set(PersistentComponentDescriptionConstants.DATATYPE, TextNode.valueOf("FileReference"));
        staticCPACSIn.set(PersistentComponentDescriptionConstants.METADATA, metaDataNode);
        staticCPACSIn.set(PersistentComponentDescriptionConstants.IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
        staticInputs.add(staticCPACSIn);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * 
     * Adds a static output.
     * 
     * @param description to change
     * @param outputName of the output to add
     * @param dataType of the output to add
     * @return PersistentComponentDescription with added output
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addStaticOutput(PersistentComponentDescription description, String outputName,
        String dataType) throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticOutputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.STATIC_OUTPUTS);
        if (staticOutputs == null) {
            staticOutputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).set(PersistentComponentDescriptionConstants.STATIC_OUTPUTS, staticOutputs);
        }
        ObjectNode newOutput = JsonNodeFactory.instance.objectNode();
        newOutput.set(PersistentComponentDescriptionConstants.NAME, TextNode.valueOf(outputName));
        newOutput.set(PersistentComponentDescriptionConstants.DATATYPE, TextNode.valueOf(dataType));
        newOutput.set(PersistentComponentDescriptionConstants.METADATA, JsonNodeFactory.instance.objectNode());
        newOutput.set(PersistentComponentDescriptionConstants.IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
        staticOutputs.add(newOutput);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Adding staticOutput "CPACS".
     * 
     * @param description of the component
     * @param outputName name of the static output to add
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addStaticOutput(PersistentComponentDescription description, String outputName)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticOutputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.STATIC_OUTPUTS);
        if (staticOutputs == null) {
            staticOutputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).set(PersistentComponentDescriptionConstants.STATIC_OUTPUTS, staticOutputs);
        }
        ObjectNode staticCPACSOut = JsonNodeFactory.instance.objectNode();
        staticCPACSOut.set(PersistentComponentDescriptionConstants.NAME, TextNode.valueOf(outputName));
        staticCPACSOut.set(PersistentComponentDescriptionConstants.DATATYPE, TextNode.valueOf("FileReference"));
        staticCPACSOut.set(PersistentComponentDescriptionConstants.METADATA, JsonNodeFactory.instance.objectNode());
        staticCPACSOut.set(PersistentComponentDescriptionConstants.IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
        staticOutputs.add(staticCPACSOut);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Adds dynamic Input.
     * 
     * @param description of the component
     * @param inputName name of the static input to add
     * @param dataType of the input to add
     * @param metaData of the input to add
     * @param identifier of the input to add
     * @param epIdentifier endpoint identifier of the input to add or "default"
     * @return updated description
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addDynamicInput(PersistentComponentDescription description, String inputName,
        String dataType, ObjectNode metaData, JsonNode identifier, String epIdentifier)
        throws IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicInputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS);
        if (dynamicInputs == null) {
            dynamicInputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).set(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS, dynamicInputs);
        }
        ObjectNode newInput = JsonNodeFactory.instance.objectNode();
        newInput.set(PersistentComponentDescriptionConstants.NAME, TextNode.valueOf(inputName));
        newInput.set(PersistentComponentDescriptionConstants.DATATYPE, TextNode.valueOf(dataType));
        newInput.set(PersistentComponentDescriptionConstants.METADATA, metaData);
        newInput.set(PersistentComponentDescriptionConstants.IDENTIFIER, identifier);
        newInput.set(PersistentComponentDescriptionConstants.EP_IDENTIFIER, TextNode.valueOf(epIdentifier));
        dynamicInputs.add(newInput);


        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * 
     * Adds a dynamic output.
     * 
     * @param description to change
     * @param outputName of the output to add
     * @param dataType of the output to add
     * @param metaData of the output to add
     * @param identifier of the output to add
     * @param epIdentifier endpoint identifier of the output to add or "default"
     * @return PersistentComponentDescription with added output
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addDynamicOutput(PersistentComponentDescription description, String outputName,
        String dataType, ObjectNode metaData, JsonNode identifier, String epIdentifier)
        throws IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicOutputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.DYNAMIC_OUTPUTS);
        if (dynamicOutputs == null) {
            dynamicOutputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).set(PersistentComponentDescriptionConstants.DYNAMIC_OUTPUTS, dynamicOutputs);
        }
        ObjectNode newOutput = JsonNodeFactory.instance.objectNode();
        newOutput.set(PersistentComponentDescriptionConstants.NAME, TextNode.valueOf(outputName));
        newOutput.set(PersistentComponentDescriptionConstants.DATATYPE, TextNode.valueOf(dataType));
        newOutput.set(PersistentComponentDescriptionConstants.METADATA, metaData);
        newOutput.set(PersistentComponentDescriptionConstants.IDENTIFIER, identifier);
        newOutput.set(PersistentComponentDescriptionConstants.EP_IDENTIFIER, TextNode.valueOf(epIdentifier));
        dynamicOutputs.add(newOutput);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Updating "consumeCPACS" flag.
     * 
     * @param description of the component
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    // This method is CPACS-specific and thus, this bundle is not the correct place for it. But it is not feasible to create a new bundle
    // for just that method.
    public static PersistentComponentDescription updateConsumeCPACSFlag(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        final String consumeCPACSInputsConfigVersion3 = "consumeCPACS";
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticInputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.STATIC_INPUTS);
        ObjectNode configuration = (ObjectNode) node.get(PersistentComponentDescriptionConstants.CONFIGURATION);
        if (configuration.get(consumeCPACSInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeCPACSInputsConfigVersion3).textValue())) {
            for (JsonNode staticInput : staticInputs) {
                if (!(staticInput.get(PersistentComponentDescriptionConstants.NAME).textValue()
                    .equals(PersistentComponentDescriptionConstants.DIRECTORY))) {
                    ObjectNode metadata = (ObjectNode) staticInput.get(PersistentComponentDescriptionConstants.METADATA);
                    metadata.set(PersistentComponentDescriptionConstants.USAGE,
                        TextNode.valueOf(PersistentComponentDescriptionConstants.REQUIRED));
                }
            }
        }
        configuration.remove(consumeCPACSInputsConfigVersion3);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Sets all dynamic input channels usage to "optional".
     * 
     * @param description of the component
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription updateDynamicInputsOptional(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicInputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS);
        if (dynamicInputs != null) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (!(dynamicInput.get(PersistentComponentDescriptionConstants.NAME).textValue()
                    .equals(PersistentComponentDescriptionConstants.DIRECTORY))) {
                    ((ObjectNode) dynamicInput.get(PersistentComponentDescriptionConstants.METADATA))
                        .set(PersistentComponentDescriptionConstants.USAGE, TextNode.valueOf("optional"));
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Updating "consumeDirectory" flag.
     * 
     * @param description of the component
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription updateConsumeDirectoryFlag(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        final String consumeDirectoryInputsConfigVersion3 = "consumeDirectory";
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicInputs = (ArrayNode) node.get(PersistentComponentDescriptionConstants.DYNAMIC_INPUTS);
        ObjectNode configuration = (ObjectNode) node.get(PersistentComponentDescriptionConstants.CONFIGURATION);
        if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeDirectoryInputsConfigVersion3).textValue())) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(PersistentComponentDescriptionConstants.NAME).textValue()
                    .equals(PersistentComponentDescriptionConstants.DIRECTORY)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(PersistentComponentDescriptionConstants.METADATA);
                    metadata.set(PersistentComponentDescriptionConstants.USAGE,
                        TextNode.valueOf(PersistentComponentDescriptionConstants.REQUIRED));
                }
            }
        } else if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && !configuration.get(consumeDirectoryInputsConfigVersion3).booleanValue()) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(PersistentComponentDescriptionConstants.NAME).textValue()
                    .equals(PersistentComponentDescriptionConstants.DIRECTORY)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(PersistentComponentDescriptionConstants.METADATA);
                    metadata.set(PersistentComponentDescriptionConstants.USAGE,
                        TextNode.valueOf(PersistentComponentDescriptionConstants.INITIAL));
                }
            }
        }
        configuration.remove(consumeDirectoryInputsConfigVersion3);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Updating Directory channel to id "directory".
     * 
     * @param direction if it is in or outputs (valid are "dynamicInputs" and "dynamicOutputs")
     * @param description of the component
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription updateDirectoryEndpointId(String direction, PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode dynEndpoints = node.get(direction);
        if (dynEndpoints != null) {
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(PersistentComponentDescriptionConstants.NAME).textValue()
                    .equals(PersistentComponentDescriptionConstants.DIRECTORY)) {
                    ((ObjectNode) endpoint).remove(PersistentComponentDescriptionConstants.EP_IDENTIFIER);
                    ((ObjectNode) endpoint).set(PersistentComponentDescriptionConstants.EP_IDENTIFIER, TextNode.valueOf("directory"));
                }
            }
        }
        // Add Static endpoints
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

}
