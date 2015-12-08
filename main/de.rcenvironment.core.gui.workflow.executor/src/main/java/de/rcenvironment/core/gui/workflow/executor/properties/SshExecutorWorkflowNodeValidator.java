/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate cluster component configuration.
 *  
 * @author Doreen Seider
 */
public class SshExecutorWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<>();
        
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_HOST));
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_PORT));
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT));
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_SCRIPT));
        
        return messages;
    }
    
    protected List<WorkflowNodeValidationMessage> addMessage(List<WorkflowNodeValidationMessage> messages, String configurationKey) {
        WorkflowNodeValidationMessage validationMessage =
            new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                configurationKey,
                StringUtils.format("", configurationKey), 
                Messages.bind(StringUtils.format(Messages.errorMissing, configurationKey),
                    configurationKey));
        messages.add(validationMessage);
        return messages;
    }
    
    protected List<WorkflowNodeValidationMessage> checkIfStringIsConfigured(String configurationKey) {
        final List<WorkflowNodeValidationMessage> messages = new LinkedList<>();

        final String value = getProperty(configurationKey);
        if (value == null || value.isEmpty()) {
            WorkflowNodeValidationMessage validationMessage =
                new WorkflowNodeValidationMessage(
                    WorkflowNodeValidationMessage.Type.ERROR,
                    configurationKey,
                    StringUtils.format(Messages.errorMissing, configurationKey),
                    Messages.bind(StringUtils.format(Messages.errorMissing, configurationKey),
                        configurationKey));
            messages.add(validationMessage);
        }
        
        return messages;
    }
    
}
