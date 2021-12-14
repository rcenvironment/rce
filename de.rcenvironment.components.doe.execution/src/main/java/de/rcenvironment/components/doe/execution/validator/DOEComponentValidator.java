/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

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
 * @author Kathrin Schaffert
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

        int outputCount = getOutputs(componentDescription).size();

        String methodName = getProperty(componentDescription, DOEConstants.KEY_METHOD);
        int runNumber = Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_RUN_NUMBER));
        
        Optional<ComponentValidationMessage> outputValidationMessage = checkForOutputs(outputCount, methodName);
        if (outputValidationMessage.isPresent()) {
            messages.add(outputValidationMessage.get());
            return messages;
        }

        Optional<ComponentValidationMessage> runNumberValidationMessage = checkForRunNumberGreaterZero(runNumber);
        if (runNumberValidationMessage.isPresent()) {
            messages.add(runNumberValidationMessage.get());
            return messages;
        }

        Optional<ComponentValidationMessage> tooManySamplesValidationMessage = checkForTooManySamples(outputCount, methodName, runNumber);
        if (tooManySamplesValidationMessage.isPresent()) {
            messages.add(tooManySamplesValidationMessage.get());
            return messages;
        }

        Optional<ComponentValidationMessage> invalidLevelsValidationMessage = checkForNumberOfLevels(methodName, runNumber);
        if (invalidLevelsValidationMessage.isPresent()) {
            messages.add(invalidLevelsValidationMessage.get());
            return messages;
        }

        if (methodName.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
            checkStartAndEndSample(componentDescription, messages);
            checkTableDimensions(componentDescription, messages);
        }

        return messages;
    }

    private Optional<ComponentValidationMessage> checkForNumberOfLevels(String methodName, int runNumber) {
        if (methodName.equals(DOEConstants.DOE_ALGORITHM_FULLFACT) && runNumber < 2) {
            final ComponentValidationMessage noInputMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_RUN_NUMBER,
                Messages.numLevelsInvalid, Messages.numLevelsInvalid);
            return Optional.of(noInputMessage);
        }

        return Optional.empty();
    }

    private Optional<ComponentValidationMessage> checkForTooManySamples(int outputCount, String methodName, int runNumber) {

        final ComponentValidationMessage tooManySamplesMessage = new ComponentValidationMessage(
            ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_RUN_NUMBER, Messages.tooManySamples, Messages.tooManySamples);

        if (methodName.equals(DOEConstants.DOE_ALGORITHM_FULLFACT)) {
            if (Math.pow(runNumber, outputCount) >= DOEAlgorithms.MAXMIMAL_RUNS) {
                return Optional.of(tooManySamplesMessage);
            }
        } else {
            if (runNumber >= DOEAlgorithms.MAXMIMAL_RUNS) {
                return Optional.of(tooManySamplesMessage);
            }
        }

        return Optional.empty();
    }

    private Optional<ComponentValidationMessage> checkForRunNumberGreaterZero(int runNumber) {
        if (runNumber == 0) {
            final ComponentValidationMessage noRun = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_RUN_NUMBER, Messages.noRunNumber,
                Messages.noRunNumber);
            return Optional.of(noRun);
        }

        return Optional.empty();
    }

    private Optional<ComponentValidationMessage> checkForOutputs(int outputCount, String methodName) {
        final boolean hasOutputs = (outputCount != 0);

        if (methodName.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE) || methodName.equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO)
            || methodName.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
            if (!hasOutputs) {
                final ComponentValidationMessage outputTooLow = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_METHOD, Messages.minOneOutput,
                    Messages.minOneOutput);
                return Optional.of(outputTooLow);
            }
        } else {
            if (!hasOutputs || outputCount < 2) {
                final ComponentValidationMessage outputTooLow = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_METHOD, Messages.minTwoOutputs,
                    Messages.minTwoOutputs);
                return Optional.of(outputTooLow);
            }
        }

        return Optional.empty();
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
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE, Messages.noTable,
                Messages.noTable);
            messages.add(startSampleErrorMessage);
        } else {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            try {
                Double[][] tableValues = mapper.readValue(table,
                    Double[][].class);
                // The table could be too long rsp. too short, only if a table as input is used. (K.Schaffert, 16.09.2020)
                if (tableValues != null && tableValues.length > 0 && (tableValues[0] != null
                    && tableValues[0].length < getOutputs(componentDescription).size())) {
                    final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE,
                        Messages.tableTooShort, Messages.tableTooShort);
                    messages.add(startSampleErrorMessage);
                }
                if (tableValues != null && tableValues.length > 0
                    && tableValues[0].length > getOutputs(componentDescription).size()) {
                    final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                        ComponentValidationMessage.Type.WARNING, DOEConstants.KEY_TABLE,
                        Messages.tableTooLong, Messages.tableTooLong);
                    messages.add(startSampleErrorMessage);
                }
                final ComponentValidationMessage noValueError = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE,
                    Messages.undefinedValues, Messages.undefinedValues);

                int startSample = Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_START_SAMPLE));
                int endSample = Integer.parseInt(getProperty(componentDescription, DOEConstants.KEY_END_SAMPLE));

                for (int i = startSample; (i <= endSample)
                    && (tableValues != null && i < tableValues.length); i++) {
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
            } catch (IOException e) {
                // should never happen (K.Schaffert, 28.09.2020)
                final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                    ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_TABLE, Messages.cannotReadTable,
                    Messages.cannotReadTable);
                messages.add(startSampleErrorMessage);
            }
        }
    }

    private void checkStartAndEndSample(ComponentDescription componentDescription,
        final Collection<ComponentValidationMessage> messages) {
        String startSampleString = getProperty(componentDescription, DOEConstants.KEY_START_SAMPLE);

        if (startSampleString.isEmpty()) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_START_SAMPLE,
                Messages.noStartSample, Messages.noStartSample);
            messages.add(startSampleErrorMessage);
        }
        String endSampleString = getProperty(componentDescription, DOEConstants.KEY_END_SAMPLE);
        if (endSampleString.isEmpty()) {
            final ComponentValidationMessage startSampleErrorMessage = new ComponentValidationMessage(
                ComponentValidationMessage.Type.ERROR, DOEConstants.KEY_END_SAMPLE, Messages.noEndSample,
                Messages.noEndSample);
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
