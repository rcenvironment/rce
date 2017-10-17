/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand}
 * Remove a dynamic output endpoint from a <code>WorkflowNode</code>.
 *
 * @author Caslav Ilic
 */
public class RemoveDynamicOutputCommand extends WorkflowNodeCommand {

    protected String endpointId;
    protected String name;
    protected Refreshable[] refreshable;

    private EndpointDescription oldDesc = null;
    private boolean executable = true;
    private boolean undoable = false;

    public RemoveDynamicOutputCommand(String endpointId, String name, Refreshable... refreshable) {
        super();
        this.endpointId = endpointId;
        this.name = name;
        this.refreshable = refreshable;
    }

    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute() {
        return executable;
    }

    @Override
    public void execute() {
        if (executable) {
            oldDesc = getProperties().getOutputDescriptionsManager().getEndpointDescription(name);
            if (oldDesc != null) {
                EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
                outputManager.removeDynamicEndpointDescription(name);
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
        if (undoable) {
            if (oldDesc != null) {
                EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
                outputManager.addDynamicEndpointDescription(endpointId, name, oldDesc.getDataType(),
                    oldDesc.getMetaData(), oldDesc.getIdentifier(), oldDesc.getParentGroupName(), true);
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
