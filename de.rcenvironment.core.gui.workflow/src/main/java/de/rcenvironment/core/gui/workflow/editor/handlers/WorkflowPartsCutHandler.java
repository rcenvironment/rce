/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Handles cut part of cut&paste.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Marc Stammerjohann
 */
public class WorkflowPartsCutHandler extends AbstractWorkflowNodeEditHandler {


    @Override
    void edit() {
        WorkflowNodeCopyHandler copyHandler = new WorkflowNodeCopyHandler();
        WorkflowNodeDeleteHandler deleteHandler = new WorkflowNodeDeleteHandler();
        ExecutionEvent event = new ExecutionEvent();

        try {
            copyHandler.execute(event);
            deleteHandler.execute(event);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

}
