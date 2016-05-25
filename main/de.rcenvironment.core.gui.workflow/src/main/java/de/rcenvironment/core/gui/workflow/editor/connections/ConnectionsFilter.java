/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import org.eclipse.jface.viewers.IFilter;

import de.rcenvironment.core.gui.workflow.parts.ConnectionPart;
import de.rcenvironment.core.gui.workflow.parts.WorkflowPart;

/**
 * Filter for the connection editor section.
 * 
 * @author Oliver Seebach
 *
 */
public class ConnectionsFilter implements IFilter {

    @Override
    public boolean select(Object object) {
        return (object instanceof WorkflowPart || object instanceof ConnectionPart);
    }

}
