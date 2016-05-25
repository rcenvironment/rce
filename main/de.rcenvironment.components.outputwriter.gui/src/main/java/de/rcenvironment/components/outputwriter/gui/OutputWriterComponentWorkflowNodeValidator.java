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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Validator for output writer component.
 * 
 * @author Sascha Zur
 */
public class OutputWriterComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    private static final int MINUS_ONE = -1;

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        String chooseAtStart = getProperty(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);
        if (!Boolean.parseBoolean(chooseAtStart) && getProperty(OutputWriterComponentConstants.CONFIG_KEY_ROOT).isEmpty()) {
            final WorkflowNodeValidationMessage noDirectory = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                Messages.noRootChosen,
                Messages.bind(Messages.noRootChosen, OutputWriterComponentConstants.CONFIG_KEY_ROOT));
            messages.add(noDirectory);
        }
        // Validate OutputLocations
        String outputLocString = getProperty(OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        ObjectMapper jsonMapper = JsonUtils.getDefaultObjectMapper();
        jsonMapper.setVisibility(JsonMethod.ALL, Visibility.ANY);
        if (outputLocString == null) {
            outputLocString = "{}";
        }
        try {
            OutputLocationList outputList = jsonMapper.readValue(outputLocString, OutputLocationList.class);
            List<String> inputNamesHavingOutput = new ArrayList<String>();
            for (OutputLocation out : outputList.getOutputLocations()) {
                inputNamesHavingOutput.addAll(out.getInputs());

                // Check if OutputLocation has at least one input
                if (out.getInputs().isEmpty()) {
                    final WorkflowNodeValidationMessage outputWithoutInput =
                        new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                            out.getFilename(), Messages.noInputForOutput,
                            Messages.bind(Messages.noInputForOutput, out.getFilename()));
                    messages.add(outputWithoutInput);
                }
                // Check if all inputs still exist and if they are connected
                boolean connectedInputs = false;
                boolean unconnectedInputs = false;
                for (String inputName : out.getInputs()) {
                    boolean stillExists = false;
                    for (EndpointDescription ed : getInputs()) {
                        if (ed.getName().equals(inputName)) {
                            stillExists = true;
                            if (ed.isConnected()) {
                                connectedInputs = true;
                            } else {
                                unconnectedInputs = true;
                            }
                        }
                    }
                    if (!stillExists) {
                        final WorkflowNodeValidationMessage missingInput =
                            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                                out.getFilename(), Messages.missingInput,
                                Messages.bind(Messages.missingInput, out.getFilename(), inputName));
                        messages.add(missingInput);
                    }
                }
                // If all inputs of a target are connected or all are unconnected, there is no problem.
                // But if some are connected and some are not, the workflow will probably fail.
                if (connectedInputs && unconnectedInputs) {
                    final WorkflowNodeValidationMessage connectedAndUnconnectedInputs =
                        new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                            out.getFilename(), Messages.connectedAndUnconnectedInputs,
                            Messages.bind(Messages.connectedAndUnconnectedInputs, out.getFilename()));
                    messages.add(connectedAndUnconnectedInputs);
                }

                // Check if all the placeholders can be resolved
                String formatString = out.getFormatString();
                Matcher matcher =
                    Pattern.compile(
                        "\\" + OutputWriterComponentConstants.PH_PREFIX + "([^\\" + OutputWriterComponentConstants.PH_SUFFIX + "]+)")
                        .matcher(formatString);

                List<String> placeholders = new ArrayList<>();
                int pos = MINUS_ONE;
                while (matcher.find(pos + 1)) {
                    pos = matcher.start();
                    placeholders.add(matcher.group(1));
                }
                for (String placeholder : placeholders) {
                    if (placeholder.equals(OutputWriterComponentConstants.TIMESTAMP)
                        || placeholder.equals(OutputWriterComponentConstants.LINEBREAK)
                        || placeholder.equals(OutputWriterComponentConstants.EXECUTION_COUNT)) {
                        continue;
                    } else {
                        boolean foundMatch = false;
                        for (String input : out.getInputs()) {
                            if (placeholder.equals(input)) {
                                foundMatch = true;
                                break;
                            }
                        }
                        if (foundMatch) {
                            continue;
                        }
                    }
                    // No input matched this placeholder
                    final WorkflowNodeValidationMessage unmatchedPlaceholder =
                        new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                            placeholder, Messages.unmatchedPlaceholder,
                            Messages.bind(Messages.unmatchedPlaceholder, out.getFilename(), placeholder));
                    messages.add(unmatchedPlaceholder);
                }

                // Check if all the header placeholders can be resolved
                String header = out.getHeader();
                matcher =
                    Pattern.compile(
                        "\\" + OutputWriterComponentConstants.PH_PREFIX + "([^\\" + OutputWriterComponentConstants.PH_SUFFIX + "]+)")
                        .matcher(header);

                List<String> headerPlaceholders = new ArrayList<>();
                pos = MINUS_ONE;
                while (matcher.find(pos + 1)) {
                    pos = matcher.start();
                    headerPlaceholders.add(matcher.group(1));
                }
                for (String placeholder : headerPlaceholders) {
                    if (placeholder.equals(OutputWriterComponentConstants.TIMESTAMP)
                        || placeholder.equals(OutputWriterComponentConstants.LINEBREAK)
                        || placeholder.equals(OutputWriterComponentConstants.EXECUTION_COUNT)) {
                        continue;
                    }
                    // No input matched this placeholder
                    final WorkflowNodeValidationMessage unmatchedHeaderPlaceholder =
                        new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                            placeholder, Messages.unmatchedHeaderPlaceholder,
                            Messages.bind(Messages.unmatchedHeaderPlaceholder, out.getFilename(), placeholder));
                    messages.add(unmatchedHeaderPlaceholder);
                }
            }
            // Check if every simple data input is connected to an OutputLocation
            for (EndpointDescription ed : getInputs()) {
                if (!ed.getDataType().equals(DataType.FileReference) && !ed.getDataType().equals(DataType.DirectoryReference)) {
                    if (!inputNamesHavingOutput.contains(ed.getName())) {
                        final WorkflowNodeValidationMessage inputWithoutOutput =
                            new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING,
                                ed.getName(), Messages.noOutputForInput,
                                Messages.bind(Messages.noOutputForInput, ed.getName()));

                        messages.add(inputWithoutOutput);
                    }
                }
            }
        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Could not validate OutputLocation configuration " + e.getMessage());
        }
        return messages;
    }
}
