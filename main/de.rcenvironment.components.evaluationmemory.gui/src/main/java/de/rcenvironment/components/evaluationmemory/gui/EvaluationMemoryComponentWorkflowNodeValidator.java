/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for Evaluation Memory component.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<>();

        boolean selectAtWfStart = Boolean.valueOf(getProperty(EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START));
        
        if (!selectAtWfStart) { 
            String memoryFile = getProperty(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
            if (memoryFile == null || memoryFile.trim().isEmpty()) {
                final WorkflowNodeValidationMessage noFile =
                    new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR, 
                        EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE,
                        "Define a evaluation memory file", "No memory file given");
                messages.add(noFile);
            }
        }
        checkIfAtLeastInputsOrOutputsAreDefined(messages);
        return messages;
    }
    
    private void checkIfAtLeastInputsOrOutputsAreDefined(List<WorkflowNodeValidationMessage> messages) {
        if (getInputs().size() == 1) {
            messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.WARNING, 
                null, "", "No inputs/outputs defined"));
        }
    }

}
