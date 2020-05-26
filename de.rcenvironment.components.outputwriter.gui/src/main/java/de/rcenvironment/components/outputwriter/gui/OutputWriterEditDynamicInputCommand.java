/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Command for editing dynamic inputs in the outputWriter component. Also removes the endpoint from its corresponding target, if it is a
 * simple data input..
 *
 * @author Brigitte Boden
 * @author Kathrin Schaffert
 */
public class OutputWriterEditDynamicInputCommand extends EditDynamicEndpointCommand {
    
    // Mapper for parsing the outputlocation json string.
    private ObjectMapper mapper;

    private String oldJsonString = "";
    
    //The outputLocation affected by the change, if any.
    private String outputLocationId;
    
    private boolean removeInput;

    /**
     * Constructor.
     * 
     * @param direction
     * @param oldDescription
     * @param newDescription
     * @param refreshable
     */
    public OutputWriterEditDynamicInputCommand(EndpointType direction, EndpointDescription oldDescription,
        EndpointDescription newDescription, String outputLocationId, boolean removeInput, Refreshable... refreshable) {
        super(direction, oldDescription, newDescription, refreshable);
        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        this.outputLocationId = outputLocationId;
        this.removeInput = removeInput;
    }

    @Override
    public void execute() {
        OutputLocationList list;
        try {
            String inputJsonString = getProperties().getConfigurationDescription().getConfigurationValue(
                OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
            if (inputJsonString != null && !inputJsonString.equals("") && outputLocationId != null && !outputLocationId.equals("")) {
                list = mapper.readValue(inputJsonString, OutputLocationList.class);
                OutputLocation location = list.getOutputLocationById(outputLocationId);
                if (this.removeInput) {
                    location.getInputs().remove(oldDesc.getName());
                }
                String outputJsonString = mapper.writeValueAsString(list);
                getProperties().getConfigurationDescription().setConfigurationValue(
                    OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
                    outputJsonString);
                oldJsonString = inputJsonString;
                newDesc.setParentGroupName(OutputWriterComponentConstants.DEFAULT_GROUP);
            }

        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Error when writing components to JSON: " + e.getMessage());
        }
        

        super.execute();
    }

    @Override
    public void undo() {
        getProperties().getConfigurationDescription().setConfigurationValue(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
            oldJsonString);
        super.undo();
    }

}
