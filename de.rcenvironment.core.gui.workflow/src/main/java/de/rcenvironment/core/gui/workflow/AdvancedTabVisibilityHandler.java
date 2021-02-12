/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * Handler to show or hide advanced tab.
 * 
 * @author Oliver Seebach
 */
public class AdvancedTabVisibilityHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        if (!CommandLineArguments.isShowAdvancedTab()) {
            if (AdvancedTabVisibilityHelper.isShowAdvancedTab()) {
                AdvancedTabVisibilityHelper.setShowAdvancedTab(false);
            } else {
                AdvancedTabVisibilityHelper.setShowAdvancedTab(true);
            }
            // refresh selection to trigger property tab update
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            if (activePage.getActiveEditor() instanceof WorkflowEditor){
                WorkflowEditor editor = (WorkflowEditor) activePage.getActiveEditor();
                ISelection currentSelection = editor.getViewer().getSelection();
                // Note: deselect and reselect again causes property tab to flicker once
                if (currentSelection != null){
                    editor.getViewer().deselectAll();
                    editor.getViewer().setSelection(currentSelection);
                }
            }
        }
        return null;
    }

}
