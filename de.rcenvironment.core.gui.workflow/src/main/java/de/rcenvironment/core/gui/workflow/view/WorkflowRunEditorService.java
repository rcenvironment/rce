/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.view;

import java.util.Optional;

/**
 * A Facade for querying the status of the currently shown WorkflowRunEditor.
 * 
 * @author Alexander Weinert
 */
public interface WorkflowRunEditorService {
    Optional<WorkflowRunEditor> getCurrentWorkflowRunEditor();
}
