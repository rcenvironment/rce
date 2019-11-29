/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.update.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Encapsulates information about a persistent component description as it is part of workflow files (aka persistent workflow descriptions).
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Robert Mischke (8.0.0 id adaptations)
 * 
 * Note: This is a model for the string representation of a component in the workflow file. I'm not right happy with it not having a
 * link to the {@link ComponentDescription} e.g. Although, I'm not sure if required.  --seid_do
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

    private LogicalNodeId componentNodeIdentifier = null;

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

        if (!node.has(COMPONENT)) {
            throw new IOException("Required attribute 'component' missing in node delcaration");
        }
        if (!node.get(COMPONENT).has(IDENTIFIER)) {
            throw new IOException("Required attribute 'identifier' missing in node's component delcaration");
        }
        componentIdentifier = node.get(COMPONENT).get(IDENTIFIER).textValue();

        if (node.get(COMPONENT).has(VERSION)) {
            componentVersion = node.get(COMPONENT).get(VERSION).textValue();
        }

        if (node.has(PLATFORM)) {
            final String encodedNodeId = node.get(PLATFORM).textValue();
            try {
                componentNodeIdentifier = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeId(encodedNodeId);
            } catch (IdentifierException e) {
                throw new IOException("Invalid node id string: " + encodedNodeId);
            }
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
    public void setNodeIdentifier(LogicalNodeId nodeId) {
        this.componentNodeIdentifier = nodeId;
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonParser jsonParser = jsonFactory.createJsonParser(persistentComponentDescriptionString)) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            JsonNode node = mapper.readTree(jsonParser);
            ((ObjectNode) node).remove(PLATFORM);
            if (nodeId != null) {
                ((ObjectNode) node).put(PLATFORM, TextNode.valueOf(nodeId.getLogicalNodeIdString()));
            }
            persistentComponentDescriptionString = mapper.writeValueAsString(node);
        } catch (JsonParseException e) {
            LOGGER.debug("", e);
        } catch (IOException e) {
            LOGGER.debug("", e);
        }
    }

    public LogicalNodeId getComponentNodeIdentifier() {
        return componentNodeIdentifier;
    }

    public String getComponentDescriptionAsString() {
        return persistentComponentDescriptionString;
    }

}
