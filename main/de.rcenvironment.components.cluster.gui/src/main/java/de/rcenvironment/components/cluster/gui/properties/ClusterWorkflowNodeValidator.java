/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cluster.gui.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.rcenvironment.components.cluster.common.ClusterComponentConstants;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;
import de.rcenvironment.core.gui.workflow.executor.properties.SshExecutorWorkflowNodeValidator;

/**
 * A {@link AbstractWorkflowNodeValidator} implementation to validate cluster component configuration.
 *  
 * @author Doreen Seider
 */
public class ClusterWorkflowNodeValidator extends SshExecutorWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        
        List<WorkflowNodeValidationMessage> messages = new ArrayList<>();

        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_HOST));
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_PORT));
        messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_SANDBOXROOT));
        if (!Boolean.valueOf(getProperty(ClusterComponentConstants.KEY_IS_SCRIPT_PROVIDED_WITHIN_INPUT_DIR))) {
            messages.addAll(checkIfStringIsConfigured(SshExecutorConstants.CONFIG_KEY_SCRIPT));
        }
        
        return messages;
    }
    
}
