/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.execution;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class DOEPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String LOOP_ENDPOINT_TYPE = "loopEndpointType_5e0ed1cd";

    private static final String METADATA = "metadata";

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";

    private static final String V3_3 = "3.3";
    
    private static final String V3_4 = "3.4";

    private static final String DYNAMIC_INPUTS = "dynamicInputs";

    private static final String STATIC_OUTPUTS = "staticOutputs";

    private static ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { DOEConstants.COMPONENT_ID };
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {

        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;
        if (silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_4) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(V3_3) < 0) {
            versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (silent) { // called first (before non-silent)
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V3_0:
                    description = updateToVersion31(description);
                case V3_1:
                    description = updateToVersion32(description);
                case V3_2:
                    description = updateToVersion33(description);
                case V3_3:
                    description = updateToVersion34(description);
                default:
                    // nothing to do here
                }
            }
        } else { // called after "silent" was called
            if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                switch (description.getComponentVersion()) {
                case V3_3:
                    description = updateToVersion34(description);
                    break;
                default:
                }
            }
        }
        return description;
    }

    private PersistentComponentDescription updateToVersion34(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        PersistentComponentDescription updatedDesc =
            PersistentComponentDescriptionUpdaterUtils.updateFaultToleranceOfLoopDriver(description);
        updatedDesc.setComponentVersion(V3_4);
        return updatedDesc;
    }
    
    private PersistentComponentDescription updateToVersion33(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        JsonNode staticOutputs = node.get(STATIC_OUTPUTS);
        if (staticOutputs != null) {
            for (JsonNode outputEndpoint : staticOutputs) {
                ObjectNode metaData = (ObjectNode) outputEndpoint.get(METADATA);
                if (metaData == null) {
                    metaData = JsonNodeFactory.instance.objectNode();
                    ((ObjectNode) outputEndpoint).put(METADATA, metaData);
                }
                if (outputEndpoint.get("name").getTextValue().equals("Outer loop done")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, "InnerLoopEndpoint");
                }
                if (outputEndpoint.get("name").getTextValue().equals("Done")) {
                    metaData.put(LOOP_ENDPOINT_TYPE, "OuterLoopEndpoint");
                }
            }
        }
        JsonNode dynamicOutputs = node.get("dynamicOutputs");
        if (dynamicOutputs != null) {
            for (JsonNode outputEndpoint : dynamicOutputs) {
                ObjectNode metaData = (ObjectNode) outputEndpoint.get(METADATA);
                metaData.put(LOOP_ENDPOINT_TYPE, "SelfLoopEndpoint");
            }
        }
        JsonNode dynamicInputs = node.get(DYNAMIC_INPUTS);
        if (dynamicInputs != null) {
            for (JsonNode inputEndpoint : dynamicInputs) {
                ObjectNode metaData = (ObjectNode) inputEndpoint.get(METADATA);
                metaData.put(LOOP_ENDPOINT_TYPE, "SelfLoopEndpoint");
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        PersistentComponentDescription newdesc = new PersistentComponentDescription(writer.writeValueAsString(node));
        newdesc.setComponentVersion(V3_3);
        return newdesc;
    }

    private PersistentComponentDescription updateToVersion31(PersistentComponentDescription description) {
        description.setComponentVersion(V3_1);
        return description;
    }

    private PersistentComponentDescription updateToVersion32(PersistentComponentDescription description) {
        description.setComponentVersion(V3_2);
        return description;
    }

}
