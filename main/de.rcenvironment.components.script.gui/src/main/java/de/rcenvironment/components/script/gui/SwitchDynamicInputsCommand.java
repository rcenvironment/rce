/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand} switching dynamic inputs from one type to another.
 * 
 * @author Doreen Seider
 */
public class SwitchDynamicInputsCommand extends WorkflowNodeCommand {

    protected String newDynamicEndpointId;
    
    protected String oldDynamicEndpointId;

    protected Refreshable[] refreshables;

   
    /**
     * The constructor.
     * 
     * @param dynamicEndpointId the new dynamic endpoint identifier
     * @param refreshables {@link Refreshable} instances which needs to be refreshed after command execution
     */
    public SwitchDynamicInputsCommand(String dynamicEndpointId, Refreshable... refreshables) {
        newDynamicEndpointId = dynamicEndpointId;
        this.refreshables = refreshables;
    }

    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public void execute() {
        EndpointDescriptionsManager inputDescriptionsManager = getProperties().getInputDescriptionsManager();
        for (EndpointDescription ep : inputDescriptionsManager.getEndpointDescriptions()) {
            oldDynamicEndpointId = ep.getDynamicEndpointIdentifier();
            inputDescriptionsManager.removeDynamicEndpointDescriptionQuietely(ep.getName());
            ep.getMetaData().remove(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT);
            inputDescriptionsManager.addDynamicEndpointDescription(newDynamicEndpointId,
                ep.getName(), ep.getDataType(), ep.getMetaData(), ep.getIdentifier());
            if (!ep.getConnectedDataTypes().isEmpty()) {
                // inputs can only be connected to one ouput
                inputDescriptionsManager.addConnectedDataType(ep.getName(), ep.getConnectedDataTypes().get(0));
            }
        }
        if (refreshables != null) {
            for (Refreshable r : refreshables) {
                r.refresh();
            }
        }
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public void undo() {
        String tempDynEndpointId = newDynamicEndpointId;
        newDynamicEndpointId = oldDynamicEndpointId;
        oldDynamicEndpointId = tempDynEndpointId;
        execute();
    }
}
