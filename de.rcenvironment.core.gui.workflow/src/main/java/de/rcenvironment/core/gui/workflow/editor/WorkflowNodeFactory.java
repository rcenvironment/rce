/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.requests.CreationFactory;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Factory to create new WorkflowNode objects.
 * 
 * @author Heinrich Wendel
 * @author Sascha Zur
 */
public class WorkflowNodeFactory implements CreationFactory {

    private ComponentInstallation compInstallation;

    public WorkflowNodeFactory(ComponentInstallation compInstallation) {
        this.compInstallation = compInstallation;
    }

    @Override
    public Object getNewObject() {
        ComponentDescription description = new ComponentDescription(compInstallation);
        description.initializeWithDefaults();
        return new WorkflowNode(description);
    }

    @Override
    public Object getObjectType() {
        return WorkflowNode.class;
    }
}
