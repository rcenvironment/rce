/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution.validator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputLocationList;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.components.outputwriter.common.OutputWriterValidatorHelper;
import de.rcenvironment.components.outputwriter.execution.Messages;
import de.rcenvironment.components.outputwriter.execution.OutputWriterPathResolver;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractComponentValidator;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Validator for output writer component.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 * @author Brigitte Boden
 * @author Kathrin Schaffert (bugfix #17058, #17322, refactoring)
 */
public class OutputWriterComponentValidator extends AbstractComponentValidator {

    protected final List<ComponentValidationMessage> messages = new LinkedList<>();

    @Override
    public String getIdentifier() {
        return OutputWriterComponentConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateComponentSpecific(ComponentDescription componentDescription) {

        messages.clear();

        checkRootLocation(componentDescription);

        // Validate OutputLocations
        String outputLocString = getProperty(componentDescription,
            OutputWriterComponentConstants.CONFIG_KEY_OUTPUTLOCATIONS);
        ObjectMapper jsonMapper = JsonUtils.getDefaultObjectMapper();
        jsonMapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        if (outputLocString == null) {
            outputLocString = "{}";
        }
        try {
            OutputLocationList outputList = jsonMapper.readValue(outputLocString, OutputLocationList.class);
            List<String> inputNamesHavingOutput = new ArrayList<>();
            for (OutputLocation out : outputList.getOutputLocations()) {

                inputNamesHavingOutput.addAll(out.getInputs());

                checkOutputHasInput(out);
                checkConnectionStatusOfInputs(componentDescription, out);
                checkForUnknownPlaceholder(out);
            }
            checkAllInputsHaveOutput(componentDescription, inputNamesHavingOutput);
        } catch (

        IOException e) {
            LogFactory.getLog(getClass()).debug("Could not validate OutputLocation configuration " + e.getMessage());
        }
        return messages;
    }

    private void checkRootLocation(ComponentDescription componentDescription) {
        String chooseAtStart = getProperty(componentDescription, OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);

        if (!Boolean.parseBoolean(chooseAtStart)) {
            String rootString = getProperty(componentDescription, OutputWriterComponentConstants.CONFIG_KEY_ROOT);
            if (rootString.isEmpty()) {
                final ComponentValidationMessage noDirectory = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                    Messages.noRootChosen,
                    Messages.bind(Messages.noRootChosen, ""));
                messages.add(noDirectory);
            } else {
                OutputWriterPathResolver pathResolver = new OutputWriterPathResolver(t -> {
                });

                try {
                    rootString = pathResolver.adaptRootToAbsoluteRootIfProjectRelative(rootString);
                } catch (ComponentException e) {
                    final ComponentValidationMessage noWorkspace = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                        Messages.noWorkspace,
                        Messages.bind(Messages.noWorkspace, ""));
                    messages.add(noWorkspace);
                    return;
                }
                File rootFile = new File(rootString);
                if (!rootFile.isAbsolute()) {
                    final ComponentValidationMessage noAbsolutePath = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                        Messages.noAbsolutePath,
                        Messages.bind(Messages.noAbsolutePath, OutputWriterComponentConstants.WORKSPACE_PREFIX
                            + OutputWriterComponentConstants.CURRENT_WORKSPACE + OutputWriterComponentConstants.WORKSPACE_SUFFIX));
                    messages.add(noAbsolutePath);
                } else if (!rootFile.isDirectory()) {
                    final ComponentValidationMessage nonExistingDirectory = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.WARNING, OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                        Messages.nonExistingRootChosen,
                        Messages.bind(Messages.nonExistingRootChosen, ""));
                    messages.add(nonExistingDirectory);
                }

            }
        }
    }

    private void checkConnectionStatusOfInputs(ComponentDescription componentDescription, OutputLocation out) {
        // Check if all inputs still exist and if they are connected
        boolean connectedInputs = false;
        boolean unconnectedInputs = false;

        for (String inputName : out.getInputs()) {
            for (EndpointDescription ed : getInputs(componentDescription)) {
                if (ed.getName().equals(inputName)) {
                    if (ed.isConnected()) {
                        connectedInputs = true;
                    } else {
                        unconnectedInputs = true;
                    }
                }
            }
        }

        // If all inputs of a target are connected or all are
        // unconnected, there is no problem.
        // But if some are connected and some are not, the workflow will
        // probably fail.
        if (connectedInputs && unconnectedInputs) {
            final ComponentValidationMessage connectedAndUnconnectedInputs = new ComponentValidationMessage(
                ComponentValidationMessage.Type.WARNING, out.getFilename(),
                Messages.connectedAndUnconnectedInputs,
                Messages.bind(Messages.connectedAndUnconnectedInputs, out.getFilename()));
            messages.add(connectedAndUnconnectedInputs);
        }
    }

    private void checkForUnknownPlaceholder(OutputLocation out) {
        // Check if all the header placeholders can be resolved
        List<String> knownPlaceholderList;
        List<String> unknownPlaceholderList;

        String header = out.getHeader();
        knownPlaceholderList = new ArrayList<>();
        knownPlaceholderList.add(OutputWriterComponentConstants.PH_LINEBREAK);
        knownPlaceholderList.add(OutputWriterComponentConstants.PH_TIMESTAMP);
        knownPlaceholderList.add(OutputWriterComponentConstants.PH_EXECUTION_COUNT);
        final StringBuilder headerWarningBuilder = new StringBuilder();
        unknownPlaceholderList = OutputWriterValidatorHelper.getValidationWarnings(headerWarningBuilder, header, knownPlaceholderList);

        // unknown placeholders are found
        for (String placeholder : unknownPlaceholderList) {
            setMessages(placeholder, out.getFilename(), Messages.unmatchedHeaderPlaceholder);
        }

        // Check if all the format placeholders can be resolved
        String formatString = out.getFormatString();
        for (String input : out.getInputs()) {
            String inputPlaceholder = OutputWriterComponentConstants.PH_PREFIX + input + OutputWriterComponentConstants.PH_SUFFIX;
            if (!knownPlaceholderList.contains(inputPlaceholder)) {
                knownPlaceholderList.add(inputPlaceholder);
            }
        }

        final StringBuilder formatWarningBuilder = new StringBuilder();
        unknownPlaceholderList =
            OutputWriterValidatorHelper.getValidationWarnings(formatWarningBuilder, formatString, knownPlaceholderList);

        // unknown placeholders are found
        for (String placeholder : unknownPlaceholderList) {
            setMessages(placeholder, out.getFilename(), Messages.unmatchedFormatPlaceholder);
        }
    }

    private void checkAllInputsHaveOutput(ComponentDescription componentDescription, List<String> inputNamesHavingOutput) {
        // Check if every simple data input is connected to an
        // OutputLocation
        for (EndpointDescription ed : getInputs(componentDescription)) {
            if (!ed.getDataType().equals(DataType.FileReference)
                && !ed.getDataType().equals(DataType.DirectoryReference)) {
                if (!inputNamesHavingOutput.contains(ed.getName())) {
                    final ComponentValidationMessage inputWithoutOutput = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.WARNING, ed.getName(), Messages.noOutputForInput,
                        Messages.bind(Messages.noOutputForInput, ed.getName()));

                    messages.add(inputWithoutOutput);
                }
            }
        }
    }

    private void checkOutputHasInput(OutputLocation out) {
        // Check if OutputLocation has at least one input
        if (out.getInputs().isEmpty()) {
            final ComponentValidationMessage outputWithoutInput = new ComponentValidationMessage(
                ComponentValidationMessage.Type.WARNING, out.getFilename(), Messages.noInputForOutput,
                Messages.bind(Messages.noInputForOutput, out.getFilename()));
            messages.add(outputWithoutInput);
        }
    }

    private void setMessages(String placeholder, String filename, String message) {
        final ComponentValidationMessage unmatchedPlaceholder = new ComponentValidationMessage(
            ComponentValidationMessage.Type.WARNING, placeholder, message,
            Messages.bind(message, filename, placeholder));
        messages.add(unmatchedPlaceholder);
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        // TODO Auto-generated method stub
        return null;
    }

}
