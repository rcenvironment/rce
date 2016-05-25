/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentDescriptionFactoryService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointGroupDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.AlignmentType;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Writes and reads {@link WorkflowDescription}s to and from {@link java.io.File}s.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Marc Stammerjohann
 * @author Oliver Seebach
 */
public class WorkflowDescriptionPersistenceHandler {

    /** Field name. */
    public static final String COORDINATES = "coordinates";

    /** Field name. */
    public static final String COLOR_RGB_TEXT = "colorText";

    /** Field name. */
    public static final String COLOR_RGB_BACKGROUND = "colorBackground";

    /** Field name. */
    public static final String ALIGNMENT_TYPE = "alignmentType";

    /** Field name. */
    public static final String ALPHA = "alpha";

    /** Field name. */
    public static final String BORDER = "border";

    /** Field name. */
    public static final String TEXT_SIZE = "textSize";

    /** Field name. */
    public static final String LABELS = "labels";

    /** Field name. */
    public static final String SIZE = "size";

    /** Field name. */
    public static final String TEXT = "text";

    /** Field name. */
    public static final String BENDPOINTS = "bendpoints";

    /** Field name. */
    public static final String INPUT = "input";

    /** Field name. */
    public static final String TARGET = "target";

    /** Field name. */
    public static final String OUTPUT = "output";

    /** Field name. */
    public static final String SOURCE = "source";

    /** Field name. */
    public static final String CONNECTIONS = "connections";

    /** Field name. */
    public static final String CONFIGURATION = "configuration";

    /** Field name. */
    public static final String CONFIGURATIONS = "configurations";

    /** Field name. */
    public static final String CURRENT_CONFIGURATION_IDENTIFIER = "currentConfigurationIdentifier";

    /** Field name. */
    public static final String CONFIGURATION_MAP = "map";

    /** Field name. */
    public static final String STATIC_INPUTS = "staticInputs";

    /** Field name. */
    public static final String DYNAMIC_INPUTS = "dynamicInputs";

    /** Field name. */
    public static final String STATIC_OUTPUTS = "staticOutputs";

    /** Field name. */
    public static final String DYNAMIC_OUTPUTS = "dynamicOutputs";

    /** Field name. */
    public static final String DYNAMIC_INPUT_GROUPS = "dynamicInputGroups";

    /** Field name. */
    public static final String OUTPUT_META_DATA = "outputMetaData";

    /** Field name. */
    public static final String INPUT_META_DATA = "inputMetaData";

    /** Field name. */
    public static final String VERSION = "version";

    /** Field name. */
    public static final String COMPONENT = "component";

    /** Field name. */
    public static final String LOCATION = "location";

    /** Field name. */
    public static final String ACTIVE = "active";

    /** Field name. */
    public static final String NODES = "nodes";

    /** Field name. */
    public static final String PLATFORM = "platform";

    /** Field name. */
    public static final String NAME = "name";

    /** Field name. */
    public static final String METADATA = "metadata";

    /** Field name. */
    public static final String DATATYPE = "datatype";

    /** Field name. */
    public static final String IDENTIFIER = "identifier";

    /** Field name. */
    public static final String GROUP = "group";

    /** Field name. */
    public static final String EP_IDENTIFIER = "epIdentifier";

    /** Field name. */
    public static final String WORKFLOW_VERSION = "workflowVersion";

    /** Field name. */
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";

    /** Field name. */
    public static final String BENDPOINT_SEPARATOR = ",";

    /** Field name. */
    public static final String BENDPOINT_COORDINATE_SEPARATOR = ":";

    protected static PlatformService platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);

    protected static DistributedComponentKnowledgeService componentKnowledgeService = ServiceUtils
        .createFailingServiceProxy(DistributedComponentKnowledgeService.class);

    protected static ComponentDescriptionFactoryService componentDescriptionFactoryService = ServiceUtils
        .createFailingServiceProxy(ComponentDescriptionFactoryService.class);

    private static final String ERROR_WHEN_PARSING_WORKFLOW_FILE = "Error when parsing workflow file: ";

    private static final Log LOG = LogFactory.getLog(WorkflowDescriptionPersistenceHandler.class);

    private static final String SEPARATOR = "/";

    private static final ObjectMapper JSON_OBJECT_MAPPER = JsonUtils.getDefaultObjectMapper();

    private final Map<String, Map<String, EndpointDescription>> endpointDescs = new HashMap<String, Map<String, EndpointDescription>>();

    public WorkflowDescriptionPersistenceHandler() {}

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        componentKnowledgeService = newService;
    }

    protected void unbindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService oldService) {
        componentKnowledgeService = ServiceUtils.createFailingServiceProxy(DistributedComponentKnowledgeService.class);
    }

    protected void bindComponentDescriptionFactoryService(ComponentDescriptionFactoryService newService) {
        componentDescriptionFactoryService = newService;
    }

    protected void unbindComponentDescriptionFactoryService(ComponentDescriptionFactoryService oldService) {
        componentDescriptionFactoryService = ServiceUtils.createFailingServiceProxy(ComponentDescriptionFactoryService.class);
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    protected void unbindPlatformService(PlatformService oldPlatformService) {
        platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);
    }

    /**
     * Writes the given {@link WorkflowDescription} into an {@link OutputStream}.
     * 
     * @param wd The {@link WorkflowDescription} to write.
     * @return An byte array with the {@link WorkflowDescription}.
     * @throws IOException if writing to {@link java.io.File} failed for some reason.
     */
    public ByteArrayOutputStream writeWorkflowDescriptionToStream(WorkflowDescription wd) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonFactory f = new JsonFactory();
        JsonGenerator g = f.createJsonGenerator(outputStream, JsonEncoding.UTF8);
        g.setPrettyPrinter(new DefaultPrettyPrinter());

        g.writeStartObject();

        g.writeStringField(IDENTIFIER, wd.getIdentifier());
        g.writeStringField(WORKFLOW_VERSION, String.valueOf(wd.getWorkflowVersion()));

        writeOptionalValue(g, NAME, wd.getName());
        if (wd.getControllerNode() != null && !wd.getIsControllerNodeIdTransient()) {
            writeOptionalValue(g, PLATFORM, wd.getControllerNode().getIdString());
        }
        writeOptionalValue(g, ADDITIONAL_INFORMATION, wd.getAdditionalInformation());

        if (wd.getWorkflowNodes().size() > 0) {
            g.writeArrayFieldStart(NODES);
            List<WorkflowNode> nodes = wd.getWorkflowNodes();
            Collections.sort(nodes);
            for (WorkflowNode node : nodes) {
                writeWorkflowNode(g, node);
            }
            g.writeEndArray(); // 'nodes'
        }

        if (wd.getConnections().size() > 0) {
            g.writeArrayFieldStart(CONNECTIONS);
            List<Connection> connections = wd.getConnections();
            Collections.sort(connections);
            for (Connection connection : connections) {
                writeConnection(g, connection);
            }
            g.writeEndArray(); // 'connections'

            Map<String, String> connectionBendpointMapping = calculateUniqueBendpointList(wd.getConnections());
            if (!connectionBendpointMapping.isEmpty()) {
                ByteArrayOutputStream bendpointsStream = new ByteArrayOutputStream();
                JsonGenerator bendpointsGenerator = f.createJsonGenerator(bendpointsStream, JsonEncoding.UTF8);
                bendpointsGenerator.writeStartArray();
                writeBendpoints(bendpointsGenerator, connectionBendpointMapping);
                bendpointsGenerator.writeEndArray();
                bendpointsGenerator.close();
                g.writeStringField(BENDPOINTS, bendpointsStream.toString());
            }
        }
        if (wd.getWorkflowLabels().size() > 0) {

            ByteArrayOutputStream labelsStream = new ByteArrayOutputStream();
            JsonGenerator labelsGenerator = f.createJsonGenerator(labelsStream, JsonEncoding.UTF8);
            labelsGenerator.writeStartArray();

            List<WorkflowLabel> workflowLabels = wd.getWorkflowLabels();
            Collections.sort(workflowLabels);
            for (WorkflowLabel label : workflowLabels) {
                writeLabel(labelsGenerator, label);
            }
            labelsGenerator.writeEndArray();
            labelsGenerator.close();
            g.writeStringField(LABELS, labelsStream.toString());

        }
        g.writeEndObject();
        g.close();

        return outputStream;
    }

    /**
     * 
     * Writes the given {@link WorkflowLabel} to the {@link JsonGenerator}.
     * 
     * @param g {@link JsonGenerator} - write to JSON
     * @param label The {@link WorkflowLabel} to write
     * @throws IOException if writing to {@link java.io.File} failed for some reason
     * @throws JsonGenerationException if writing to {@link JsonGenerator} failed for some reason
     */
    public void writeLabel(JsonGenerator g, WorkflowLabel label) throws IOException, JsonGenerationException {
        g.writeStartObject();
        g.writeStringField(IDENTIFIER, label.getIdentifier());
        g.writeStringField(TEXT, label.getText());
        g.writeStringField(LOCATION,
            StringUtils.escapeAndConcat(new String[] { String.valueOf(label.getX()), String.valueOf(label.getY()) }));
        g.writeStringField(SIZE,
            StringUtils.escapeAndConcat(new String[] { String.valueOf(label.getWidth()),
                String.valueOf(label.getHeight()) }));
        g.writeStringField(ALPHA, String.valueOf(label.getAlphaDisplay()));
        g.writeStringField(COLOR_RGB_TEXT,
            StringUtils.escapeAndConcat(new String[] { String.valueOf(label.getColorText()[0]),
                String.valueOf(label.getColorText()[1]), String.valueOf(label.getColorText()[2]) }));
        g.writeStringField(COLOR_RGB_BACKGROUND,
            StringUtils.escapeAndConcat(new String[] { String.valueOf(label.getColorBackground()[0]),
                String.valueOf(label.getColorBackground()[1]), String.valueOf(label.getColorBackground()[2]) }));
        g.writeStringField(ALIGNMENT_TYPE, label.getAlignmentType().name());
        g.writeStringField(BORDER, String.valueOf(label.hasBorder()));
        g.writeStringField(TEXT_SIZE, String.valueOf(label.getTextSize()));
        g.writeEndObject();
    }

    /**
     * 
     * Writes the given {@link Connection} to the {@link JsonGenerator}.
     * 
     * @param g {@link JsonGenerator} - write to JSON
     * @param connection The {@link Connection} to write
     * @throws IOException if writing to {@link java.io.File} failed for some reason
     * @throws JsonGenerationException if writing to {@link JsonGenerator} failed for some reason
     */
    public void writeConnection(JsonGenerator g, Connection connection) throws IOException, JsonGenerationException {
        g.writeStartObject();
        g.writeStringField(SOURCE, connection.getSourceNode().getIdentifier());
        g.writeStringField(OUTPUT, connection.getOutput().getIdentifier());
        g.writeStringField(TARGET, connection.getTargetNode().getIdentifier());
        g.writeStringField(INPUT, connection.getInput().getIdentifier());
        g.writeEndObject();
    }

    /**
     * 
     * Writes the given {@link WorkflowNode} to the {@link JsonGenerator}.
     * 
     * @param g {@link JsonGenerator} - write to JSON
     * @param node The {@link WorkflowNode} to write
     * @throws IOException if writing to {@link java.io.File} failed for some reason
     * @throws JsonGenerationException if writing to {@link JsonGenerator} failed for some reason
     */
    public void writeWorkflowNode(JsonGenerator g, WorkflowNode node) throws IOException, JsonGenerationException {
        g.writeStartObject();
        g.writeStringField(IDENTIFIER, node.getIdentifier());
        g.writeStringField(NAME, node.getName());
        g.writeStringField(LOCATION,
            StringUtils.escapeAndConcat(new String[] { String.valueOf(node.getX()), String.valueOf(node.getY()) }));
        g.writeStringField(ACTIVE, Boolean.toString(node.isEnabled()));

        ComponentDescription cd = node.getComponentDescription();
        NodeIdentifier nodeId = cd.getNode();
        if (nodeId != null && !cd.getIsNodeIdTransient()) {
            g.writeStringField(PLATFORM, nodeId.getIdString());
        }

        g.writeObjectFieldStart(COMPONENT);
        // check if this component was missing the last time the workflow was loaded and
        // thus,
        // starts with a pre-defined prefix
        String identifier = cd.getIdentifier();
        if (identifier.startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
            identifier = identifier.substring(ComponentUtils.MISSING_COMPONENT_PREFIX.length());
        }
        g.writeStringField(IDENTIFIER, identifier.split(SEPARATOR)[0]);
        writeOptionalValue(g, VERSION, cd.getVersion());
        g.writeStringField(NAME, cd.getName());

        g.writeEndObject(); // 'component'

        writeConfiguation(g, node);

        writeEndpointDescriptions(g, cd.getInputDescriptionsManager().getStaticEndpointDescriptions(), STATIC_INPUTS);
        writeEndpointDescriptions(g, cd.getInputDescriptionsManager().getDynamicEndpointDescriptions(), DYNAMIC_INPUTS);
        writeEndpointDescriptions(g, cd.getOutputDescriptionsManager().getStaticEndpointDescriptions(), STATIC_OUTPUTS);
        writeEndpointDescriptions(g, cd.getOutputDescriptionsManager().getDynamicEndpointDescriptions(), DYNAMIC_OUTPUTS);
        writeEndpointGroupDescriptions(g, cd.getInputDescriptionsManager().getDynamicEndpointGroupDescriptions(), DYNAMIC_INPUT_GROUPS);

        g.writeEndObject();
    }

    private void writeEndpointDescriptions(JsonGenerator g, Set<EndpointDescription> endpoints, String nodeName)
        throws JsonGenerationException, IOException {
        if (endpoints.size() > 0) {
            g.writeArrayFieldStart(nodeName);
            List<EndpointDescription> sortedEndpoints = new ArrayList<>(endpoints);
            Collections.sort(sortedEndpoints);
            for (EndpointDescription desc : sortedEndpoints) {
                g.writeStartObject();
                g.writeStringField(IDENTIFIER, desc.getIdentifier());
                g.writeStringField(NAME, desc.getName());
                g.writeStringField(EP_IDENTIFIER, desc.getDynamicEndpointIdentifier());
                g.writeStringField(GROUP, desc.getParentGroupName());
                g.writeStringField(DATATYPE, desc.getDataType().name());
                Map<String, String> metaData = desc.getMetaData();
                if (metaData.size() > 0) {
                    g.writeObjectFieldStart(METADATA);
                    SortedMap<String, String> sortedMetaData = new TreeMap<>(metaData);
                    for (String key : sortedMetaData.keySet()) {
                        g.writeStringField(key, sortedMetaData.get(key));
                    }
                    g.writeEndObject();
                }
                g.writeEndObject();
            }
            g.writeEndArray(); // 'endpoints'
        }
    }

    private void writeEndpointGroupDescriptions(JsonGenerator g, Set<EndpointGroupDescription> endpointGroups, String nodeName)
        throws JsonGenerationException, IOException {
        if (endpointGroups.size() > 0) {
            g.writeArrayFieldStart(nodeName);
            for (EndpointGroupDescription desc : endpointGroups) {
                g.writeStartObject();
                g.writeStringField(NAME, desc.getName());
                g.writeStringField(EP_IDENTIFIER, desc.getDynamicEndpointIdentifier());
                g.writeEndObject();
            }
            g.writeEndArray();
        }
    }

    private void writeConfiguation(JsonGenerator g, WorkflowNode node) throws IOException {
        Map<String, String> configuration = node.getComponentDescription().getConfigurationDescription().getConfiguration();
        if (configuration.size() > 0) {
            g.writeObjectFieldStart(CONFIGURATION);
            writeConfigurationValues(g, configuration);
            g.writeEndObject(); // 'configuration'
        }

    }

    private void writeConfigurationValues(JsonGenerator g, Map<String, String> configuration) throws IOException {
        SortedMap<String, String> sortedConfiguration = new TreeMap<>(configuration);
        for (String key : sortedConfiguration.keySet()) {
            final String value = sortedConfiguration.get(key);
            if (value != null) {
                g.writeStringField(key, value);
            }
        }
    }

    private void writeOptionalValue(JsonGenerator g, String fieldname, String value) throws IOException {
        if (value != null) {
            g.writeStringField(fieldname, value);
        }
    }

    /**
     * Writes the given bendpoints to the {@link JsonGenerator}.
     * 
     * @param g {@link JsonGenerator} - write to JSON
     * @param connectionBendpointMapping The mapping between connection and bendpoint to write
     * @throws IOException if writing to {@link java.io.File} failed for some reason
     * @throws JsonGenerationException if writing to {@link JsonGenerator} failed for some reason
     */
    public void writeBendpoints(JsonGenerator g, Map<String, String> connectionBendpointMapping)
        throws JsonGenerationException, IOException {
        for (String key : connectionBendpointMapping.keySet()) {
            g.writeStartObject();
            g.writeStringField(SOURCE, key.split(BENDPOINT_COORDINATE_SEPARATOR)[0]);
            g.writeStringField(TARGET, key.split(BENDPOINT_COORDINATE_SEPARATOR)[1]);
            g.writeStringField(COORDINATES, connectionBendpointMapping.get(key));
            g.writeEndObject();
        }
    }

    /**
     * Reads a {@link WorkflowDescription} from a given {@link java.io.File} and adds the {@link PlaceHolderDescription} after that.
     * 
     * @param inputStream The {@link InputStream} to read from.
     * @return the read {@link WorkflowDescription}.
     * @throws IOException if reading the workflow file input stream failed for some reason.
     * @throws WorkflowFileException if parsing the the workflow file input stream didn't succeed for some reason, but still a valid but
     *         reduced {@link WorkflowDescription} exists
     */
    public synchronized WorkflowDescription readWorkflowDescriptionFromStream(InputStream inputStream)
        throws IOException, WorkflowFileException {
        ParsingFailedFlagHolder parsingFailedFlag = new ParsingFailedFlagHolder();
        WorkflowDescription wd = parseWorkflow(inputStream, parsingFailedFlag);
        if (parsingFailedFlag.parsingFailed) {
            throw new WorkflowFileException("Failed to read (parts of) the workflow", wd);
        }
        return wd;
    }

    private ObjectNode workflowFileStreamToJsonNode(InputStream inputStream) throws IOException {
        return (ObjectNode) JSON_OBJECT_MAPPER.readTree(IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()));
    }

    private void handleParseFailure(ParsingFailedFlagHolder parsingFailedFlag, String logMessage) {
        LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + logMessage); // TODO improve log messages passed
        parsingFailedFlag.parsingFailed = true;
    }

    private void handleParsingExceptions(Exception e, String message, JsonNode jsonNode, ParsingFailedFlagHolder parsingFailedFlag) {
        String logMessage = message;
        if (e.getMessage() != null) {
            logMessage = logMessage + "; cause: " + e.getMessage();
        }
        handleParseFailure(parsingFailedFlag, logMessage + "; " + jsonNode.toString());
    }

    private WorkflowDescription parseWorkflow(InputStream inputStream, ParsingFailedFlagHolder parsingFailedFlag)
        throws IOException, WorkflowFileException {

        ObjectNode wfRootJsonNode = workflowFileStreamToJsonNode(inputStream);

        if (!wfRootJsonNode.has(IDENTIFIER)) {
            throw new WorkflowFileException("Workflow identifier not found: " + wfRootJsonNode.toString());
        }
        WorkflowDescription wd = new WorkflowDescription(wfRootJsonNode.get(IDENTIFIER).asText());
        wd.setWorkflowVersion(WorkflowConstants.INITIAL_WORKFLOW_VERSION_NUMBER);
        wd.setControllerNode(platformService.getLocalNodeId());

        wd.setWorkflowVersion(readWorkflowVersionNumber(wfRootJsonNode));
        if (wfRootJsonNode.has(NAME)) {
            wd.setName(wfRootJsonNode.get(NAME).asText());
        }
        if (wfRootJsonNode.has(ADDITIONAL_INFORMATION)) {
            wd.setAdditionalInformation(wfRootJsonNode.get(ADDITIONAL_INFORMATION).asText());
        }
        String wfControllerNodeId = readWorkflowControllerNodeId(wfRootJsonNode);
        if (wfControllerNodeId != null) {
            wd.setControllerNode(NodeIdentifierFactory.fromNodeId(wfControllerNodeId));
        }
        Map<String, WorkflowNode> nodes = null;
        if (wfRootJsonNode.has(NODES) && wfRootJsonNode.get(NODES) instanceof ArrayNode) {
            nodes = parseNodesEntry((ArrayNode) wfRootJsonNode.get(NODES), wd, parsingFailedFlag);
        }
        List<Connection> connections = null;
        if (wfRootJsonNode.has(CONNECTIONS) && wfRootJsonNode.get(CONNECTIONS) instanceof ArrayNode) {
            connections = parseConnectionsEntry((ArrayNode) wfRootJsonNode.get(CONNECTIONS), nodes, wd, parsingFailedFlag);
        }
        if (wfRootJsonNode.has(BENDPOINTS)) {
            parseBendpointsEntry(wfRootJsonNode.get(BENDPOINTS), nodes, connections, parsingFailedFlag);
        }
        if (wfRootJsonNode.has(LABELS)) {
            parseLabelsEntry(wfRootJsonNode.get(LABELS), wd, parsingFailedFlag);
        }
        return wd;
    }

    /**
     * Reads workflow version of given persistent workflow description.
     * 
     * @param inputStream given persistent workflow description
     * @return workflow version number (if no version is defined, version 0 is returned)
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public int readWorkflowVersionNumber(InputStream inputStream) throws IOException {
        return readWorkflowVersionNumber(workflowFileStreamToJsonNode(inputStream));
    }

    private int readWorkflowVersionNumber(ObjectNode wfRootJsonNode) throws IOException {
        int workflowVersion = 0;
        if (wfRootJsonNode.has(WORKFLOW_VERSION)) {
            workflowVersion = wfRootJsonNode.get(WORKFLOW_VERSION).asInt();
        }
        return workflowVersion;
    }

    /**
     * Reads node identifier of workflow controller node of given persistent workflow description.
     * 
     * @param inputStream given persistent workflow description
     * @return node identifier of workflow controller node; in case node is the local one <code>null</code> is returned
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public String readWorkflowControllerNodeId(InputStream inputStream) throws IOException {
        return readWorkflowControllerNodeId(workflowFileStreamToJsonNode(inputStream));
    }

    private String readWorkflowControllerNodeId(ObjectNode wfRootJsonNode) throws IOException {
        String controllerNode = null;
        if (wfRootJsonNode.has(PLATFORM)) {
            controllerNode = wfRootJsonNode.get(PLATFORM).asText();
        }
        return controllerNode;
    }

    /**
     * Reads node identifiers of component controller nodes of given persistent workflow description.
     * 
     * @param inputStream given persistent workflow description
     * @return map with component identifier -> node identifier of controller node; in case node is the local one node identifier is
     *         <code>null</code>
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public Map<String, String> readComponentControllerNodeIds(InputStream inputStream) throws IOException {

        Map<String, String> componentControllerNodes = new HashMap<>();

        ArrayNode workflowNodesArrayNode = (ArrayNode) workflowFileStreamToJsonNode(inputStream).get(NODES);
        if (workflowNodesArrayNode != null) {
            Iterator<JsonNode> jsonNodeIterator = workflowNodesArrayNode.iterator();
            while (jsonNodeIterator.hasNext()) {
                ObjectNode workflowNodeJsonNode = (ObjectNode) jsonNodeIterator.next();
                JsonNode identifierJsonNode = workflowNodeJsonNode.get(IDENTIFIER);
                JsonNode controllerJsonNode = workflowNodeJsonNode.get(PLATFORM);
                if (identifierJsonNode != null) {
                    if (controllerJsonNode != null) {
                        componentControllerNodes.put(identifierJsonNode.asText(), controllerJsonNode.asText());
                    } else {
                        componentControllerNodes.put(identifierJsonNode.asText(), null);
                    }
                }
            }
        }
        return componentControllerNodes;
    }

    private Map<String, WorkflowNode> parseNodesEntry(ArrayNode nodesJsonNode, WorkflowDescription wd,
        ParsingFailedFlagHolder parsingFailedFlag) {
        Map<String, WorkflowNode> nodes = parseNodes(nodesJsonNode, parsingFailedFlag);
        wd.addWorkflowNodes(new ArrayList<WorkflowNode>(nodes.values()));
        return nodes;
    }

    /**
     * Parses {@link ArrayNode} to a map of {@link WorkflowNode}.
     * 
     * @param nodesJsonNode {@link ArrayNode} that defines the workflow nodes
     * @return Map of {@link WorkflowNode}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public Map<String, WorkflowNode> parseNodes(ArrayNode nodesJsonNode) throws IOException {
        ParsingFailedFlagHolder parsingFailedFlag = new ParsingFailedFlagHolder();
        Map<String, WorkflowNode> nodes = parseNodes(nodesJsonNode, parsingFailedFlag);
        if (parsingFailedFlag.parsingFailed) {
            throw new IOException("Failed to parse some of the workflow nodes");
        }
        return nodes;
    }

    private Map<String, WorkflowNode> parseNodes(ArrayNode nodesJsonNode, ParsingFailedFlagHolder parsingFailedFlag) {
        Map<String, WorkflowNode> nodes = new HashMap<String, WorkflowNode>();
        final String message = "Failed to parse a workflow node; skip it";

        Iterator<JsonNode> nodeJsonNodeIterator = nodesJsonNode.getElements();
        while (nodeJsonNodeIterator.hasNext()) {
            ObjectNode nodeJsonNode = (ObjectNode) nodeJsonNodeIterator.next();
            try {
                WorkflowNode node = parseNode(nodeJsonNode, parsingFailedFlag);
                nodes.put(node.getIdentifier(), node);
            } catch (WorkflowFileException | RuntimeException e) {
                handleParsingExceptions(e, message, nodeJsonNode, parsingFailedFlag);
                continue;
            }
        }
        return nodes;
    }

    private WorkflowNode parseNode(ObjectNode nodeJsonNode, ParsingFailedFlagHolder parsingFailedFlag) throws WorkflowFileException {
        NodeIdentifier nodeId = platformService.getLocalNodeId();
        if (nodeJsonNode.has(PLATFORM)) {
            nodeId = NodeIdentifierFactory.fromNodeId(nodeJsonNode.get(PLATFORM).asText());
        }

        if (!nodeJsonNode.has(COMPONENT)) {
            throw new WorkflowFileException("Component declaration not found");
        }
        ComponentDescription cd = parseComponentDescription((ObjectNode) nodeJsonNode.get(COMPONENT), nodeId);

        WorkflowNode node = new WorkflowNode(cd);

        if (!nodeJsonNode.has(IDENTIFIER)) {
            throw new WorkflowFileException("Identifier not found");
        }
        node.setIdentifier(nodeJsonNode.get(IDENTIFIER).asText());
        if (!nodeJsonNode.has(NAME)) {
            throw new WorkflowFileException("Name not found");
        }
        node.setName(nodeJsonNode.get(NAME).asText());
        if (nodeJsonNode.has(LOCATION)) {
            String[] location = StringUtils.splitAndUnescape(nodeJsonNode.get(LOCATION).asText());
            try {
                node.setLocation(Integer.parseInt(location[0]), Integer.parseInt(location[1]));
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse location of component; use default: " + e.getMessage());
            }
        }
        boolean active = true;
        if (nodeJsonNode.has(ACTIVE)) {
            active = nodeJsonNode.get(ACTIVE).asBoolean();
        }
        node.setEnabled(active);

        if (nodeJsonNode.has(CONFIGURATION)) {
            node.getConfigurationDescription().setConfiguration(parseConfigurationValues((ObjectNode) nodeJsonNode.get(CONFIGURATION)));
        }
        Map<String, EndpointDescription> wfNodeEndpointDescs = new HashMap<>();
        if (nodeJsonNode.has(STATIC_INPUTS)) {
            EndpointDescriptionsManager manager = node.getComponentDescription().getInputDescriptionsManager();
            for (EndpointDescription desc : parseEndpointDescriptions(wfNodeEndpointDescs, (ArrayNode) nodeJsonNode.get(STATIC_INPUTS),
                manager, true)) {
                EndpointDescription d = desc;
                if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                    manager.addStaticEndpointDescription(desc);
                } else {
                    d = manager.editStaticEndpointDescription(desc.getName(), desc.getDataType(),
                        desc.getMetaData());
                }
                d.setIdentifier(desc.getIdentifier());
            }
        }
        if (nodeJsonNode.has(STATIC_OUTPUTS)) {
            EndpointDescriptionsManager manager = node.getComponentDescription().getOutputDescriptionsManager();
            for (EndpointDescription desc : parseEndpointDescriptions(wfNodeEndpointDescs, (ArrayNode) nodeJsonNode.get(STATIC_OUTPUTS),
                manager, true)) {
                EndpointDescription d = desc;
                if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                    manager.addStaticEndpointDescription(desc);
                } else {
                    d = manager.editStaticEndpointDescription(desc.getName(), desc.getDataType(),
                        desc.getMetaData());
                }
                d.setIdentifier(desc.getIdentifier());
            }
        }
        if (nodeJsonNode.has(DYNAMIC_INPUTS)) {
            EndpointDescriptionsManager manager = node.getComponentDescription().getInputDescriptionsManager();
            for (EndpointDescription desc : parseEndpointDescriptions(wfNodeEndpointDescs, (ArrayNode) nodeJsonNode.get(DYNAMIC_INPUTS),
                manager, false)) {
                manager.addDynamicEndpointDescription(desc.getDynamicEndpointIdentifier(), desc.getName(),
                    desc.getDataType(), desc.getMetaData(), desc.getIdentifier(), desc.getParentGroupName(),
                    !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX));
            }
        }
        if (nodeJsonNode.has(DYNAMIC_OUTPUTS)) {
            EndpointDescriptionsManager manager = node.getComponentDescription().getOutputDescriptionsManager();
            for (EndpointDescription desc : parseEndpointDescriptions(wfNodeEndpointDescs, (ArrayNode) nodeJsonNode.get(DYNAMIC_OUTPUTS),
                manager, false)) {
                manager.addDynamicEndpointDescription(desc.getDynamicEndpointIdentifier(), desc.getName(),
                    desc.getDataType(), desc.getMetaData(), desc.getIdentifier(), null,
                    !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX));
            }
        }
        if (nodeJsonNode.has(DYNAMIC_INPUT_GROUPS)) {
            EndpointDescriptionsManager manager = node.getComponentDescription().getInputDescriptionsManager();
            for (EndpointGroupDescription desc : parseEndpointGroupDescriptions((ArrayNode) nodeJsonNode.get(DYNAMIC_INPUT_GROUPS),
                manager)) {
                manager.addDynamicEndpointGroupDescription(desc.getDynamicEndpointIdentifier(), desc.getName(),
                    !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX));
            }
        }
        endpointDescs.put(node.getIdentifier(), wfNodeEndpointDescs);
        return node;
    }

    private ComponentDescription parseComponentDescription(ObjectNode componentJsonNode, NodeIdentifier pi)
        throws WorkflowFileException {

        if (!componentJsonNode.has(IDENTIFIER)) {
            throw new WorkflowFileException("No component identifier found; skip workflow node");
        }
        String identifier = componentJsonNode.get(IDENTIFIER).asText();
        String version = "";
        if (componentJsonNode.has(VERSION)) {
            version = componentJsonNode.get(VERSION).asText();
        }
        String name = null;
        if (componentJsonNode.has(NAME)) {
            name = componentJsonNode.get(NAME).asText();
        }

        ComponentDescription cd = getComponentDecription(identifier + SEPARATOR + version, version, name, pi);
        if (cd == null) {
            throw new WorkflowFileException("No component registered for: " + identifier);
        }
        return cd;
    }

    private ComponentDescription getComponentDecription(String identifier, String version, String name, NodeIdentifier node) {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
        List<ComponentInstallation> matchingInstallations = new ArrayList<ComponentInstallation>();
        ComponentInstallation resultInstallation = null;

        // get all matching components
        for (ComponentInstallation installation : compKnowledge.getAllInstallations()) {
            ComponentInterface compInterface = installation.getComponentRevision().getComponentInterface();
            if (compInterface.getIdentifiers().contains(identifier) && compInterface.getVersion().equals(version)) {
                if (installation.getNodeId() != null && installation.getNodeId().equals(node.getIdString())) {
                    resultInstallation = installation;
                    break;
                } else {
                    matchingInstallations.add(installation);
                }

            }
        }
        if (resultInstallation == null) {
            if (matchingInstallations.isEmpty()) {
                resultInstallation = ComponentUtils.createPlaceholderComponentInstallation(identifier, version, name, node.getIdString());
                // update the identifier, because it starts with a pre-defined prefix now
                identifier = resultInstallation.getInstallationId();
            } else {
                // check if one is installed on desired platform
                for (ComponentInstallation inst : matchingInstallations) {
                    ComponentInterface compInterface = inst.getComponentRevision().getComponentInterface();
                    if (compInterface.getIdentifiers().contains(identifier) && compInterface.getVersion().equals(version)
                        && inst.getNodeId() != null && node != null && inst.getNodeId().equals(node.getIdString())) {
                        resultInstallation = inst;
                        break;
                    }
                }
                // check if one is installed locally
                if (resultInstallation == null) {
                    for (ComponentInstallation inst : matchingInstallations) {
                        ComponentInterface compInterface = inst.getComponentRevision().getComponentInterface();
                        if (compInterface.getIdentifiers().contains(identifier) && compInterface.getVersion().equals(version)
                            && inst.getNodeId() != null && inst.getNodeId().equals(platformService.getLocalNodeId().getIdString())) {
                            resultInstallation = inst;
                            break;
                        }
                    }
                }
                // take any component
                if (resultInstallation == null) {
                    resultInstallation = matchingInstallations.get(0);
                }
            }
        }
        return componentDescriptionFactoryService.createComponentDescription(resultInstallation);
    }

    private Set<EndpointDescription> parseEndpointDescriptions(Map<String, EndpointDescription> wfNodeEndpointDescs,
        ArrayNode endpointsJsonNode, EndpointDescriptionsManager endpointDescsManager, boolean isStaticEndpoint)
        throws WorkflowFileException {

        Set<EndpointDescription> endpoints = new HashSet<EndpointDescription>();

        Iterator<JsonNode> endpointsJsonNodeIterator = endpointsJsonNode.getElements();
        while (endpointsJsonNodeIterator.hasNext()) {
            ObjectNode endpointJsonNode = (ObjectNode) endpointsJsonNodeIterator.next();

            if (!endpointJsonNode.has(IDENTIFIER)) {
                throw new WorkflowFileException("Identifier of endpoint not found");
            }
            String identifier = endpointJsonNode.get(IDENTIFIER).asText();
            if (!endpointJsonNode.has(NAME)) {
                throw new WorkflowFileException("Name of endpoint not found");
            }
            String name = endpointJsonNode.get(NAME).asText();
            if (!endpointJsonNode.has(DATATYPE)) {
                throw new WorkflowFileException("Data type of endpoint not found");
            }
            DataType dataType = DataType.valueOf(endpointJsonNode.get(DATATYPE).asText());
            String dynEpIdentifier = null;
            if (endpointJsonNode.has(EP_IDENTIFIER)) {
                dynEpIdentifier = endpointJsonNode.get(EP_IDENTIFIER).asText();
            }
            String group = null;
            if (endpointJsonNode.has(GROUP)) {
                group = endpointJsonNode.get(GROUP).asText();
            }
            Map<String, String> metaData = new HashMap<String, String>();
            if (endpointJsonNode.has(METADATA)) {
                ObjectNode metaDataJsonNode = (ObjectNode) endpointJsonNode.get(METADATA);
                Iterator<String> metaDataJsonNodeIterator = metaDataJsonNode.getFieldNames();
                while (metaDataJsonNodeIterator.hasNext()) {
                    String key = metaDataJsonNodeIterator.next();
                    metaData.put(key, metaDataJsonNode.get(key).asText());
                }
            }

            EndpointDescription endpoint;
            if (isStaticEndpoint) {
                endpoint = new EndpointDescription(endpointDescsManager.getStaticEndpointDefinition(name), identifier);
                if (endpointDescsManager.getStaticEndpointDefinition(name) == null) {
                    endpoint.setName(name);
                }
            } else {
                endpoint = new EndpointDescription(endpointDescsManager.getDynamicEndpointDefinition(dynEpIdentifier), identifier);
                endpoint.setName(name);
                if (dynEpIdentifier == null || dynEpIdentifier.equals("null")) {
                    endpoint.setDynamicEndpointIdentifier(null);
                } else {
                    endpoint.setDynamicEndpointIdentifier(dynEpIdentifier);
                }
            }
            endpoint.setParentGroupName(group);
            endpoint.setDataType(dataType);
            endpoint.setMetaData(metaData);
            endpoints.add(endpoint);
            wfNodeEndpointDescs.put(identifier, endpoint);
        }
        return endpoints;
    }

    private Set<EndpointGroupDescription> parseEndpointGroupDescriptions(ArrayNode endpointGroupsJsonNode,
        EndpointDescriptionsManager endpointDescsManager) throws WorkflowFileException {

        Set<EndpointGroupDescription> endpointGroups = new HashSet<>();

        Iterator<JsonNode> endpointGroupsJsonNodeIterator = endpointGroupsJsonNode.getElements();
        while (endpointGroupsJsonNodeIterator.hasNext()) {
            ObjectNode endpointGroupJsonNode = (ObjectNode) endpointGroupsJsonNodeIterator.next();

            if (!endpointGroupJsonNode.has(NAME)) {
                throw new WorkflowFileException("Name of endpoint group not found");
            }
            String name = endpointGroupJsonNode.get(NAME).asText();
            if (!endpointGroupJsonNode.has(EP_IDENTIFIER)) {
                throw new WorkflowFileException("Endpoint identifier of endpoint group not found");
            }
            String dynEpIdentifier = endpointGroupJsonNode.get(EP_IDENTIFIER).asText();

            EndpointGroupDescription desc = new EndpointDescription(endpointDescsManager
                .getDynamicEndpointDefinition(dynEpIdentifier), endpointDescsManager.getManagedEndpointType());
            desc.setName(name);
            if (dynEpIdentifier.equals("null")) {
                desc.setDynamicEndpointIdentifier(null);
            } else {
                desc.setDynamicEndpointIdentifier(dynEpIdentifier);
            }
            endpointGroups.add(desc);
        }
        return endpointGroups;
    }

    private List<Connection> parseConnectionsEntry(ArrayNode connectionsJsonNode, Map<String, WorkflowNode> nodes, WorkflowDescription wd,
        ParsingFailedFlagHolder parsingFailedFlag) {
        if (nodes != null) {
            List<Connection> connections = parseConnections(connectionsJsonNode, nodes, parsingFailedFlag);
            wd.addConnections(connections);
            return connections;
        } else {
            handleParseFailure(parsingFailedFlag, "Failed to read connections (no workflow nodes defined); skip them");
            return null;
        }
    }

    /**
     * Parse {@link ArrayNode} to a set of {@link Connection}s.
     * 
     * @param connectionsJsonNode {@link ArrayNode} that defines the connections
     * @param nodes {@link WorkflowNode}s considered as source and target of {@link Connection}s
     * @return Set of {@link Connection}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public synchronized List<Connection> parseConnections(ArrayNode connectionsJsonNode, Map<String, WorkflowNode> nodes)
        throws IOException {
        ParsingFailedFlagHolder parsingFailedFlag = new ParsingFailedFlagHolder();
        List<Connection> connections = parseConnections(connectionsJsonNode, nodes, parsingFailedFlag);
        if (parsingFailedFlag.parsingFailed) {
            throw new IOException("Failed to parse some of the connections");
        }
        return connections;
    }

    private List<Connection> parseConnections(ArrayNode connectionsJsonNode, Map<String, WorkflowNode> nodes,
        ParsingFailedFlagHolder parsingFailedFlag) {
        final String message = "Failed to parse connection; skip it";
        List<Connection> connections = new ArrayList<Connection>();

        Iterator<JsonNode> connectionJsonNodeIterator = connectionsJsonNode.getElements();
        while (connectionJsonNodeIterator.hasNext()) {
            ObjectNode connectionJsonNode = (ObjectNode) connectionJsonNodeIterator.next();
            try {
                Connection connection = parseConnection(connectionJsonNode, nodes, parsingFailedFlag);
                connections.add(connection);
            } catch (WorkflowFileException | RuntimeException e) {
                handleParsingExceptions(e, message, connectionJsonNode, parsingFailedFlag);
                continue;
            }
        }
        return connections;
    }

    private Connection parseConnection(ObjectNode connectionJsonNode, Map<String, WorkflowNode> nodes,
        ParsingFailedFlagHolder parsingFailedFlag) throws WorkflowFileException {

        if (!connectionJsonNode.has(SOURCE)) {
            throw new WorkflowFileException("Source workflow node definition of connection not found");
        }
        WorkflowNode outputNode = nodes.get(connectionJsonNode.get(SOURCE).asText());
        if (outputNode == null) {
            throw new WorkflowFileException("Source workflow node of connection not found: " + connectionJsonNode.get(SOURCE).asText());
        }
        if (!connectionJsonNode.has(OUTPUT)) {
            throw new WorkflowFileException("Output definition of connection not found");
        }
        Map<String, EndpointDescription> outputNodesEpDescs = endpointDescs.get(outputNode.getIdentifier());
        if (outputNodesEpDescs == null) {
            throw new WorkflowFileException("Output's workflow node of connection not exists: " + outputNode.getIdentifier());
        }
        EndpointDescription outputEpDesc = outputNodesEpDescs.get(connectionJsonNode.get(OUTPUT).asText());
        if (outputEpDesc == null) {
            throw new WorkflowFileException("Output of connection not exists: " + connectionJsonNode.get(OUTPUT).asText());
        }
        if (!connectionJsonNode.has(TARGET)) {
            throw new WorkflowFileException("Target workflow node definition of connection not found");
        }
        WorkflowNode inputNode = nodes.get(connectionJsonNode.get(TARGET).asText());
        if (inputNode == null) {
            throw new WorkflowFileException("Target workflow node of connection not found: " + connectionJsonNode.get(TARGET).asText());
        }
        if (!connectionJsonNode.has(INPUT)) {
            throw new WorkflowFileException("Input definition of connection not found");
        }
        Map<String, EndpointDescription> inputNodesEpDescs = endpointDescs.get(inputNode.getIdentifier());
        if (inputNodesEpDescs == null) {
            throw new WorkflowFileException("Input's workflow node of connection not exists: " + outputNode.getIdentifier());
        }
        EndpointDescription inputEpDesc = inputNodesEpDescs.get(connectionJsonNode.get(INPUT).asText());
        if (inputEpDesc == null) {
            throw new WorkflowFileException("Input of connection not exists: " + connectionJsonNode.get(INPUT).asText());
        }

        return new Connection(outputNode, outputEpDesc, inputNode, inputEpDesc);
    }

    // This method supports two versions of parsing bendpoints: as json string and as json array object.
    // this is due to problems with the workflow parser in version 6.1.0 (adding new json object
    // does not work properly)
    private void parseBendpointsEntry(JsonNode bendpointsJsonNode, Map<String, WorkflowNode> nodes, List<Connection> connections,
        ParsingFailedFlagHolder parsingFailedFlag) throws IOException {
        if (nodes != null || connections == null) {
            if (bendpointsJsonNode instanceof ArrayNode) {
                parseBendpoints((ArrayNode) bendpointsJsonNode, nodes, connections, parsingFailedFlag);
            } else {
                parseBendpoints((ArrayNode) JSON_OBJECT_MAPPER.readTree(bendpointsJsonNode.asText()),
                    nodes, connections, parsingFailedFlag);
            }
        } else {
            handleParseFailure(parsingFailedFlag, "Failed to read bendpoints (no workflow nodes or connections defined); skip them");
        }
    }

    /**
     * Parses {@link ArrayNode} and add them to the given {@link Set} of {@link Connections}s.
     * 
     * @param bendpointsJsonNode {@link ArrayNode} that defines the connections
     * @param nodes {@link WorkflowNode}s considered as source and target of {@link Connection}s
     * @param connections {@link Connection}s the bendpoints belong to
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public synchronized void parseBendpoints(ArrayNode bendpointsJsonNode, Map<String, WorkflowNode> nodes, List<Connection> connections)
        throws IOException {
        ParsingFailedFlagHolder parsingFailedFlag = new ParsingFailedFlagHolder();
        parseBendpoints(bendpointsJsonNode, nodes, connections, parsingFailedFlag);
        if (parsingFailedFlag.parsingFailed) {
            throw new IOException("Failed to parse bendpoint(s)");
        }
    }

    private void parseBendpoints(ArrayNode bendpointsJsonNode, Map<String, WorkflowNode> nodes, List<Connection> connections,
        ParsingFailedFlagHolder parsingFailedFlag) {
        final String message = "Failed to parse bendpoint; skip it";

        Iterator<JsonNode> bendpointJsonNodeIterator = bendpointsJsonNode.getElements();
        while (bendpointJsonNodeIterator.hasNext()) {
            ObjectNode bendpointJsonNode = (ObjectNode) bendpointJsonNodeIterator.next();
            try {
                parseBendpoint(bendpointJsonNode, nodes, connections);
            } catch (WorkflowFileException e) {
                handleParsingExceptions(e, message, bendpointJsonNode, parsingFailedFlag);
                continue;
            }
        }
    }

    private void parseBendpoint(ObjectNode bendpointJsonNode, Map<String, WorkflowNode> nodes, List<Connection> connections)
        throws WorkflowFileException {
        if (!bendpointJsonNode.has(SOURCE)) {
            throw new WorkflowFileException("Source of bendpoint not found");
        }
        WorkflowNode outputNode = nodes.get(bendpointJsonNode.get(SOURCE).asText());
        if (outputNode == null) {
            throw new WorkflowFileException(
                "Source workflow node of connection bendpoint not found: " + bendpointJsonNode.get(SOURCE).asText());
        }
        if (!bendpointJsonNode.has(TARGET)) {
            throw new WorkflowFileException("Target of bendpoint not found");
        }
        WorkflowNode inputNode = nodes.get(bendpointJsonNode.get(TARGET).asText());
        if (inputNode == null) {
            throw new WorkflowFileException(
                "Target workflow node of connection bendpoint not found: " + bendpointJsonNode.get(TARGET).asText());
        }
        if (!bendpointJsonNode.has(COORDINATES)) {
            throw new WorkflowFileException("Coordinates of bendpoint not found");
        }
        String bendpointListString = bendpointJsonNode.get(COORDINATES).asText();

        List<Location> bendpoints = new ArrayList<>();

        for (String bendpointString : bendpointListString.split(BENDPOINT_SEPARATOR)) {
            String[] bendpointParts = bendpointString.split(BENDPOINT_COORDINATE_SEPARATOR);
            try {
                Location bendpoint = new Location(Integer.parseInt(bendpointParts[0]), Integer.parseInt(bendpointParts[1]));
                bendpoints.add(bendpoint);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse bendpoint; skip it: " + e.getMessage());
            }
        }

        for (Connection connection : connections) {
            if ((connection.getTargetNode().getIdentifier().equals(inputNode.getIdentifier())
                && connection.getSourceNode().getIdentifier().equals(outputNode.getIdentifier()))) {
                connection.setBendpoints(bendpoints);
            } else if (connection.getTargetNode().getIdentifier().equals(outputNode.getIdentifier())
                && connection.getSourceNode().getIdentifier().equals(inputNode.getIdentifier())) {
                List<Location> invertedBendpointsToAdd = new ArrayList<>();
                for (Location l : bendpoints) {
                    invertedBendpointsToAdd.add(0, l);
                }
                connection.setBendpoints(invertedBendpointsToAdd);
            }
        }
    }

    /**
     * Creates a map containing the string "source:target" as key and a string representation of the contained bendpoints as value.
     * 
     * @param connections The connections to be considered
     * @return A map with source+target as key and the list of bendpoints as string as value
     */
    public Map<String, String> calculateUniqueBendpointList(List<Connection> connections) {
        Map<String, String> connectionBendpointMapping = new HashMap<>();
        Collections.sort(connections);
        for (Connection c : connections) {
            String sourceId = c.getSourceNode().getIdentifier();
            String targetId = c.getTargetNode().getIdentifier();
            List<Location> bendpoints = c.getBendpoints();
            if (!bendpoints.isEmpty()) {
                String bendpointString = parseListOfBendpointsToString(bendpoints);
                // note that for sake of simplicity the same separator as for bendpoints is used here
                String connectionString = sourceId + BENDPOINT_COORDINATE_SEPARATOR + targetId;
                String inverseConnectionString = targetId + BENDPOINT_COORDINATE_SEPARATOR + sourceId;
                // if not already existent - add it
                if (!connectionBendpointMapping.keySet().contains(connectionString)
                    && !connectionBendpointMapping.keySet().contains(inverseConnectionString)) {
                    connectionBendpointMapping.put(connectionString, bendpointString);
                }
            }
        }
        return connectionBendpointMapping;
    }

    private String parseListOfBendpointsToString(List<Location> locations) {
        String assembledBendpointString = "";
        for (Location location : locations) {
            assembledBendpointString += location.x;
            assembledBendpointString += BENDPOINT_COORDINATE_SEPARATOR;
            assembledBendpointString += location.y;
            assembledBendpointString += BENDPOINT_SEPARATOR;
        }
        // remove last comma
        assembledBendpointString = assembledBendpointString.substring(0, assembledBendpointString.length() - 1);

        return assembledBendpointString;
    }

    // This method supports two versions of parsing labels: as json string and as json array object.
    // this is due to problems with the workflow parser in version 6.1.0 (adding new json object
    // does not work properly)
    private void parseLabelsEntry(JsonNode labelsJsonNode, WorkflowDescription wd, ParsingFailedFlagHolder parsingFailedFlag)
        throws IOException {
        if (labelsJsonNode instanceof ArrayNode) {
            for (WorkflowLabel label : parseLabels((ArrayNode) labelsJsonNode, parsingFailedFlag)) {
                wd.addWorkflowLabel(label);
            }
        } else {
            for (WorkflowLabel label : parseLabels((ArrayNode) JSON_OBJECT_MAPPER.readTree(labelsJsonNode.asText()), parsingFailedFlag)) {
                wd.addWorkflowLabel(label);
            }
        }
    }

    private Map<String, String> parseConfigurationValues(ObjectNode configurationJsonNode) {

        Map<String, String> configuration = new HashMap<String, String>();

        Iterator<String> configurationJsonNodeIterator = configurationJsonNode.getFieldNames();
        while (configurationJsonNodeIterator.hasNext()) {
            String key = configurationJsonNodeIterator.next();
            configuration.put(key, configurationJsonNode.get(key).asText());
        }
        return configuration;
    }

    /**
     * Parses {@link ArrayNode} to a set of {@link WorkflowLabel}s.
     * 
     * @param labelsJsonNode {@link ArrayNode} that defines the workflow labels
     * @return set of {@link WorkflowLabel}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     */
    public Set<WorkflowLabel> parseLabels(ArrayNode labelsJsonNode) throws IOException {
        ParsingFailedFlagHolder parsingFailedFlag = new ParsingFailedFlagHolder();
        Set<WorkflowLabel> labels = parseLabels(labelsJsonNode, parsingFailedFlag);
        if (parsingFailedFlag.parsingFailed) {
            throw new IOException("Failed to parse some of the bendpoints");
        }
        return labels;
    }

    private Set<WorkflowLabel> parseLabels(ArrayNode labelsJsonNode, ParsingFailedFlagHolder parsingFailedFlag) {
        final String message = "Failed to parse label; skip it";

        Set<WorkflowLabel> labels = new LinkedHashSet<WorkflowLabel>();

        Iterator<JsonNode> labelsJsonNodeIterator = labelsJsonNode.getElements();
        while (labelsJsonNodeIterator.hasNext()) {
            ObjectNode labelJsonNode = (ObjectNode) labelsJsonNodeIterator.next();
            try {
                WorkflowLabel label = parseLabel(labelJsonNode);
                labels.add(label);
            } catch (WorkflowFileException e) {
                handleParsingExceptions(e, message, labelJsonNode, parsingFailedFlag);
                continue;
            }
        }
        return labels;
    }

    private WorkflowLabel parseLabel(ObjectNode labelJsonNode) throws WorkflowFileException {

        if (!labelJsonNode.has(TEXT)) {
            throw new WorkflowFileException("Text for workflow label not found");
        }

        WorkflowLabel label = new WorkflowLabel(labelJsonNode.get(TEXT).asText());
        if (labelJsonNode.has(IDENTIFIER)) {
            label.setIdentifier(labelJsonNode.get(IDENTIFIER).asText());
        }
        if (labelJsonNode.has(LOCATION)) {
            String[] location = StringUtils.splitAndUnescape(labelJsonNode.get(LOCATION).asText());
            try {
                label.setLocation(Integer.parseInt(location[0]), Integer.parseInt(location[1]));
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse location of label; use default: " + e.getMessage());
            }
        }
        if (labelJsonNode.has(SIZE)) {
            String[] size = StringUtils.splitAndUnescape(labelJsonNode.get(SIZE).asText());
            try {
                label.setSize(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse size of label; use default: " + e.getMessage());
            }
        }
        if (labelJsonNode.has(ALPHA)) {
            label.setAlpha(labelJsonNode.get(ALPHA).asInt());
        }
        if (labelJsonNode.has(COLOR_RGB_TEXT)) {
            String[] colorString = StringUtils.splitAndUnescape(labelJsonNode.get(COLOR_RGB_TEXT).asText());
            try {
                int[] color =
                    new int[] { Integer.parseInt(colorString[0]), Integer.parseInt(colorString[1]), Integer.parseInt(colorString[2]) };
                label.setColorText(color);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse text color of label; use default: " + e.getMessage());
            }
        }
        if (labelJsonNode.has(COLOR_RGB_BACKGROUND)) {
            String[] colorString = StringUtils.splitAndUnescape(labelJsonNode.get(COLOR_RGB_BACKGROUND).asText());
            try {
                int[] color =
                    new int[] { Integer.parseInt(colorString[0]), Integer.parseInt(colorString[1]), Integer.parseInt(colorString[2]) };
                label.setColorBackground(color);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                LOG.error(ERROR_WHEN_PARSING_WORKFLOW_FILE + "Failed to parse background color of label; use default: " + e.getMessage());
            }
        }
        if (labelJsonNode.has(ALIGNMENT_TYPE)) {
            AlignmentType alignmentType = AlignmentType.valueOf(labelJsonNode.get(ALIGNMENT_TYPE).asText());
            label.setAlignmentType(alignmentType);
        }
        if (labelJsonNode.has(BORDER)) {
            label.setHasBorder(labelJsonNode.get(BORDER).asBoolean());
        }
        if (labelJsonNode.has(TEXT_SIZE)) {
            label.setTextSize(labelJsonNode.get(TEXT_SIZE).asInt());
        }
        return label;
    }

    /**
     * Container class for flag used to indicate that parsing the workflow file didn't succeeded and some parts are skipped.
     * 
     * @author Doreen Seider
     */
    public class ParsingFailedFlagHolder {

        /**
         * <code>true</code> if parsing succeeded, <code>false</code> if some parts of the workflow file could not be parsed and were
         * skipped.
         */
        public boolean parsingFailed = false;
    }
}
