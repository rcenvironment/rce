/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.verify;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Wizard page to enter the component verification token.
 * 
 * @author Doreen Seider
 */
public class ComponentResultVerificationInfoWizardPage extends WizardPage {

    private static final String RETRIEVING = "[Fetching...]";

    private static final String UNKNOWN = "[Unknown]";

    private static final String TITLE = "Tool Result Verification";

    private String verificationToken = null;

    private Label compNameLabel;

    private Label wfNameLabel;

    private Label compHostLabel;

    private Label wfHostLabel;

    private AsyncTaskService asyncTaskService;

    private ComponentExecutionService componentExecutionService;

    private Button verifyButton;

    private Button denyButton;

    private Display display;

    private ComponentExecutionInformation compExeInfo = null;

    protected ComponentResultVerificationInfoWizardPage() {
        super(TITLE);
        setTitle(TITLE);

        ServiceRegistryAccess registryAccess = ServiceRegistry.createAccessFor(this);
        asyncTaskService = registryAccess.getService(AsyncTaskService.class);
        componentExecutionService = registryAccess.getService(ComponentExecutionService.class);
    }

    @Override
    public void createControl(Composite parent) {

        display = parent.getDisplay();

        Composite content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(2, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(layoutData);

        Label compNameLeftLabel = new Label(content, SWT.NONE);
        compNameLeftLabel.setText("Tool:");

        compNameLabel = new Label(content, SWT.NONE);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        compNameLabel.setLayoutData(layoutData);

        Label compHostLeftLabel = new Label(content, SWT.NONE);
        compHostLeftLabel.setText("Tool host machine:");

        compHostLabel = new Label(content, SWT.NONE);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        compHostLabel.setLayoutData(layoutData);

        Label wfNameLeftLabel = new Label(content, SWT.NONE);
        wfNameLeftLabel.setText("Workflow:");

        wfNameLabel = new Label(content, SWT.NONE);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        wfNameLabel.setLayoutData(layoutData);

        Label wfHostLeftLabel = new Label(content, SWT.NONE);
        wfHostLeftLabel.setText("Workflow host machine:");

        wfHostLabel = new Label(content, SWT.NONE);
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        wfHostLabel.setLayoutData(layoutData);

        Label separator = new Label(content, SWT.HORIZONTAL | SWT.SEPARATOR);
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.horizontalSpan = 2;
        separator.setLayoutData(layoutData);

        Composite decisionComposite = new Composite(content, SWT.NONE);
        decisionComposite.setLayout(new GridLayout(2, true));
        layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        layoutData.horizontalSpan = 2;
        decisionComposite.setLayoutData(layoutData);

        verifyButton = new Button(decisionComposite, SWT.RADIO);
        verifyButton.setText("Approve tool results");
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        layoutData.horizontalAlignment = SWT.RIGHT;
        verifyButton.setLayoutData(layoutData);

        verifyButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                validateInput();
            }
        });

        denyButton = new Button(decisionComposite, SWT.RADIO);
        denyButton.setText("Reject tool results");
        layoutData = new GridData(SWT.FILL, SWT.NONE, true, false);
        layoutData.horizontalAlignment = SWT.LEFT;
        denyButton.setLayoutData(layoutData);

        denyButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                widgetDefaultSelected(event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                validateInput();
            }
        });

        setControl(content);

        setComponentInfos();
        validateInput();
    }

    private void setComponentInfos() {
        if (compExeInfo == null) {
            compNameLabel.setText(UNKNOWN);
            wfNameLabel.setText(UNKNOWN);
            compHostLabel.setText(UNKNOWN);
            wfHostLabel.setText(UNKNOWN);
        } else {
            compNameLabel.setText(compExeInfo.getInstanceName());
            wfNameLabel.setText(compExeInfo.getWorkflowInstanceName());
            compHostLabel.setText(compExeInfo.getNodeId().getAssociatedDisplayName());
            wfHostLabel.setText(compExeInfo.getWorkflowNodeId().getAssociatedDisplayName());
        }
    }

    private void clearComponentInfos() {
        compNameLabel.setText(RETRIEVING);
        wfNameLabel.setText(RETRIEVING);
        compHostLabel.setText(RETRIEVING);
        wfHostLabel.setText(RETRIEVING);
    }

    private void setupDecisionButtons(boolean enable) {
        verifyButton.setEnabled(enable);
        denyButton.setEnabled(enable);

        if (!enable) {
            verifyButton.setSelection(enable);
            denyButton.setSelection(enable);
        }
    }

    private void validateInput(String errorMessage) {
        setErrorMessage(errorMessage);
        setPageComplete(false);
        setupDecisionButtons(false);
    }

    private void validateInput() {
        if (compNameLabel.getText().toString().equals(UNKNOWN)) {
            setErrorMessage("No tool run found that belongs to key: " + verificationToken);
            setPageComplete(false);
            setupDecisionButtons(false);
        } else if (compNameLabel.getText().toString().equals(RETRIEVING)) {
            setErrorMessage(null);
            setDescription("Verify results of tool run (as soon as information fetched)");
            setPageComplete(false);
            setupDecisionButtons(false);
        } else if (!verifyButton.getSelection() && !denyButton.getSelection()) {
            setErrorMessage("Decide whether to confirm or deny the tool results");
            setPageComplete(false);
            setupDecisionButtons(true);
        } else {
            setErrorMessage(null);
            setDescription("Verify results of tool run");
            setPageComplete(true);
            setupDecisionButtons(true);
        }
    }

    protected void setVerificationToken(final String verificationToken) {

        if (verificationToken != null && (this.verificationToken == null || !this.verificationToken.equals(verificationToken))) {

            this.verificationToken = verificationToken;
            clearComponentInfos();
            validateInput();

            asyncTaskService.submit(new Runnable() {

                @TaskDescription("Fetch component information for result verification")
                @Override
                public void run() {
                    try {
                        final ComponentExecutionInformation info =
                            componentExecutionService.getComponentExecutionInformation(verificationToken);

                        display.asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                if (ComponentResultVerificationInfoWizardPage.this.verificationToken != null
                                    && ComponentResultVerificationInfoWizardPage.this.verificationToken.equals(verificationToken)) {
                                    ComponentResultVerificationInfoWizardPage.this.compExeInfo = info;
                                    setComponentInfos();
                                    validateInput();
                                }
                            }
                        });
                    } catch (final RemoteOperationException e) {
                        LogFactory.getLog(getClass()).error("Failed to fetch information about component execution: " + e.getMessage());
                        display.asyncExec(new Runnable() {

                            @Override
                            public void run() {
                                setComponentInfos();
                                validateInput("Failed to find tool run: " + e.getMessage());
                            }
                        });
                    }
                }
            });
        }
    }

    protected String getVerificationToken() {
        return this.verificationToken;
    }

    protected boolean getVerificationResult() {
        return verifyButton.getSelection();
    }

    protected ComponentExecutionInformation getComponentExecutionInformation() {
        return this.compExeInfo;
    }

}
