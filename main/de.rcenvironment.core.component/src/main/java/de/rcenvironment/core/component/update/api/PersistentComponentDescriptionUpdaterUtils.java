/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

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
 */
public final class PersistentComponentDescriptionUpdaterUtils {

    /** json key. */
    public static final String EP_IDENTIFIER = "epIdentifier";
    
    private static final String INITIAL = "initial";

    private static final String REQUIRED = "required";

    private static final String CONFIGURATION = "configuration";

    private static final String USAGE = "usage";

    private static final String NAME = "name";

    private static final String STATIC_INPUTS = "staticInputs";
    
    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String METADATA = "metadata";

    private static final String DIRECTORY = "Directory";
    
    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private PersistentComponentDescriptionUpdaterUtils() {}

    /**
     * @param direction if it is in or outsputs (valid are "dynmaicInputs" and "dynamicOutputs")
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
                if (endpoint.get(EP_IDENTIFIER) == null || endpoint.get(EP_IDENTIFIER).equals("null")
                    || endpoint.get(EP_IDENTIFIER).isNull()) {
                    ((ObjectNode) endpoint).remove(EP_IDENTIFIER);
                    ((ObjectNode) endpoint).put(EP_IDENTIFIER, TextNode.valueOf(identifier));
                }
            }
        }
        // Add Static endpoints
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
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
                String faultToleranceNAV = configObjectNode.get(confgKeyFaultToleranceNAV).getTextValue();
                configObjectNode.put(confgKeyFaultToleranceNAV, "Fail");
                configObjectNode.put("faultTolerance-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyMaxRerunBeforeFail = "loopRerunAndFail_5e0ed1cd";
            if (configObjectNode.has(configKeyMaxRerunBeforeFail)) {
                String faultToleranceNAV = configObjectNode.get(configKeyMaxRerunBeforeFail).getTextValue();
                configObjectNode.remove(configKeyMaxRerunBeforeFail);
                configObjectNode.put("maxRerunBeforeFail-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyMaxRerunBeforeDiscard = "loopRerunAndDiscard_5e0ed1cd";
            if (configObjectNode.has(configKeyMaxRerunBeforeDiscard)) {
                String faultToleranceNAV = configObjectNode.get(configKeyMaxRerunBeforeDiscard).getTextValue();
                configObjectNode.remove(configKeyMaxRerunBeforeDiscard);
                configObjectNode.put("maxRerunBeforeDiscard-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyFailLoopOnly = "failLoop_5e0ed1cd";
            if (configObjectNode.has(configKeyFailLoopOnly)) {
                String faultToleranceNAV = configObjectNode.get(configKeyFailLoopOnly).getTextValue();
                configObjectNode.remove(configKeyFailLoopOnly);
                configObjectNode.put("failLoopOnly-NAV_5e0ed1cd", faultToleranceNAV);
            }
            String configKeyFinallyFailIfDiscarded = "finallyFail_5e0ed1cd";
            if (configObjectNode.has(configKeyFinallyFailIfDiscarded)) {
                String faultToleranceNAV = configObjectNode.get(configKeyFinallyFailIfDiscarded).getTextValue();
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
        JsonParser jsonParser = jsonFactory.createJsonParser(description.getComponentDescriptionAsString());
        JsonNode node = mapper.readTree(jsonParser);
        jsonParser.close();
        
        updateInputNode(node.get(DYNAMIC_INPUTS));
        updateInputNode(node.get(STATIC_INPUTS));
        
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
        JsonParser jsonParser = jsonFactory.createJsonParser(description.getComponentDescriptionAsString());
        JsonNode node = mapper.readTree(jsonParser);
        jsonParser.close();
        
        String oldConfigKey = "isNestedLoop";
        ObjectNode configurationsNode = (ObjectNode) node.get(CONFIGURATION);
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
            Iterator<JsonNode> nodeIterator = inputNode.getElements();
            while (nodeIterator.hasNext()) {
                JsonNode dynInputNode = nodeIterator.next();
                ObjectNode jsonNode = (ObjectNode) dynInputNode.get("metadata");
                JsonNode usageJsonNode = jsonNode.get(USAGE);
                if (usageJsonNode != null) {
                    String usage = usageJsonNode.getTextValue();
                    switch (usage) {
                    case REQUIRED:
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                            EndpointDefinition.InputDatumHandling.Single.name());
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.Required.name());
                        break;
                    case INITIAL:
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
                    jsonNode.remove(USAGE);
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

        ArrayNode staticInputs = (ArrayNode) node.get(STATIC_INPUTS);
        if (staticInputs == null) {
            staticInputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).put(STATIC_INPUTS, staticInputs);
        }
        ObjectNode staticCPACSIn = JsonNodeFactory.instance.objectNode();
        ObjectNode metaDataNode = JsonNodeFactory.instance.objectNode();
        metaDataNode.put(USAGE, TextNode.valueOf(INITIAL));
        staticCPACSIn.put(NAME, TextNode.valueOf(inputName));
        staticCPACSIn.put("datatype", TextNode.valueOf("FileReference"));
        staticCPACSIn.put(METADATA, metaDataNode);
        staticCPACSIn.put("identifier", TextNode.valueOf(UUID.randomUUID().toString()));
        staticInputs.add(staticCPACSIn);
        
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

        ArrayNode staticOutputs = (ArrayNode) node.get("staticOutputs");
        if (staticOutputs == null) {
            staticOutputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).put("staticOutputs", staticOutputs);
        }
        ObjectNode staticCPACSOut = JsonNodeFactory.instance.objectNode();
        staticCPACSOut.put(NAME, TextNode.valueOf(outputName));
        staticCPACSOut.put("datatype", TextNode.valueOf("FileReference"));
        staticCPACSOut.put(METADATA, JsonNodeFactory.instance.objectNode());
        staticCPACSOut.put("identifier", TextNode.valueOf(UUID.randomUUID().toString()));
        staticOutputs.add(staticCPACSOut);
        
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

        ArrayNode staticInputs = (ArrayNode) node.get(STATIC_INPUTS);
        ObjectNode configuration = (ObjectNode) node.get(CONFIGURATION);
        if (configuration.get(consumeCPACSInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeCPACSInputsConfigVersion3).getTextValue())) {
            for (JsonNode staticInput : staticInputs) {
                if (!(staticInput.get(NAME).getTextValue().equals(DIRECTORY))) {
                    ObjectNode metadata = (ObjectNode) staticInput.get(METADATA);
                    metadata.put(USAGE, TextNode.valueOf(REQUIRED));
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

        ArrayNode dynamicInputs = (ArrayNode) node.get(DYNAMIC_INPUTS);
        if (dynamicInputs != null) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (!(dynamicInput.get(NAME).getTextValue().equals(DIRECTORY))) {
                    ((ObjectNode) dynamicInput.get(METADATA)).put(USAGE, TextNode.valueOf("optional"));
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

        ArrayNode dynamicInputs = (ArrayNode) node.get(DYNAMIC_INPUTS);
        ObjectNode configuration = (ObjectNode) node.get(CONFIGURATION);
        if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeDirectoryInputsConfigVersion3).getTextValue())) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(NAME).getTextValue().equals(DIRECTORY)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(METADATA);
                    metadata.put(USAGE, TextNode.valueOf(REQUIRED));
                }
            }
        } else if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && !configuration.get(consumeDirectoryInputsConfigVersion3).getBooleanValue()) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(NAME).getTextValue().equals(DIRECTORY)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(METADATA);
                    metadata.put(USAGE, TextNode.valueOf(INITIAL));
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
                if (endpoint.get(NAME).getTextValue().equals(DIRECTORY)) {
                    ((ObjectNode) endpoint).remove(EP_IDENTIFIER);
                    ((ObjectNode) endpoint).put(EP_IDENTIFIER, TextNode.valueOf("directory"));
                }
            }
        }
        // Add Static endpoints
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }
    
}
