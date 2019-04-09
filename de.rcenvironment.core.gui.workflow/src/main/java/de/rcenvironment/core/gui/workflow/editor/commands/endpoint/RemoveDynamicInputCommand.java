/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
 * Remove a dynamic input endpoint from a <code>WorkflowNode</code>.
 *
 * @author Caslav Ilic
 */
public class RemoveDynamicInputCommand extends WorkflowNodeCommand {

    protected String endpointId;
    protected String name;
    protected Refreshable[] refreshable;

    private EndpointDescription oldDesc = null;
    private boolean executable = true;
    private boolean undoable = false;

    public RemoveDynamicInputCommand(String endpointId, String name, Refreshable... refreshable) {
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
            oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(name);
            if (oldDesc != null) {
                EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
                inputManager.removeDynamicEndpointDescription(name);
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
                EndpointDescriptionsManager inputManager = getProperties().getInputDescriptionsManager();
                inputManager.addDynamicEndpointDescription(endpointId, name, oldDesc.getDataType(),
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
