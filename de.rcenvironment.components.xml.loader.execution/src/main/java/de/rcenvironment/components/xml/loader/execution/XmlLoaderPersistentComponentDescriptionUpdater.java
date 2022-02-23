/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.xml.loader.execution;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.xml.loader.common.XmlLoaderComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentComponentDescriptionUpdaterUtils;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Markus Kunde
 * @author Doreen Seider
 */
public class XmlLoaderPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String CPACS = "CPACS";

    private static final String V3_0 = "3.0";
    
    private static final String V3_1 = "3.1";
    
    private static final String V3_2 = "3.2";
    
    private final String currentVersion = V3_2;
    
    private JsonFactory jsonFactory = new JsonFactory();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return XmlLoaderComponentConstants.COMPONENT_IDS;
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int versionsToUpdate = PersistentDescriptionFormatVersion.NONE;

        if (!silent && persistentComponentDescriptionVersion != null) {
            if (persistentComponentDescriptionVersion.compareTo(V3_0) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.FOR_VERSION_THREE;
            }
            if (persistentComponentDescriptionVersion.compareTo(currentVersion) < 0) {
                versionsToUpdate = versionsToUpdate | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
            }
        }
        return versionsToUpdate;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion,
        PersistentComponentDescription description, boolean silent) throws IOException {
        if (!silent) {
            if (formatVersion == PersistentDescriptionFormatVersion.FOR_VERSION_THREE) {
                description = updateToV30(description);
            } else if (formatVersion == PersistentDescriptionFormatVersion.AFTER_VERSION_THREE) {
                if (description.getComponentVersion().compareTo(V3_1) < 0) {
                    description = updateFrom3To31(description);
                }
                if (description.getComponentVersion().compareTo(V3_2) < 0) {
                    description = updateFrom31To32(description);
                }
            }
        }
        return description;
    }
    
    /**
     * Updates the component from version 0 to 3.0.
     * */
    private PersistentComponentDescription updateToV30(PersistentComponentDescription description) throws JsonParseException, IOException {

        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicOutputs", "default", description);
        description =
            PersistentComponentDescriptionUpdaterUtils.updateAllDynamicEndpointsToIdentifier("dynamicInputs", "default", description);
        
        
        // StaticOutput CPACS=FileReference
        description = PersistentComponentDescriptionUpdaterUtils.addStaticOutput(description, "CPACS");
 
        // Sets all incoming channels usage to "optional."
        description = PersistentComponentDescriptionUpdaterUtils.updateDynamicInputsOptional(description);
               
        
        description.setComponentVersion(currentVersion);
        
        return description;
    }
    
    private PersistentComponentDescription updateFrom3To31(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        JsonParser jsonParser = jsonFactory.createParser(description.getComponentDescriptionAsString());
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonParser);
        
        final String name = "name";
        TextNode nameNode = (TextNode) rootNode.get(name);
        String nodeName = nameNode.textValue();
        if (nodeName.contains("CPACS Loading")) {
            nodeName = nodeName.replaceAll("CPACS Loading", "XML Loader");
            ((ObjectNode) rootNode).set(name, TextNode.valueOf(nodeName));
        }
        
        JsonNode staticOutputs = rootNode.get("staticOutputs");
        for (JsonNode staticOutput : staticOutputs) {
            ((ObjectNode) staticOutput).set(name, TextNode.valueOf(staticOutput.get(name).textValue().replace(CPACS, "XML")));
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(rootNode));
        description.setComponentVersion(V3_1);
        return description;
    }
    
    private PersistentComponentDescription updateFrom31To32(PersistentComponentDescription description)
        throws JsonParseException, IOException {
        description = PersistentComponentDescriptionUpdaterUtils.updateSchedulingInformation(description);
        description.setComponentVersion(V3_2);
        return description;
    }
}
