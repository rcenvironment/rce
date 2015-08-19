/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;

/**
 * Utils for updater classes.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public final class PersistentComponentDescriptionUpdaterUtils {

    /** json key. */
    public static final String EP_IDENTIFIER = "epIdentifier";

    private PersistentComponentDescriptionUpdaterUtils() {

    }

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
        ObjectMapper mapper = new ObjectMapper();
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonParser);
        
        updateInputNode(node.get("dynamicInputs"));
        updateInputNode(node.get("staticInputs"));
        
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonParser);
        
        String oldConfigKey = "isNestedLoop";
        ObjectNode configurationsNode = (ObjectNode) node.get("configuration");
        if (configurationsNode != null && configurationsNode.has(oldConfigKey)) {
            boolean isNestedLoop = configurationsNode.get(oldConfigKey).asBoolean();
            configurationsNode.remove(oldConfigKey);
            configurationsNode.put(ComponentConstants.CONFIG_KEY_IS_NESTED_LOOP, isNestedLoop);
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
                JsonNode usageJsonNode = jsonNode.get("usage");
                if (usageJsonNode != null) {
                    String usage = usageJsonNode.getTextValue();
                    switch (usage) {
                    case "required":
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING,
                            EndpointDefinition.InputDatumHandling.Single.name());
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.Required.name());
                        break;
                    case "initial":
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
                    jsonNode.remove("usage");
                }
            }
        }
    }
    
}
