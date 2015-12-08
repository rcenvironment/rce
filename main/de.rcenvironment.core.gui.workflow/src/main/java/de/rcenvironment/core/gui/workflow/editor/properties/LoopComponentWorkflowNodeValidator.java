/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for loop components.
 * 
 * @author Doreen Seider
 */
public class LoopComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        
        List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        
        LoopBehaviorInCaseOfFailure loopBehaviorInCaseOfFailure = LoopBehaviorInCaseOfFailure
            .fromString(getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE));
        
        if (loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndFail)) {
            if (getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_FAIL) == null
                || getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_FAIL).isEmpty()) {
                messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                    LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_FAIL, "Define maximum of reruns", "Maximum of reruns missing"));
            }
        }
        
        if (loopBehaviorInCaseOfFailure.equals(LoopBehaviorInCaseOfFailure.RerunAndDiscard)) {
            if (getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_DISCARD) == null
                || getProperty(LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_DISCARD).isEmpty()) {
                messages.add(new WorkflowNodeValidationMessage(WorkflowNodeValidationMessage.Type.ERROR,
                    LoopComponentConstants.CONFIG_KEY_LOOP_RERUN_DISCARD, "Define maximum of reruns", "Maximum of reruns missing"));
            }
        }
        
        return messages;
    }

}
