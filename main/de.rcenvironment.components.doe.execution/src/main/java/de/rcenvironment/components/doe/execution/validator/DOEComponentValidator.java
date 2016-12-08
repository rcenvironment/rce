/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.doe.common.DOEAlgorithms;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.components.doe.execution.Messages;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.spi.AbstractLoopComponentValidator;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Validator for DOE component.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel
 */
public class DOEComponentValidator extends AbstractLoopComponentValidator {

    @Override
    public String getIdentifier() {
        return DOEConstants.COMPONENT_ID;
    }

    @Override
    protected List<ComponentValidationMessage> validateLoopComponentSpecific(
        ComponentDescription componentDescription) {

        List<ComponentValidationMessage> messages = new ArrayList<>();

        final boolean hasOutputs = getOutputs(componentDescription).size() > 0;

        if (!hasOutputs) {
            final ComponentValidationMessage noInputMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, "", Messages.noOutputsDefinedLong,
                Messages.noOutputsDefinedLong);
            messages.add(noInputMessage);
        } else {
            int outputCount = getOutputs(componentDescription).size();
            int runNumber = Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER));
            final ComponentValidationMessage tooManySamplesMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, "", Messages.tooManySamples, Messages.tooManySamples);

            if (getProperty(componentDescription, DOEConstants.KEY_METHOD)
                .equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
                if (Math.pow(runNumber, outputCount) >= DOEAlgorithms.MAXMIMAL_RUNS) {
                    messages.add(tooManySamplesMessage);
                }
            } else {
                if (runNumber >= DOEAlgorithms.MAXMIMAL_RUNS) {
                    messages.add(tooManySamplesMessage);
                }
            }
        }
        if (!(getProperty(componentDescription, DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            || getProperty(componentDescription, DOEConstants.KEY_METHOD)
                .equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO))
            && getOutputs(componentDescription).size() < 2) {
            final ComponentValidationMessage outputTooLow = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_METHOD, Messages.numOutputsG2Long,
                Messages.numOutputsG2Long);
            messages.add(outputTooLow);

        }
        if (getProperty(componentDescription, DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
            checkStartAndEndSample(componentDescription, messages);
            checkTableDimensions(componentDescription, messages);
        }
        if (getProperty(componentDescription, DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
            if (getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER) == null
                || Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER)) < 2) {
                final ComponentValidationMessage noInputMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_RUN_NUMBER,
                    Messages.numLevelsInvalidLong, Messages.numLevelsInvalidLong);
                messages.add(noInputMessage);
            }
        }

        return messages;
    }

    @Override
    protected List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription) {
        // TODO Auto-generated method stub
        return null;
    }

    private void checkTableDimensions(ComponentDescription componentDescription,
        List<ComponentValidationMessage> messages) {
        String table = getProperty(componentDescription, DOEConstants.KEY_TABLE);

        if (table == null || table.isEmpty() || table.equals("null")) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE, Messages.noTableLong,
                Messages.noTableLong);
            messages.add(startSampleErrorMessage);
        } else {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            try {
                Double[][] tableValues = mapper.readValue(table,
                    Double[][].class);
                if (tableValues != null && tableValues.length > 0 && (tableValues[0] != null
                    && tableValues[0].length < getOutputs(componentDescription).size())) {
                    final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE,
                        Messages.tableTooShortLong, Messages.tableTooShortLong);
                    messages.add(startSampleErrorMessage);
                }
                if (tableValues != null && tableValues.length > 0
                    && tableValues[0].length > getOutputs(componentDescription).size()) {
                    final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.WARNING, DOEConstants.KEY_TABLE,
                        Messages.tableTooLongLong, Messages.tableTooLongLong);
                    messages.add(startSampleErrorMessage);
                }
                final ComponentValidationMessage noValueError = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE,
                    Messages.undefinedValues, Messages.undefinedValues);
                for (int i = 0; (i < Integer
                    .parseInt(getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER))
                    && (tableValues != null && i < tableValues.length)); i++) {
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
                if (getProperty(componentDescription, DOEConstants.KEY_METHOD)
                    .equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                    && (getProperty(componentDescription, DOEConstants.KEY_END_SAMPLE) != null
                        && !getProperty(componentDescription, DOEConstants.KEY_END_SAMPLE).isEmpty()
                        && Double.parseDouble(getProperty(componentDescription,
                            DOEConstants.KEY_END_SAMPLE)) > Double.parseDouble(
                                getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER))
                                - 1)) {
                    final ComponentValidationMessage endSampleErrorMessage = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_END_SAMPLE,
                        Messages.endSampleTooHighLong, Messages.endSampleTooHighLong);
                    messages.add(endSampleErrorMessage);
                }
            } catch (IOException e) {
                final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE, Messages.noTableLong,
                    Messages.noTableLong);
                messages.add(startSampleErrorMessage);
            }

        }

    }

    private void checkStartAndEndSample(ComponentDescription componentDescription,
        final Collection<ComponentValidationMessage> messages) {
        String startSampleString = getProperty(componentDescription, DOEConstants.KEY_START_SAMPLE);
        Integer startSample = null;
        try {
            startSample = Integer.parseInt(startSampleString);
        } catch (NumberFormatException e) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_START_SAMPLE,
                Messages.startSampleNotInteger, Messages.startSampleNotInteger);
            messages.add(startSampleErrorMessage);
        }
        if (startSample != null && startSample < 0) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_START_SAMPLE, Messages.startSampleG0,
                Messages.startSampleG0);
            messages.add(startSampleErrorMessage);
        }
        if (startSample != null
            && startSample > Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER))) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_START_SAMPLE, Messages.startSampleTooHigh,
                Messages.startSampleTooHigh);
            messages.add(startSampleErrorMessage);
        }
        String endSampleString = getProperty(componentDescription, DOEConstants.KEY_END_SAMPLE);
        Integer endSample = null;
        try {
            endSample = Integer.parseInt(endSampleString);
        } catch (NumberFormatException e) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_END_SAMPLE, Messages.endSampleNotInteger,
                Messages.endSampleNotInteger);
            messages.add(startSampleErrorMessage);
        }
        if (endSample != null && endSample < 0) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_END_SAMPLE, Messages.endSampleG0,
                Messages.endSampleG0);
            messages.add(startSampleErrorMessage);
        }
        if (startSample == null || endSample == null || startSample > endSample) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_END_SAMPLE, Messages.endSampleGStart,
                Messages.endSampleGStart);
            messages.add(startSampleErrorMessage);
        }

    }

    @Override
    protected Set<EndpointDescription> getOutputs(ComponentDescription componentDescription) {
        Set<EndpointDescription> outputs = new HashSet<>(super.getOutputs(componentDescription));

        Iterator<EndpointDescription> outputsIterator = outputs.iterator();
        while (outputsIterator.hasNext()) {
            EndpointDescription next = outputsIterator.next();
            if (LoopComponentConstants.ENDPOINT_ID_TO_FORWARD.equals(next.getDynamicEndpointIdentifier())
                || LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE.equals(next.getName())
                || DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES.equals(next.getName())) {
                outputsIterator.remove();
            }
        }
        return outputs;
    }

}
