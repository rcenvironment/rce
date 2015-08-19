/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.eclipse.draw2d.geometry.Dimension;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel.AlignmentType;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Writes and reads {@link WorkflowDescription}s to and from {@link java.io.File}s.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Marc Stammerjohann
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
    public static final String EP_IDENTIFIER = "epIdentifier";

    /** Field name. */
    public static final String WORKFLOW_VERSION = "workflowVersion";

    /** Field name. */
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    
    /** Field name. */
    public static final String BENDPOINT_SEPARATOR = ",";

    /** Field name. */
    public static final String BENDPOINT_COORDINATE_SEPARATOR = ":";

    protected static DistributedComponentKnowledgeService componentKnowledgeService = ServiceUtils
        .createFailingServiceProxy(DistributedComponentKnowledgeService.class);

    protected static PlatformService platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);

    private static final String SEPARATOR = "/";

    private final Map<String, EndpointDescription> endpointDescs = new HashMap<String, EndpointDescription>();

    public WorkflowDescriptionPersistenceHandler() {}

    protected void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        componentKnowledgeService = newService;
    }

    protected void unbindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService oldService) {
        componentKnowledgeService = ServiceUtils.createFailingServiceProxy(DistributedComponentKnowledgeService.class);
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
        if (wd.getControllerNode() != null) {
            writeOptionalValue(g, PLATFORM, wd.getControllerNode().getIdString());
        }
        writeOptionalValue(g, ADDITIONAL_INFORMATION, wd.getAdditionalInformation());

        if (wd.getWorkflowNodes().size() > 0) {
            g.writeArrayFieldStart(NODES);
            for (WorkflowNode node : wd.getWorkflowNodes()) {
                writeWorkflowNode(g, node);
            }
            g.writeEndArray(); // 'nodes'
        }

        if (wd.getConnections().size() > 0) {
            g.writeArrayFieldStart(CONNECTIONS);
            for (Connection connection : wd.getConnections()) {
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

            for (WorkflowLabel label : wd.getWorkflowLabels()) {
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
            StringUtils.escapeAndConcat(new String[] { String.valueOf(label.getSize().width),
                String.valueOf(label.getSize().height) }));
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
        if (nodeId != null && !cd.getIsNodeTransient()) {
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

        g.writeEndObject();
    }

    private void writeEndpointDescriptions(JsonGenerator g, Set<EndpointDescription> endpoints, String nodeName)
        throws JsonGenerationException, IOException {
        if (endpoints.size() > 0) {
            g.writeArrayFieldStart(nodeName);
            for (EndpointDescription desc : endpoints) {
                g.writeStartObject();
                g.writeStringField(IDENTIFIER, desc.getIdentifier());
                g.writeStringField(NAME, desc.getName());
                g.writeStringField(EP_IDENTIFIER, desc.getDynamicEndpointIdentifier());
                g.writeStringField(DATATYPE, desc.getDataType().name());
                Map<String, String> metaData = desc.getMetaData();
                if (metaData.size() > 0) {
                    g.writeObjectFieldStart(METADATA);
                    for (String key : metaData.keySet()) {
                        g.writeStringField(key, metaData.get(key));
                    }
                    g.writeEndObject();
                }
                g.writeEndObject();
            }
            g.writeEndArray(); // 'endpoints'
        }
    }

    /**
     * Writes the given {@link WorkflowDescription} into the given {@link OutputStream}.
     * 
     * @param wd The {@link WorkflowDescription} to write.
     * @param output The stream to write to.
     * @throws IOException if writing to {@link java.io.File} failed for some reason.
     */
    public void writeWorkflowDescriptionToStream(WorkflowDescription wd, OutputStream output) throws IOException {
        OutputStream outputStream = writeWorkflowDescriptionToStream(wd);
        output.write(outputStream.toString().getBytes());
    }

    /**
     * Reads workflow version of given persistent workflow description.
     * 
     * @param inputStream given persistent workflow description
     * @return workflow version number (if no version is defined, version 0 is returned)
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     * @throws ParseException if parsing the {@link java.io.File} failed for some reason
     */
    public int readWorkflowVersionNumer(InputStream inputStream) throws ParseException, IOException {

        int workflowVersion = 0;

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(inputStream);

        jp.nextToken(); // will return JsonToken.START_OBJECT
        jp.nextToken();
        jp.nextToken(); // will return identifier

        // read and parse remaining optional fields
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY

            if (WORKFLOW_VERSION.equals(jp.getCurrentName())) {
                workflowVersion = Integer.valueOf(jp.getText());
                break;
            }
        }
        jp.close();
        return workflowVersion;
    }

    /**
     * Reads a {@link WorkflowDescription} from a given {@link java.io.File} and adds the
     * {@link PlaceHolderDescription} after that.
     * 
     * @param inputStream The {@link InputStream} to read from.
     * @param user user to use in order to load {@link ComponentDescription}s from the
     *        {@link de.rcenvironment.core.component.registration.api.ComponentRegistry}.
     * @return the read {@link WorkflowDescription}.
     * @throws IOException if reading from {@link java.io.File} failed for some reason.
     * @throws ParseException if parsing the {@link java.io.File} failed for some reason.
     */
    public WorkflowDescription readWorkflowDescriptionFromStream(InputStream inputStream, User user)
        throws IOException, ParseException {
        WorkflowDescription wd = parseWorkflow(inputStream, user);
        return wd;
    }

    /**
     * Reads a {@link WorkflowDescription} from a given {@link java.io.File}.
     * 
     * @param inputStream The {@link InputStream} to read from.
     * @param user user to use in order to load {@link ComponentDescription}s from the
     *        {@link de.rcenvironment.core.component.registration.api.ComponentRegistry}.
     * @throws IOException
     * @throws JsonParseException
     * @throws ParseException
     * 
     */
    private WorkflowDescription parseWorkflow(InputStream inputStream, User user) throws JsonParseException, IOException, ParseException {
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(inputStream);
        WorkflowDescription wd;

        Map<String, WorkflowNode> nodes = new HashMap<String, WorkflowNode>();

        jp.nextToken(); // will return JsonToken.START_OBJECT
        jp.nextToken();
        
        // read required field 'identifier'
        if (IDENTIFIER.equals(jp.getCurrentName())) {
            jp.nextToken(); // move to value
            wd = new WorkflowDescription(jp.getText());
            wd.setWorkflowVersion(WorkflowConstants.INITIAL_WORKFLOW_VERSION_NUMBER);
            wd.setControllerNode(platformService.getLocalNodeId());
        } else {
            jp.close();
            throw new ParseException("No identifier found.", jp.getCurrentLocation().getLineNr());
        }

        // read and parse remaining optional fields
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
            if (WORKFLOW_VERSION.equals(jp.getCurrentName())) {
                wd.setWorkflowVersion(Integer.valueOf(jp.getText()));
            } else if (NAME.equals(jp.getCurrentName())) {
                wd.setName(jp.getText());
            } else if (ADDITIONAL_INFORMATION.equals(fieldname)) {
                wd.setAdditionalInformation(jp.getText());
            } else if (PLATFORM.equals(fieldname)) {
                wd.setControllerNode(NodeIdentifierFactory.fromNodeId(jp.getText()));
            } else if (NODES.equals(fieldname)) { // contains an array
                nodes = parseNodes(jp, user);
                wd.addWorkflowNodes(new ArrayList<WorkflowNode>(nodes.values()));
            } else if (CONNECTIONS.equals(fieldname)) { // contains an array
                Set<Connection> connections = parseConnections(jp, nodes);
                wd.addConnections(new ArrayList<Connection>(connections));
            } else if (BENDPOINTS.equals(fieldname)) { // contains an array
                parseBendpointsEntry(f, jp, nodes, wd);
            } else if (LABELS.equals(fieldname)) { // contains an text or an array
                parseLabelsEntry(f, jp, wd);
            } else {
                jp.nextToken();
            }
        }
        jp.close();
        return wd;
    }
    
    // This method supports two versions of parsing bendpoints: as json string and as json array object.
    // this is due to problems with the workflow parser in version 6.1.0 (adding new json object
    // does not work properly)
    private void parseBendpointsEntry(JsonFactory f, JsonParser jp, Map<String, WorkflowNode> nodes, WorkflowDescription wd)
        throws IOException, ParseException, JsonParseException {
        if (jp.isExpectedStartArrayToken()) {
            parseBendpoints(jp, nodes, wd);
        } else {
            JsonParser bendpointsParser = f.createJsonParser(new ByteArrayInputStream(jp.getText().getBytes()));
            bendpointsParser.nextToken();
            parseBendpoints(bendpointsParser, nodes, wd);
            bendpointsParser.close();
        }
    }

    private void parseBendpoints(JsonParser jp, Map<String, WorkflowNode> nodes, WorkflowDescription wd)
        throws JsonParseException, NumberFormatException, IOException, ParseException {
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                
                WorkflowNode output = null;
                WorkflowNode input = null;
                List<Location> bendpoints = new ArrayList<>();
                String bendpointListString = null;
                
                String bendpointField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (SOURCE.equals(bendpointField)) {
                    output = nodes.get(jp.getText());
                } else {
                    throw new ParseException("No source definition.", jp.getCurrentLocation().getLineNr());
                }
                jp.nextToken();
                bendpointField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (TARGET.equals(bendpointField)) {
                    input = nodes.get(jp.getText());
                } else {
                    throw new ParseException("No target definition.", jp.getCurrentLocation().getLineNr());
                }
                jp.nextToken();
                bendpointField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (COORDINATES.equals(bendpointField)) {
                    bendpointListString = jp.getText();
                } else {
                    throw new ParseException("No input definition.", jp.getCurrentLocation().getLineNr());
                }
                
                if (bendpointListString != null) {
                    for (String bendpointString : bendpointListString.split(BENDPOINT_SEPARATOR)){
                        Location bendpoint = new Location(Integer.parseInt(bendpointString.split(BENDPOINT_COORDINATE_SEPARATOR)[0])
                            , Integer.parseInt(bendpointString.split(BENDPOINT_COORDINATE_SEPARATOR)[1]));
                        bendpoints.add(bendpoint);
                    }
                    
                    for (Connection connection : wd.getConnections()){
                        if ((connection.getTargetNode().getIdentifier().equals(input.getIdentifier()) 
                            && connection.getSourceNode().getIdentifier().equals(output.getIdentifier()))){
                            connection.setBendpoints(bendpoints);
                        } else if (connection.getTargetNode().getIdentifier().equals(output.getIdentifier()) 
                            && connection.getSourceNode().getIdentifier().equals(input.getIdentifier())){
                            List<Location> invertedBendpointsToAdd = new ArrayList<>();
                            for (Location l : bendpoints){
                                invertedBendpointsToAdd.add(0, l);
                            }
                            connection.setBendpoints(invertedBendpointsToAdd);
                        }
                    }
                }
            }
        }
    }

    // This method supports two versions of parsing labels: as json string and as json array object.
    // this is due to problems with the workflow parser in version 6.1.0 (adding new json object
    // does not work properly)
    private void parseLabelsEntry(JsonFactory f, JsonParser jp, WorkflowDescription wd) throws IOException, ParseException,
        JsonParseException {
        if (jp.isExpectedStartArrayToken()) {
            Set<WorkflowLabel> labels = parseLabels(jp);
            for (WorkflowLabel label : labels) {
                wd.addWorkflowLabel(label);
            }
        } else {
            JsonParser labelParser = f.createJsonParser(new ByteArrayInputStream(jp.getText().getBytes()));
            labelParser.nextToken();
            Set<WorkflowLabel> labels = parseLabels(labelParser);
            for (WorkflowLabel label : labels) {
                wd.addWorkflowLabel(label);
            }
            labelParser.close();
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
        for (String key : configuration.keySet()) {
            final String value = configuration.get(key);
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

    private Map<String, String> parseConfigurationValues(JsonParser jp) throws IOException {

        Map<String, String> configuration = new HashMap<String, String>();
        // contains an object
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            String value = jp.getText();
            configuration.put(key, value);
        }
        return configuration;
    }

    /**
     * 
     * Parse {@link JsonParser} to a map of {@link WorkflowNode}.
     * 
     * @param jp {@link JsonParser} - parse JSON
     * @param user {@link User} to use in order to load {@link ComponentDescription}s
     * @return Map of {@link WorkflowNode}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     * @throws ParseException if parsing the {@link java.io.File} failed for some reason
     */
    public Map<String, WorkflowNode> parseNodes(JsonParser jp, User user) throws IOException, ParseException {
        Map<String, WorkflowNode> nodes = new HashMap<String, WorkflowNode>();

        while (jp.nextToken() != JsonToken.END_ARRAY) { // contains an object
            jp.nextToken();
            while (jp.getCurrentToken() != JsonToken.END_OBJECT) {
                // read required fields
                String nodeField = jp.getCurrentName();
                jp.nextToken(); // move to value

                String identifier;
                if (IDENTIFIER.equals(nodeField)) {
                    identifier = jp.getText();
                } else {
                    throw new ParseException("No node identifier found.", jp.getCurrentLocation().getLineNr());
                }

                jp.nextToken();
                nodeField = jp.getCurrentName();
                jp.nextToken(); // move to value

                String name;
                if (NAME.equals(nodeField)) {
                    name = jp.getText();
                } else {
                    throw new ParseException("No node name found.", jp.getCurrentLocation().getLineNr());
                }

                jp.nextToken();
                nodeField = jp.getCurrentName();
                jp.nextToken(); // move to value

                int x;
                int y;
                if (LOCATION.equals(nodeField)) {
                    String[] location = StringUtils.splitAndUnescape(jp.getText());
                    try {
                        x = Integer.parseInt(location[0]);
                        y = Integer.parseInt(location[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new ParseException("Invalid location definition.", jp.getCurrentLocation().getLineNr());
                    }
                } else {
                    throw new ParseException("No node location found.", jp.getCurrentLocation().getLineNr());
                }

                jp.nextToken();
                nodeField = jp.getCurrentName();
                jp.nextToken();

                boolean active = true;

                if (ACTIVE.equals(nodeField)) {
                    active = Boolean.valueOf(jp.getText());
                    jp.nextToken();
                    nodeField = jp.getCurrentName();
                    jp.nextToken();
                }

                NodeIdentifier pi = platformService.getLocalNodeId();
                if (PLATFORM.equals(nodeField)) {
                    pi = NodeIdentifierFactory.fromNodeId(jp.getText());
                    jp.nextToken();
                    nodeField = jp.getCurrentName();
                    jp.nextToken(); // move to START_OBJECT
                }

                ComponentDescription cd;
                if (COMPONENT.equals(nodeField)) { // contains an object
                    cd = parseComponentDescription(jp, pi, user);
                } else {
                    throw new ParseException("No component definition found.", jp.getCurrentLocation().getLineNr());
                }

                WorkflowNode node = new WorkflowNode(cd);
                node.setIdentifier(identifier);
                node.setName(name);
                node.setLocation(x, y);
                node.setEnabled(active);

                // read remaining optional fields
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    nodeField = jp.getCurrentName();
                    jp.nextToken(); // move to value or start object
                    if (CONFIGURATION.equals(nodeField)) { // contains an object
                        node.getConfigurationDescription()
                            .setConfiguration(parseConfigurationValues(jp));
                    } else if (STATIC_INPUTS.equals(nodeField)) {
                        EndpointDescriptionsManager manager = node.getComponentDescription().getInputDescriptionsManager();
                        for (EndpointDescription desc : parseEndpointDescriptions(jp, manager, true)) {
                            EndpointDescription d = desc;
                            if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                                manager.addStaticEndpointDescription(desc);
                            } else {
                                d = manager.editStaticEndpointDescription(desc.getName(), desc.getDataType(),
                                    desc.getMetaData());
                            }
                            d.setIdentifier(desc.getIdentifier());
                        }
                    } else if (STATIC_OUTPUTS.equals(nodeField)) {
                        EndpointDescriptionsManager manager = node.getComponentDescription().getOutputDescriptionsManager();
                        for (EndpointDescription desc : parseEndpointDescriptions(jp, manager, true)) {
                            EndpointDescription d = desc;
                            if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                                manager.addStaticEndpointDescription(desc);
                            } else {
                                d = manager.editStaticEndpointDescription(desc.getName(), desc.getDataType(),
                                    desc.getMetaData());
                            }
                            d.setIdentifier(desc.getIdentifier());
                        }
                    } else if (DYNAMIC_INPUTS.equals(nodeField)) {
                        EndpointDescriptionsManager manager = node.getComponentDescription().getInputDescriptionsManager();
                        for (EndpointDescription desc : parseEndpointDescriptions(jp, manager, false)) {
                            manager.addDynamicEndpointDescription(desc.getDynamicEndpointIdentifier(), desc.getName(), desc.getDataType(),
                                desc.getMetaData(), desc.getIdentifier(),
                                !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX));
                        }
                    } else if (DYNAMIC_OUTPUTS.equals(nodeField)) {
                        EndpointDescriptionsManager manager = node.getComponentDescription().getOutputDescriptionsManager();
                        for (EndpointDescription desc : parseEndpointDescriptions(jp, manager, false)) {
                            manager.addDynamicEndpointDescription(desc.getDynamicEndpointIdentifier(), desc.getName(),
                                desc.getDataType(), desc.getMetaData(), desc.getIdentifier(),
                                !node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX));
                        }
                    }
                }
                nodes.put(identifier, node);
            }
        }
        return nodes;
    }

    private Set<EndpointDescription> parseEndpointDescriptions(JsonParser jp, EndpointDescriptionsManager endpointDescsManager,
        boolean isStaticEndpoint)
        throws JsonParseException, IOException {

        Set<EndpointDescription> descs = new HashSet<EndpointDescription>();

        // contains an array
        while (jp.nextToken() != JsonToken.END_ARRAY) {

            String id = null;
            String name = null;
            String dynamicEndpointId = null;
            DataType dataType = null;
            Map<String, String> metaData = new HashMap<String, String>();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (fieldname.equals(IDENTIFIER)) {
                    id = jp.getText();
                } else if (fieldname.equals(NAME)) {
                    name = jp.getText();
                } else if (fieldname.equals(EP_IDENTIFIER)) {
                    dynamicEndpointId = jp.getText();
                } else if (fieldname.equals(DATATYPE)) {
                    dataType = DataType.valueOf(jp.getText());
                } else if (fieldname.equals(METADATA)) {
                    while (jp.nextToken() != JsonToken.END_OBJECT) {
                        String key = jp.getCurrentName();
                        jp.nextToken();
                        String value = jp.getText();
                        metaData.put(key, value);
                    }
                }
            }

            if (id == null || name == null || dataType == null || (!isStaticEndpoint && dynamicEndpointId == null)) {
                throw new IOException("parsing endpoint failed");
            }
            EndpointDescription desc;
            if (isStaticEndpoint) {
                desc = new EndpointDescription(endpointDescsManager.getStaticEndpointDefinition(name),
                    endpointDescsManager.getManagedEndpointType(), id);
                if (endpointDescsManager.getStaticEndpointDefinition(name) == null) {
                    desc.setName(name);
                }
            } else {
                desc = new EndpointDescription(endpointDescsManager.getDynamicEndpointDefinition(dynamicEndpointId),
                    endpointDescsManager.getManagedEndpointType(), id);
                desc.setName(name);
                if (dynamicEndpointId == null || dynamicEndpointId.equals("null")) {
                    desc.setDynamicEndpointIdentifier(null);
                } else {
                    desc.setDynamicEndpointIdentifier(dynamicEndpointId);
                }
            }
            desc.setDataType(dataType);
            desc.setMetaData(metaData);
            descs.add(desc);
            endpointDescs.put(id, desc);
        }
        return descs;
    }

    private ComponentDescription parseComponentDescription(JsonParser jp, NodeIdentifier pi, User user)
        throws IOException, ParseException {

        String cIdentifier;
        String cVersion = "";
        String cName = null;
        jp.nextToken();
        String cField = jp.getCurrentName();
        jp.nextToken(); // move to value
        if (IDENTIFIER.equals(cField)) {
            cIdentifier = jp.getText();
        } else {
            throw new ParseException("No component identifier found.", jp.getCurrentLocation().getLineNr());
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            cField = jp.getCurrentName();
            jp.nextToken(); // move to value
            if (VERSION.equals(cField)) {
                cVersion = jp.getText();
            } else if (NAME.equals(cField)) {
                cName = jp.getText();
            }
        }

        ComponentDescription cd = getComponentDecription(cIdentifier + SEPARATOR + cVersion, cVersion, cName, pi, user);
        if (cd == null) {
            throw new ParseException("No component with this definition registered: " + cIdentifier,
                jp.getCurrentLocation().getLineNr());
        }
        return cd;
    }

    private ComponentDescription getComponentDecription(String identifier, String version, String name, NodeIdentifier node, User user) {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
        List<ComponentInstallation> matchingInstallations = new ArrayList<ComponentInstallation>();
        ComponentInstallation resultInstallation = null;

        // get all matching components
        for (ComponentInstallation installation : compKnowledge.getAllInstallations()) {
            ComponentInterface compInterface = installation.getComponentRevision().getComponentInterface();
            if (compInterface.getIdentifiers().contains(identifier) && compInterface.getVersion().equals(version)) {
                if (installation.getNodeId() != null && installation.getNodeId().equals(node)) {
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
        return new ComponentDescription(resultInstallation);
    }

    /**
     * 
     * Pars {@link JsonParser} to a set of {@link Connection}s.
     * 
     * @param jp {@link JsonParser} - parse JSON
     * @param nodes Map of {@link WorkflowNode}s to add the {@link Connection}s
     * @return Set of {@link Connection}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     * @throws ParseException if parsing the {@link java.io.File} failed for some reason
     */
    public Set<Connection> parseConnections(JsonParser jp, Map<String, WorkflowNode> nodes)
        throws IOException, ParseException {
        Set<Connection> connections = new HashSet<Connection>();

        while (jp.nextToken() != JsonToken.END_ARRAY) {
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                WorkflowNode output = null;
                String outputId = null;
                WorkflowNode input = null;
                String inputId = null;
                String connectionField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (SOURCE.equals(connectionField)) {
                    output = nodes.get(jp.getText());
                } else {
                    throw new ParseException("No source definition.", jp.getCurrentLocation().getLineNr());
                }
                jp.nextToken();
                connectionField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (OUTPUT.equals(connectionField)) {
                    outputId = jp.getText();
                } else {
                    throw new ParseException("No output definition.", jp.getCurrentLocation().getLineNr());
                }
                jp.nextToken();
                connectionField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (TARGET.equals(connectionField)) {
                    input = nodes.get(jp.getText());
                } else {
                    throw new ParseException("No target definition.", jp.getCurrentLocation().getLineNr());
                }
                jp.nextToken();
                connectionField = jp.getCurrentName();
                jp.nextToken(); // move to value
                if (INPUT.equals(connectionField)) {
                    inputId = jp.getText();
                } else {
                    throw new ParseException("No input definition.", jp.getCurrentLocation().getLineNr());
                }

                if (output != null && endpointDescs.get(outputId) != null && input != null && endpointDescs.get(inputId) != null) {
                    connections.add(new Connection(output, endpointDescs.get(outputId),
                        input, endpointDescs.get(inputId)));
                } else {
                    throw new ParseException("Invalid connection definition.", jp.getCurrentLocation().getLineNr());
                }
            }
        }
        return connections;
    }

    /**
     * 
     * Pars {@link JsonParser} to a set of {@link WorkflowLabel}s.
     * 
     * @param jp {@link JsonParser} - parse JSON
     * @return Set of {@link WorkflowLabel}s
     * @throws IOException if reading from {@link java.io.File} failed for some reason
     * @throws ParseException if parsing the {@link java.io.File} failed for some reason
     */
    public Set<WorkflowLabel> parseLabels(JsonParser jp) throws IOException, ParseException {
        Set<WorkflowLabel> labels = new LinkedHashSet<WorkflowLabel>();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            WorkflowLabel label = new WorkflowLabel("");
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String name = jp.getText();
                jp.nextToken();
                String value = jp.getText();
                if (IDENTIFIER.equals(name)) {
                    label.setIdentifier(value);
                }
                if (TEXT.equals(name)) {
                    label.setText(value);
                }
                if (LOCATION.equals(name)) {
                    String[] location = StringUtils.splitAndUnescape(value);
                    label.setLocation(Integer.parseInt(location[0]), Integer.parseInt(location[1]));
                }
                if (SIZE.equals(name)) {
                    String[] size = StringUtils.splitAndUnescape(value);
                    label.setSize(new Dimension(Integer.parseInt(size[0]), Integer.parseInt(size[1])));
                }
                if (ALPHA.equals(name)) {
                    label.setAlpha(Integer.parseInt(value));
                }
                if (COLOR_RGB_TEXT.equals(name)) {
                    String[] colorString = StringUtils.splitAndUnescape(value);
                    int[] color =
                        new int[] { Integer.parseInt(colorString[0]), Integer.parseInt(colorString[1]), Integer.parseInt(colorString[2]) };
                    label.setColorText(color);
                }
                if (COLOR_RGB_BACKGROUND.equals(name)) {
                    String[] colorString = StringUtils.splitAndUnescape(value);
                    int[] color =
                        new int[] { Integer.parseInt(colorString[0]), Integer.parseInt(colorString[1]), Integer.parseInt(colorString[2]) };
                    label.setColorBackground(color);
                }
                if (ALIGNMENT_TYPE.equals(name)) {
                    AlignmentType alignmentType = AlignmentType.valueOf(value);
                    label.setAlignmentType(alignmentType);
                }
                if (BORDER.equals(name)) {
                    label.setHasBorder(Boolean.valueOf(value));
                }
                if (TEXT_SIZE.equals(name)) {
                    label.setTextSize(Integer.valueOf(value));
                }

            }
            labels.add(label);
        }
        return labels;
    }
    
    /**
     * 
     * Writes the given bendpoints to the {@link JsonGenerator}.
     * 
     * @param g {@link JsonGenerator} - write to JSON
     * @param connectionBendpointMapping The mapping between connection and bendpoint to write
     * @throws IOException if writing to {@link java.io.File} failed for some reason
     * @throws JsonGenerationException if writing to {@link JsonGenerator} failed for some reason
     */
    public void writeBendpoints(JsonGenerator g, Map<String, String> connectionBendpointMapping) 
        throws JsonGenerationException, IOException{
        for (String key : connectionBendpointMapping.keySet()) {
            g.writeStartObject();
            g.writeStringField(SOURCE, key.split(BENDPOINT_COORDINATE_SEPARATOR)[0]);
            g.writeStringField(TARGET, key.split(BENDPOINT_COORDINATE_SEPARATOR)[1]);
            g.writeStringField(COORDINATES, connectionBendpointMapping.get(key));
            g.writeEndObject();
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

}
