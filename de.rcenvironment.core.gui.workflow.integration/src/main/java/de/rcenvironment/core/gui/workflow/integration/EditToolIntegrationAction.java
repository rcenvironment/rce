/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.gui.wizards.toolintegration.ShowIntegrationEditWizardHandler;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Action that allows the user to edit the integration of a tool directly from its context menu in the workflow editor.
 * 
 * @author Alexander Weinert
 */
public class EditToolIntegrationAction extends WorkflowEditorAction {

    @Override
    public void run() {
        try {
            new ShowIntegrationEditWizardHandler(workflowNode.getName()).execute(new ExecutionEvent());
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).error("Opening Tool Edit wizard failed", e);
        }
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }

        final DistributedComponentKnowledgeService service =
            ServiceRegistry.createAccessFor(this).getService(DistributedComponentKnowledgeService.class);
        return service.getCurrentSnapshot().getAllLocalInstallations().stream()
            .anyMatch(installation -> installation.getComponentInterface().getIdentifierAndVersion()
                .equals(workflowNode.getComponentIdentifierWithVersion()));
    }
}
