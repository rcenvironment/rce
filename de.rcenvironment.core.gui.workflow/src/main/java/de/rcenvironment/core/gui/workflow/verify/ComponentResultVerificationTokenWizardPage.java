/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.verify;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard page to enter the component verification token.
 * 
 * @author Doreen Seider
 */
public class ComponentResultVerificationTokenWizardPage extends WizardPage {

    private static final String TITLE = "Verification Key";

    private Text verificationTokenText;

    protected ComponentResultVerificationTokenWizardPage() {
        super(TITLE);
        setTitle(TITLE);
        setDescription("Enter the verfication key related to the tool results to verify");
    }

    @Override
    public void createControl(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        content.setLayout(new GridLayout(1, false));
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(layoutData);

        verificationTokenText = new Text(content, SWT.BORDER);
        verificationTokenText.setMessage("Enter verification key");
        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        verificationTokenText.setLayoutData(layoutData);

        verificationTokenText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent event) {
                validateUserInput();
            }
        });

        setControl(content);

        validateUserInput();
    }

    private void validateUserInput() {
        if (verificationTokenText.getText().isEmpty()) {
            setErrorMessage(
                "Enter verification key related to tool results to verify (retrieve from email or file)");
            setPageComplete(false);
        } else {
            setErrorMessage(null);
            setPageComplete(true);
        }
    }
    
    protected String getVerificationToken() {
        return verificationTokenText.getText().toString();
    }

}
