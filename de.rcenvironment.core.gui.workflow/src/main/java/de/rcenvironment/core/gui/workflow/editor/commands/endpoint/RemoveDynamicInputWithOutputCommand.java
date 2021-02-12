/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.List;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Implementation of {@link RemoveDynamicEndpointCommand}, which also removes an output with same name and data type.
 * 
 * @author Doreen Seider
 */
public class RemoveDynamicInputWithOutputCommand extends RemoveDynamicEndpointCommand {
    
    protected final String dynEndpointId;

    public RemoveDynamicInputWithOutputCommand(String dynEndpointId, List<String> names, Refreshable... refreshable) {
        super(EndpointType.INPUT, dynEndpointId, names, refreshable);
        this.dynEndpointId = dynEndpointId;
    }
    
    @Override
    public void execute() {
        for (String name : names) {
            EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
            outputDescriptionsManager.removeDynamicEndpointDescription(name);
        }
        super.execute();
    }
    
    @Override
    public void undo() {
        EndpointDescriptionsManager outputDescriptionsManager = getProperties().getOutputDescriptionsManager();
        for (String name : names) {
            EndpointDescription oldDesc = oldDescriptions.get(name);
            outputDescriptionsManager.addDynamicEndpointDescription(dynEndpointId, oldDesc.getName(), 
                oldDesc.getDataType(), oldDesc.getMetaData());
        }
        super.undo();
    }

}
