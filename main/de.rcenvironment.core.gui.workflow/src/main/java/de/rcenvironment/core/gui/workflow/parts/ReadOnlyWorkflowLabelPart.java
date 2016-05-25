/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import java.beans.PropertyChangeEvent;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Read only part of the {@link WorkflowLabel}.
 * 
 * @author Sascha Zur
 */
public class ReadOnlyWorkflowLabelPart extends WorkflowLabelPart {

    @Override
    protected void createEditPolicies() {}

    @Override
    public void propertyChange(PropertyChangeEvent evt) {}

}
