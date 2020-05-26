/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Extends the {@link WizardDialog} for adding a "save as" button to the ButtonBar.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationWizardDialog extends WizardDialog {

    private static final Object LOCK_OBJECT = new Object();

    protected Button backButton;

    protected Button nextButton;

    protected Button finishButton;

    protected Button cancelButton;

    protected Button saveAsButton;

    private final boolean isEdit;

    public ToolIntegrationWizardDialog(Shell parentShell, IWizard newWizard,
        boolean isEdit) {
        super(parentShell, newWizard);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE
            | getDefaultOrientation());
        this.isEdit = isEdit;
        ((ToolIntegrationWizard) newWizard).setIsEdit(isEdit);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
        if (getWizard().isHelpAvailable()) {
            createButton(parent, IDialogConstants.HELP_ID,
                IDialogConstants.HELP_LABEL, false);
        }
        if (getWizard().needsPreviousAndNextButtons()) {
            backButton = createButton(parent, IDialogConstants.BACK_ID,
                IDialogConstants.BACK_LABEL, true);
            nextButton = createButton(parent, IDialogConstants.NEXT_ID,
                IDialogConstants.NEXT_LABEL, true);
        }
        saveAsButton = createButton(parent, IDialogConstants.OPEN_ID,
            Messages.saveAsLabel, false);
        String buttonText = Messages.integrateLabel;
        if (isEdit) {
            buttonText = Messages.updateLabel;
        }
        finishButton = createButton(parent, IDialogConstants.FINISH_ID,
            buttonText, true);

        cancelButton = createButton(parent, IDialogConstants.CANCEL_ID,
            IDialogConstants.CANCEL_LABEL, true);
    }

    @Override
    public void updateButtons() {
        boolean canFlipToNextPage = false;
        boolean canFinish = true;
        for (IWizardPage p : getWizard().getPages()) {
            canFinish &= p.isPageComplete();
        }
        if (backButton != null) {
            backButton.setEnabled(getCurrentPage().getPreviousPage() != null);
        }
        if (nextButton != null) {
            canFlipToNextPage = getCurrentPage().canFlipToNextPage();
            nextButton.setEnabled(canFlipToNextPage);
            saveAsButton.setEnabled(canFlipToNextPage);
        }
        finishButton.setEnabled(canFinish);
        saveAsButton.setEnabled(canFinish);
        // finish is default unless it is disabled and next is enabled
        if (canFlipToNextPage && !canFinish) {
            getShell().setDefaultButton(nextButton);
        } else {
            getShell().setDefaultButton(finishButton);
        }
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case IDialogConstants.HELP_ID:
            helpPressed();
            break;

        case IDialogConstants.BACK_ID:
            backPressed();
            break;

        case IDialogConstants.NEXT_ID:
            nextPressed();
            break;

        case IDialogConstants.FINISH_ID:
            if (!isEdit) {
                finishPressed();           
                break;
            }
            synchronized (LOCK_OBJECT) {
                ((ToolIntegrationWizard) getWizard()).removeOldIntegration();
                finishPressed();
            }
            break;
        case IDialogConstants.OPEN_ID:
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            String folder = dialog.open();
            if (folder != null) {
                ((ToolIntegrationWizard) getWizard()).performSaveAs(folder);
            }

            break;
        case IDialogConstants.CANCEL_ID:
            cancelPressed();
            break;
        // The Cancel button has a listener which calls cancelPressed
        // directly
        default:

        }
    }

    @Override
    public int open() {
        int returnValue = super.open();
        ((ToolIntegrationWizard) getWizard()).open();
        return returnValue;
    }
}
