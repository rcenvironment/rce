/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Command for deleting dynamic inputs from an outputwriter component. If the input is a simple data type, it is also removed from the
 * corresponding target.
 *
 * @author Brigitte Boden
 */
public class OutputWriterRemoveDynamicInputCommand extends RemoveDynamicEndpointCommand {

    // Mapper for parsing the outputlocation json string.
    private ObjectMapper mapper;

    private String oldJsonString = "";

    private List<String> outputLocationIds;

    /**
     * Constructor.
     * 
     * @param type
     * @param dynamicEndpointId
     * @param names
     * @param refreshable
     */
    public OutputWriterRemoveDynamicInputCommand(EndpointType type, String dynamicEndpointId, List<String> names,
        List<String> outputLocationIds, Refreshable... refreshable) {
        super(type, dynamicEndpointId, names, refreshable);
        this.outputLocationIds = outputLocationIds;
        mapper = JsonUtils.getDefaultObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
    }

    @Override
    public void execute() {

        OutputLocationList list;
        try {
            String inputJsonString = getProperties().getConfigurationDescription().getConfigurationValue(
                OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
            if (inputJsonString != null && !inputJsonString.equals("")) {
                list = mapper.readValue(inputJsonString, OutputLocationList.class);
                for (String id : outputLocationIds) {
                    OutputLocation location = list.getOutputLocationById(id);
                    location.getInputs().removeAll(names);
                }
                String outputJsonString = mapper.writeValueAsString(list);
                getProperties().getConfigurationDescription().setConfigurationValue(
                    OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS,
                    outputJsonString);
                oldJsonString = inputJsonString;
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
