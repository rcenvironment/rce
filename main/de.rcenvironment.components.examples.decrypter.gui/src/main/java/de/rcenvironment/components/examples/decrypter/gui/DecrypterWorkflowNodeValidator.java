/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.decrypter.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.examples.decrypter.common.DecrypterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for Decrypter component. This validator is used for checking if some configuration is set
 * correctly or if inputs/outputs were created. If a validation is false, the GUI will automatically
 * show it by coloring the widget for the configuration red (error) or yellow (warning) and marking
 * the component in the workflow editor. Note that the validator must be set active in the
 * plugin.xml.
 * 
 * @author Sascha Zur
 */
public class DecrypterWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    public DecrypterWorkflowNodeValidator() {}

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {

        final List<WorkflowNodeValidationMessage> messages = new
            LinkedList<WorkflowNodeValidationMessage>();

        // Check is algorithm is selected
        String algorithmSelection = getProperty(DecrypterComponentConstants.CONFIG_KEY_ALGORITHM);
        if (algorithmSelection == null || algorithmSelection.isEmpty()) {
            final WorkflowNodeValidationMessage noAlgorithmSelectedMessage = new
                WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    DecrypterComponentConstants.CONFIG_KEY_ALGORITHM,
                    Messages.noAlgorithmLarge,
                    Messages.noAlgorithmSmall);
            messages.add(noAlgorithmSelectedMessage);
        }
        return messages;
    }
}
