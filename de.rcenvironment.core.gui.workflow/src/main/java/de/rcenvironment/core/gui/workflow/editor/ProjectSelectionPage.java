/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;

/**
 * This page handles the association of a new workflow with a project. The workflow can be placed in an existing project or in a project
 * that is created on the fly.
 * 
 * @author Oliver Seebach
 */
public class ProjectSelectionPage extends WizardPage {

    private static final char[] FORBIDDEN_CHARS = new char[] { '/', '\\', ':',
        '*', '?', '\"', '>', '<', '|' };

    private static final int TEXT_WIDTH = 100;

    private static final int TREE_HEIGHT = 200;

    private static final int TREE_WIDTH = 100;

    private static final int LIST_HEIGHT = 200;

    private static final int PANEL_HEIGHT = 250;

    private static final int DIALOGWINDOWIDTH = 500;

    private static final int BORDERSIZE = 5;

    /**
     * The project custom name. Public for extending classes.
     */
    public Text projectNameTextField;

    protected Button useDefaultNameButton;

    protected String workflowName;

    protected ModifyListener projectNameTextFieldModifyListener;

    protected TreeViewer projectTreeViewer;

    private ProjectUsages usage;

    private IStructuredSelection selection;

    private Button newProjectRadioButton;

    private Button existingProjectRadioButton;

    public ProjectSelectionPage(final Wizard parentWizard, final IStructuredSelection selection) {
        super("Project");
        this.selection = selection;
        setTitle("Project");
        setDescription("Place the workflow in a project");
    }

    protected void dialogChanged() {
        final String newProjectName = getProjectNameTextField().getText();
        if (newProjectName.length() == 0
            && newProjectRadioButton.getSelection()) {
            updateStatus("Please chose a name for the new project");
            return;
        }

        if (newProjectName.length() > 0) {
            IProject existingProject = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(newProjectName);
            if (existingProject != null && existingProject.exists()
                && newProjectRadioButton.getSelection()) {
                updateStatus("This project name is already in use");
                return;
            }
        }

        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        if (message == null) {
            NewWorkflowProjectWizard.allowFinish();
        } else {
            NewWorkflowProjectWizard.preventFinish();
        }
        setPageComplete(message == null);
    }

    private void createExistingProjectPanel(Composite parent) {
        // ------------------
        // EXISTING PROJECT STUFF
        // ------------------

        GridLayout existingGrid = new GridLayout(1, false);
        // composite
        GridData existingGridDataComp = new GridData();
        existingGridDataComp.widthHint = (DIALOGWINDOWIDTH / 2) - BORDERSIZE;
        existingGridDataComp.heightHint = PANEL_HEIGHT;
        existingGridDataComp.verticalAlignment = GridData.BEGINNING;
        existingGridDataComp.horizontalAlignment = GridData.BEGINNING;
        existingGridDataComp.grabExcessVerticalSpace = true;
        // radiobutton
        GridData existingGridDataRadio = new GridData();
        existingGridDataRadio.widthHint = (DIALOGWINDOWIDTH / 2) - 2
            * BORDERSIZE;
        existingGridDataRadio.verticalAlignment = GridData.BEGINNING;
        existingGridDataRadio.horizontalAlignment = GridData.BEGINNING;
        existingGridDataRadio.grabExcessVerticalSpace = true;
        // list
        GridData existingGridDataList = new GridData();
        existingGridDataList.widthHint = (DIALOGWINDOWIDTH / 2) - 7
            * BORDERSIZE;
        existingGridDataList.heightHint = LIST_HEIGHT;
        existingGridDataList.verticalAlignment = GridData.BEGINNING;
        existingGridDataList.horizontalAlignment = GridData.BEGINNING;
        existingGridDataList.grabExcessVerticalSpace = true;
        existingGridDataList.grabExcessHorizontalSpace = true;

        Composite existingProjectComposite = new Composite(parent, SWT.BORDER);
        existingProjectComposite.setLayout(existingGrid);
        existingProjectComposite.setLayoutData(existingGridDataComp);

        setExistingProjectRadioButton(new Button(existingProjectComposite,
            SWT.RADIO));
        getExistingProjectRadioButton().setText("Place in existing project");
        getExistingProjectRadioButton().setVisible(true);
        getExistingProjectRadioButton().setLayoutData(existingGridDataRadio);

        projectTreeViewer = new TreeViewer(existingProjectComposite, SWT.SINGLE
            | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        projectTreeViewer.setContentProvider(new ProjectTreeContentProvider());
        projectTreeViewer.setLabelProvider(new ProjectTreeLabelProvider());

        projectTreeViewer.setInput(File.listRoots());
        projectTreeViewer.getTree().setSize(TREE_WIDTH, TREE_HEIGHT);
        projectTreeViewer.getTree().setLayoutData(existingGridDataList);

        setControl(existingProjectComposite);
    }

    private void createNewProjectPanel(Composite parent) {
        // ------------------
        // NEW PROJECT STUFF
        // ------------------

        GridLayout newGrid = new GridLayout(1, false);
        GridData newGridDataComp = new GridData();
        newGridDataComp.widthHint = (DIALOGWINDOWIDTH / 2) - BORDERSIZE;
        newGridDataComp.heightHint = PANEL_HEIGHT;
        newGridDataComp.verticalAlignment = GridData.BEGINNING;
        newGridDataComp.horizontalAlignment = GridData.BEGINNING;
        newGridDataComp.grabExcessVerticalSpace = false;

        GridData newGridDataRadio = new GridData();
        newGridDataRadio.widthHint = (DIALOGWINDOWIDTH / 2) - BORDERSIZE;
        newGridDataRadio.verticalAlignment = GridData.BEGINNING;
        newGridDataRadio.horizontalAlignment = GridData.BEGINNING;

        GridData newGridDataCheck = new GridData();
        newGridDataCheck.widthHint = (DIALOGWINDOWIDTH / 2) - BORDERSIZE;
        newGridDataCheck.verticalAlignment = GridData.BEGINNING;
        newGridDataCheck.horizontalAlignment = GridData.BEGINNING;

        GridData newGridDataLabel = new GridData();
        newGridDataLabel.widthHint = (DIALOGWINDOWIDTH / 2) - BORDERSIZE;
        newGridDataLabel.verticalAlignment = GridData.BEGINNING;
        newGridDataLabel.horizontalAlignment = GridData.BEGINNING;

        GridData newGridDataText = new GridData();
        newGridDataText.widthHint = (DIALOGWINDOWIDTH / 2) - 10 * BORDERSIZE;
        newGridDataText.minimumWidth = TEXT_WIDTH;
        newGridDataText.verticalAlignment = GridData.BEGINNING;
        newGridDataText.horizontalAlignment = GridData.BEGINNING;
        newGridDataText.grabExcessHorizontalSpace = true;

        Composite newProjectComposite = new Composite(parent, SWT.BORDER);
        newProjectComposite.setLayout(newGrid);
        newProjectComposite.setLayoutData(newGridDataComp);

        setNewProjectRadioButton(new Button(newProjectComposite, SWT.RADIO));
        getNewProjectRadioButton().setText("Place in new project");
        getNewProjectRadioButton().setLayoutData(newGridDataRadio);
        getNewProjectRadioButton().setVisible(true);
        if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0) {
            getNewProjectRadioButton().setSelection(false);
            getExistingProjectRadioButton().setSelection(true);
            setUsage(ProjectUsages.EXISTING);
        } else {
            getNewProjectRadioButton().setSelection(true);
            getExistingProjectRadioButton().setSelection(false);
            setUsage(ProjectUsages.NEW);
        }

        useDefaultNameButton = new Button(newProjectComposite, SWT.CHECK);
        useDefaultNameButton.setSelection(true);
        useDefaultNameButton.setText("Use default project name");
        useDefaultNameButton.setEnabled(getNewProjectRadioButton()
            .getSelection());
        useDefaultNameButton.setVisible(true);
        useDefaultNameButton.setLayoutData(newGridDataCheck);

        Label projectNameLabel = new Label(newProjectComposite, SWT.NONE);
        projectNameLabel.setText("Project name: ");
        projectNameLabel.setVisible(true);
        projectNameLabel.setLayoutData(newGridDataLabel);

        setProjectNameTextField(new Text(newProjectComposite, SWT.SINGLE
            | SWT.BORDER));
        projectNameTextField.setEnabled(false);
        projectNameTextField.setVisible(true);
        projectNameTextField.setLayoutData(newGridDataText);
        projectNameTextField.addListener(SWT.Verify, new AlphanumericalTextContraintListener(false, true));
        projectNameTextField.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        projectNameTextFieldModifyListener = new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                dialogChanged();
            }
        };
        projectNameTextField.addModifyListener(projectNameTextFieldModifyListener);
        if (NewWorkflowProjectWizard.sharedWorkflowName != null) {
            projectNameTextField.setText(NewWorkflowProjectWizard.sharedWorkflowName);
        }

        setControl(newProjectComposite);
    }

    @Override
    public void createControl(Composite parent) {

        Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayout(new GridLayout(2, false));

        // create gui and register listeners
        createExistingProjectPanel(comp);
        createNewProjectPanel(comp);
        registerListeners();
        handleInitialTreeSelection();

    }

    private void handleInitialTreeSelection() {
        boolean useExistingProject = false;

        if (this.selection != null) {
            useExistingProject = true;
            if (this.selection.getFirstElement() != null) {
                useExistingProject = true;
            } else {
                useExistingProject = false;
            }
        }

        if (useExistingProject) {
            if (this.selection.size() == 1
                && this.selection.getFirstElement() instanceof IProject) {
                // (a) Project selected
                projectTreeViewer.setSelection(selection);
                getExistingProjectRadioButton().setSelection(true);
                getNewProjectRadioButton().setSelection(false);
            } else if (this.selection.size() == 1
                && this.selection.getFirstElement() instanceof IFolder) {
                // (b) Folder selected
                IFolder folder = (IFolder) this.selection.getFirstElement();
                String pathToExpand2 = folder.getFullPath().toOSString()
                    .substring(1);
                java.util.List<String> pathToExpand = Arrays
                    .asList(pathToExpand2.split("\\\\"));
                TreeItem[] currentCandidates = projectTreeViewer.getTree()
                    .getItems();
                TreeItem lastItem = null;
                for (String segment : pathToExpand) {
                    TreeItem matchedItem = null;
                    for (TreeItem item : currentCandidates) {
                        if (item.getText().equals(segment)) {
                            item.setExpanded(true);
                            matchedItem = item;
                            projectTreeViewer.refresh();
                            break;
                        }
                    }
                    if (matchedItem != null) {
                        currentCandidates = matchedItem.getItems();
                        lastItem = matchedItem;
                    }
                }
                if (lastItem != null) {
                    projectTreeViewer.getTree().setSelection(lastItem);
                }
                projectTreeViewer.refresh();
                getExistingProjectRadioButton().setSelection(true);
                getNewProjectRadioButton().setSelection(false);
            } else if (this.selection.size() == 1
                && this.selection.getFirstElement() instanceof IFile) {
                // (c) File selected
                IFile file = (IFile) this.selection.getFirstElement();
                IStructuredSelection iss = new StructuredSelection(
                    file.getProject());
                projectTreeViewer.setSelection(iss);
                getExistingProjectRadioButton().setSelection(true);
                getNewProjectRadioButton().setSelection(false);
            } else {
                // None of this selected, but selection not null (e.g.
                // WorkflowPart counts as not null)
                activateExistingProjectPart();
            }
        } else {
            // Selection null (e.g. Focus on ProjectExplorer counts as selection
            // null)
            activateExistingProjectPart();
        }

        // in case workspace contains no projects so far
        if (projectTreeViewer.getTree().getItemCount() == 0) {
            getExistingProjectRadioButton().setEnabled(false);
            projectTreeViewer.getTree().setEnabled(false);
            getNewProjectRadioButton().setSelection(true);
        }
    }

    private void registerListeners() {
        newProjectRadioButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (getNewProjectRadioButton().getSelection()) {
                    if (NewWorkflowProjectWizard.sharedWorkflowName != null) {
                        getProjectNameTextField().setText(NewWorkflowProjectWizard.sharedWorkflowName);
                    }
                    useDefaultNameButton.setEnabled(true);
                    getExistingProjectRadioButton().setSelection(false);
                    projectTreeViewer.getTree().setEnabled(false);
                    setUsage(ProjectUsages.NEW);
                    dialogChanged();
                }
            }
        });

        existingProjectRadioButton.addSelectionListener(
            new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    getNewProjectRadioButton().setSelection(false);
                    useDefaultNameButton.setEnabled(false);
                    projectTreeViewer.getTree().setEnabled(true);
                    if (projectTreeViewer.getTree().getSelection().length == 0) {
                        projectTreeViewer.getTree().setSelection(
                            projectTreeViewer.getTree().getItem(0));
                    }
                    NewWorkflowProjectWizard.setWorkbenchSelection(new StructuredSelection(
                        projectTreeViewer.getSelection()));
                    setUsage(ProjectUsages.EXISTING);
                    dialogChanged();
                }
            });

        useDefaultNameButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // if checked -> enabled+editable , else -> disable+non-editable
                boolean selectionState = useDefaultNameButton.getSelection();
                getProjectNameTextField().setEditable(!selectionState);
                getProjectNameTextField().setEnabled(!selectionState);
            }
        });

        projectTreeViewer
            .addSelectionChangedListener(new ISelectionChangedListener() {

                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    NewWorkflowProjectWizard.setWorkbenchSelection((IStructuredSelection) event.getSelection());
                }
            });
    }

    protected void activateExistingProjectPart() {
        getNewProjectRadioButton().setSelection(true);
        getExistingProjectRadioButton().setSelection(false);
        if (workflowName != null) {
            getProjectNameTextField().setText(workflowName);
        }
    }

    public Text getProjectNameTextField() {
        return projectNameTextField;
    }

    public void setProjectNameTextField(Text projectNameTextField) {
        this.projectNameTextField = projectNameTextField;
    }

    public ProjectUsages getUsage() {
        return usage;
    }

    public void setUsage(ProjectUsages usage) {
        this.usage = usage;
    }

    public Button getNewProjectRadioButton() {
        return newProjectRadioButton;
    }

    public void setNewProjectRadioButton(Button newProjectRadioButton) {
        this.newProjectRadioButton = newProjectRadioButton;
    }

    public Button getExistingProjectRadioButton() {
        return existingProjectRadioButton;
    }

    public void setExistingProjectRadioButton(Button existingProjectRadioButton) {
        this.existingProjectRadioButton = existingProjectRadioButton;
    }
}
