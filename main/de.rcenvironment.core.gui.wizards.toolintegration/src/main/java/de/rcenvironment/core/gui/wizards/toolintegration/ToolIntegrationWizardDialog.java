/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;

/**
 * Extends the {@link WizardDialog} for adding a "save as" button to the ButtonBar.
 * 
 * @author Sascha Zur
 */
public class ToolIntegrationWizardDialog extends WizardDialog {

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
        if (!isEdit) {
            finishButton = createButton(parent, IDialogConstants.FINISH_ID,
                Messages.integrateLabel, true);
        } else {
            finishButton = createButton(parent, IDialogConstants.FINISH_ID,
                Messages.updateLabel, true);
        }
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
                if (((ToolIntegrationWizard) getWizard()).isConfigOK()) {
                    MessageBox infoDialog = new MessageBox(this.getParentShell(),
                        SWT.ICON_INFORMATION | SWT.OK);
                    infoDialog.setText("Tool integrated");
                    Map<String, Object> configurationMap = ((ToolIntegrationWizard) getWizard())
                        .getConfigurationMap();
                    String groupName = (String) configurationMap
                        .get(ToolIntegrationConstants.KEY_TOOL_GROUPNAME);
                    if (groupName == null || groupName.isEmpty()) {
                        groupName = ToolIntegrationConstants.DEFAULT_COMPONENT_GROUP_ID;
                    }
                    infoDialog.setMessage(String.format("Tool \"%s\" was successfully integrated to group \"%s\".",
                        configurationMap.get(ToolIntegrationConstants.KEY_TOOL_NAME), groupName));
                    infoDialog.open();
                }
                break;
            } else {
                ((ToolIntegrationWizard) getWizard()).removeOldIntegration();
                finishPressed();
                if (((ToolIntegrationWizard) getWizard()).isConfigOK()) {
                    MessageBox infoDialog = new MessageBox(this.getParentShell(),
                        SWT.ICON_INFORMATION | SWT.OK);
                    infoDialog.setText("Tool updated");
                    infoDialog
                        .setMessage(String
                            .format("Tool \"%s\" was successfully updated.",
                                ((ToolIntegrationWizard) getWizard())
                                    .getConfigurationMap()
                                    .get(ToolIntegrationConstants.KEY_TOOL_NAME)));
                    infoDialog.open();
                }
                break;
            }
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
}
