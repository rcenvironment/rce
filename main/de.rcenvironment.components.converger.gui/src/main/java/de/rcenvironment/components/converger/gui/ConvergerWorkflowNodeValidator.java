/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for converger component.
 * 
 * @author Sascha Zur
 */
public class ConvergerWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();

        final boolean hasInputs = hasInputs();

        if (!hasInputs) {
            final WorkflowNodeValidationMessage noInputMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.WARNING,
                "",
                "No input defined",
                Messages.noInput);
            messages.add(noInputMessage);
        }
        checkPropertyFloat(ConvergerComponentConstants.KEY_EPS_A, messages);
        checkPropertyFloat(ConvergerComponentConstants.KEY_EPS_R, messages);
        checkPropertyInteger(ConvergerComponentConstants.KEY_MAX_ITERATIONS, messages);
        return messages;
    }

    private void checkPropertyFloat(String key, List<WorkflowNodeValidationMessage> messages) {
        String stringValue = (String) getProperty(key);
        final WorkflowNodeValidationMessage noFloatValueMessage = new WorkflowNodeValidationMessage(
            WorkflowNodeValidationMessage.Type.ERROR,
            key,
            "Property incorrect",
            Messages.bind(String.format(Messages.propertyIncorrectFloat, getPropertyDisplayName(key)), key));
        try {
            double value = Double.parseDouble(stringValue);
            if (value < 0) {
                final WorkflowNodeValidationMessage lessZeroMessage = new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    key,
                    "Property smaller zero",
                    Messages.bind(String.format(Messages.smallerZero, getPropertyDisplayName(key)), key));
                messages.add(lessZeroMessage);
            }
        } catch (NumberFormatException e) {
            messages.add(noFloatValueMessage);
        } catch (NullPointerException e) {
            messages.add(noFloatValueMessage);
        }

    }

    private void checkPropertyInteger(String key, List<WorkflowNodeValidationMessage> messages) {
        String stringValue = (String) getProperty(key);
        if (stringValue == null || stringValue.isEmpty()) {
            return;
        }
        final WorkflowNodeValidationMessage noIntValueMessage = new WorkflowNodeValidationMessage(
            WorkflowNodeValidationMessage.Type.ERROR,
            key,
            "Property incorrect",
            Messages.bind(String.format(Messages.propertyIncorrectInt, getPropertyDisplayName(key)), key));
        try {
            int value = Integer.parseInt(stringValue);
            if (value <= 0) {
                final WorkflowNodeValidationMessage smallerEqualsZeroMessage = new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    key,
                    "Property smaller or equals zero",
                    Messages.bind(String.format(Messages.smallerEqualsZero, getPropertyDisplayName(key)), key));
                messages.add(smallerEqualsZeroMessage);
            }
        } catch (NumberFormatException e) {
            messages.add(noIntValueMessage);
        } catch (NullPointerException e) {
            messages.add(noIntValueMessage);
        }

    }
    
    private String getPropertyDisplayName(String key) {
        if (key.equals(ConvergerComponentConstants.KEY_EPS_A)) {
            return "absolute convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_EPS_R)) {
            return "relative convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_MAX_ITERATIONS)) {
            return "maximum iterations";
        } else {
            return "";
        }
    }
}
