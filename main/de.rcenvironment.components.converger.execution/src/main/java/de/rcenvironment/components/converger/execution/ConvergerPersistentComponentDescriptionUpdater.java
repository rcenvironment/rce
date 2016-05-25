/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ConvergerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String LOOP_ENDPOINT_TYPE = "loopEndpointType_5e0ed1cd";

    private static final String OUTER_LOOP_ENDPOINT = "OuterLoopEndpoint";

    private static final String METADATA = "metadata";

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

    private static final String V4_1 = "4.1";

    private static final String V5_0 = "5";

    private static final String V5_1 = "5.1";
    
    private static final String V5_1_1 = "5.1.1";

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
        // Update 3 non-silent : 3.0 -> latest
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V5_0) < 0) {
            versions = versions | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        // Update 3 silent : 3.0 -> latest
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V5_1_1) < 0) {
            versions = versions | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versions;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) { // called after "silent" was called
            if (formatVersion == PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE) {
                description = firstUpdate(description);
                description.setComponentVersion(V1_0);
            } else if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                description = secondUpdate(description);
                description.setComponentVersion(V3_0);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V3_0:
                    description = updateFrom30To31(description);
                case V3_1:
                    description = updateFrom31To32(description);
                case V3_2:
                    description = updateFrom32To40(description);
                case V4_0:
                    description = updateFrom40To41(description);
                case V4_1:
                    description = updateFrom41To5(description);
                case V5_0:
                    description = updateFrom50To51(description);
                case V5_1:
                    description = updateFrom51To511(description);
                default:
                    // nothing to do here
                }
            }
        } else { // called first (before non-silent)
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V5_0:
                    description = updateFrom50To51(description);
                case V5_1:
                    description = updateFrom51To511(description);
                default:
                    // nothing to do here
                }
            }
        }
        return description;
    }
    
    private PersistentComponentDescription updateFrom51To511(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        description =
            PersistentComponentDescriptionUpdaterUtils.updateFaultToleranceOfLoopDriver(description);
        description.setComponentVersion(V5_1_1);
        return description;
    }
    
    private PersistentComponentDescription updateFrom50To51(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configNode = (ObjectNode) node.get(CONFIGURATION);
        configNode.put(ConvergerComponentConstants.NOT_CONVERGED_IGNORE, true);
        configNode.put(ConvergerComponentConstants.NOT_CONVERGED_FAIL, false);
        configNode.put(ConvergerComponentConstants.NOT_CONVERGED_NOT_A_VALUE, false);
        JsonNode dynamicOutputs = node.get(DYNAMIC_OUTPUTS);
        if (dynamicOutputs != null) {
            List<ObjectNode> newOutputEndpoints = new ArrayList<ObjectNode>();
            for (JsonNode outputEndpoint : dynamicOutputs) {
                String outputName = outputEndpoint.get(NAME).getTextValue();
                String outputEndpointId = outputEndpoint.get(EP_IDENTIFIER).getTextValue();
                if (outputEndpointId.equals("valueToConverge") && !outputName.endsWith("_converged")) {
                    ObjectNode convergedEndpoint = mapper.createObjectNode();
                    convergedEndpoint.put(NAME, TextNode.valueOf(outputName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX));
                    convergedEndpoint.put(EP_IDENTIFIER, ConvergerComponentConstants.ENDPOINT_ID_AUXILIARY);
                    convergedEndpoint.put(DATATYPE, TextNode.valueOf(BOOLEAN));
                    convergedEndpoint.put(IDENTIFIER, TextNode.valueOf(UUID.randomUUID().toString()));
                    ObjectNode metaData = mapper.createObjectNode();
                    metaData.put(LOOP_ENDPOINT_TYPE, "SelfLoopEndpoint");
                    convergedEndpoint.put(METADATA, metaData);
                    newOutputEndpoints.add(convergedEndpoint);
                }
            }
            for (JsonNode newOutputEndpoint : newOutputEndpoints) {
                ((ArrayNode) dynamicOutputs).add(newOutputEndpoint);
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        return description;
    }

    private PersistentComponentDescription updateFrom41To5(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ObjectNode metaData;
                if (outputEndpoint.has(METADATA)) {
                    metaData = (ObjectNode) outputEndpoint.get(METADATA);
                } else {
                    metaData = mapper.createObjectNode();
                }
                if (outputEndpoint.get(NAME).getTextValue().equals("Outer loop done")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, "InnerLoopEndpoint");
                } else if (outputEndpoint.get(NAME).getTextValue().equals("Converged")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                } else if (outputEndpoint.get(NAME).getTextValue().equals("Converged absolute")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                } else if (outputEndpoint.get(NAME).getTextValue().equals("Converged relative")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                }
                ((ObjectNode) outputEndpoint).put(METADATA, metaData);
            }
        }
        JsonNode dynamicOutputs = node.get(DYNAMIC_OUTPUTS);
        if (dynamicOutputs != null) {
            for (JsonNode outputEndpoint : dynamicOutputs) {
                if (outputEndpoint.get(NAME).getTextValue().endsWith("_converged")) {
                    ObjectNode metaData;
                    if (outputEndpoint.has(METADATA)) {
                        metaData = (ObjectNode) outputEndpoint.get(METADATA);
                    } else {
                        metaData = mapper.createObjectNode();
                    }
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                    ((ObjectNode) outputEndpoint).put(METADATA, metaData);
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V5_0);
        return newdesc;
    }

    private PersistentComponentDescription updateFrom40To41(PersistentComponentDescription description) throws IOException {

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode dynOutputsNode = node.get(DYNAMIC_OUTPUTS);

        if (dynOutputsNode != null) {
            Iterator<JsonNode> nodeIterator = dynOutputsNode.getElements();
            while (nodeIterator.hasNext()) {
                ObjectNode dynOutputNode = (ObjectNode) nodeIterator.next();
                if (dynOutputNode.get(EP_IDENTIFIER).asText().equals("convergedValue")) {
                    dynOutputNode.put(EP_IDENTIFIER, ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE);
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V4_1);
        return description;
    }

    private PersistentComponentDescription updateFrom32To40(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configNode = (ObjectNode) node.get(CONFIGURATION);
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

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        JsonNode dynInputsNode = node.get(DYNAMIC_INPUTS);

        if (dynInputsNode != null) {
            Iterator<JsonNode> nodeIterator = dynInputsNode.getElements();
            while (nodeIterator.hasNext()) {
                JsonNode dynInputNode = nodeIterator.next();
                ObjectNode jsonNode = (ObjectNode) dynInputNode.get(METADATA);
                JsonNode hasStartValueJsonNode = jsonNode.get("hasStartValue");
                if (hasStartValueJsonNode != null) {
                    String hasStartValue = hasStartValueJsonNode.getTextValue();
                    if (hasStartValue.equals("true")) {
                        FloatTD floatValue = typedDatumService.getFactory()
                            .createFloat(Double.valueOf(jsonNode.get("startValue").getTextValue()));
                        jsonNode.put("initValue_dca67e34", typedDatumService.getSerializer().serialize(floatValue));
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
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode objectNode = (ObjectNode) node.get(CONFIGURATION);
        objectNode.put(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, "1");
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V3_1);
        return description;
    }

    /**
     * The second update is for the Workflow update 3.0, and though for versions between 1.0 and 3.0. It updates the IDs for the dynamic in
     * and outputs.
     */
    private PersistentComponentDescription secondUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode dynInputs = node.get(DYNAMIC_INPUTS);
        if (dynInputs != null) {
            for (JsonNode inputEndpoint : dynInputs) {
                if (inputEndpoint.get(EP_IDENTIFIER).isNull()) {
                    ((ObjectNode) inputEndpoint).remove(EP_IDENTIFIER);
                    if (inputEndpoint.get(DATATYPE).getTextValue().equals("Float")) {
                        ((ObjectNode) inputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(
                            ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE));
                    } else {
                        ((ObjectNode) inputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(LoopComponentConstants.INPUT_ID_OUTER_LOOP_DONE));
                    }
                }
            }
        }

        JsonNode dynOutputs = node.get(DYNAMIC_OUTPUTS);
        if (dynOutputs != null) {
            for (JsonNode outputEndpoint : dynOutputs) {
                ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
                if (outputEndpoint.get(EP_IDENTIFIER) == null) {
                    ((ObjectNode) outputEndpoint).put(EP_IDENTIFIER, TextNode.valueOf(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE));
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
            inputEndpoint.put(METADATA, metadata);
            statInputs.add(inputEndpoint);

        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs",
                ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE, description);
        return description;
    }

    /**
     * The first update is for Componentversions < 1.0 and updates the eps configuration from Double to String. It further adds new output
     * channel that were needed.
     */
    private PersistentComponentDescription firstUpdate(PersistentComponentDescription description) throws IOException, JsonParseException,
        JsonProcessingException, JsonGenerationException, JsonMappingException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
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
