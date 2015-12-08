/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.gui.properties;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.doe.common.DOEAlgorithms;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.gui.workflow.editor.properties.LoopComponentWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for DOE component.
 * 
 * @author Sascha Zur
 */
public class DOEWorkflowNodeValidator extends LoopComponentWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final Collection<WorkflowNodeValidationMessage> messages = super.validate();

        final boolean hasOutputs = getOutputs().size() > 0;

        if (!hasOutputs) {
            final WorkflowNodeValidationMessage noInputMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                "",
                Messages.noOutputsDefinedLong,
                Messages.noOutputsDefinedLong);
            messages.add(noInputMessage);
        } else {
            int outputCount = getOutputs().size();
            int runNumber = Integer.parseInt(getProperty(DOEConstants.KEY_RUN_NUMBER));
            final WorkflowNodeValidationMessage tooManySamplesMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                "", Messages.tooManySamples, Messages.tooManySamples);

            if (getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
                if (Math.pow(runNumber, outputCount) >= DOEAlgorithms.MAXMIMAL_RUNS) {
                    messages.add(tooManySamplesMessage);
                }
            } else {
                if (runNumber >= DOEAlgorithms.MAXMIMAL_RUNS) {
                    messages.add(tooManySamplesMessage);
                }
            }
        }
        if (!(getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            || getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO)) && getOutputs().size() < 2) {
            final WorkflowNodeValidationMessage outputTooLow = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_METHOD,
                Messages.numOutputsG2Long,
                Messages.numOutputsG2Long);
            messages.add(outputTooLow);

        }
        if (getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
            checkStartAndEndSample(messages);
            checkTableDimensions(messages);
        }
        if (getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
            if (getProperty(DOEConstants.KEY_RUN_NUMBER) == null || Integer.parseInt(getProperty(DOEConstants.KEY_RUN_NUMBER)) < 2) {
                final WorkflowNodeValidationMessage noInputMessage = new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    DOEConstants.KEY_RUN_NUMBER,
                    Messages.numLevelsInvalidLong,
                    Messages.numLevelsInvalidLong);
                messages.add(noInputMessage);
            }
        }

        return messages;
    }

    private void checkTableDimensions(Collection<WorkflowNodeValidationMessage> messages) {
        if (getProperty(DOEConstants.KEY_TABLE) == null || getProperty(DOEConstants.KEY_TABLE).isEmpty()) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_TABLE,
                Messages.noTableLong,
                Messages.noTableLong);
            messages.add(startSampleErrorMessage);
        } else {
            if (getProperty(DOEConstants.KEY_TABLE) != null && !getProperty(DOEConstants.KEY_TABLE).isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Double[][] tableValues = mapper.readValue(getProperty(DOEConstants.KEY_TABLE), Double[][].class);
                    if (tableValues != null && tableValues.length > 0
                        && (tableValues[0] != null && tableValues[0].length < getOutputs().size())) {
                        final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                            WorkflowNodeValidationMessage.Type.ERROR,
                            DOEConstants.KEY_TABLE,
                            Messages.tableTooShortLong,
                            Messages.tableTooShortLong);
                        messages.add(startSampleErrorMessage);
                    }
                    if (tableValues != null && tableValues.length > 0 && tableValues[0].length > getOutputs().size()) {
                        final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                            WorkflowNodeValidationMessage.Type.WARNING,
                            DOEConstants.KEY_TABLE,
                            Messages.tableTooLongLong,
                            Messages.tableTooLongLong);
                        messages.add(startSampleErrorMessage);
                    }
                    final WorkflowNodeValidationMessage noValueError = new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.ERROR,
                        DOEConstants.KEY_TABLE,
                        "There are not defined values in the table",
                        "There are not defined values in the table");
                    for (int i = 0; (i < Integer.parseInt(
                        getProperty(DOEConstants.KEY_RUN_NUMBER)) && (tableValues != null && i < tableValues.length)); i++) {
                        for (int j = 0; j < tableValues[i].length; j++) {
                            if (tableValues[i][j] == null) {
                                messages.add(noValueError);
                                break;
                            }
                        }
                        if (messages.contains(noValueError)) {
                            break;
                        }
                    }
                    if (getProperty(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                        && (getProperty(DOEConstants.KEY_END_SAMPLE) != null
                            && !getProperty(DOEConstants.KEY_END_SAMPLE).isEmpty() && Double
                                .parseDouble(getProperty(DOEConstants.KEY_END_SAMPLE)) > Double
                                    .parseDouble(getProperty(DOEConstants.KEY_RUN_NUMBER)) - 1)) {
                        final WorkflowNodeValidationMessage endSampleErrorMessage = new WorkflowNodeValidationMessage(
                            WorkflowNodeValidationMessage.Type.ERROR,
                            DOEConstants.KEY_END_SAMPLE,
                            Messages.endSampleTooHighLong,
                            Messages.endSampleTooHighLong);
                        messages.add(endSampleErrorMessage);
                    }
                } catch (IOException e) {
                    final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                        WorkflowNodeValidationMessage.Type.ERROR,
                        DOEConstants.KEY_TABLE,
                        Messages.noTableLong,
                        Messages.noTableLong);
                    messages.add(startSampleErrorMessage);
                }

            }
        }
    }

    private void checkStartAndEndSample(final Collection<WorkflowNodeValidationMessage> messages) {
        String startSampleString = getProperty(DOEConstants.KEY_START_SAMPLE);
        Integer startSample = null;
        try {
            startSample = Integer.parseInt(startSampleString);
        } catch (NumberFormatException e) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_START_SAMPLE,
                Messages.startSampleNotInteger,
                Messages.startSampleNotInteger);
            messages.add(startSampleErrorMessage);
        }
        if (startSample != null && startSample < 0) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_START_SAMPLE,
                Messages.startSampleG0,
                Messages.startSampleG0);
            messages.add(startSampleErrorMessage);
        }
        if (startSample != null && startSample > Integer.parseInt(getProperty(DOEConstants.KEY_RUN_NUMBER))) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_START_SAMPLE,
                Messages.startSampleTooHigh,
                Messages.startSampleTooHigh);
            messages.add(startSampleErrorMessage);
        }
        String endSampleString = getProperty(DOEConstants.KEY_END_SAMPLE);
        Integer endSample = null;
        try {
            endSample = Integer.parseInt(endSampleString);
        } catch (NumberFormatException e) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_END_SAMPLE,
                Messages.endSampleNotInteger,
                Messages.endSampleNotInteger);
            messages.add(startSampleErrorMessage);
        }
        if (endSample != null && endSample < 0) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_END_SAMPLE,
                Messages.endSampleG0,
                Messages.endSampleG0);
            messages.add(startSampleErrorMessage);
        }
        if (startSample == null || endSample == null || startSample > endSample) {
            final WorkflowNodeValidationMessage startSampleErrorMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                DOEConstants.KEY_END_SAMPLE,
                Messages.endSampleGStart,
                Messages.endSampleGStart);
            messages.add(startSampleErrorMessage);
        }

    }

    @Override
    protected Set<EndpointDescription> getOutputs() {
        Set<EndpointDescription> outputs = new HashSet<>(super.getOutputs());

        Iterator<EndpointDescription> outputsIterator = outputs.iterator();
        while (outputsIterator.hasNext()) {
            EndpointDescription next = outputsIterator.next();
            if (LoopComponentConstants.ENDPOINT_ID_TO_FORWARD.equals(next.getDynamicEndpointIdentifier())
                || LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE.equals(next.getName())
                || LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE.equals(next.getName())) {
                outputsIterator.remove();
            }
        }
        return outputs;
    }
}
