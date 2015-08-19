/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate cluster component configuration.
 *  
 * @author Doreen Seider
 */
public class SshExecutorWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        
        List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();
        
        messages = checkIfStringIsConfigured(messages, SshExecutorConstants.CONFIG_KEY_HOST);
        messages = checkIfStringIsConfigured(messages, SshExecutorConstants.CONFIG_KEY_PORT);
        messages = checkIfStringIsConfigured(messages, SshExecutorConstants.CONFIG_KEY_SANDBOXROOT);
        messages = checkIfStringIsConfigured(messages, SshExecutorConstants.CONFIG_KEY_SCRIPT);            
        
        return messages;
    }
    
    protected List<WorkflowNodeValidationMessage> addMessage(List<WorkflowNodeValidationMessage> messages, String configurationKey) {
        WorkflowNodeValidationMessage validationMessage =
            new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                configurationKey,
                String.format("", configurationKey), 
                Messages.bind(String.format(Messages.errorMissing, configurationKey),
                    configurationKey));
        messages.add(validationMessage);
        return messages;
    }
    
    protected List<WorkflowNodeValidationMessage> checkIfStringIsConfigured(List<WorkflowNodeValidationMessage> messages,
        String configurationKey) {
        final String value = getProperty(configurationKey);
        if (value == null || value.isEmpty()) {
            WorkflowNodeValidationMessage validationMessage =
                new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    configurationKey,
                    String.format(Messages.errorMissing, configurationKey),
                    Messages.bind(String.format(Messages.errorMissing, configurationKey),
                        configurationKey));
            messages.add(validationMessage);
        }
        
        return messages;
    }
    
}
