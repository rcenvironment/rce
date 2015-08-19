/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cluster.execution;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 */
public class ClusterPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String CONFIGURATION = "configuration";

    private static final String V3_0 = "3.0";
    
    private static final String CURRENT_VERSION = "3.1";

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return ClusterComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        
        int versions = PersistentDescriptionFormatVersion.NONE;

        // Version 0 to 3.0
        if (!silent && ((persistentComponentDescriptionVersion == null
            || persistentComponentDescriptionVersion.compareTo(V3_0) < 0))) {
            versions = versions | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
        }
        // Update 2 : 3.0 -> 3.1
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
            versions = versions | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return versions;
        
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) {
            PersistentComponentDescription updatedDescription = null;
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                updatedDescription = firstUpdate(description);
                updatedDescription.setComponentVersion(V3_0);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                updatedDescription = uodateFrom30To31(description);
                updatedDescription.setComponentVersion(CURRENT_VERSION);
            }
            return updatedDescription;
        }
        return description;
    }
    
    /**
     * Updates the component from version 3.0 to 3.1.
     * */
    private PersistentComponentDescription uodateFrom30To31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode objectNode = (ObjectNode) node.get(CONFIGURATION);
        objectNode.put(ClusterComponentConstants.CONFIG_KEY_PATHTOQUEUINGSYSTEMCOMMANDS, "");
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription firstUpdate(PersistentComponentDescription description) throws JsonParseException, IOException {
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs", "default", description);
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "default", description);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        ObjectNode oldConfig = (ObjectNode) node.get(CONFIGURATION);
        ((ObjectNode) node).remove(CONFIGURATION);
        ObjectNode newConfig = JsonNodeFactory.instance.objectNode();

        checkConfig("host", oldConfig, newConfig);
        checkConfig("port", oldConfig, newConfig);
        checkConfig("queuingSystem", oldConfig, newConfig);
        checkConfig("authUser", oldConfig, newConfig);
        checkConfig("authPhrase", oldConfig, newConfig);
        checkConfig("sandboxRoot", oldConfig, newConfig);
        checkConfig("deleteSandbox", oldConfig, newConfig);
        checkConfig("uploadFiles", oldConfig, newConfig);
        checkConfig("uploadInputFiles", oldConfig, newConfig);
        checkConfig("filesToUpload", oldConfig, newConfig);
        checkConfig("inputsToUpload", oldConfig, newConfig);
        checkConfig("usageOfScript", oldConfig, newConfig);
        checkConfig("localScript", oldConfig, newConfig);
        checkConfig("localScriptName", oldConfig, newConfig);
        checkConfig("remotePathOfScript", oldConfig, newConfig);
        checkConfig("remoteUploadPathOfNewScript", oldConfig, newConfig);
        checkConfig("downloadFiles", oldConfig, newConfig);
        checkConfig("downloadOutputFiles", oldConfig, newConfig);
        checkConfig("filesToDownload", oldConfig, newConfig);
        checkConfig("outputsToDownload", oldConfig, newConfig);
        checkConfig("script", oldConfig, newConfig);

        ((ObjectNode) node).put(CONFIGURATION, newConfig);
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return new PersistentComponentDescription(writer.writeValueAsString(node));
    }

    private void checkConfig(String configKey, ObjectNode oldConfig, ObjectNode newConfig) {
        if (!oldConfig.get(configKey).getTextValue().isEmpty()) {
            newConfig.put(configKey, oldConfig.get(configKey));
        }
    }
}
