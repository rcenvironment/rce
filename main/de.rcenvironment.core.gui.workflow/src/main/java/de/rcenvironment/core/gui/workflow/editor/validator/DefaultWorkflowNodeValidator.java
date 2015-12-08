/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Default implementation of {@link AbstractWorkflowNodeValidator}.
 * 
 * @author Doreen Seider
 */
public class DefaultWorkflowNodeValidator extends AbstractWorkflowNodeValidator {

    @Override
    protected Collection<WorkflowNodeValidationMessage> validate() {
        return new LinkedList<>();
    }
    
}
