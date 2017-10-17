/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * A command class for deleting output locations.
 *
 * @author Brigitte Boden
 */
public class RemoveOutputLocationsCommand extends WorkflowNodeCommand {

    private List<String> identifiers;

    private ObjectMapper mapper;

    private boolean executable = true;

    private Refreshable[] refreshable;

    private boolean undoable = false;

    private String oldJsonString;

    private List<OutputLocation> oldLocations;

    public RemoveOutputLocationsCommand(List<String> ids, Refreshable... refreshable) {
        this.identifiers = ids;
        this.refreshable = refreshable;
        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(JsonMethod.ALL, Visibility.ANY);
    }

    public RemoveOutputLocationsCommand(String id, Refreshable... refreshable) {
        this.identifiers = new ArrayList<String>();
        this.identifiers.add(id);
        this.refreshable = refreshable;
        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(JsonMethod.ALL, Visibility.ANY);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#initialize()
     */
    @Override
    public void initialize() {
        // Do nothing

    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#canExecute()
     */
    @Override
    public boolean canExecute() {
        return executable;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#canUndo()
     */
    @Override
    public boolean canUndo() {
        return undoable;
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#execute()
     */
    @Override
    public void execute() {
        OutputLocationList list;
        try {
            // Parse current location
            String inputJsonString = getProperties().getConfigurationDescription().getConfigurationValue(
                OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
            list = mapper.readValue(inputJsonString, OutputLocationList.class);
            oldLocations = new ArrayList<OutputLocation>();
            for (String id : identifiers) {
                for (String inputName : list.getOutputLocationById(id).getInputs()) {
                    oldLocations.add(list.getOutputLocationById(id));
                    EndpointDescription oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(inputName);
                    if (oldDesc != null) {
                        getProperties().getInputDescriptionsManager().editDynamicEndpointDescription(inputName, inputName,
                            oldDesc.getDataType(),
                            oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), OutputWriterComponentConstants.DEFAULT_GROUP);
                    }
                }
                getProperties().getInputDescriptionsManager().removeDynamicEndpointGroupDescription(
                    list.getOutputLocationById(id).getGroupId());
                list.removeLocation(id);
            }
            String outputJsonString = mapper.writeValueAsString(list);
            getProperties().getConfigurationDescription().setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
                outputJsonString);

            oldJsonString = inputJsonString;
            executable = false;
            undoable = true;

            if (refreshable != null) {
                for (Refreshable r : refreshable) {
                    r.refresh();
                }
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when writing components to JSON: " + e.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#undo()
     */
    @Override
    public void undo() {
        getProperties().getConfigurationDescription().setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
            oldJsonString);

        for (OutputLocation out : oldLocations) {
            if (getProperties().getInputDescriptionsManager().isValidEndpointGroupName(out.getGroupId())) {
                getProperties().getInputDescriptionsManager().addDynamicEndpointGroupDescription(
                    OutputWriterComponentConstants.EP_IDENTIFIER, out.getGroupId());
            }
            for (String inputName : out.getInputs()) {
                EndpointDescription oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(inputName);
                if (oldDesc != null) {
                    getProperties().getInputDescriptionsManager().editDynamicEndpointDescription(inputName, inputName,
                        oldDesc.getDataType(),
                        oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), out.getGroupId());
                }
            }
        }

        executable = true;
        undoable = false;
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

}
