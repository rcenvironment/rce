/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.update.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.update.api.DistributedPersistentComponentDescriptionUpdateService;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
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

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private NodeIdentifier localNodeId;

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

        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE, description, false);

        String workflowVersion = description.getWorkflowVersion();
        description = checkForWorkflowDescriptionUpdateAndPerformOnDemand(description, workflowVersion);

        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.FOR_VERSION_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.FOR_VERSION_THREE, description, false);

        description = checkForConnectionDescriptionUpdateAndPerformOnDemand(description, workflowVersion);

        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.AFTER_VERSION_THREE, description, true);
        description = performComponentDescriptionUpdates(PersistentDescriptionFormatVersion.AFTER_VERSION_THREE, description, false);

        description = updateWorkflowToCurrentVersion(description);

        return description;
    }

    private PersistentWorkflowDescription updateWorkflowToCurrentVersion(PersistentWorkflowDescription description) {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            JsonNode workflowDescriptionAsTree = mapper.readTree(description.getWorkflowDescriptionAsString());
            ((ObjectNode) workflowDescriptionAsTree).put(WORKFLOW_VERSION, TextNode.valueOf(CURRENT_VERSION));

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
                workflowDescriptionAsTree.put(NODES, componentNodes);
                if (connections != null) {
                    workflowDescriptionAsTree.put(CONNECTIONS, connections);
                }
                if (bendpoints != null) {
                    workflowDescriptionAsTree.put(BENDPOINTS, bendpoints);
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
            ((ObjectNode) workflowDescriptionAsTree).put(CONNECTIONS, connections);
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
        ((ObjectNode) workflowDescriptionAsTree).put(WORKFLOW_VERSION, TextNode.valueOf(VERSION_3));
        if (nodes != null) {
            ((ObjectNode) workflowDescriptionAsTree).put(NODES, nodes);
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
                newConnectionsObjectNode.put(SOURCE, connectionObject.get(SOURCE));
                String sourceOutput = connectionObject.get(OUTPUT).getTextValue();
                for (JsonNode component : nodes) {
                    if (((ObjectNode) component).get(IDENTIFIER).getTextValue().equals(connectionObject.get(SOURCE).getTextValue())) {
                        boolean foundOutput = false;
                        if (((ObjectNode) component).get(DYNAMIC_OUTPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(DYNAMIC_OUTPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).getTextValue().equals(sourceOutput)) {
                                    newConnectionsObjectNode.put(OUTPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundOutput = true;
                                }
                            }
                        }
                        if (((ObjectNode) component).get(STATIC_OUTPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(STATIC_OUTPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).getTextValue().equals(sourceOutput)) {
                                    newConnectionsObjectNode.put(OUTPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundOutput = true;
                                }
                            }
                        }
                        if (!foundOutput) {
                            ArrayNode staticOutputs = (ArrayNode) component.get(STATIC_OUTPUTS);
                            if (staticOutputs == null) {
                                staticOutputs = JsonNodeFactory.instance.arrayNode();
                                ((ObjectNode) component).put(STATIC_OUTPUTS, staticOutputs);
                            }
                            ObjectNode newStaticOutput = JsonNodeFactory.instance.objectNode();
                            newStaticOutput.put(NAME, sourceOutput);
                            newStaticOutput.put(IDENTIFIER, UUID.randomUUID().toString());
                            newStaticOutput.put(DATATYPE, SHORT_TEXT);
                            staticOutputs.add(newStaticOutput);

                            newConnectionsObjectNode.put(OUTPUT, newStaticOutput.get(IDENTIFIER));
                        }
                    }
                }

                newConnectionsObjectNode.put(TARGET, connectionObject.get(TARGET));

                String targetInput = connectionObject.get(INPUT).getTextValue();
                for (JsonNode component : nodes) {
                    if (((ObjectNode) component).get(IDENTIFIER).getTextValue().equals(connectionObject.get(TARGET).getTextValue())) {
                        boolean foundInput = false;
                        if (((ObjectNode) component).get(DYNAMIC_INPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(DYNAMIC_INPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).getTextValue().equals(targetInput)) {
                                    newConnectionsObjectNode.put(INPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundInput = true;
                                }
                            }
                        }
                        if (((ObjectNode) component).get(STATIC_INPUTS) != null) {
                            for (JsonNode endpoint : ((ObjectNode) component).get(STATIC_INPUTS)) {
                                if (((ObjectNode) endpoint).get(NAME).getTextValue().equals(targetInput)) {
                                    newConnectionsObjectNode.put(INPUT, ((ObjectNode) endpoint).get(IDENTIFIER));
                                    foundInput = true;
                                }
                            }
                        }
                        if (!foundInput) {
                            ArrayNode staticInputs = (ArrayNode) component.get(STATIC_INPUTS);
                            if (staticInputs == null) {
                                staticInputs = JsonNodeFactory.instance.arrayNode();
                                ((ObjectNode) component).put(STATIC_INPUTS, staticInputs);
                            }
                            ObjectNode newStaticInput = JsonNodeFactory.instance.objectNode();
                            newStaticInput.put(NAME, targetInput);
                            newStaticInput.put(IDENTIFIER, UUID.randomUUID().toString());
                            newStaticInput.put(DATATYPE, SHORT_TEXT);

                            staticInputs.add(newStaticInput);
                            newConnectionsObjectNode.put(INPUT, newStaticInput.get(IDENTIFIER));
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
                String[] splitted = StringUtils.splitAndUnescape(config.getTextValue());

                if (splitted.length > 2) {
                    newConfigNode.put(splitted[0], TextNode.valueOf(splitted[2]));
                } else {
                    newConfigNode.put(splitted[0], jsonFactory.textNode(""));
                }
            }
        }
        newComponentNode.put(CONFIGURATION, newConfigNode);
    }

    private static void updateDynamicEndpoints(String type,
        JsonNodeFactory jsonFactory, JsonNode componentNode, ObjectNode newComponentNode) {
        ArrayNode newEndpointList = jsonFactory.arrayNode();
        ArrayNode oldEndpointList = (ArrayNode) componentNode.get("add" + type);
        JsonNode metadata = componentNode.get(type.toLowerCase() + "MetaData");
        if (oldEndpointList != null) {
            for (JsonNode endpoint : oldEndpointList) {
                ObjectNode newEndpoint = jsonFactory.objectNode();
                String oldEndpoint = endpoint.getTextValue();
                newEndpoint.put(IDENTIFIER, UUID.randomUUID().toString());
                newEndpoint.put("epIdentifier", jsonFactory.nullNode());
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
                        String metadataText = metaDatum.getTextValue();
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
                    newEndpoint.put("metadata", newMetadata);
                }
                newEndpointList.add(newEndpoint);

            }
        }

        newComponentNode.remove("add" + type);
        newComponentNode.remove(type.toLowerCase() + "MetaData");

        newComponentNode.put("dynamic" + type + "s", newEndpointList);
    }

    @Override
    public PersistentWorkflowDescription createPersistentWorkflowDescription(String persistentWorkflowDescriptionString)
        throws JsonParseException, IOException {

        try (JsonParser jsonParser = new JsonFactory().createJsonParser(persistentWorkflowDescriptionString)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(jsonParser);

            List<PersistentComponentDescription> nodeDescriptionList = new ArrayList<PersistentComponentDescription>();
            JsonNode componentNodes = node.get(NODES);
            if (componentNodes != null) {
                nodeDescriptionList = createComponentDescriptions(componentNodes);
            }

            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            String workflowDescriptionString = writer.writeValueAsString(node);
            return new PersistentWorkflowDescription(nodeDescriptionList, workflowDescriptionString);
        }
    }

    private List<PersistentComponentDescription> createComponentDescriptions(JsonNode nodes)
        throws JsonGenerationException, JsonMappingException, IOException {

        List<PersistentComponentDescription> componentDescriptions = new LinkedList<PersistentComponentDescription>();
        if (nodes != null) {
            Iterator<JsonNode> nodeIterator = nodes.getElements();
            while (nodeIterator.hasNext()) {
                JsonNode component = nodeIterator.next();

                ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
                ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
                componentDescriptions.add(new PersistentComponentDescription(writer.writeValueAsString(component)));
            }
            DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();

            for (PersistentComponentDescription componentDescription : componentDescriptions) {
                checkAndSetNodeIdentifier(componentDescription, compKnowledge.getAllInstallations());
            }
        }

        return componentDescriptions;
    }

    protected PersistentComponentDescription checkAndSetNodeIdentifier(PersistentComponentDescription compDesc,
        Collection<ComponentInstallation> collection) {

        ComponentInstallation exactlyMatchingComponent = null;
        
        List<ComponentInstallation> matchingComponents = new ArrayList<ComponentInstallation>();

        // for all registered components which match the persistent one (identifiers are equal and version of persistent one is greater or
        // equal of registered one) decide:
        // if the platform is equal as well, the component is registered on the node where it was when workflow was created, the update
        // check can be directly done on the given node, the description can be returned as it is and this method is done otherwise add the
        // basically matching component to the list of matching components which will be considered later on
        for (ComponentInstallation compInst : collection) {
            ComponentInterface compInterface = compInst.getComponentRevision().getComponentInterface();
            String compId = compInterface.getIdentifier();
            if (compId.contains(ComponentConstants.ID_SEPARATOR)) {
                compId = compInterface.getIdentifier().split(ComponentConstants.ID_SEPARATOR)[0];
            }
            if (compId.equals(compDesc.getComponentIdentifier())
                && (compDesc.getComponentVersion().equals("")
                || compInterface.getVersion().compareTo(compDesc.getComponentVersion()) >= 0)) {
                if (compInst.getNodeId() == null || compInst.getNodeId().equals(localNodeId.getIdString())) {
                    compDesc.setNodeIdentifier(null);
                    return compDesc;
                } else if (compInst.getNodeId() != null && compDesc.getComponentNodeIdentifier() != null
                    && compInst.getNodeId().equals(compDesc.getComponentNodeIdentifier().getIdString())) {
                    exactlyMatchingComponent = compInst;
                } else {
                    matchingComponents.add(compInst);
                }
            }
        }

        // if there is not local component, take the exactly matching remote component if there is one
        if (exactlyMatchingComponent != null) {
            compDesc.setNodeIdentifier(NodeIdentifierFactory.fromNodeId(exactlyMatchingComponent.getNodeId()));
            return compDesc;
        }
        // a matching component on the originally registered node was not found. thus set the node
        // identifier of any matching component if there is at least one found
        if (matchingComponents.size() > 0) {
            compDesc.setNodeIdentifier(NodeIdentifierFactory.fromNodeId(matchingComponents.get(0).getNodeId()));
            return compDesc;
        }

        // if there is no matching component found in the RCE network set the node identifier to
        // local, thus the local update service will be requested and will return that it has no
        // updater registered for the component as it is not registered at all
        compDesc.setNodeIdentifier(null);
        return compDesc;

    }

    /**
     * OSGi bind method. This bind method is set to public for tests of this class.
     * 
     * @param updateService to bind.
     */
    public void bindComponentDescriptionUpdateService(DistributedPersistentComponentDescriptionUpdateService updateService) {
        this.componentUpdateService = updateService;
    }

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService service) {
        this.componentKnowledgeService = service;
    }

    protected void bindPlatformService(PlatformService platformService) {
        localNodeId = platformService.getLocalNodeId();
    }

}
