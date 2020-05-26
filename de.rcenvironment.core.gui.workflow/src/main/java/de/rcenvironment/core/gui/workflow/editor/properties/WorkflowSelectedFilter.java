/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.gui.workflow.parts.WorkflowExecutionInformationPart;


/**
 * Filter class to display the general property tab for all workflows.
 *
 * @author Doreen Seider
 */
public class WorkflowSelectedFilter implements IFilter {

    @Override
    public boolean select(Object object) {
        return object instanceof WorkflowExecutionInformationPart;
    }

}
