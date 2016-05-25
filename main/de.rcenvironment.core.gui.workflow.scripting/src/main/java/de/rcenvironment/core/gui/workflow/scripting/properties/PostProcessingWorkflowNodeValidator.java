/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.scripting.properties;

import de.rcenvironment.core.utils.scripting.ScriptableComponentConstants.ScriptTime;

/**
 * {@link de.rcenvironment.core.gui.workflow.editor.validator.WorkflowNodeValidator} for workflow nodes with preprocessing.
 *
 * @author Christian Weiss
 */
public class PostProcessingWorkflowNodeValidator extends AbstractProcessingWorkflowNodeValidator {

    public PostProcessingWorkflowNodeValidator() {
        super(ScriptTime.POST);
    }

}
