/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.xml.merger.execution;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.components.xml.merger.common.XmlMergerComponentConstants;
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
public class XmlMergerPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String CPACS = "CPACS";

    private static final String V3_0 = "3.0";

    private static final String V3_1 = "3.1";

    private static final String V3_2 = "3.2";

    private static final String V4_0 = "4.0";

    private final String currentVersion = V4_0;

    private JsonFactory jsonFactory = new JsonFactory();

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return XmlMergerComponentConstants.COMPONENT_IDS;
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
                if (description.getComponentVersion().compareTo(V4_0) < 0) {
                    description = updateFrom32To40(description);
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

        // StaticOutput CPACS=FileReference, Static Input CPACS=FileReference, Integrate=FileReference
        description = PersistentComponentDescriptionUpdaterUtils.addStaticInput(description, CPACS);
        description = PersistentComponentDescriptionUpdaterUtils.addStaticInput(description, "CPACS to integrate");
        description = PersistentComponentDescriptionUpdaterUtils.addStaticOutput(description, CPACS);

        // if ConfigValue consumeCPACS==true; CPACS and Integrate CPACS StaticInputs = required
        // else StaticInput CPACS=initialized, Integrate = required
        // Delete ConfigValue consumeCPACS
        description = PersistentComponentDescriptionUpdaterUtils.updateConsumeCPACSFlag(description);

        // Sets all incoming channels usage to "optional."
        description = PersistentComponentDescriptionUpdaterUtils.updateDynamicInputsOptional(description);

        description.setComponentVersion(V3_0);

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
        if (nodeName.contains("CPACS Joiner")) {
            nodeName = nodeName.replaceAll("CPACS Joiner", "XML Merger");
            ((ObjectNode) rootNode).set(name, TextNode.valueOf(nodeName));
        }

        JsonNode dynInputs = rootNode.get("staticInputs");
        for (JsonNode staticInput : dynInputs) {
            ((ObjectNode) staticInput).set(name, TextNode.valueOf(staticInput.get(name).textValue().replace(CPACS, "XML")));
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

    private PersistentComponentDescription updateFrom32To40(PersistentComponentDescription description) throws JsonParseException,
        IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configuration = (ObjectNode) node.get("configuration");
        configuration.put(XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_CONFIGNAME,
            XmlMergerComponentConstants.MAPPINGFILE_DEPLOYMENT_LOADED);
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));
        description.setComponentVersion(V4_0);
        return description;
    }
}
