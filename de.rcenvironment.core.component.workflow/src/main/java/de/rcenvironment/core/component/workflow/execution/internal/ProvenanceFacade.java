/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.provenance.api.ProvenanceEventListener;

class ProvenanceFacade {

    private final Optional<ProvenanceEventListener> service;

    private final Map<String, String> displayNamesByComponentExecutionId = new HashMap<>();

    private WorkflowExecutionContext executionContext;

    ProvenanceFacade(Optional<ProvenanceEventListener> service) {
        this.service = service;
    }

    public void onWorkflowStart(WorkflowDescription workflowDescription, WorkflowExecutionContext executionContext) {
        this.executionContext = executionContext;

        service.ifPresent(
            actualService -> actualService.workflowRunStarted(executionContext.getExecutionIdentifier(), workflowDescription.getFileName(),
                workflowDescription.getControllerNode().getAssociatedDisplayName(), workflowDescription.getAdditionalInformation()));
    }

    public void onWorkflowFinish() {
        service.ifPresent(actualService -> actualService.workflowRunFinished(executionContext.getExecutionIdentifier()));
    }

    public synchronized void onComponentInit(String executionId, String displayName) {
        this.displayNamesByComponentExecutionId.put(executionId, displayName.replace(' ', '_'));
    }
}
