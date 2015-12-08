/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerFileLoader;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class OptimizerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String SELF_LOOP_ENDPOINT = "SelfLoopEndpoint";

    private static final String OUTER_LOOP_ENDPOINT = "OuterLoopEndpoint";

    private static final String DYNAMIC_OUTPUTS = "dynamicOutputs";

    private static final String GOAL = "goal";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String SPECIFIC_SETTINGS = "specificSettings";

    private static final String DAKOTA_COLINY_EVOLUTIONARY_ALGORITHM = "Dakota Coliny Evolutionary Algorithm";

    private static final String DAKOTA_COLINY_COBYLA_CONSTRAINT_OPTIMIZATION_BY_LINEAR_APPROXIMATIONS =
        "Dakota Coliny COBYLA (Constraint Optimization By Linear Approximations)";

    private static final String DAKOTA_QUASI_NEWTON_METHOD = "Dakota Quasi-Newton method";

    private static final String EP_IDENTIFIER = "epIdentifier";

    private static final String CONSTRAINT = "Constraint";

    private static final String OBJECTIVE = "Objective";

    private static final String NAME = "name";

    private static final String STATIC_OUTPUTS = "staticOutputs";

    private static final String NAN = "NaN";

    private static final String WEIGHT = "weight";

    private static final String METADATA = "metadata";

    private static final String METHOD_CONFIGURATIONS = "methodConfigurations";

    private static final String ALGORITHM = "algorithm";

    private static final String CONFIGURATION = "configuration";

    private static final String LOOP_ENDPOINT_TYPE = "loopEndpointType_5e0ed1cd";

    private static final Log LOGGER = LogFactory.getLog(OptimizerPersistentComponentDescriptionUpdater.class);

    private static final String COLON = ":";

    private static final String V1_0 = "1.0";

    private static final String V3_0 = "3.0";

    private static final String V5_0 = "5.0";

    private static final String V5_1 = "5.1";

    private static final String V6_0 = "6.0";

    private static final String V6_1 = "6.1";

    private static final String V6_2 = "6.2";

    private static final String V7_0 = "7.0";

    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return OptimizerComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (!silent && (persistentComponentDescriptionVersion == null
            || persistentComponentDescriptionVersion.compareTo(V1_0) < 0)) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
        }
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
        }
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V5_0) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V5_1) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V6_0) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V6_1) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V6_2) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V7_0) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE) {
                description = updateBeforeVersion3(description);

            } else if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                description = updateToVersion3(description);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE
                && description.getComponentVersion().compareTo(V5_0) < 0) {
                description = updateToVersion50(description);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE
                && description.getComponentVersion().compareTo(V7_0) < 0) {
                description = updateToVersion70(description);
            }
        } else {
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                if (description.getComponentVersion().compareTo(V5_1) < 0) {
                    description = updateToVersion51(description);
                }
                if (description.getComponentVersion().compareTo(V6_0) < 0) {
                    description = updateToVersion60(description);
                }
                if (description.getComponentVersion().compareTo(V6_1) < 0) {
                    description = updateToVersion61(description);
                }
                if (description.getComponentVersion().compareTo(V6_2) < 0) {
                    description = updateFrom61To62(description);
                }
            }
        }
        return description;
    }

    private PersistentComponentDescription updateToVersion70(PersistentComponentDescription description)
        throws JsonParseException, JsonGenerationException, JsonMappingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ObjectNode metaData = (ObjectNode) outputEndpoint.get(METADATA);
                if (metaData == null) {
                    metaData = JsonNodeFactory.instance.objectNode();
                    ((ObjectNode) outputEndpoint).put(METADATA, metaData);
                }
                if (outputEndpoint.get(NAME).getTextValue().equals("Outer loop done")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, "InnerLoopEndpoint");
                }
                if (outputEndpoint.get(NAME).getTextValue().equals("Iteration")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, SELF_LOOP_ENDPOINT);
                }
                if (outputEndpoint.get(NAME).getTextValue().equals("Gradient request")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, SELF_LOOP_ENDPOINT);
                }
                if (outputEndpoint.get(NAME).getTextValue().equals("Done")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                }
            }
        }
        JsonNode dynamicOutputs = node.get(DYNAMIC_OUTPUTS);
        if (dynamicOutputs != null) {
            for (JsonNode outputEndpoint : dynamicOutputs) {
                ObjectNode metaData = (ObjectNode) outputEndpoint.get(METADATA);
                if (outputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("Design")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, SELF_LOOP_ENDPOINT);
                }
                if (outputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("optima")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                }
            }
        }
        JsonNode dynamicInputs = node.get(DYNAMIC_INPUTS);
        if (dynamicInputs != null) {
            for (JsonNode inputEndpoint : dynamicInputs) {
                ObjectNode metaData = (ObjectNode) inputEndpoint.get(METADATA);
                if (inputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("Objective")
                    || inputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("Constraint")
                    || inputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("gradients")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, SELF_LOOP_ENDPOINT);
                }
                if (inputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("startvalues")
                    || inputEndpoint.get(EP_IDENTIFIER).getTextValue().equals("outerLoopDone")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, OUTER_LOOP_ENDPOINT);
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V7_0);
        return newdesc;
    }

    private PersistentComponentDescription updateFrom61To62(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
                if (outputEndpoint.get(NAME).getTextValue().equals("Optimizer is finished")) {
                    ((ObjectNode) outputEndpoint).put(NAME, LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V6_2);
        return newdesc;

    }

    @SuppressWarnings("unchecked")
    private PersistentComponentDescription updateToVersion61(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        ((ObjectNode) node.get(CONFIGURATION)).put("preCalcFilePath", "${preCalcFilePath}");
        TextNode methodConfigurations = (TextNode) node.get(CONFIGURATION).get(METHOD_CONFIGURATIONS);
        Map<String, Object> configs = mapper.readValue(methodConfigurations.getTextValue(), new HashMap<String, Object>().getClass());
        ObjectNode accuracyNode = mapper.createObjectNode();
        accuracyNode.put("GuiName", "Solution accuracy");
        accuracyNode.put("dataType", "Real");
        accuracyNode.put("SWTWidget", "Text");
        accuracyNode.put("DefaultValue", "1.E-4");
        accuracyNode.put("Value", "");
        accuracyNode.put("Validation", "required");
        if (configs != null && configs.get(DAKOTA_COLINY_COBYLA_CONSTRAINT_OPTIMIZATION_BY_LINEAR_APPROXIMATIONS) != null) {

            Map<String, Object> specifics =
                (Map<String, Object>) ((HashMap<String, Object>) configs
                    .get(DAKOTA_COLINY_COBYLA_CONSTRAINT_OPTIMIZATION_BY_LINEAR_APPROXIMATIONS)).get(
                        SPECIFIC_SETTINGS);
            specifics.put("solution_accuracy", accuracyNode);

        }
        if (configs != null && configs.get(DAKOTA_COLINY_EVOLUTIONARY_ALGORITHM) != null) {
            Map<String, Object> specifics =
                (Map<String, Object>) ((HashMap<String, Object>) configs.get(DAKOTA_COLINY_EVOLUTIONARY_ALGORITHM)).get(
                    SPECIFIC_SETTINGS);
            specifics.put("solution_accuracy", accuracyNode);

        }
        ((ObjectNode) node.get(CONFIGURATION)).remove(METHOD_CONFIGURATIONS);
        ((ObjectNode) node.get(CONFIGURATION)).put(METHOD_CONFIGURATIONS, mapper.writeValueAsString(configs));
        ArrayNode inputs = ((ArrayNode) node.get(DYNAMIC_INPUTS));
        if (inputs != null) {
            Iterator<JsonNode> it = inputs.iterator();
            while (it.hasNext()) {
                ObjectNode input = (ObjectNode) it.next();
                if (((ObjectNode) input.get(METADATA)).get(GOAL) != null
                    && ((ObjectNode) input.get(METADATA)).get(GOAL).getTextValue().equals("Solve for")) {
                    ((ObjectNode) input.get(METADATA)).put(GOAL, "Minimize");
                    ((ObjectNode) input.get(METADATA)).remove("solve");
                }
            }
        }
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V6_1);
        return description;
    }

    @SuppressWarnings("unchecked")
    private PersistentComponentDescription updateToVersion60(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateSchedulingInformation(description);
        description = PersistentComponentDescriptionUpdaterUtils.updateIsNestedLoop(description);
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        ArrayNode statEndpoints = (ArrayNode) node.get("staticOutputs");
        if (statEndpoints != null) {
            ObjectNode gradientRequest = mapper.createObjectNode();
            gradientRequest.put("identifier", UUID.randomUUID().toString());
            gradientRequest.put(NAME, OptimizerComponentConstants.DERIVATIVES_NEEDED);
            gradientRequest.put(EP_IDENTIFIER, NullNode.instance);
            gradientRequest.put("datatype", "Boolean");
            statEndpoints.add(gradientRequest);
            for (JsonNode o : statEndpoints) {
                ObjectNode outNode = (ObjectNode) o;
                if (outNode.get(NAME).getTextValue().equals("Iteration count")) {
                    outNode.remove(NAME);
                    outNode.put(NAME, "Iteration");
                }
            }
        }

        ((ObjectNode) node.get(CONFIGURATION)).put("preCalcFilePath", "${preCalcFilePath}");
        TextNode methodConfigurations = (TextNode) node.get(CONFIGURATION).get(METHOD_CONFIGURATIONS);
        Map<String, Object> configs = mapper.readValue(methodConfigurations.getTextValue(), new HashMap<String, Object>().getClass());
        if (configs != null && configs.get("Dakota Surrogate-Based Local") != null) {
            ((HashMap<String, Object>) configs.get("Dakota Surrogate-Based Local")).put("methodCode", "surrogate_based_local");

        }
        if (configs != null && configs.get(DAKOTA_QUASI_NEWTON_METHOD) != null) {
            Map<String, Object> specifics =
                (Map<String, Object>) ((HashMap<String, Object>) configs.get(DAKOTA_QUASI_NEWTON_METHOD)).get(SPECIFIC_SETTINGS);
            specifics.remove("central_path");
            ((Map<String, Object>) specifics.get("merit_function")).put("dataType", "None");
            ((Map<String, Object>) specifics.get("merit_function")).put("defaultValue", "argaez_tapia");
        }
        Map<String, Object> moga =
            mapper.readValue(OptimizerFileLoader.class.getResourceAsStream("/resources/optimizer/dakota/dakotaMOGA.json"),
                new HashMap<String, Object>().getClass());
        Map<String, Object> soga =
            mapper.readValue(OptimizerFileLoader.class.getResourceAsStream("/resources/optimizer/dakota/dakotaSOGA.json"),
                new HashMap<String, Object>().getClass());
        Map<String, Object> defaults =
            mapper.readValue(OptimizerFileLoader.class.getResourceAsStream("/resources/optimizer/dakota/defaults.json"),
                new HashMap<String, Object>().getClass());
        moga.put("commonSettings", defaults);
        soga.put("commonSettings", defaults);

        if (configs != null) {
            configs.put("Dakota Multi Objective Genetic Algorithm", moga);
            configs.put("Dakota Single Objective Genetic Algorithm", soga);
        }
        ((ObjectNode) node.get(CONFIGURATION)).remove(METHOD_CONFIGURATIONS);
        ((ObjectNode) node.get(CONFIGURATION)).put(METHOD_CONFIGURATIONS, mapper.writeValueAsString(configs));

        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V6_0);
        return description;
    }

    private PersistentComponentDescription updateToVersion51(PersistentComponentDescription description) throws JsonParseException,
        IOException {
        if (!description.getComponentVersion().equals(V5_0)) {
            description = updateToVersion50(description);
        }
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        ArrayNode dynEndpoints = (ArrayNode) node.get(DYNAMIC_INPUTS);

        if (dynEndpoints != null) {
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(NAME) != null
                    && endpoint.get(NAME).getTextValue().contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                    ((ObjectNode) endpoint).put(EP_IDENTIFIER, "gradients");
                }
            }
        }
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V5_1);
        return description;
    }

    private PersistentComponentDescription updateToVersion50(PersistentComponentDescription description) throws JsonParseException,
        IOException {

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        ArrayNode dynEndpoints = (ArrayNode) node.get(DYNAMIC_OUTPUTS);

        if (dynEndpoints != null) {
            List<JsonNode> newNodes = new LinkedList<JsonNode>();
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(NAME) != null
                    && dynEndpoints.get(endpoint.get(NAME).getTextValue()
                        + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX) == null) {
                    ObjectNode optimalNode = (ObjectNode) copy(endpoint);
                    optimalNode.put("identifier", UUID.randomUUID().toString());
                    optimalNode.put(NAME, optimalNode.get(NAME).getTextValue() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX);
                    optimalNode.put(EP_IDENTIFIER, OptimizerComponentConstants.ID_OPTIMA);
                    newNodes.add(optimalNode);
                }

                if (endpoint.get(METADATA) != null) {
                    ((ObjectNode) endpoint.get(METADATA)).put(OptimizerComponentConstants.META_HAS_STARTVALUE, true);
                }
            }
            for (JsonNode e : newNodes) {
                dynEndpoints.add(e);
            }
        }
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V5_0);
        return description;
    }

    @SuppressWarnings("unchecked")
    private <T extends JsonNode> T copy(T node) {
        try {
            return (T) new ObjectMapper().readTree(node.traverse());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private PersistentComponentDescription updateToVersion3(PersistentComponentDescription description) throws JsonParseException,
        IOException {
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier(DYNAMIC_OUTPUTS, "Design", description);

        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        JsonNode dynEndpoints = node.get(DYNAMIC_INPUTS);
        if (dynEndpoints != null) {
            for (JsonNode endpoint : dynEndpoints) {
                if (endpoint.get(PersistentComponentDescriptionUpdaterUtils.EP_IDENTIFIER) == null
                    || endpoint.get(PersistentComponentDescriptionUpdaterUtils.EP_IDENTIFIER).getTextValue().equals("null")
                    || endpoint.get(PersistentComponentDescriptionUpdaterUtils.EP_IDENTIFIER).isNull()) {
                    ObjectNode objectEndpoint = (ObjectNode) endpoint;
                    objectEndpoint.remove(PersistentComponentDescriptionUpdaterUtils.EP_IDENTIFIER);
                    String identifier = "";
                    if ((objectEndpoint.get(METADATA)) != null
                        && !((ObjectNode) objectEndpoint.get(METADATA)).get(WEIGHT).getTextValue().equals(NAN)) {
                        identifier = OBJECTIVE;
                        updateMetaData(objectEndpoint, dynEndpoints);
                    } else if (((ObjectNode) objectEndpoint.get(METADATA)) != null) {
                        identifier = CONSTRAINT;
                        updateMetaData(objectEndpoint, dynEndpoints);
                    } else {
                        if (objectEndpoint.get(NAME).getTextValue().contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                            ObjectNode newMetadataObjectNode = JsonNodeFactory.instance.objectNode();
                            objectEndpoint.put(METADATA, newMetadataObjectNode);
                            String functionName =
                                objectEndpoint.get(NAME).getTextValue()
                                    .substring(1, objectEndpoint.get(NAME).getTextValue().indexOf('.'));
                            for (JsonNode otherEndpoint : dynEndpoints) {
                                if (otherEndpoint.get(NAME).getTextValue().equals(functionName)
                                    && (otherEndpoint.get(METADATA)) != null
                                    && !((ObjectNode) otherEndpoint.get(METADATA)).get(WEIGHT).getTextValue().equals(NAN)) {
                                    identifier = OBJECTIVE;
                                    break;
                                } else {
                                    identifier = CONSTRAINT;
                                }
                            }
                        }
                    }
                    objectEndpoint.put(PersistentComponentDescriptionUpdaterUtils.EP_IDENTIFIER, TextNode.valueOf(identifier));

                }
            }
        }
        // Add Static endpoints
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V3_0);
        return description;
    }

    private void updateMetaData(ObjectNode objectEndpoint, JsonNode dynEndpoints) {
        ObjectNode metadata = (ObjectNode) objectEndpoint.get(METADATA);
        if (metadata.get(GOAL).getTextValue().equals("0")) {
            metadata.put(GOAL, TextNode.valueOf("Minimize"));
        } else if (metadata.get(GOAL).getTextValue().equals("1")) {
            metadata.put(GOAL, TextNode.valueOf("Maximize"));
        } else {
            metadata.put(GOAL, TextNode.valueOf("Solve for"));
        }
        boolean hasMetaData = false;
        for (JsonNode otherEndpoint : dynEndpoints) {
            if (otherEndpoint.get(NAME).getTextValue().contains(OptimizerComponentConstants.GRADIENT_DELTA)
                && otherEndpoint.get(NAME).getTextValue()
                    .contains(OptimizerComponentConstants.GRADIENT_DELTA + objectEndpoint.get(NAME).getTextValue() + ".")) {
                hasMetaData = true;
            }
        }
        metadata.put("hasGradient", JsonNodeFactory.instance.booleanNode(hasMetaData));
        objectEndpoint.remove(METADATA);
        objectEndpoint.put(METADATA, metadata);
    }

    private PersistentComponentDescription updateBeforeVersion3(PersistentComponentDescription description) throws IOException,
        JsonParseException,
        JsonProcessingException, JsonGenerationException, JsonMappingException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonNode completeComponent;
        try (JsonParser jsonParser = jsonFactory.createJsonParser(description.getComponentDescriptionAsString())) {
            completeComponent = mapper.readTree(jsonParser);
        }
        JsonNode completeConfiguration = completeComponent.get(CONFIGURATION);
        JsonNode algorithmNode = null;
        JsonNode methodsConfigurationNode = null;
        JsonNode optimizerPackageNode = null;
        boolean foundMethodConfigurations = false;
        boolean foundPackageDeclaration = false;
        for (int i = 0; i < completeConfiguration.size(); i++) {
            if (completeConfiguration.get(i).getTextValue() != null) {
                String[] configItem = completeConfiguration.get(i).getTextValue().split(COLON);
                if (configItem[0].equals(ALGORITHM)) {
                    algorithmNode = updateAlgorithm(completeConfiguration.get(i));
                }
                if (configItem[0].contains(METHOD_CONFIGURATIONS) && configItem.length > 2 && configItem[2] != null) {
                    String configs = completeConfiguration.get(i).getTextValue();
                    configs = configs.substring(configs.indexOf("{"));
                    methodsConfigurationNode = updateMethods(configs);
                    foundMethodConfigurations = true;
                }
                if (configItem[0].equals(OptimizerComponentConstants.OPTIMIZER_PACKAGE)) {
                    foundPackageDeclaration = true;
                    if (configItem.length > 2 && configItem[2] != null && !configItem[2].isEmpty()
                        && (algorithmNode != null && algorithmNode.getTextValue().contains("Pyranha"))) {
                        optimizerPackageNode = TextNode.valueOf(OptimizerComponentConstants.OPTIMIZER_PACKAGE
                            + ":java.lang.String:pyranha");
                    } else if (configItem.length > 2 && configItem[2] != null && !configItem[2].isEmpty()
                        && (algorithmNode != null && algorithmNode.getTextValue().contains("Dakota"))) {
                        optimizerPackageNode = TextNode.valueOf(OptimizerComponentConstants.OPTIMIZER_PACKAGE
                            + ":java.lang.String:dakota");
                    } else {
                        optimizerPackageNode = TextNode.valueOf(OptimizerComponentConstants.OPTIMIZER_PACKAGE
                            + ":java.lang.String:generic");
                    }
                }
            }
        }
        if (!foundMethodConfigurations) {
            methodsConfigurationNode = writeNewMethodConfigurationsNode();
        }
        if (!foundPackageDeclaration) {
            optimizerPackageNode = TextNode.valueOf(OptimizerComponentConstants.OPTIMIZER_PACKAGE
                + ":java.lang.String:dakota");
        }
        if (completeConfiguration.get("genericPythonPath") != null) {
            ((ObjectNode) completeConfiguration).put("genericPythonPath", TextNode.valueOf("${genericPythonPath}"));
        }
        if (completeConfiguration.get("pyranhaPythonPath") != null) {
            ((ObjectNode) completeConfiguration).put("pyranhaPythonPath", TextNode.valueOf("${pyranhaPythonPath}"));
        }
        ArrayNode newConfig = mapper.createArrayNode();
        newConfig.add(algorithmNode);
        if (methodsConfigurationNode != null) {
            newConfig.add(methodsConfigurationNode);
        }
        newConfig.add(optimizerPackageNode);
        ((ObjectNode) completeComponent).remove(CONFIGURATION);
        ((ObjectNode) completeComponent).put(CONFIGURATION, newConfig);
        PersistentComponentDescription newDesc =
            new PersistentComponentDescription(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(completeComponent));
        newDesc.setComponentVersion(V1_0);
        return newDesc;
    }

    @SuppressWarnings("unchecked")
    private JsonNode updateMethods(String methodConfigs) {
        JsonNode returnNode = null;

        try {
            String newMethodConfiguations = METHOD_CONFIGURATIONS + ":java.lang.String:";
            Map<String, MethodDescription> defaultMethodConfigurations = OptimizerFileLoader.getAllMethodDescriptions("/optimizer");
            for (String key : defaultMethodConfigurations.keySet()) {
                defaultMethodConfigurations.put(key, mapper.convertValue(defaultMethodConfigurations.get(key), MethodDescription.class));
            }
            Map<String, MethodDescription> methodConfigurations;
            if (!methodConfigs.equals("")) {
                methodConfigs = StringUtils.unescapeSeparator(methodConfigs);
                methodConfigurations = mapper.readValue(methodConfigs, new HashMap<String, MethodDescription>().getClass());
                for (String key : methodConfigurations.keySet()) {
                    methodConfigurations.put(key, mapper.convertValue(methodConfigurations.get(key), MethodDescription.class));
                }
                replaceConfigurations(methodConfigurations, defaultMethodConfigurations);

            } else {
                return writeNewMethodConfigurationsNode();
            }
            returnNode =
                TextNode.valueOf(newMethodConfiguations
                    + StringUtils.escapeSeparator(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        methodConfigurations)));

        } catch (JsonParseException e) {
            LOGGER.error("Could not parse method file ", e);
        } catch (JsonMappingException e) {
            LOGGER.error("Could not map method file ", e);
        } catch (IOException e) {
            LOGGER.error("Could not load method file ", e);
        }
        return returnNode;
    }

    private static JsonNode writeNewMethodConfigurationsNode() {
        JsonNode returnNode = null;

        String newMethodConfiguations = METHOD_CONFIGURATIONS + ":java.lang.String:";
        Map<String, MethodDescription> defaultMethodConfigurations;
        try {
            defaultMethodConfigurations = OptimizerFileLoader.getAllMethodDescriptions("/optimizer");
            for (String key : defaultMethodConfigurations.keySet()) {
                defaultMethodConfigurations.put(key, mapper.convertValue(defaultMethodConfigurations.get(key), MethodDescription.class));
            }
            String configs = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultMethodConfigurations);
            configs = StringUtils.escapeSeparator(configs);
            returnNode = mapper.valueToTree(newMethodConfiguations + configs);
            return returnNode;
        } catch (JsonParseException e) {
            LOGGER.warn("", e);
        } catch (JsonMappingException e) {
            LOGGER.warn("", e);
        } catch (IOException e) {
            LOGGER.warn("", e);
        }
        return null;
    }

    private static JsonNode updateAlgorithm(JsonNode algorithm) {
        JsonNode returnNode = null;
        String prefix = "algorithm:java.lang.String:";
        String[] algorithmItem = algorithm.getTextValue().split(COLON);
        if (algorithmItem.length > 2) {
            if (algorithmItem[2] != null && algorithmItem[2].equals("HOPSPACK Asynch Pattern Search")) {
                returnNode = TextNode.valueOf(prefix + "Dakota HOPSPACK Asynch Pattern Search");
            }
            if (algorithmItem[2] != null && !algorithmItem[2].contains("Dakota")) {
                if (algorithmItem[2].contains("COBYLA")) {
                    returnNode = TextNode.valueOf(prefix + DAKOTA_COLINY_COBYLA_CONSTRAINT_OPTIMIZATION_BY_LINEAR_APPROXIMATIONS);
                }
                if (algorithmItem[2].contains("Newton")) {
                    returnNode = TextNode.valueOf(prefix + DAKOTA_QUASI_NEWTON_METHOD);
                }
                if (algorithmItem[2].contains("Evolutionary")) {
                    returnNode = TextNode.valueOf(prefix + DAKOTA_COLINY_EVOLUTIONARY_ALGORITHM);
                }
            }
        }
        if (returnNode != null) {
            return returnNode;
        }
        return algorithm;
    }

    private void replaceConfigurations(Map<String, MethodDescription> methodConfigurations,
        Map<String, MethodDescription> defaultMethodConfigurations) {

        // delete old methods from the configuration
        List<String> keysToDelete = new LinkedList<String>();
        for (String key : methodConfigurations.keySet()) {
            if (!defaultMethodConfigurations.containsKey(key)) {
                keysToDelete.add(key);
            }
        }
        for (String key : keysToDelete) {
            methodConfigurations.remove(key);
        }
        // replace existing keys
        for (String key : methodConfigurations.keySet()) {
            MethodDescription oldDescription = methodConfigurations.get(key);
            MethodDescription defaultdescription = defaultMethodConfigurations.get(key);

            oldDescription.setFollowingMethods(defaultdescription.getFollowingMethods());
            oldDescription.setMethodCode(defaultdescription.getMethodCode());
            oldDescription.setMethodName(defaultdescription.getMethodName());
            oldDescription.setOptimizerPackage(defaultdescription.getOptimizerPackage());

            replaceKeyValueMaps(oldDescription.getSpecificSettings(), defaultdescription.getSpecificSettings());
            replaceKeyValueMaps(oldDescription.getResponsesSettings(), defaultdescription.getResponsesSettings());

        }

        // add new methods to the configuration
        for (String key : defaultMethodConfigurations.keySet()) {
            if (!methodConfigurations.containsKey(key)) {
                methodConfigurations.put(key, defaultMethodConfigurations.get(key));
            }
        }
    }

    private void replaceKeyValueMaps(Map<String, Map<String, String>> currentSettings,
        Map<String, Map<String, String>> defaultSettings) {
        // delete old keys from the configuration
        if (currentSettings != null) {
            // delete old methods from the configuration
            List<String> keysToDelete = new LinkedList<String>();
            for (String key : currentSettings.keySet()) {
                if (!defaultSettings.containsKey(key)) {
                    keysToDelete.add(key);
                }
            }
            for (String key : keysToDelete) {
                currentSettings.remove(key);
            }
        }
        if (currentSettings != null) {

            for (String controlKey : currentSettings.keySet()) {
                Map<String, String> currentProperties = currentSettings.get(controlKey);
                Map<String, String> defaultProperties = defaultSettings.get(controlKey);
                // delete old properties
                for (String propertyKey : currentProperties.keySet()) {
                    if (!defaultProperties.containsKey(propertyKey)) {
                        currentProperties.remove(propertyKey);
                    }
                }
                // replace all other proerties but the value which could be user defined
                for (String propertyKey : currentProperties.keySet()) {
                    if (!propertyKey.equalsIgnoreCase(OptimizerComponentConstants.VALUE_KEY)) {
                        currentProperties.put(propertyKey, defaultProperties.get(propertyKey));
                    } else {
                        if (currentProperties.get(propertyKey).equals(
                            defaultProperties.get(OptimizerComponentConstants.DEFAULT_VALUE_KEY))) {
                            currentProperties.put(propertyKey, defaultProperties.get(propertyKey));
                        }
                    }
                }

                // add new properties
                for (String propertyKey : defaultProperties.keySet()) {
                    if (!currentProperties.containsKey(propertyKey)) {
                        currentProperties.put(propertyKey, defaultProperties.get(propertyKey));
                    }
                }
            }
        }
        // add new methods to the configuration
        if (defaultSettings != null && currentSettings != null) {
            for (String controlKey : defaultSettings.keySet()) {
                if (!currentSettings.containsKey(controlKey)) {
                    currentSettings.put(controlKey, defaultSettings.get(controlKey));
                }
            }
        }
    }

}
