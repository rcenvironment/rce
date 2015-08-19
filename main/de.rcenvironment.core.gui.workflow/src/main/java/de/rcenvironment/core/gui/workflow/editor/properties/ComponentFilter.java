/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;


/**
 * Abstract base class to filter for a component identifier.
 *
 * @author Heinrich Wendel
 */
public abstract class ComponentFilter implements IFilter {

    @Override
    public boolean select(final Object object) {
        boolean result = false;
        WorkflowNode workflowNode = null;
        if (object instanceof WorkflowNodePart) {
            workflowNode = (WorkflowNode) ((WorkflowNodePart) object).getModel();
        } else if (object instanceof WorkflowNode) {
            workflowNode = (WorkflowNode) object;
        }
        if (workflowNode != null) {
            final ComponentDescription componentDescription = workflowNode.getComponentDescription();
            result |= filterComponentName(componentDescription.getIdentifier());
            result |= filterComponentDescripton(componentDescription);
        }
        return result;
    }

    /**
     * Must return whether the given component identifier will be accepted.
     * 
     * @param componentId name of the component 
     * @return true, if the component with the given component identifier will be accepted
     */
    public abstract boolean filterComponentName(String componentId);

    /**
     * Must return whether the component with the given {@link ComponentDescription} will be accepted.
     * 
     * @param componentDescription {@link ComponentDescription} of the component 
     * @return true, if the component with the given {@link ComponentDescription} will be accepted
     */
    public boolean filterComponentDescripton(final ComponentDescription componentDescription) {
        return false;
    }

}
