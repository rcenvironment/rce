/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.integration.workflowintegration.editor;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator for the workflow integration editor input.
 *
 * @author Jan Flink
 */
public final class WorkflowIntegrationEditorInputValidator {
    
    private WorkflowIntegrationEditorInputValidator() {}

    protected static Optional<String> validate(WorkflowIntegrationEditorInput editorInput) {
        SortedSet<String> nonValidComponents = new TreeSet<>();
        
        WorkflowDescription workflowDescription = editorInput.getAdapter(WorkflowDescription.class);
        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            for (EndpointDescription input : node.getInputDescriptionsManager().getEndpointDescriptions()) {
                if (input.getEndpointDefinition() == null) {
                    nonValidComponents.add(node.getComponentDescription().getComponentInterface().getDisplayName());
                }
            }
            for (EndpointDescription output : node.getOutputDescriptionsManager().getEndpointDescriptions()) {
                if (output.getEndpointDefinition() == null) {
                    nonValidComponents.add(node.getComponentDescription().getComponentInterface().getDisplayName());
                }
            }
        }
        if (!nonValidComponents.isEmpty()) {
            return Optional.of(StringUtils.format("Component meta data information missing for the following components:\n\n%s\n\n"
                + "Most likely these components are currently not available at the local RCE instance.",
                String.join("\n", nonValidComponents)));
        }
        return Optional.empty();
    }

}
