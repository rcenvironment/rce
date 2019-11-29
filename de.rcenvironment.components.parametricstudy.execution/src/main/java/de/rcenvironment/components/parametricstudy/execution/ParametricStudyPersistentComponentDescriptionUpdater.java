/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.execution;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Doreen Seider
 */
public class ParametricStudyPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String DESIGN_VARIABLE = "Design Variable";

    private static final String V1_0 = "1.0";

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";

    private static final String V3_3 = "3.3";

    private static final String V4 = "4";

    private static final String OUTPUT_NAME = "name";

    private static final String STATIC_OUTPUTS = "staticOutputs";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String EP_IDENTIFIER = "epIdentifier";

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return ParametricStudyComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (!silent) {
            if (persistentComponentDescriptionVersion == null
                || persistentComponentDescriptionVersion.compareTo(V1_0) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE;
            }
            if (persistentComponentDescriptionVersion != null) {
                if (persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
                    versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
                }
                if (persistentComponentDescriptionVersion.compareTo(V4) < 0) {
                    versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
                }
            }
        } else if (persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_3) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent) throws IOException {

        if (!silent) { // called after "silent" was called
            if (formatVersion == PersistentDescriptionFormatVersion.BEFORE_VERSON_THREE) {
                return updateToV10(description);
            }
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                return updateFrom10To30(description);
            }
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V3_0:
                    description = updateFrom30To31(description);
                case V3_1:
                    description = updateFrom31To32(description);
                case V3_2:
                    description = updateFrom32To33(description);
                case V3_3:
                    description = updateFrom33To4(description);
                default:
                    // nothing to do here
                }
            }
        } else { // called first (before non-silent)
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V3_1:
                    description = updateFrom31To32(description);
                case V3_2:
                    description = updateFrom32To33(description);
                default:
                    // nothing to do here
                }
            }
        }
        return description;
    }

    private PersistentComponentDescription updateFrom33To4(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        description =
            PersistentComponentDescriptionUpdaterUtils.removeOuterLoopDoneEndpoints(description);
        description = PersistentComponentDescriptionUpdaterUtils.removeEndpointCharacterInfoFromMetaData(description);
        description = PersistentComponentDescriptionUpdaterUtils.reassignEndpointIdentifiers(description, DYNAMIC_INPUTS, "toForward",
            "startToForward", "_start");
        description.setComponentVersion(V4);
        return description;
    }

    private PersistentComponentDescription updateFrom32To33(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        PersistentComponentDescription updatedDesc =
            PersistentComponentDescriptionUpdaterUtils.updateFaultToleranceOfLoopDriver(description);
        updatedDesc.setComponentVersion(V3_3);
        return updatedDesc;
    }

    private PersistentComponentDescription updateFrom31To32(PersistentComponentDescription description) throws JsonProcessingException,
        IOException {

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        for (JsonNode outputEndpoint : staticOutputs) {
            ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
            if (outputEndpoint.get(OUTPUT_NAME).textValue().equals(DESIGN_VARIABLE)) {
                ((ObjectNode) outputEndpoint).put(OUTPUT_NAME, ParametricStudyComponentConstants.OUTPUT_NAME_DV);
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V3_2);
        return newdesc;

    }

    private PersistentComponentDescription updateFrom30To31(PersistentComponentDescription description) throws JsonProcessingException,
        IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        // remove epIdentifier for static outputs
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
                if (outputEndpoint.get(OUTPUT_NAME).textValue().equals("DesignVariable")) {
                    ((ObjectNode) outputEndpoint).put(OUTPUT_NAME, DESIGN_VARIABLE);
                    ObjectNode metadata = createStaticOutputMetaData(node);
                    ((ObjectNode) outputEndpoint).set("metadata", metadata);
                    ((ObjectNode) outputEndpoint).put("datatype", "Float");
                }
            }

        } else {
            ArrayNode statOutputs = JsonNodeFactory.instance.arrayNode();
            ObjectNode output =
                JsonNodeFactory.instance.objectNode();
            output.set("identifier",
                TextNode.valueOf(UUID.randomUUID().toString()));
            output.set(OUTPUT_NAME,
                TextNode.valueOf(DESIGN_VARIABLE));
            output.set("datatype", TextNode.valueOf("Float"));
            ObjectNode metadata = createStaticOutputMetaData(node);
            output.set("metadata", metadata);
            statOutputs.add(output);
            ((ObjectNode) node).set(STATIC_OUTPUTS, statOutputs);
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V3_1);
        return newdesc;
    }

    private ObjectNode createStaticOutputMetaData(JsonNode node) {
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        ObjectNode config = (ObjectNode) node.get("configuration");
        metadata.set("FromValue", TextNode.valueOf(config.get("FromValue").textValue()));
        metadata.set("StepSize", TextNode.valueOf(config.get("StepSize").textValue()));
        metadata.set("ToValue", TextNode.valueOf(config.get("ToValue").textValue()));
        return metadata;
    }

    private PersistentComponentDescription updateFrom10To30(PersistentComponentDescription description) throws JsonProcessingException,
        IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "parameters", description);

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        // remove epIdentifier for static outputs
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ((ObjectNode) outputEndpoint).remove(EP_IDENTIFIER);
            }
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V3_0);
        return newdesc;
    }

    private PersistentComponentDescription updateToV10(PersistentComponentDescription description) throws JsonParseException, IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);
        jsonParser.close();
        JsonNode dynamicInputsNode = rootNode.findPath(STATIC_OUTPUTS);
        Iterator<JsonNode> it = dynamicInputsNode.elements();
        while (it.hasNext()) {
            JsonNode inputNode = it.next();
            ((ObjectNode) inputNode).remove(OUTPUT_NAME);
            ((ObjectNode) inputNode).set(OUTPUT_NAME, TextNode.valueOf(ParametricStudyComponentConstants.OUTPUT_NAME_DV));
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V1_0);
        return description;
    }
}
