/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;

/**
 * This page receives the name for the new workflow.
 * 
 * @author Oliver Seebach
 */
final class NewWorkflowPage extends WizardPage {

    private Text workflownameTextfield = null;
    
    public NewWorkflowPage(final NewWorkflowProjectWizard parentWizard, final IStructuredSelection selection){
        super("Workflow");
        setTitle("Workflow");
        setDescription("Please enter the name of the new workflow");
        setPageComplete(false);  
    }
    
    
    @Override
    public void createControl(Composite parent) {
        GridLayout grid = new GridLayout(2, false);
        
        GridData gridDataComp = new GridData();
        gridDataComp.horizontalAlignment = GridData.BEGINNING;
        gridDataComp.verticalAlignment = GridData.BEGINNING;
        
        GridData gridDataLabel = new GridData();
        gridDataLabel.horizontalAlignment = GridData.BEGINNING;
        gridDataLabel.verticalAlignment = GridData.BEGINNING;
        
        GridData gridDataText = new GridData();
        gridDataText.horizontalAlignment = GridData.FILL;
        gridDataText.verticalAlignment = GridData.BEGINNING;
        gridDataText.grabExcessHorizontalSpace = true;
        
        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayout(grid);
        comp.setLayoutData(gridDataComp);

        Label workflownameLabel = new Label(comp, SWT.LEFT);
        workflownameLabel.setLayoutData(gridDataLabel);
        workflownameLabel.setText("Workflow name: ");
        
        setWorkflownameTextfield(new Text(comp, SWT.SINGLE | SWT.BORDER));
        workflownameTextfield.setLayoutData(gridDataText);
        workflownameTextfield.setData("name", "WorkflowNameTextfield");
        workflownameTextfield.setFocus();
        workflownameTextfield.addListener(SWT.Verify, new AlphanumericalTextContraintListener(false, true));
        workflownameTextfield.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (workflownameTextfield.getText().length() > 0){
                    setPageComplete(true);
                } else {
                    setPageComplete(false);
                }
                NewWorkflowProjectWizard.sharedWorkflowName = getWorkflownameTextfield().getText();
            }
        });
        setControl(comp);
    }


    public Text getWorkflownameTextfield() {
        return workflownameTextfield;
    }


    public void setWorkflownameTextfield(Text workflownameTextfield) {
        this.workflownameTextfield = workflownameTextfield;
    }
}
