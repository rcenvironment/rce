package de.rcenvironment.core.component.workflow.api;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

public class MockWorkflowDescriptionBuilder {
    
    public static WorkflowDescription workflowDescription(String identifier) {
        return new WorkflowDescription(identifier);
    }

}
