/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.util.Collection;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.LoopComponentWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for optimizer component.
 * 
 * @author Sascha Zur
 */
public class OptimizerWorkflowNodeValidator extends LoopComponentWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final Collection<WorkflowNodeValidationMessage> messages = super.validate();

        if (getProperty(OptimizerComponentConstants.ALGORITHMS) == null
            || getProperty(OptimizerComponentConstants.ALGORITHMS).isEmpty()) {
            final WorkflowNodeValidationMessage noAlgorithmMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                OptimizerComponentConstants.ALGORITHMS,
                "No Algorithm",
                Messages.noAlgorithmSelected);
            messages.add(noAlgorithmMessage);
        }
        return messages;
    }

}
