/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.ui.views.properties.IPropertySource;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.gui.workflow.view.properties.WorkflowInstancePropertySource;


/**
 * Root edit part holding a WorkflowInformation.
 *
 * @author Heinrich Wendel
 */
public class WorkflowExecutionInformationPart extends AbstractGraphicalEditPart {
    
    @Override
    protected List<WorkflowDescription> getModelChildren() {
        List<WorkflowDescription> child = new ArrayList<WorkflowDescription>();
        child.add(((WorkflowExecutionInformation) getModel()).getWorkflowDescription());
        return child;
    }

    @Override
    protected IFigure createFigure() {
        Figure f = new FreeformLayer();
        f.setLayoutManager(new FreeformLayout());
        return f;
    }
    
    @Override
    protected void createEditPolicies() {
    }
    
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type == IPropertySource.class) {
            return new WorkflowInstancePropertySource(getWorkflowExecutionInformation());
        }
        return super.getAdapter(type);
    }
    
    private WorkflowExecutionInformation getWorkflowExecutionInformation() {
        return (WorkflowExecutionInformation) getModel();
    }
}
