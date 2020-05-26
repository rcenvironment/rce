/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.scpoutputcollector.execution;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.component.update.api.PersistentComponentDescription;
import de.rcenvironment.core.component.update.api.PersistentDescriptionFormatVersion;
import de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.utils.common.JsonUtils;


/**
 * Updater for Scp Output Collector.
 *
 * @author Brigitte Boden
 */
@Component
public class ScpOutputCollectorPersistentComponentDescriptionUpdater implements PersistentComponentDescriptionUpdater {

    private static final String V1_1 = "1.1";
    private static final String CURRENT_VERSION = V1_1;
    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater#getComponentIdentifiersAffectedByUpdate()
     */
    @Override
    public String[] getComponentIdentifiersAffectedByUpdate() {
        return new String[] { "de.rcenvironment.scpoutputcollector" };
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater#getFormatVersionsAffectedByUpdate
     * (java.lang.String, boolean)
     */
    @Override
    public int getFormatVersionsAffectedByUpdate(String persistentComponentDescriptionVersion, boolean silent) {
        int update = PersistentDescriptionFormatVersion.NONE;
        if (!silent && persistentComponentDescriptionVersion != null
            && persistentComponentDescriptionVersion.compareTo(CURRENT_VERSION) < 0) {
            update = PersistentDescriptionFormatVersion.AFTER_VERSION_THREE;
        }
        return update;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.component.update.spi.PersistentComponentDescriptionUpdater#performComponentDescriptionUpdate
     * (int, de.rcenvironment.core.component.update.api.PersistentComponentDescription, boolean)
     */
    @Override
    public PersistentComponentDescription performComponentDescriptionUpdate(int formatVersion, PersistentComponentDescription description,
        boolean silent) throws IOException {
        switch (formatVersion) {
        case PersistentDescriptionFormatVersion.AFTER_VERSION_THREE:
            if (description.getComponentVersion().compareTo(V1_1) < 0) {
                description = updateFromV10ToV11(description);
            }
            break;
        default:
            break;
        }
        return description;
    }

    private PersistentComponentDescription updateFromV10ToV11(PersistentComponentDescription description)
        throws JsonProcessingException, IOException {
        final String affectedConfigFieldName = "SimpleDescriptionFormat";

        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode node = mapper.readTree(description.getComponentDescriptionAsString());
        ObjectNode configurationsNode = (ObjectNode) node.get(WorkflowDescriptionPersistenceHandler.CONFIGURATION);
        if (configurationsNode != null && !configurationsNode.has(affectedConfigFieldName)) {
            configurationsNode.put(affectedConfigFieldName, "##SIMPLE_FORMAT_FLAG##");
        }

        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        description = new PersistentComponentDescription(writer.writeValueAsString(node));

        description.setComponentVersion(V1_1);

        return description;
    }
}
