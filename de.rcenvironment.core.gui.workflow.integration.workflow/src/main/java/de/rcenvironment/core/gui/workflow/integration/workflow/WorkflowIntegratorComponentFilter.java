/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration.workflow;

import de.rcenvironment.core.component.integration.workflow.WorkflowIntegrationConstants;
import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

/**
 * Filter for workflow integrator components.
 * 
 * @author Jan Flink
 */
public class WorkflowIntegratorComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        boolean isMatch = false;
        for (String id : WorkflowIntegrationConstants.COMPONENT_IDS) {
            if (componentId.startsWith(id)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

}
