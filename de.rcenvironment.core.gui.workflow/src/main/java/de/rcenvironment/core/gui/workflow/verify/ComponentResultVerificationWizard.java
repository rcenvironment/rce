/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.verify;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Wizard page for component result verification.
 * 
 * @author Doreen Seider
 */
public class ComponentResultVerificationWizard extends Wizard {

    private ComponentResultVerificationInfoWizardPage decisionPage;

    private ComponentExecutionService componentExecutionService;
    
    public ComponentResultVerificationWizard() {
        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        componentExecutionService = registryAccess.getService(ComponentExecutionService.class);
        setWindowTitle("Tool Result Verification");
    }

    @Override
    public void addPages() {
        addPage(new ComponentResultVerificationTokenWizardPage());
        decisionPage = new ComponentResultVerificationInfoWizardPage();
        addPage(decisionPage);
    }

    @Override
    public boolean performCancel() {
        decisionPage.setVerificationToken(null);
        return super.performCancel();
    }

    @Override
    public boolean performFinish() {

        final String verificationToken = decisionPage.getVerificationToken();
        final ComponentExecutionInformation compExeInfo = decisionPage.getComponentExecutionInformation();
        final boolean verificationResult = decisionPage.getVerificationResult();

        Job job = new Job("Send verification results for tool run") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    boolean successfullyApplied =
                        componentExecutionService.verifyResults(compExeInfo.getExecutionIdentifier(), compExeInfo.getNodeId(),
                            verificationToken, verificationResult);
                    if (!successfullyApplied) {
                        openErrorDialog(compExeInfo);
                    }
                } catch (RemoteOperationException | ExecutionControllerException e) {
                    LogFactory.getLog(ComponentResultVerificationWizard.class)
                        .error(StringUtils.format("Error when sending verification results for component '%s' (%s) of workflow '%s' (%s)",
                            compExeInfo.getInstanceName(), compExeInfo.getExecutionIdentifier(), compExeInfo.getWorkflowInstanceName(),
                            compExeInfo.getWorkflowExecutionIdentifier()), e);
                    if (e instanceof RemoteOperationException) {
                        openErrorDialog((RemoteOperationException) e, compExeInfo);
                    } else {
                        openErrorDialog(compExeInfo);
                    }
                }
                return Status.OK_STATUS;
            }
        };

        job.setUser(true);
        job.schedule();

        return true;
    }

    private void openErrorDialog(ComponentExecutionInformation compExeInfo) {
        openErrorDialog(null, compExeInfo);
    }

    private void openErrorDialog(final RemoteOperationException e, final ComponentExecutionInformation compExeInfo) {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                String errorMessage;
                if (e != null) {
                    errorMessage = StringUtils.format("Failed to send verification results for tool '%s' to '%s'.\n\nReason: %s",
                        compExeInfo.getInstanceName(), compExeInfo.getNodeId().getAssociatedDisplayName(), e.getMessage());
                } else {
                    errorMessage = StringUtils.format("Failed to send verification results for tool '%s' to '%s'. "
                        + "Most likely reason: In the meantime, verification results are already sent by someone else.",
                        compExeInfo.getInstanceName(), compExeInfo.getNodeId().getAssociatedDisplayName());
                }
                MessageDialog.openError(ComponentResultVerificationWizard.this.getShell(), "Error Sending Verification Results",
                    errorMessage);
            }
        });
    }
}
