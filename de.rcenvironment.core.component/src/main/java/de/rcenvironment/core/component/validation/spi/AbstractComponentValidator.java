/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.validation.spi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Contains base implementation of a validator. Validation code that is done for every component should be inside this class.
 * 
 * All validators should extend this class.
 * 
 * @author Jascha Riedel
 *
 */
public abstract class AbstractComponentValidator implements ComponentValidator {

    /**
     * Here should be all validation steps that are specific to the implementing component. (If there are steps only required on workflow
     * start, describe them in validateOnWorkflowStartComponentSepcific(...)). If there are none return null.
     * 
     * @param componentDescription {@link ComponentDescription} of the component instance.
     * @return
     */
    protected abstract List<ComponentValidationMessage> validateComponentSpecific(
        ComponentDescription componentDescription);

    /**
     * Here should be all validation steps that are specific to the implementing component and only required on workflow start. If there are
     * none return null.
     * 
     * @param componentDescription {@link ComponentDescription} of the component instance.
     * @return
     */
    protected abstract List<ComponentValidationMessage> validateOnWorkflowStartComponentSpecific(
        ComponentDescription componentDescription);

    @Override
    public List<ComponentValidationMessage> validate(ComponentDescription componentDescription,
        boolean onWorkflowStart) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        messages.addAll(defaultValidations(componentDescription));

        List<ComponentValidationMessage> componentSpecificMessages = validateComponentSpecific(componentDescription);
        if (componentSpecificMessages != null) {
            messages.addAll(componentSpecificMessages);
        }

        if (onWorkflowStart) {
            messages.addAll(validateOnWorkflowStart(componentDescription));
        }

        return messages;
    }

    protected List<ComponentValidationMessage> validateOnWorkflowStart(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();
        // No default validation steps

        List<ComponentValidationMessage> componentSpecificMessages = validateOnWorkflowStartComponentSpecific(
            componentDescription);
        if (componentSpecificMessages != null) {
            messages.addAll(componentSpecificMessages);
        }

        return messages;
    }

    /**
     * The default validations that are done for every component should be placed here.
     */
    protected List<ComponentValidationMessage> defaultValidations(ComponentDescription componentDescription) {
        List<ComponentValidationMessage> messages = new ArrayList<>();

        messages.addAll(validateInputExecutionConstraints(componentDescription));

        return messages;
    }

    protected List<ComponentValidationMessage> validateInputExecutionConstraints(
        ComponentDescription componentDescription) {
        List<ComponentValidationMessage> m = new LinkedList<>();
        for (EndpointDescription inputEp : getInputs(componentDescription)) {
            EndpointDefinition.InputExecutionContraint exeConstraint = inputEp.getEndpointDefinition()
                .getDefaultInputExecutionConstraint();
            if (inputEp.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                exeConstraint = EndpointDefinition.InputExecutionContraint.valueOf(
                    inputEp.getMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
            }

            if (exeConstraint.equals(EndpointDefinition.InputExecutionContraint.Required) && !inputEp.isConnected()) {
                m.add(new ComponentValidationMessage(ComponentValidationMessage.Type.ERROR, "",
                    StringUtils.format(
                        "Connect input '%s' to an output (of an enabled component) as it is required",
                        inputEp.getName()),
                    StringUtils.format("Input '%s' is required but not connected to an output", inputEp.getName())));
            }
        }
        return m;
    }

    protected Set<EndpointDescription> getInputs(ComponentDescription componentDescription) {
        return componentDescription.getInputDescriptionsManager().getEndpointDescriptions();
    }

    protected Set<EndpointDescription> getInputs(ComponentDescription componentDescription, DataType dataType) {
        Set<EndpointDescription> returnSet = new HashSet<>();
        for (EndpointDescription endpointDescription : componentDescription.getInputDescriptionsManager()
            .getEndpointDescriptions()) {
            if (endpointDescription.getDataType().equals(dataType)) {
                returnSet.add(endpointDescription);
            }
        }
        return returnSet;
    }

    protected boolean hasInputs(ComponentDescription componentDescription) {
        return !componentDescription.getInputDescriptionsManager().getEndpointDescriptions().isEmpty();
    }

    protected Set<EndpointDescription> getOutputs(ComponentDescription componentDescription) {
        return componentDescription.getOutputDescriptionsManager().getEndpointDescriptions();
    }

    protected Set<EndpointDescription> getOutputs(ComponentDescription componentDescription, DataType dataType) {
        Set<EndpointDescription> returnSet = new HashSet<>();
        for (EndpointDescription endpointDescription : componentDescription.getOutputDescriptionsManager()
            .getEndpointDescriptions()) {
            if (endpointDescription.getDataType().equals(dataType)) {
                returnSet.add(endpointDescription);
            }
        }
        return returnSet;
    }

    protected String getProperty(ComponentDescription componentDescription, String property) {
        return componentDescription.getConfigurationDescription().getConfigurationValue(property);
    }

    protected boolean isPropertySet(ComponentDescription componentDescription, String property) {
        return getProperty(componentDescription, property) != null;
    }

}
