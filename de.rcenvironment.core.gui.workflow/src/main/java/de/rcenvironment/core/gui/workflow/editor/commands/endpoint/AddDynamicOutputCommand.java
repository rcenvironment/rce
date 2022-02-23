/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.Map;
import java.util.UUID;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand}
 * Add a dynamic output endpoint to a <code>WorkflowNode</code>.
 *
 * @author Caslav Ilic
 */
public class AddDynamicOutputCommand extends WorkflowNodeCommand {

    protected String endpointId;
    protected String name;
    protected DataType type;
    protected Map<String, String> metaData;
    protected String group;
    protected Refreshable[] refreshable;

    private boolean executable = true;
    private boolean undoable = false;

    public AddDynamicOutputCommand(String endpointId, String name, DataType type, Map<String, String> metaData, String group,
                                   Refreshable... refreshable) {
        super();
        this.endpointId = endpointId;
        this.name = name;
        this.type = type;
        this.metaData = metaData;
        this.group = group;
        this.refreshable = refreshable;
    }

    public AddDynamicOutputCommand(String endpointId, String name, DataType type, Map<String, String> metaData,
                                   Refreshable... refreshable) {
        this(endpointId, name, type, metaData, null, refreshable);
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
            outputManager.addDynamicEndpointDescription(endpointId, name, type, metaData, UUID.randomUUID().toString(), group, true);
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
            outputManager.removeDynamicEndpointDescription(name);
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
