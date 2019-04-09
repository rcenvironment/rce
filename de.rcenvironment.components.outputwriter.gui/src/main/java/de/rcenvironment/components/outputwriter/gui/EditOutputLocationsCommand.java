/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * A class for writing the outputLocation information into the component's configuration.
 *
 * @author Brigitte Boden
 */
public class EditOutputLocationsCommand extends WorkflowNodeCommand {

    private String oldGroupId;

    private OutputLocation out;

    private boolean executable = true;

    private boolean undoable = false;

    private Refreshable[] refreshable;

    private ObjectMapper mapper;

    private String oldJsonString;

    private List<String> oldInputs;

    public EditOutputLocationsCommand(OutputLocation out, Refreshable... refreshable) {
        super();
        this.out = out;
        this.refreshable = refreshable;
        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand#initialize()
     */
    @Override
    public void initialize() {

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
            if (inputJsonString != null && !inputJsonString.isEmpty()) {
                list = mapper.readValue(inputJsonString, OutputLocationList.class);
            } else {
                list = new OutputLocationList();
            }

            if (list.getOutputLocationById(out.getGroupId()) != null) {
                oldInputs = list.getOutputLocationById(out.getGroupId()).getInputs();
            } else {
                oldInputs = new ArrayList<String>();
            }

            list.addOrReplaceOutputLocation(out);

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

            for (String inputName : oldInputs) {
                if (!out.getInputs().contains(inputName)) {
                    // For inputs that were removed from this OutputLocation, the input group must be changed
                    EndpointDescription oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(inputName);
                    if (oldDesc != null) {
                        getProperties().getInputDescriptionsManager().editDynamicEndpointDescription(inputName, inputName,
                            oldDesc.getDataType(),
                            oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), OutputWriterComponentConstants.DEFAULT_GROUP);
                    }
                }
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

        if (oldGroupId != null) {
            getProperties().getInputDescriptionsManager().removeDynamicEndpointGroupDescription(out.getGroupId());
            getProperties().getInputDescriptionsManager().addDynamicEndpointGroupDescription(OutputWriterComponentConstants.EP_IDENTIFIER,
                oldGroupId);
        }

        String formerGroupId = oldGroupId;
        if (formerGroupId == null) {
            formerGroupId = out.getGroupId();
        }
        for (String inputName : oldInputs) {
            EndpointDescription oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(inputName);
            if (oldDesc != null) {
                getProperties().getInputDescriptionsManager().editDynamicEndpointDescription(inputName, inputName, oldDesc.getDataType(),
                    oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), formerGroupId);
            }
        }

        for (String inputName : out.getInputs()) {
            if (!oldInputs.contains(inputName)) {
                // For inputs that were removed from this OutputLocation (by the undo operation), the input group must be changed
                EndpointDescription oldDesc = getProperties().getInputDescriptionsManager().getEndpointDescription(inputName);
                if (oldDesc != null) {
                    getProperties().getInputDescriptionsManager().editDynamicEndpointDescription(inputName, inputName,
                        oldDesc.getDataType(),
                        oldDesc.getMetaData(), oldDesc.getDynamicEndpointIdentifier(), OutputWriterComponentConstants.DEFAULT_GROUP);
                }
            }
        }

        getProperties().getConfigurationDescription().setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
            oldJsonString);

        undoable = false;
        executable = true;
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

}
