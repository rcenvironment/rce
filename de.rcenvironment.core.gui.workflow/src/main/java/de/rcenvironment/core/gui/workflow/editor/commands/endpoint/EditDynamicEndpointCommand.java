/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand} editing dynamic endpoints in a <code>WorkflowNode</code>.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class EditDynamicEndpointCommand extends WorkflowNodeCommand {

    protected final EndpointType direction;

    protected EndpointDescription oldDesc;

    protected EndpointDescription newDesc;

    protected Refreshable[] refreshable;

    protected EndpointDescriptionsManager manager;

    private boolean executable = true;

    private boolean undoable = false;


    /**
     * The constructor.
     * 
     * @param direction of the endpoint
     * @param oldDescription to be replaced
     * @param newDescription to replace
     */
    public EditDynamicEndpointCommand(final EndpointType direction, final EndpointDescription oldDescription,
        final EndpointDescription newDescription, Refreshable... refreshable) {
        this.direction = direction;
        oldDesc = oldDescription;
        newDesc = newDescription;
        this.refreshable = refreshable;
    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean canExecute() {
        return executable;
    }

    @Override
    public void execute() {
        if (direction == EndpointType.INPUT) {
            manager = getProperties().getInputDescriptionsManager();
        } else {
            manager = getProperties().getOutputDescriptionsManager();
        }
        if (executable) {
            if (manager.getEndpointDescription(oldDesc.getName()).getEndpointDefinition().isStatic()) {
                manager.editStaticEndpointDescription(oldDesc.getName(), newDesc.getDataType(), newDesc.getMetaData());
            } else {
                manager.editDynamicEndpointDescription(oldDesc.getName(), newDesc.getName(), newDesc.getDataType(),
                    newDesc.getMetaData(), newDesc.getDynamicEndpointIdentifier());
            }
            executable = false;
            undoable = true;
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    @Override
    public boolean canUndo() {
        return undoable;
    }

    @Override
    public void undo() {
        if (direction == EndpointType.INPUT) {
            manager = getProperties().getInputDescriptionsManager();
        } else {
            manager = getProperties().getOutputDescriptionsManager();
        }
        if (undoable) {
            if (manager.getEndpointDescription(newDesc.getName()).getEndpointDefinition().isStatic()) {
                manager.editStaticEndpointDescription(newDesc.getName(), oldDesc.getDataType(), oldDesc.getMetaData());
            } else {
                manager.editDynamicEndpointDescription(newDesc.getName(), oldDesc.getName(), oldDesc.getDataType(),
                    oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier());
            }
            executable = true;
            undoable = false;
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }
}
