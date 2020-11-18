/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration.workflow.gui;

import de.rcenvironment.core.gui.workflow.editor.properties.ComponentFilter;

public class WorkflowComponentFilter extends ComponentFilter {

    @Override
    public boolean filterComponentName(String componentId) {
        return componentId.startsWith("de.rcenvironment.integration.workflow");
    }

}
