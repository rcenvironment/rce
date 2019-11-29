/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class InputProviderPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String VERSION_3_0 = "3.0";

    private static final String VERSION_3_1 = "3.1";

    private static final String VERSION_3_2 = "3.2";

    private static final String CURRENT_VERSION = VERSION_3_2;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return InputProviderComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int update = PersistentDescriptionFormatVersion.NONE;
        if (!silent) {
            if ((persistentComponentDescriptionVersion == null
                || persistentComponentDescriptionVersion.compareTo(VERSION_3_0) < 0)) {
                update = update | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if ((persistentComponentDescriptionVersion != null
                && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0)) {
                update = update | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
            }
        }
        return update;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) {
            switch (formatVersion) {
            case PersistentDescriptionFormatVersion.FOR_VERSION_THREE:
                description = updateTo30(description);
                break;
            case PersistentDescriptionFormatVersion.AFTER_VERSION_THREE:
                if (description.getComponentVersion().compareTo(VERSION_3_1) < 0) {
                    description = updateFrom30To31(description);
                }
                if (description.getComponentVersion().compareTo(VERSION_3_2) < 0) {
                    description = updateFrom31To32(description);
                }
                
                break;
            default:
                break;
            }
        }
        return description;
    }

    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription updateTo30(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier(
            WorkflowDescriptionPersistenceHandler.DYNAMIC_OUTPUTS, "default", description);
        description.setComponentVersion(VERSION_3_0);
        return description;
    }

    /**
     * Updates the component from version 3.0 to 3.1.
     * */
    private PersistentComponentDescription updateFrom30To31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());

        JsonNode configNode = node.get(WorkflowDescriptionPersistenceHandler.DYNAMIC_OUTPUTS);
        Iterator<JsonNode> iterator = configNode.elements();
        while (iterator.hasNext()) {
            ObjectNode outputNode = (ObjectNode) iterator.next();
            if (outputNode.get(WorkflowDescriptionPersistenceHandler.DATATYPE).textValue().equals(DataType.FileReference.name())) {
                ObjectNode metaDataNode = (ObjectNode) outputNode.get(WorkflowDescriptionPersistenceHandler.METADATA);
                metaDataNode.set(InputProviderComponentConstants.META_VALUE,
                    TextNode.valueOf(StringUtils.format(ConfigurationDefinitionConstants.PLACEHOLDER_FORMAT_STRING,
                        outputNode.get(WorkflowDescriptionPersistenceHandler.NAME).textValue())));
                metaDataNode.set(InputProviderComponentConstants.META_FILESOURCETYPE,
                    TextNode.valueOf(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART));
            }
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(VERSION_3_1);
        return description;
    }
    
    /**
     * Updates the component from version 3.1 to 3.2.
     * */
    private PersistentComponentDescription updateFrom31To32(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        final String filePlaceholderRegex = "\\$\\{((\\w*)(\\.))?((\\*)(\\.))?(.*) \\(File\\)\\}";
        final String directoryPlaceholderRegex = "\\$\\{((\\w*)(\\.))?((\\*)(\\.))?(.*) \\(Directory\\)\\}";

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configurationsNode = (ObjectNode) node.get(WorkflowDescriptionPersistenceHandler.CONFIGURATION);
        if (configurationsNode != null) {
            Iterator<Entry<String, JsonNode>> fields = configurationsNode.fields();
            Map<String, String> fileOutputs = new HashMap<>();
            Map<String, String> dirOutputs = new HashMap<>();
            
            while (fields.hasNext()) {
                Entry<String, JsonNode> configurationNode = fields.next();
                String ouputName = configurationNode.getKey();
                String value = ((TextNode) configurationNode.getValue()).textValue();
                if (value.matches(ComponentUtils.PLACEHOLDER_REGEX) || value.matches(filePlaceholderRegex)) {
                    fields.remove();
                    fileOutputs.put(ouputName, value);
                } else if (value.matches(directoryPlaceholderRegex)) {
                    fields.remove();
                    dirOutputs.put(ouputName, value);
                }
            }
            
            for (String outputName : fileOutputs.keySet()) {
                configurationsNode.put(outputName, fileOutputs.get(outputName).replace(" (File)", ""));
                configurationsNode.put(outputName + PlaceholdersMetaDataConstants.DATA_TYPE, PlaceholdersMetaDataConstants.TYPE_FILE);
            }
            
            for (String outputName : dirOutputs.keySet()) {
                configurationsNode.put(outputName, dirOutputs.get(outputName).replace(" (Directory)", ""));
                configurationsNode.put(outputName + PlaceholdersMetaDataConstants.DATA_TYPE, PlaceholdersMetaDataConstants.TYPE_DIR);
            }
        }
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(VERSION_3_2);
        
        return description;
    }

}
