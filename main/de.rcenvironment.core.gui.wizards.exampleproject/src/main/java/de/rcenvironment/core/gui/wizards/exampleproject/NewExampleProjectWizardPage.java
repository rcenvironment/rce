/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.wizards.exampleproject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The wizard GUI page. The only input element is a field for choosing a project name.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 */
public class NewExampleProjectWizardPage extends WizardPage {

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private Text textFieldProjectName;

    private NewExampleProjectWizard newExampleProjectWizard;

    private Button createExampleIntegratedToolButton;

    /**
     * Constructor for SampleNewWizardPage.
     * 
     * @param newExampleProjectWizard
     * 
     * @param pageName
     */
    public NewExampleProjectWizardPage(ISelection selection, NewExampleProjectWizard newExampleProjectWizard) {
        super("wizardPage");
        setTitle("Create Workflow Examples Project");
        setDescription("This wizard generates an example project containing a workflow template.");
        this.newExampleProjectWizard = newExampleProjectWizard;
    }

    /**
     * @see IDialogPage#createControl(Composite)
     * @param parent :
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        layout.verticalSpacing = 9;
        Label label = new Label(container, SWT.NULL);
        label.setText("Project &Name:");

        textFieldProjectName = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        textFieldProjectName.setLayoutData(gd);
        textFieldProjectName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        createExampleIntegratedToolButton = new Button(container, SWT.CHECK);
        createExampleIntegratedToolButton.setText("Integrate example tool");
        GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
        gd2.horizontalSpan = 2;
        createExampleIntegratedToolButton.setLayoutData(gd2);

        initialize();
        dialogChanged();
        setControl(container);
        getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
    }

    private void initialize() {
        textFieldProjectName.setText(newExampleProjectWizard.getProjectDefaultName());
    }

    /**
     * Ensures that both text fields are set.
     */

    private void dialogChanged() {
        final String newProjectName = textFieldProjectName.getText();
        if (newProjectName.length() == 0) {
            updateStatus("Please chose a name for the new project");
            return;
        }

        IProject existingProject = ResourcesPlugin.getWorkspace().getRoot()
            .getProject(newProjectName);
        if (existingProject != null && existingProject.exists()) {
            updateStatus("This project name is already in use");
            return;
        }

        // FIXME validate name structure

        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public String getNewProjectName() {
        return textFieldProjectName.getText();
    }

    public boolean getCreateTIExample() {
        return createExampleIntegratedToolButton.getSelection();
    }

}
