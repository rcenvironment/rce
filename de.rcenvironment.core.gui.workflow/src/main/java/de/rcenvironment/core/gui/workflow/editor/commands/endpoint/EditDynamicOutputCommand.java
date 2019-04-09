/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand}
 * Edit a dynamic output endpoint in a <code>WorkflowNode</code>.
 *
 * @author Caslav Ilic
 */
public class EditDynamicOutputCommand extends WorkflowNodeCommand {

    protected String endpointId;
    protected String oldName;
    protected String newName;
    protected DataType newType;
    protected Map<String, String> newMetaData;
    protected String newGroup;
    protected Refreshable[] refreshable;

    private EndpointDescription oldDesc = null;
    private boolean executable = true;
    private boolean undoable = false;

    public EditDynamicOutputCommand(String endpointId, String oldName, String newName, DataType newType, Map<String, String> newMetaData,
                                    String newGroup, Refreshable... refreshable) {
        super();
        this.endpointId = endpointId;
        this.oldName = oldName;
        this.newName = newName;
        this.newType = newType;
        this.newMetaData = newMetaData;
        this.newGroup = newGroup;
        this.refreshable = refreshable;
    }

    public EditDynamicOutputCommand(String endpointId, String oldName, String newName, DataType newType, Map<String, String> newMetaData,
                                    Refreshable... refreshable) {
        this(endpointId, oldName, newName, newType, newMetaData, null, refreshable);
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
            EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
            oldDesc = getProperties().getOutputDescriptionsManager().getEndpointDescription(oldName);
            DataType type = newType;
            if (newType == null) {
                type = oldDesc.getDataType();
            }
            Map<String, String> metaData = newMetaData;
            if (metaData == null) {
                metaData = oldDesc.getMetaData();
            }
            String group = newGroup;
            if (group == null) {
                group = oldDesc.getParentGroupName();
            }
            outputManager.editDynamicEndpointDescription(oldName, newName, type, metaData, endpointId, group);
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
            EndpointDescriptionsManager outputManager = getProperties().getOutputDescriptionsManager();
            outputManager.editDynamicEndpointDescription(newName, oldName, oldDesc.getDataType(),
                oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), oldDesc.getParentGroupName());
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
