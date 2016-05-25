/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.script.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.components.script.common.ScriptComponentConstants;
import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.workflow.editor.validator.AbstractWorkflowNodeValidator;
import de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidationMessage;

/**
 * Validator for script component.
 * 
 * @author Sascha Zur
 */
public class ScriptComponentWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {

        final List<WorkflowNodeValidationMessage> messages = new LinkedList<WorkflowNodeValidationMessage>();

        String script = getProperty(SshExecutorConstants.CONFIG_KEY_SCRIPT);
        if (script == null || script.isEmpty()) {
            final WorkflowNodeValidationMessage noScriptMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.ERROR,
                SshExecutorConstants.CONFIG_KEY_SCRIPT,
                Messages.noScript,
                Messages.bind(Messages.noScriptMessage, SshExecutorConstants.CONFIG_KEY_SCRIPT));
            messages.add(noScriptMessage);
        } else if (script.endsWith(ScriptComponentConstants.DEFAULT_SCRIPT_LAST_LINE)) {
            final WorkflowNodeValidationMessage defaultScriptMessage = new WorkflowNodeValidationMessage(
                WorkflowNodeValidationMessage.Type.WARNING,
                SshExecutorConstants.CONFIG_KEY_SCRIPT,
                Messages.defaultScript,
                Messages.bind(Messages.defaultScriptMessage, SshExecutorConstants.CONFIG_KEY_SCRIPT));
            messages.add(defaultScriptMessage);
        }
        return messages;
    }

}
