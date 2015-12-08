/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.converger.gui;

import java.util.Collection;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.LoopComponentWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for converger component.
 * 
 * @author Sascha Zur
 */
public class ConvergerWorkflowNodeValidator extends LoopComponentWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final Collection<WorkflowNodeValidationMessage> messages = super.validate();

        final boolean hasInputs = hasInputs();

        if (!hasInputs) {
            final WorkflowNodeValidationMessage noInputMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.WARNING,
                "",
                "No input defined",
                Messages.noInput);
            messages.add(noInputMessage);
        }
        checkIfDefined(ConvergerComponentConstants.KEY_EPS_A, messages);
        checkIfDefined(ConvergerComponentConstants.KEY_EPS_R, messages);
        checkIfDefined(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER, messages);
        return messages;
    }

    private void checkIfDefined(String key, Collection<WorkflowNodeValidationMessage> messages) {
        String prop = getProperty(key);
        
        if (prop == null || prop.isEmpty()) {
            String text = StringUtils.format("'%s' is not defined", getPropertyDisplayName(key));
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, key, text, text));
        }
    }
    
    private String getPropertyDisplayName(String key) {
        if (key.equals(ConvergerComponentConstants.KEY_EPS_A)) {
            return "Absolute convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_EPS_R)) {
            return "Relative convergence";
        } else if (key.equals(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER)) {
            return "Iterations to consider";
        } else {
            return "";
        }
    }
}
