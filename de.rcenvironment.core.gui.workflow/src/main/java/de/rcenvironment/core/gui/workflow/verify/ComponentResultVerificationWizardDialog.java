/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.verify;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * {@link WizardDialog} for {@link ComponentResultVerificationWizard}.
 * 
 * @author Doreen Seider
 */
public class ComponentResultVerificationWizardDialog extends WizardDialog {

    public ComponentResultVerificationWizardDialog(Shell parentShell, IWizard newWizard) {
        super(parentShell, newWizard);
        setTitle("Tool Result Verification");
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation());
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
        createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, true);
        createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, true);
        createButton(parent, IDialogConstants.FINISH_ID, IDialogConstants.FINISH_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }

    @Override
    public void updateButtons() {
        boolean canFinish = true;
        for (IWizardPage page : getWizard().getPages()) {
            canFinish &= page.isPageComplete();
        }
        boolean canFlipToNextPage = getCurrentPage().canFlipToNextPage();
        
        getButton(IDialogConstants.BACK_ID).setEnabled(getCurrentPage().getPreviousPage() != null);
        getButton(IDialogConstants.NEXT_ID).setEnabled(canFlipToNextPage);
        getButton(IDialogConstants.FINISH_ID).setEnabled(canFinish && !canFlipToNextPage);

        if (canFlipToNextPage && !canFinish) {
            getShell().setDefaultButton(getButton(IDialogConstants.NEXT_ID));
        } else {
            getShell().setDefaultButton(getButton(IDialogConstants.FINISH_ID));
        }
    }
    
    @Override
    protected void nextPressed() {
        // assume a certain constellation of pages that breaks if a new page is added, but as not likely right now, a more generic approach
        // is not applied from the beginning
        ((ComponentResultVerificationInfoWizardPage) getCurrentPage().getNextPage())
            .setVerificationToken(((ComponentResultVerificationTokenWizardPage) getCurrentPage()).getVerificationToken());
        super.nextPressed();
    }
    
    @Override
    protected void buttonPressed(int buttonId) {
        // Don't know why this doesn't work by default
        if (buttonId == IDialogConstants.CANCEL_ID) {
            cancelPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

}
