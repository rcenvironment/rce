/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Implementation of {@link AddDynamicEndpointCommand}, which also adds an output with same name and data type.
 * 
 * @author Doreen Seider
 */
public class AddDynamicInputWithOutputCommand extends AddDynamicEndpointCommand {
    
    protected final String dynEndpointId;
    
    private Map<String, String> metaDataOutput;
    
    public AddDynamicInputWithOutputCommand(String dynEndpointId, String name, DataType type, Map<String, String> metaData, 
        Refreshable... refreshables) {
        super(EndpointType.INPUT, dynEndpointId, name, type, metaData, refreshables);
        this.dynEndpointId = dynEndpointId;
    }
    
    @Override
    public void execute() {
        EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
        outputDescriptionsManager.addDynamicEndpointDescription(dynEndpointId, name, type, metaDataOutput);
        super.execute();
    }
    
    @Override
    public void undo() {
        EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
        outputDescriptionsManager.removeDynamicEndpointDescription(name);
        super.undo();
    }
    
    public void setMetaDataOutput(Map<String, String> metaDataOutput) {
        this.metaDataOutput = metaDataOutput;
    }
    
}
