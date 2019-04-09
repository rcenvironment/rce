/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;

/**
 * Wizard to create a new workflow file.
 * 
 * @author Heinrich Wendel
 */
public class NewWorkflowWizard extends Wizard implements INewWizard {

    /** The current selection in the navigator. */
    private IStructuredSelection selection;
    
    /** Content of the wizard. */
    private NewWorkflowPage mainPage;

    @Override
    public boolean performFinish() {
        IFile file = mainPage.createNewFile();
        WorkflowDescription wd = new WorkflowDescription(UUID.randomUUID().toString());
        WorkflowDescriptionPersistenceHandler wdHandler = new WorkflowDescriptionPersistenceHandler();
        try {
            file.setContents(new ByteArrayInputStream(wdHandler.writeWorkflowDescriptionToStream(wd).toByteArray()), 0, null);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file != null;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        selection = currentSelection;
        setWindowTitle(Messages.newWorkflow);
    }

    @Override
    public void addPages() {
        mainPage = new NewWorkflowPage(selection);
        addPage(mainPage);
    }
    
    /**
     * Content of the new Workflow wizard.
     *
     * @author Heinrich Wendel
     */
    class NewWorkflowPage extends WizardNewFileCreationPage {

        /**
         * Constructor.
         * @param selection The current selection in the navigator.
         */
        NewWorkflowPage(IStructuredSelection selection) {
            super("NewWorkflowPage", selection); //$NON-NLS-1$
            setFileExtension("wf"); //$NON-NLS-1$
            setDescription(Messages.createWorkflow);
            setTitle(Messages.fileWorkflow);
        }
    }
}
