/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.gef.requests.CreationFactory;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Factory to create new {@link WorkflowLabel} objects.
 * 
 * @author Sascha Zur
 */
public class LabelFactory implements CreationFactory {

    @Override
    public Object getNewObject() {
        return new WorkflowLabel(WorkflowLabel.INITIAL_TEXT);
    }

    @Override
    public Object getObjectType() {
        return WorkflowLabel.class;
    }

}
