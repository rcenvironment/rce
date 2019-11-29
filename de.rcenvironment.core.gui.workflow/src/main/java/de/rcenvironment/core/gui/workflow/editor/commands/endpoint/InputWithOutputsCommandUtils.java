/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Methods needed by multiple command endpoint command classes.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public final class InputWithOutputsCommandUtils {

    private InputWithOutputsCommandUtils() {}

    protected static void addOutputWithSuffix(ComponentInstanceProperties properties, String dynamicEndpointId, String name, DataType type,
        String nameSuffix, Map<String, String> metaData) {
        EndpointDescriptionsManager outputManager = properties.getOutputDescriptionsManager();
        outputManager.addDynamicEndpointDescription(dynamicEndpointId, name + nameSuffix, type, metaData);
    }
    
    protected static void addOutputWithSuffix(ComponentInstanceProperties properties, String dynamicEndpointId, String name, DataType type,
        String nameSuffix) {
        addOutputWithSuffix(properties, dynamicEndpointId, name, type, nameSuffix, new HashMap<String, String>());
    }

    protected static void removeOutputWithSuffix(ComponentInstanceProperties properties, String name, String nameSuffix) {
        EndpointDescriptionsManager outputManager = properties.getOutputDescriptionsManager();
        outputManager.removeDynamicEndpointDescription(name + nameSuffix);
    }

    protected static void addInputWithSuffix(ComponentInstanceProperties properties, String dynEndpointId, String name, DataType type,
        String inputNameSuffix, String group, Map<String, String> metaData) {
        EndpointDescriptionsManager inputManager = properties.getInputDescriptionsManager();
        inputManager.addDynamicEndpointDescription(dynEndpointId, name + inputNameSuffix, type, metaData,
            UUID.randomUUID().toString(), group, true);
    }
    
    protected static void addInputWithSuffix(ComponentInstanceProperties properties, String dynEndpointId, String name, DataType type,
        String inputNameSuffix, String group) {
        addInputWithSuffix(properties, dynEndpointId, name, type, inputNameSuffix, group, new HashMap<String, String>());
    }

    protected static void removeInputWithSuffix(ComponentInstanceProperties properties, String name, String inputNameSuffix) {
        EndpointDescriptionsManager inputManager = properties.getInputDescriptionsManager();
        inputManager.removeDynamicEndpointDescription(name + inputNameSuffix);
    }

}
