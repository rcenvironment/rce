/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.script.gui;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.executor.SshExecutorConstants;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.gui.workflow.executor.properties.AbstractEditScriptRunnable;

/**
 * {@link WorkflowEditorAction} used to open or edit the underlying script.
 * 
 * @author Doreen Seider
 */
public class EditScriptWorkflowEditorAction extends WorkflowEditorAction {

    @Override
    public void run() {
        new EditScriptRunnable().run();
    }

    /**
     * Implementation of {@link AbstractEditScriptRunnable}.
     * 
     * @author Doreen Seider
     */
    private class EditScriptRunnable extends AbstractEditScriptRunnable {

        @Override
        protected void setScript(String script) {
            commandStack.execute(new EditScriptCommand(script));
        }

        @Override
        protected String getScript() {
            return workflowNode.getConfigurationDescription()
                .getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT);
        }

        @Override
        protected String getScriptName() {
            return Messages.scriptname;
        }
    }

    /**
     * Command to edit the underlying script.
     * 
     * @author Doreen Seider
     */
    private class EditScriptCommand extends Command {

        private String newScript;

        private String oldScript;

        protected EditScriptCommand(String newScript) {
            oldScript =
                workflowNode.getConfigurationDescription()
                    .getConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT);
            this.newScript = newScript;
        }

        @Override
        public void execute() {
            workflowNode.getConfigurationDescription()
                .setConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, newScript);
        }

        @Override
        public void undo() {
            workflowNode.getConfigurationDescription()
                .setConfigurationValue(SshExecutorConstants.CONFIG_KEY_SCRIPT, oldScript);
        }

        @Override
        public void redo() {
            execute();
        }
    }

}
