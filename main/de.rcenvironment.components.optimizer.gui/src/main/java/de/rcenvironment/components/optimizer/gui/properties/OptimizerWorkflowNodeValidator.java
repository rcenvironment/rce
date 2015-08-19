/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for optimizer component.
 * 
 * @author Sascha Zur
 */
public class OptimizerWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();

        if (getProperty(OptimizerComponentConstants.ALGORITHMS) == null
            || ((String) getProperty(OptimizerComponentConstants.ALGORITHMS)).isEmpty()) {
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
