/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.examples.encrypter.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.examples.encrypter.common.EncrypterComponentConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for encoder component. This validator is used for checking if some configuration is set
 * correctly or of inputs/outputs were created. If a validation is false, the GUI will automatically
 * show it by coloring the widget for the configuration red (error) or yellow (warning) and marking
 * the component in the workflow editor. Note that the vaidator must be set active in the
 * plugin.xml.
 * 
 * @author Sascha Zur
 */
public class EncrypterWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    public EncrypterWorkflowNodeValidator() {}

    /**
     * 
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator#validate()
     */
    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {

        final List<WorkflowNodeValidationMessage> messages = new
            LinkedList<WorkflowNodeValidationMessage>();

        // Check is algorithm is selected
        String algorithmSelection = getProperty(EncrypterComponentConstants.CONFIG_KEY_ALGORITHM);
        if (algorithmSelection == null || algorithmSelection.isEmpty()) {
            final WorkflowNodeValidationMessage noAlgorithmSelectedMessage = new
                WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    EncrypterComponentConstants.CONFIG_KEY_ALGORITHM,
                    Messages.noAlgorithmLarge,
                    Messages.noAlgorithmSmall);
            messages.add(noAlgorithmSelectedMessage);
        }
        return messages;
    }
}
