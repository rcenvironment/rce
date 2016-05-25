/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Encapsulates information about a persistent component description as it is part of workflow files
 * (aka persistent workflow descriptions).
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class PersistentComponentDescription implements Serializable {

    private static final long serialVersionUID = -1647564436671866175L;

    private static final String PLATFORM = "platform";

    private static final String VERSION = "version";

    private static final String IDENTIFIER = "identifier";

    private static final String COMPONENT = "component";

    private static final Log LOGGER = LogFactory.getLog(PersistentComponentDescription.class);

    private String componentIdentifier;

    private String componentVersion = "";

    private NodeIdentifier componentNodeIdentifier = null;

    private String persistentComponentDescriptionString;

    /**
     * Constructor.
     * 
     * @param persistentComponentDescription JSON string of one single component stored und the 'nodes' key in the workflow file
     * @throws IOException on unexpected error
     * @throws JsonParseException on unexpected error
     */
    public PersistentComponentDescription(String persistentComponentDescription) throws JsonParseException, IOException {

        this.persistentComponentDescriptionString = persistentComponentDescription;

        // parse information for convenient access via getter
        JsonFactory jsonFactory = new JsonFactory();
        JsonNode node;
        try (JsonParser jsonParser = jsonFactory.createJsonParser(persistentComponentDescription)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            node = mapper.readTree(jsonParser);
        }

        componentIdentifier = node.get(COMPONENT).get(IDENTIFIER).getTextValue();

        if (node.get(COMPONENT).get(VERSION) != null) {
            componentVersion = node.get(COMPONENT).get(VERSION).getTextValue();
        }

        if (node.get(PLATFORM) != null) {
            componentNodeIdentifier = NodeIdentifierFactory.fromNodeId(node.get(PLATFORM).getTextValue());
        }
    }

    public String getComponentIdentifier() {
        return componentIdentifier;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    /**
     * Sets the component version and adds it to the persistentComponentDescription.
     * 
     * @param componentVersion :
     */
    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
        setComponentValueInDescription(VERSION, componentVersion);
    }

    private void setComponentValueInDescription(String componentKey, String value) {
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonParser jsonParser = jsonFactory.createJsonParser(persistentComponentDescriptionString)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(jsonParser);
            JsonNode componentNode = node.get(COMPONENT);
            ((ObjectNode) componentNode).remove(componentKey);
            ((ObjectNode) componentNode).put(componentKey, TextNode.valueOf(value));
            persistentComponentDescriptionString = mapper.writeValueAsString(node);
        } catch (JsonParseException e) {
            LOGGER.debug("", e);
        } catch (IOException e) {
            LOGGER.debug("", e);
        }

    }

    /**
     * 
     * Sets the platform identifier and adds it to the persistentComponentDescription.
     * 
     * @param nodeId :
     */
    public void setNodeIdentifier(NodeIdentifier nodeId) {
        this.componentNodeIdentifier = nodeId;
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonParser jsonParser = jsonFactory.createJsonParser(persistentComponentDescriptionString)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(jsonParser);
            ((ObjectNode) node).remove(PLATFORM);
            if (nodeId != null) {
                ((ObjectNode) node).put(PLATFORM, TextNode.valueOf(nodeId.getIdString()));
            }
            persistentComponentDescriptionString = mapper.writeValueAsString(node);
        } catch (JsonParseException e) {
            LOGGER.debug("", e);
        } catch (IOException e) {
            LOGGER.debug("", e);
        }
    }

    public NodeIdentifier getComponentNodeIdentifier() {
        return componentNodeIdentifier;
    }

    public String getComponentDescriptionAsString() {
        return persistentComponentDescriptionString;
    }

}
