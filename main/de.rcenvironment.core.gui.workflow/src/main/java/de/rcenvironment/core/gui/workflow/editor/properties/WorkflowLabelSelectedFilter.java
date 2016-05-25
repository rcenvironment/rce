/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.gui.workflow.parts.WorkflowLabelPart;

/**
 * Filter class to display the general property tab for all workflow labels.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelSelectedFilter implements IFilter {

    @Override
    public boolean select(Object object) {
        return object instanceof WorkflowLabelPart;
    }

}
