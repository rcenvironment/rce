/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand} adding dynamic endpoints to a <code>WorkflowNode</code>.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class AddDynamicEndpointCommand extends WorkflowNodeCommand {

    protected EndpointDescriptionsManager endpointDescManager;

    protected String id;

    protected String name;

    protected DataType type;

    protected Map<String, String> metaData;

    protected Refreshable[] refreshable;

    private boolean executable = true;

    private boolean undoable = false;

    private EndpointType direction;

    /**
     * Constructor.
     * 
     * @param direction
     * @param name
     * @param type
     * @param executable
     * @param undoable
     */
    public AddDynamicEndpointCommand(EndpointType direction, String id, String name, DataType type, Map<String, String> metaData,
        Refreshable... refreshable) {
        super();
        this.name = name;
        this.id = id;
        this.type = type;
        this.metaData = metaData;
        this.direction = direction;
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
            endpointDescManager = getProperties().getInputDescriptionsManager();
        } else {
            endpointDescManager = getProperties().getOutputDescriptionsManager();
        }
        if (executable) {
            endpointDescManager.addDynamicEndpointDescription(id, name, type, metaData);
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
            endpointDescManager = getProperties().getInputDescriptionsManager();
        } else {
            endpointDescManager = getProperties().getOutputDescriptionsManager();
        }
        if (undoable) {
            endpointDescManager.removeDynamicEndpointDescription(name);
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
