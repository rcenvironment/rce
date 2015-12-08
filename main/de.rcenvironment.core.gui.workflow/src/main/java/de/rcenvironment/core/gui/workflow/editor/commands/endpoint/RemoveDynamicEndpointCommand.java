/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand} editing dynamic endpoints in a <code>WorkflowNode</code>.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class RemoveDynamicEndpointCommand extends WorkflowNodeCommand {

    protected final EndpointType direction;

    protected final String id;

    protected Refreshable[] refreshable;

    protected final List<String> names;

    protected Map<String, EndpointDescription> oldDescriptions;

    private boolean executable = true;

    private boolean undoable = false;

    /**
     * The constructor.
     * 
     * @param name the name of the endpoint
     * @param type the type of the endpoint
     * @param outputPane
     */
    public RemoveDynamicEndpointCommand(EndpointType type, String dynamicEndpointId, List<String> names, Refreshable... refreshable) {
        direction = type;
        id = dynamicEndpointId;
        this.names = names;
        this.refreshable = refreshable;
        this.oldDescriptions = new HashMap<String, EndpointDescription>();
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
            for (String name : names) {
                final ComponentInstanceProperties componentInstanceConfiguration = getProperties();
                if (direction == EndpointType.INPUT) {
                    oldDescriptions.put(name, getProperties().getInputDescriptionsManager().getEndpointDescription(name));
                    componentInstanceConfiguration.getInputDescriptionsManager().removeDynamicEndpointDescription(name);
                } else {
                    oldDescriptions.put(name, getProperties().getOutputDescriptionsManager().getEndpointDescription(name));
                    componentInstanceConfiguration.getOutputDescriptionsManager().removeDynamicEndpointDescription(name);
                }
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
            for (String name : names) {
                EndpointDescription oldDescription = oldDescriptions.get(name);
                final ComponentInstanceProperties componentInstanceConfiguration = getProperties();
                if (direction == EndpointType.INPUT) {
                    componentInstanceConfiguration.getInputDescriptionsManager().addDynamicEndpointDescription(id, name,
                        oldDescription.getDataType(), oldDescription.getMetaData(), oldDescription.getIdentifier(),
                        oldDescription.getParentGroupName(), true);
                } else {
                    componentInstanceConfiguration.getOutputDescriptionsManager().addDynamicEndpointDescription(id, name,
                        oldDescription.getDataType(), oldDescription.getMetaData());
                }
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
