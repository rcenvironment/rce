/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for output writer component.
 * 
 * @author Sascha Zur
 */
public class OutputWriterComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {

        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        String chooseAtStart = getProperty(OutputWriterComponentConstants.CONFIG_KEY_ONWFSTART);
        if (!Boolean.parseBoolean(chooseAtStart) && getProperty(OutputWriterComponentConstants.CONFIG_KEY_ROOT).isEmpty()) {
            final WorkflowNodeValidationMessage noDirectory = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                OutputWriterComponentConstants.CONFIG_KEY_ROOT,
                Messages.noRootChosen,
                Messages.bind(Messages.noRootChosen, OutputWriterComponentConstants.CONFIG_KEY_ROOT));
            messages.add(noDirectory);
        }

        return messages;
    }

}
