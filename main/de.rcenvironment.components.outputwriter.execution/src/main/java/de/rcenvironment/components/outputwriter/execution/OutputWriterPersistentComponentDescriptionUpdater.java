/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;

/**
 * Implementation of {@link PersistentComponentDescriptionUpdater}.
 * 
 * @author Doreen Seider
 */
public class OutputWriterPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V1_1 = "1.1";

    private static final String CURRENT_VERSION = V1_1;

    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { OutputWriterComponentConstants.COMPONENT_ID };
    }

    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int update = PersistentDescriptionFormatVersion.NONE;
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
            update = update | PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return update;
    }

    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        if (!silent) {
            switch (formatVersion) {
            case PersistentDescriptionFormatVersion.AFTER_VERSION_THREE:
                if (description.getComponentVersion().compareTo(V1_1) < 0) {
                    description = updateFromV10ToV11(description);
                }
                break;
            default:
                break;
            }
        }
        return description;
    }

    private PersistentComponentDescription updateFromV10ToV11(PersistentComponentDescription description)
        throws JsonParseException, IOException {

        final String affectedConfigFieldName = "OWWritePath";
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configurationsNode = (ObjectNode) node.get(WorkflowDescriptionPersistenceHandler.CONFIGURATION);
        if (configurationsNode != null && configurationsNode.has(affectedConfigFieldName)) {
            configurationsNode.remove(affectedConfigFieldName);
            configurationsNode.put(affectedConfigFieldName, "${targetRootFolder}");
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V1_1);

        return description;
    }

}
