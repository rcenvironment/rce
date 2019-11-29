/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PlatformUI;

/**
 * Handler for the WorkflowProjectWizard.
 * 
 * @author Oliver Seebach
 */
public class ShowNewWorkflowProjectWizardHandler extends AbstractHandler{

    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        //get the selection from the project explorer tree
        ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().
            findView(IPageLayout.ID_PROJECT_EXPLORER).getSite().getSelectionProvider().getSelection();
        
        Shell shell = Display.getCurrent().getActiveShell();
        NewWorkflowProjectWizard wizard = new NewWorkflowProjectWizard(selection);
        WorkflowProjectWizardDialog dialog = new WorkflowProjectWizardDialog(shell, wizard);
        
        dialog.setBlockOnOpen(true);
        dialog.open();

        return null;
    }

    
    /**
     * Dialog controlling the WorkflowProjectWizard.
     * 
     * @author Oliver Seebach
     *
     */
    private static final class WorkflowProjectWizardDialog extends WizardDialog{
        
        
        WorkflowProjectWizardDialog(Shell parentShell, Wizard newWizard) {
            super(parentShell, newWizard);
        }
        
        @Override
        public void create() {
            setShellStyle(SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
            super.create();
        }
        
        @Override
        protected void backPressed() {
            NewWorkflowProjectWizard.preventFinish();
            super.backPressed();
        }
        
        @Override
        protected boolean canHandleShellCloseEvent() {
            return true;
        }
        
        @Override
        protected void cancelPressed() {
            NewWorkflowProjectWizard.preventFinish();
            super.cancelPressed();
        }
    }
    
    
    
}
