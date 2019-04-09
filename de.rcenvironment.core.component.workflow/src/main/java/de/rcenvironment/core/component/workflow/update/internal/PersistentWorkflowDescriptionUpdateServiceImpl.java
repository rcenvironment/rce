/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.api.RemotablePersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescription;
import de.rcenvironment.core.component.workflow.update.api.PersistentWorkflowDescriptionUpdateService;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link PersistentWorkflowDescriptionUpdateService}.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 * 
 * Note: See note in {@link RemotablePersistentComponentDescriptionUpdateService}. --seid_do
 */
public class PersistentWorkflowDescriptionUpdateServiceImpl implements PersistentWorkflowDescriptionUpdateService {

    private static final String WORKFLOW_VERSION = "workflowVersion";

    private static final String DYNAMIC_OUTPUTS = "dynamicOutputs";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String DATATYPE = "datatype";

    private static final String SHORT_TEXT = "ShortText";

    private static final String STATIC_INPUTS = "staticInputs";

    private static final String STATIC_OUTPUTS = "staticOutputs";

    private static final String CURRENT_VERSION = String.valueOf(WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER);

    private static final String VERSION_3 = "3";

    private static final String NAME = "name";

    private static final String INPUT = "input";

    private static final String OUTPUT = "output";

    private static final String IDENTIFIER = "identifier";

    private static final String TARGET = "target";

    private static final String SOURCE = "source";

    private static final String CONFIGURATION = "configuration";

    @SuppressWarnings("serial")
    private static final Map<String, String> OLD_TO_NEW_TYPES = new HashMap<String, String>() {

        {
            put("java.lang.String", SHORT_TEXT);
            put("java.lang.Double", "Float");
            put("java.lang.Integer", "Integer");
            put("java.lang.Float", "Float");
            put("java.lang.Long", "Integer");
            put("java.lang.Boolean", "Boolean");
            put("de.rcenvironment.commons.channel.DataManagementFileReference", "FileReference");
            put("de.rcenvironment.commons.channel.VariantArray", "SmallTable");
        }
    };

    private static final String BENDPOINTS = "bendpoints";

    private static final String CONNECTIONS = "connections";

    private static final String NODES = "nodes";

    private static final Log LOGGER = LogFactory.getLog(PersistentWorkflowDescriptionUpdateServiceImpl.class);

    private DistributedPersistentComponentDescriptionUpdateService componentUpdateService;

    @Override
    public boolean isUpdateForWorkflowDescriptionAvailable(PersistentWorkflowDescription description, boolean silent) {
        if (!silent) {
            if (description.getWorkflowVersion().compareTo(VERSION_3) < 0) {
                return true;
            }
        } else {
            if (description.getWorkflowVersion().compareTo(CURRENT_VERSION) < 0) {
                return true;
            }
        }
        if (componentUpdateService.getFormatVersionsAffectedByUpdate(description.getComponentDescriptions(),
            silent) != PersistentDescriptionFormatVersion.NONE) {
            return true;
        }
        return false;
    }

    @Override
    public PersistentWorkflowDescription performWorkflowDescriptionUpdate(PersistentWorkflowDescription persistentDescription)
        throws IOException {

        PersistentWorkflowDescription description = persistentDescription;

        Set<String> endpoints = getEndpoints(description);

        // TODO warum immer erst einmal silent=true, dann silent=false?
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, description, false);

        String workflowVersion = description.getWorkflowVersion();
        description = checkForWorkflowDescriptionUpdateAndPerformOnDemand(description, workflowVersion);

        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.FOR_VERSION_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.FOR_VERSION_THREE, description, false);

        description = checkForConnectionDescriptionUpdateAndPerformOnDemand(description, workflowVersion);

        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.AFTER_VERSION_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.AFTER_VERSION_THREE, description, false);

        endpoints.removeAll(getEndpoints(description));
        description = removeConnectionsRelatedToRemovedEndpoints(description, endpoints);

        description = updateWorkflowToCurrentVersion(description);

        return description;
    }

    private Set<String> getEndpoints(PersistentWorkflowDescription persWfDescription) throws JsonProcessingException, IOException {
        Set<String> endpoints = new HashSet<>();
        for (PersistentComponentDescription persCompDesc : persWfDescription.getComponentDescriptions()) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(persCompDesc.getComponentDescriptionAsString());
            endpoints.addAll(getEndpointsOfGroup(node, STATIC_INPUTS));
            endpoints.addAll(getEndpointsOfGroup(node, STATIC_OUTPUTS));
            endpoints.addAll(getEndpointsOfGroup(node, DYNAMIC_INPUTS));
            endpoints.addAll(getEndpointsOfGroup(node, DYNAMIC_OUTPUTS));
        }
        return endpoints;
    }

    private Set<String> getEndpointsOfGroup(JsonNode node, String endpointGroup) throws JsonProcessingException, IOException {
        Set<String> endpoints = new HashSet<>();
        if (node.has(endpointGroup)) {
            Iterator<JsonNode> outputJsonNodes = node.get(endpointGroup).elements();
            while (outputJsonNodes.hasNext()) {
                endpoints.add(outputJsonNodes.next().get(IDENTIFIER).textValue());
            }
        }
        return endpoints;
    }

    private PersistentWorkflowDescription removeConnectionsRelatedToRemovedEndpoints(PersistentWorkflowDescription persWfDescription,
        Set<String> endpointsRemoved) throws JsonProcessingException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode workflowDescriptionAsTree = mapper.readTree(persWfDescription.getWorkflowDescriptionAsString());
        for (String endpointRemoved : endpointsRemoved) {
            if (workflowDescriptionAsTree.has(CONNECTIONS)) {
                ArrayNode connectionsJsonNode = (ArrayNode) workflowDescriptionAsTree.get(CONNECTIONS);

                Iterator<JsonNode> connectionJsonNodes = connectionsJsonNode.elements();
                while (connectionJsonNodes.hasNext()) {
                    JsonNode connectionJsonNode = connectionJsonNodes.next();
                    if (connectionJsonNode.get("input").textValue().equals(endpointRemoved)
                        || connectionJsonNode.get("output").textValue().equals(endpointRemoved)) {
                        connectionJsonNodes.remove();
                    }
                }
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentWorkflowDescription(persWfDescription.getComponentDescriptions(),
            writer.writeValueAsString(workflowDescriptionAsTree));
    }

    /**
     * Increases only the version number of the given {@link PersistentComponentDescription}.
     */
    private PersistentWorkflowDescription updateWorkflowToCurrentVersion(PersistentWorkflowDescription description) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            JsonNode workflowDescriptionAsTree = mapper.readTree(description.getWorkflowDescriptionAsString());
            ((ObjectNode) workflowDescriptionAsTree).set(WORKFLOW_VERSION, TextNode.valueOf(CURRENT_VERSION));

            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

            JsonNode nodes = workflowDescriptionAsTree.get(NODES);
            return new PersistentWorkflowDescription(createComponentDescriptions(nodes),
                writer.writeValueAsString(workflowDescriptionAsTree));
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getStackTrace());
        } catch (IOException e) {
            LOGGER.error(e.getStackTrace());
        }
        return null;

    }

    private PersistentWorkflowDescription checkForWorkflowDescriptionUpdateAndPerformOnDemand(PersistentWorkflowDescription description,
        String version) throws IOException {
        if (version.compareTo(VERSION_3) < 0) {
            description = performWorkflowDescriptionUpdateVersionOneToTwo(description);
        }
        return description;
    }

    private PersistentWorkflowDescription checkForConnectionDescriptionUpdateAndPerformOnDemand(PersistentWorkflowDescription description,
        String version) throws IOException {
        if (version.compareTo(VERSION_3) < 0) {
            description = performWorkflowDescriptionUpdateVersionOneToTwoForConnections(description);
        }
        return description;
    }

    private PersistentWorkflowDescription performComponentDescriptionUpdates(int formatVersion,
        PersistentWorkflowDescription description, boolean silent) throws IOException {

        List<PersistentComponentDescription> componentDescriptions = componentUpdateService
            .performComponentDescriptionUpdates(formatVersion, description.getComponentDescriptions(), silent);

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        try {
            ObjectNode workflowDescriptionAsTree = (ObjectNode) mapper.readTree(description.getWorkflowDescriptionAsString());
            ArrayNode componentNodes = new ArrayNode(jsonFactory);
            if (componentDescriptions.size() > 0) {
                for (PersistentComponentDescription pcd : componentDescriptions) {
                    JsonNode component = mapper.readTree(pcd.getComponentDescriptionAsString());
                    componentNodes.add(component);
                }

                JsonNode connections = workflowDescriptionAsTree.remove(CONNECTIONS);
                JsonNode bendpoints = workflowDescriptionAsTree.remove(BENDPOINTS);
                workflowDescriptionAsTree.remove(NODES);
                workflowDescriptionAsTree.set(NODES, componentNodes);
                if (connections != null) {
                    workflowDescriptionAsTree.set(CONNECTIONS, connections);
                }
                if (bendpoints != null) {
                    workflowDescriptionAsTree.set(BENDPOINTS, bendpoints);
                }
            }
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            return new PersistentWorkflowDescription(componentDescriptions, writer.writeValueAsString(workflowDescriptionAsTree));
        } catch (JsonProcessingException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return null;
    }

    private PersistentWorkflowDescription performWorkflowDescriptionUpdateVersionOneToTwoForConnections(
        PersistentWorkflowDescription description) throws IOException {

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

        JsonNode workflowDescriptionAsTree = mapper.readTree(description.getWorkflowDescriptionAsString());
        JsonNode nodes = workflowDescriptionAsTree.get(NODES);
        JsonNode connections = workflowDescriptionAsTree.get(CONNECTIONS);
        connections = updateConnectionsToVersion3(connections, nodes, jsonFactory);
        ((ObjectNode) workflowDescriptionAsTree).remove(CONNECTIONS);
        if (connections != null) {
            ((ObjectNode) workflowDescriptionAsTree).set(CONNECTIONS, connections);
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentWorkflowDescription(createComponentDescriptions(nodes),
            writer.writeValueAsString(workflowDescriptionAsTree));
    }

    private PersistentWorkflowDescription performWorkflowDescriptionUpdateVersionOneToTwo(PersistentWorkflowDescription description)
        throws IOException {

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

        JsonNode workflowDescriptionAsTree = mapper.readTree(description.getWorkflowDescriptionAsString());
        JsonNode nodes = workflowDescriptionAsTree.get(NODES);
        if (nodes != null) {
            nodes = updateNodesToVersion3(nodes, jsonFactory);
        }

        ((ObjectNode) workflowDescriptionAsTree).remove(NODES);
        ((ObjectNode) workflowDescriptionAsTree).remove(WORKFLOW_VERSION);
        ((ObjectNode) workflowDescriptionAsTree).set(WORKFLOW_VERSION, TextNode.valueOf(VERSION_3));
        if (nodes != null) {
            ((ObjectNode) workflowDescriptionAsTree).set(NODES, nodes);
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentWorkflowDescription(createComponentDescriptions(nodes),
            writer.writeValueAsString(workflowDescriptionAsTree));
    }

    private static JsonNode updateNodesToVersion3(JsonNode nodes, JsonNodeFactory jsonFactory) {
        ArrayNode newNodes = jsonFactory.arrayNode();

        for (JsonNode componentNode : nodes) {
            ObjectNode newComponentNode = (ObjectNode) componentNode;

            updateDynamicEndpoints("Output", jsonFactory, componentNode, newComponentNode);
            updateDynamicEndpoints("Input", jsonFactory, componentNode, newComponentNode);
            updateConfiguration(jsonFactory, componentNode, newComponentNode);
            newNodes.add(newComponentNode);
        }

        return newNodes;
    }

    private static JsonNode updateConnectionsToVersion3(JsonNode connections, JsonNode nodes, JsonNodeFactory jsonFactory) {
        ArrayNode newConnectionsArrayNode = jsonFactory.arrayNode();
        if (connections != null) {
            for (JsonNode connection : ((ArrayNode) connections)) {
                ObjectNode connectionObject = (ObjectNode) connection;
                ObjectNode newConnectionsObjectNode = jsonFactory.objectNode();
                newConnectionsObjectNode.set(SOURCE, connectionObject.get(SOURCE));
                String sourceOutput = connectionObject.get(OUTPUT).textValue();
                for (JsonNode component : nodes) {
                    if (((ObjectNode) component).get(IDENTIFIER).textValue().equals(connectionObject.get(SOURCE).textValue())) {
                        boolean foundOutput = false;
                        if (((ObjectNode) component).get(DYNAMIC_OUTPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(DYNAMIC_OUTPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).textValue().equals(sourceOutput)) {
                                    newConnectionsObjectNode.set(OUTPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundOutput = true;
                                }
                            }
                        }
                        if (((ObjectNode) component).get(STATIC_OUTPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(STATIC_OUTPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).textValue().equals(sourceOutput)) {
                                    newConnectionsObjectNode.set(OUTPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundOutput = true;
                                }
                            }
                        }
                        if (!foundOutput) {
                            ArrayNode staticOutputs = (ArrayNode) component.get(STATIC_OUTPUTS);
                            if (staticOutputs == null) {
                                staticOutputs = JsonNodeFactory.instance.arrayNode();
                                ((ObjectNode) component).set(STATIC_OUTPUTS, staticOutputs);
                            }
                            ObjectNode newStaticOutput = JsonNodeFactory.instance.objectNode();
                            newStaticOutput.put(NAME, sourceOutput);
                            newStaticOutput.put(IDENTIFIER, UUID.randomUUID().toString());
                            newStaticOutput.put(DATATYPE, SHORT_TEXT);
                            staticOutputs.add(newStaticOutput);

                            newConnectionsObjectNode.set(OUTPUT, newStaticOutput.get(IDENTIFIER));
                        }
                    }
                }

                newConnectionsObjectNode.set(TARGET, connectionObject.get(TARGET));

                String targetInput = connectionObject.get(INPUT).textValue();
                for (JsonNode component : nodes) {
                    if (((ObjectNode) component).get(IDENTIFIER).textValue().equals(connectionObject.get(TARGET).textValue())) {
                        boolean foundInput = false;
                        if (((ObjectNode) component).get(DYNAMIC_INPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(DYNAMIC_INPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).textValue().equals(targetInput)) {
                                    newConnectionsObjectNode.set(INPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundInput = true;
                                }
                            }
                        }
                        if (((ObjectNode) component).get(STATIC_INPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(STATIC_INPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).textValue().equals(targetInput)) {
                                    newConnectionsObjectNode.set(INPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundInput = true;
                                }
                            }
                        }
                        if (!foundInput) {
                            ArrayNode staticInputs = (ArrayNode) component.get(STATIC_INPUTS);
                            if (staticInputs == null) {
                                staticInputs = JsonNodeFactory.instance.arrayNode();
                                ((ObjectNode) component).set(STATIC_INPUTS, staticInputs);
                            }
                            ObjectNode newStaticInput = JsonNodeFactory.instance.objectNode();
                            newStaticInput.put(NAME, targetInput);
                            newStaticInput.put(IDENTIFIER, UUID.randomUUID().toString());
                            newStaticInput.put(DATATYPE, SHORT_TEXT);

                            staticInputs.add(newStaticInput);
                            newConnectionsObjectNode.set(INPUT, newStaticInput.get(IDENTIFIER));
                        }
                    }
                }
                newConnectionsArrayNode.add(newConnectionsObjectNode);
            }
        }
        return newConnectionsArrayNode;
    }

    private static void updateConfiguration(JsonNodeFactory jsonFactory, JsonNode componentNode, ObjectNode newComponentNode) {
        ObjectNode newConfigNode = jsonFactory.objectNode();
        ArrayNode oldConfigNode = (ArrayNode) componentNode.get(CONFIGURATION);
        if (oldConfigNode != null) {
            for (JsonNode config : oldConfigNode) {
                String[] splitted = StringUtils.splitAndUnescape(config.textValue());

                if (splitted.length > 2) {
                    newConfigNode.set(splitted[0], TextNode.valueOf(splitted[2]));
                } else {
                    newConfigNode.set(splitted[0], jsonFactory.textNode(""));
                }
            }
        }
        newComponentNode.set(CONFIGURATION, newConfigNode);
    }

    private static void updateDynamicEndpoints(String type,
        JsonNodeFactory jsonFactory, JsonNode componentNode, ObjectNode newComponentNode) {
        ArrayNode newEndpointList = jsonFactory.arrayNode();
        ArrayNode oldEndpointList = (ArrayNode) componentNode.get("add" + type);
        JsonNode metadata = componentNode.get(type.toLowerCase() + "MetaData");
        if (oldEndpointList != null) {
            for (JsonNode endpoint : oldEndpointList) {
                ObjectNode newEndpoint = jsonFactory.objectNode();
                String oldEndpoint = endpoint.textValue();
                newEndpoint.put(IDENTIFIER, UUID.randomUUID().toString());
                newEndpoint.set("epIdentifier", jsonFactory.nullNode());
                String[] splittedEndpoint = StringUtils.splitAndUnescape(oldEndpoint);

                newEndpoint.put(NAME, splittedEndpoint[0]);
                newEndpoint.put(DATATYPE, OLD_TO_NEW_TYPES.get(splittedEndpoint[1]));
                if (splittedEndpoint.length > 2) {
                    newEndpoint.put("value", splittedEndpoint[2]);
                } else {
                    newEndpoint.put("value", "");
                }
                if (metadata != null && metadata.get(splittedEndpoint[0]) != null) {
                    ObjectNode newMetadata = jsonFactory.objectNode();
                    for (JsonNode metaDatum : metadata.get(splittedEndpoint[0])) {
                        String metadataText = metaDatum.textValue();
                        String[] splittedMetaDatum = StringUtils.splitAndUnescape(metadataText);
                        if (splittedMetaDatum[0].equals("usage")) {
                            if (splittedMetaDatum[2].equals("init")) {
                                splittedMetaDatum[2] = "initial";
                            } else if (splittedMetaDatum[2].equals("Required")) {
                                splittedMetaDatum[2] = "required";
                            } else if (splittedMetaDatum[2].equals("Optional")) {
                                splittedMetaDatum[2] = "optional";
                            }
                        }
                        newMetadata.put(splittedMetaDatum[0], splittedMetaDatum[2]);
                    }
                    newEndpoint.set("metadata", newMetadata);
                }
                newEndpointList.add(newEndpoint);

            }
        }

        newComponentNode.remove("add" + type);
        newComponentNode.remove(type.toLowerCase() + "MetaData");

        newComponentNode.set("dynamic" + type + "s", newEndpointList);
    }

    @Override
    public PersistentWorkflowDescription createPersistentWorkflowDescription(String persistentWorkflowDescriptionString)
        throws JsonParseException, IOException {

        try (JsonParser jsonParser = new JsonFactory().createParser(persistentWorkflowDescriptionString)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(jsonParser);

            List<PersistentComponentDescription> nodeDescriptionList = new ArrayList<PersistentComponentDescription>();
            JsonNode componentNodes = node.get(NODES);
            if (componentNodes != null) {
                nodeDescriptionList = createComponentDescriptions(componentNodes);
            }

            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            String workflowDescriptionString = writer.writeValueAsString(node);
            PersistentWorkflowDescription result = new PersistentWorkflowDescription(nodeDescriptionList, workflowDescriptionString);
            return result;
        }
    }

    /**
     * Creates a List of {@link PersistentComponentDescription}s from the {@link JsonNode} containing the component information.
     * Furthermore, it replaces the persisted target node identifiers of the components with target nodes which are currently available in
     * the network.
     * 
     * @param nodes
     * @return
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    private List<PersistentComponentDescription> createComponentDescriptions(JsonNode nodes)
        throws JsonGenerationException, JsonMappingException, IOException {

        List<PersistentComponentDescription> componentDescriptions = new LinkedList<PersistentComponentDescription>();
        if (nodes != null) {
            Iterator<JsonNode> nodeIterator = nodes.elements();
            while (nodeIterator.hasNext()) {
                JsonNode component = nodeIterator.next();

                ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

                String compontentStr = writer.writeValueAsString(component);
                PersistentComponentDescription pcd = new PersistentComponentDescription(compontentStr);
                componentDescriptions.add(pcd);
            }
        }

        return componentDescriptions;
    }

    /**
     * OSGi bind method. This bind method is set to public for tests of this class.
     * 
     * @param updateService to bind.
     */
    public void bindComponentDescriptionUpdateService(DistributedPersistentComponentDescriptionUpdateService updateService) {
        this.componentUpdateService = updateService;
    }

}
