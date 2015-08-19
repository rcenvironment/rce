/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.cpacs.utils.common.components;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;


/**
 * Utils class for updater of CPACS-Components for component version three.
 *
 * @author Markus Kunde
 */
public final class PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree {

    private static String usageConfigVersion3 = "usage";
    private static String nameConfigVersion3 = "name";
    private static String staticInputsConfigVersion3 = "staticInputs";
    private static String metadataInputsConfigVersion3 = "metadata";
    private static String directoryInputsConfigVersion3 = "Directory";
    private static String epIdentifierInputsConfigVersion3 = "epIdentifier";
    
    private PersistentCpacsComponentDescriptionUpdaterUtilsForVersionThree() {}
    
    /**
     * Adding staticInput "CPACS".
     * 
     * @param description of the component
     * @param channelName name of the channel
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addStaticInput(PersistentComponentDescription description, String channelName)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticInputs = (ArrayNode) node.get(staticInputsConfigVersion3);
        if (staticInputs == null) {
            staticInputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).put(staticInputsConfigVersion3, staticInputs);
        }
        ObjectNode staticCPACSIn = JsonNodeFactory.instance.objectNode();
        ObjectNode metaDataNode = JsonNodeFactory.instance.objectNode();
        metaDataNode.put(usageConfigVersion3, TextNode.valueOf("initial"));
        staticCPACSIn.put(nameConfigVersion3, TextNode.valueOf(channelName));
        staticCPACSIn.put("datatype", TextNode.valueOf("FileReference"));
        staticCPACSIn.put(metadataInputsConfigVersion3, metaDataNode);
        staticCPACSIn.put("identifier", TextNode.valueOf(UUID.randomUUID().toString()));
        staticInputs.add(staticCPACSIn);
        
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }
    
    
    /**
     * Adding staticOutput "CPACS".
     * 
     * @param description of the component
     * @return updated description
     * @throws JsonParseException thrown on an error
     * @throws JsonGenerationException thrown on an error
     * @throws JsonMappingException thrown on an error
     * @throws IOException thrown on an error
     */
    public static PersistentComponentDescription addStaticOutputCPACS(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticOutputs = (ArrayNode) node.get("staticOutputs");
        if (staticOutputs == null) {
            staticOutputs = JsonNodeFactory.instance.arrayNode();
            ((ObjectNode) node).put("staticOutputs", staticOutputs);
        }
        ObjectNode staticCPACSOut = JsonNodeFactory.instance.objectNode();
        staticCPACSOut.put(nameConfigVersion3, TextNode.valueOf("CPACS"));
        staticCPACSOut.put("datatype", TextNode.valueOf("FileReference"));
        staticCPACSOut.put(metadataInputsConfigVersion3, JsonNodeFactory.instance.objectNode());
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
    public static PersistentComponentDescription updateConsumeCPACSFlag(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        final String consumeCPACSInputsConfigVersion3 = "consumeCPACS";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode staticInputs = (ArrayNode) node.get(staticInputsConfigVersion3);
        ObjectNode configuration = (ObjectNode) node.get("configuration");
        if (configuration.get(consumeCPACSInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeCPACSInputsConfigVersion3).getTextValue())) {
            for (JsonNode staticInput : staticInputs) {
                if (!(staticInput.get(nameConfigVersion3).getTextValue().equals(directoryInputsConfigVersion3))) {
                    ObjectNode metadata = (ObjectNode) staticInput.get(metadataInputsConfigVersion3);
                    metadata.put(usageConfigVersion3, TextNode.valueOf("required"));
                }
            }
        }           
        ((ObjectNode) configuration).remove(consumeCPACSInputsConfigVersion3);
        
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicInputs = (ArrayNode) node.get("dynamicInputs");
        if (dynamicInputs != null) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (!(dynamicInput.get(nameConfigVersion3).getTextValue().equals(directoryInputsConfigVersion3))) {
                    ((ObjectNode) dynamicInput.get(metadataInputsConfigVersion3)).put(usageConfigVersion3, TextNode.valueOf("optional"));
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ArrayNode dynamicInputs = (ArrayNode) node.get("dynamicInputs");
        ObjectNode configuration = (ObjectNode) node.get("configuration");
        if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && Boolean.valueOf(configuration.get(consumeDirectoryInputsConfigVersion3).getTextValue())) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(nameConfigVersion3).getTextValue().equals(directoryInputsConfigVersion3)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(metadataInputsConfigVersion3);
                    metadata.put(usageConfigVersion3, TextNode.valueOf("required"));
                }
            }
        } else if (configuration.get(consumeDirectoryInputsConfigVersion3) != null
            && !configuration.get(consumeDirectoryInputsConfigVersion3).getBooleanValue()) {
            for (JsonNode dynamicInput : dynamicInputs) {
                if (dynamicInput.get(nameConfigVersion3).getTextValue().equals(directoryInputsConfigVersion3)) {
                    ObjectNode metadata = (ObjectNode) dynamicInput.get(metadataInputsConfigVersion3);
                    metadata.put(usageConfigVersion3, TextNode.valueOf("initial"));
                }
            }
        }
        ((ObjectNode) configuration).remove(consumeDirectoryInputsConfigVersion3);
        
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
    public static PersistentComponentDescription updateDirectoryChannelId(String direction, PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode dynEndpoints = node.get(direction);
        if (dynEndpoints != null) {
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(nameConfigVersion3).getTextValue().equals(directoryInputsConfigVersion3)) {
                    ((ObjectNode) endpoint).remove(epIdentifierInputsConfigVersion3);
                    ((ObjectNode) endpoint).put(epIdentifierInputsConfigVersion3, TextNode.valueOf("directory"));
                }
            }
        }
        // Add Static endpoints
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }
    
}
