/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ConvergerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String BOOLEAN = "Boolean";

    private static final String IDENTIFIER = "identifier";

    private static final String NAME = "name";

    private static final String DATATYPE = "datatype";

    private static final String DYNAMIC_OUTPUTS = "dynamicOutputs";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String V1_0 = "1.0";

    private static final String CONFIGURATION = "configuration";

    private static final String EP_IDENTIFIER = "epIdentifier";

    private static final String COLON = ":";

    private static final String STATIC_OUTPUTS = "staticOutputs";

    private static final String STATIC_INPUTS = "staticInputs";

    private static final String V3_0 = "3.0";
    
    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";
    
    private static final String V4_0 = "4.0";
    
    private static final String CURRENT_VERSION = V4_0;
    
    private static TypedDatumService typedDatumService;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return ConvergerComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versions = PersistentDescriptionFormatVersion.NONE;

        // Update 1 : Version < 1.0
        if (!silent && (persistentComponentDescriptionVersion == null
            || persistentComponentDescriptionVersion.compareTo(V1_0) < 0)) {
            versions = versions | PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        }
        // Update 2 : 1.0 <= Version < 3.0
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
            versions = versions | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
        }
        // Update 3 : 3.0 -> latest
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
            versions = versions | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versions;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE) {
                description = firstUpdate(description);
                description.setComponentVersion(V1_0);
            } else if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                description = secondUpdate(description);
                description.setComponentVersion(V3_0);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                if (description.getComponentVersion().compareTo(V3_1) < 0) {
                    description = updateFrom30To31(description);
                }
                if (description.getComponentVersion().compareTo(V3_2) < 0) {
                    description = updateFrom31To32(description);
                }
                if (description.getComponentVersion().compareTo(V4_0) < 0) {
                    description = updateFrom32To40(description);
                }
            }
        }
        return description;
    }

    private PersistentComponentDescription updateFrom32To40(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configNode = (ObjectNode) node.get("configuration");
        JsonNode maxIterNode = configNode.get("maxIterations");
        if (maxIterNode != null) {
            String maxConvChecks = maxIterNode.getTextValue();
            configNode.remove("maxIterations");
            configNode.put(ConvergerComponentConstants.KEY_MAX_CONV_CHECKS, maxConvChecks);
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V4_0);
        return description;
    }
    
    private PersistentComponentDescription updateFrom31To32(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateIsNestedLoop(description);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        
        JsonNode dynInputsNode = node.get("dynamicInputs");
        
        if (dynInputsNode != null) {
            Iterator<JsonNode> nodeIterator = dynInputsNode.getElements();
            while (nodeIterator.hasNext()) {
                JsonNode dynInputNode = nodeIterator.next();
                ObjectNode jsonNode = (ObjectNode) dynInputNode.get("metadata");
                JsonNode hasStartValueJsonNode = jsonNode.get("hasStartValue");
                if (hasStartValueJsonNode != null) {
                    String hasStartValue = hasStartValueJsonNode.getTextValue();
                    if (hasStartValue.equals("true")) {
                        FloatTD floatValue = typedDatumService.getFactory()
                            .createFloat(Double.valueOf(jsonNode.get("startValue").getTextValue()));
                        jsonNode.put(ComponentConstants.INPUT_METADATA_KEY_INIT_VALUE,
                            typedDatumService.getSerializer().serialize(floatValue));
                    }
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V3_2);
        return description;
    }
    
    /**
     * Updates descriptions of version 3.0 to 3.1.
     **/
    private PersistentComponentDescription updateFrom30To31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode objectNode = (ObjectNode) node.get(CONFIGURATION);
        objectNode.put(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, "1");
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V3_1);
        return description;
    }
    
    /**
     * The second update is for the Workflow update 3.0, and though for versions between 1.0 and
     * 3.0. It updates the IDs for the dynamic in and outputs.
     * */
    private PersistentComponentDescription secondUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode dynInputs = node.get(DYNAMIC_INPUTS);
        if (dynInputs != null) {
            for (JsonNode inputEndpoint : dynInputs) {
                if (inputEndpoint.get(EP_IDENTIFIER).isNull()) {
                    ((ObjectNode) inputEndpoint).remove(EP_IDENTIFIER);
                    if (inputEndpoint.get(DATATYPE).getTextValue().equals("Float")) {
                        ((ObjectNode) inputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(ConvergerComponentConstants.ID_VALUE_TO_CONVERGE));
                    } else {
                        ((ObjectNode) inputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(ComponentConstants.OUPUT_ID_OUTERLOOP_DONE));
                    }
                }
            }
        }

        JsonNode dynOutputs = node.get(DYNAMIC_OUTPUTS);
        if (dynOutputs != null) {
            for (JsonNode outputEndpoint : dynOutputs) {
                ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
                if (outputEndpoint.get(EP_IDENTIFIER) == null) {
                    ((ObjectNode) outputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(ConvergerComponentConstants.ID_CONVERGED_VALUE));
                }
            }
        }
        ArrayNode statOutputs = (ArrayNode) node.get(STATIC_OUTPUTS);
        if (statOutputs == null) {
            statOutputs = JsonNodeFactory.instance.arrayNode();
            ObjectNode outputEndpoint = JsonNodeFactory.instance.objectNode();
            outputEndpoint.put(NAME, TextNode.valueOf("Converged"));
            outputEndpoint.put(DATATYPE, TextNode.valueOf(BOOLEAN));
            outputEndpoint.put(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
            statOutputs.add(outputEndpoint);
            outputEndpoint = JsonNodeFactory.instance.objectNode();
            outputEndpoint.put(NAME, TextNode.valueOf("Converged absolute"));
            outputEndpoint.put(DATATYPE, TextNode.valueOf(BOOLEAN));
            outputEndpoint.put(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
            statOutputs.add(outputEndpoint);

            outputEndpoint = JsonNodeFactory.instance.objectNode();
            outputEndpoint.put(NAME, TextNode.valueOf("Converged relative"));
            outputEndpoint.put(DATATYPE, TextNode.valueOf(BOOLEAN));
            outputEndpoint.put(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
            statOutputs.add(outputEndpoint);

            ((ObjectNode) node).put(STATIC_OUTPUTS, statOutputs);
        }

        ArrayNode statInputs = (ArrayNode) node.get(STATIC_INPUTS);
        if (statInputs == null) {
            statInputs = JsonNodeFactory.instance.arrayNode();
            ObjectNode inputEndpoint = JsonNodeFactory.instance.objectNode();
            inputEndpoint.put(NAME, TextNode.valueOf("outerLoopDone"));
            inputEndpoint.put(DATATYPE, TextNode.valueOf(BOOLEAN));
            inputEndpoint.put(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
            inputEndpoint.put("readonly", "true");
            ObjectNode metadata = JsonNodeFactory.instance.objectNode();
            metadata.put("usage", "optional");
            inputEndpoint.put("metadata", metadata);
            statInputs.add(inputEndpoint);

        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs",
                ConvergerComponentConstants.ID_CONVERGED_VALUE, description);
        return description;
    }

    /**
     * The first update is for Componentversions < 1.0 and updates the eps configuration from Double
     * to String. It further adds new output channel that were needed.
     * */
    private PersistentComponentDescription firstUpdate(PersistentComponentDescription description) throws IOException, JsonParseException,
        JsonProcessingException, JsonGenerationException, JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        description = replaceEpsDataType(mapper, node, "epsR");
        description = replaceEpsDataType(mapper, node, "epsA");
        description = addConvergedOutputs(mapper, node);

        return description;
    }

    private PersistentComponentDescription replaceEpsDataType(ObjectMapper mapper, JsonNode node, String eps)
        throws JsonParseException, IOException, JsonGenerationException, JsonMappingException {
        PersistentComponentDescription description;
        // TODO actually constants from DescriptionWorkflowPersistenceHandler must be used. but they
        // must be moved to
        // core.component beforehand to prohibit bad dependencies
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        ArrayNode configurationNode = (ArrayNode) node.get(CONFIGURATION);
        ArrayNode newConfigurationNode = JsonNodeFactory.instance.arrayNode();
        JsonNode oldEpsNode = null;
        for (JsonNode n : configurationNode) {
            if (n.getTextValue().startsWith(eps + ":java.lang.Double:")) {
                oldEpsNode = n;
            } else {
                newConfigurationNode.add(n);
            }
        }
        if (oldEpsNode != null) {
            String nodeString = oldEpsNode.getTextValue();
            String value = "";
            if (nodeString.split(COLON).length > 1) {
                value = nodeString.split(COLON)[2];
            }

            newConfigurationNode.add(TextNode.valueOf(eps + ":java.lang.String:" + value));

            ((ObjectNode) node).remove(CONFIGURATION);
            ((ObjectNode) node).put(CONFIGURATION, newConfigurationNode);
        }
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        return description;
    }

    private PersistentComponentDescription addConvergedOutputs(ObjectMapper mapper, JsonNode node) throws JsonParseException,
        JsonGenerationException, JsonMappingException, IOException {
        PersistentComponentDescription description;
        // TODO actually constants from DescriptionWorkflowPersistenceHandler must be used. but they
        // must be moved to
        // core.component beforehand to prohibit bad dependencies
        JsonNode addOutputNode = node.get("addOutput");
        Map<String, String> outputs = getOutputs(addOutputNode);
        setOutputs(addOutputNode, outputs);

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        return description;
    }

    private Map<String, String> getOutputs(JsonNode addOutputNode) {
        Map<String, String> outputs = new HashMap<String, String>();
        Iterator<JsonNode> nodeIterator = addOutputNode.getElements();
        while (nodeIterator.hasNext()) {
            String[] output = nodeIterator.next().getTextValue().split(COLON);
            outputs.put(output[0], output[1]);
        }
        return outputs;
    }

    private JsonNode setOutputs(JsonNode addOutputNode, Map<String, String> outputs) {
        ((ArrayNode) addOutputNode).removeAll();
        for (String outputName : outputs.keySet()) {
            ((ArrayNode) addOutputNode).add(outputName + COLON + outputs.get(outputName));
            if (!outputName.endsWith(ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX)
                && !outputs.containsKey(outputName + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX)) {
                ((ArrayNode) addOutputNode).add(outputName
                    + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX + COLON + outputs.get(outputName));
            }
        }
        return addOutputNode;
    }
    
    protected void bindTypedDatumService(TypedDatumService newService) {
        typedDatumService = newService;
    }

}
