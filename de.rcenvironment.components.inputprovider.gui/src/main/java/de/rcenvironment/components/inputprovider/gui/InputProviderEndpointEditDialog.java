/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.components.inputprovider.common.InputProviderComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.TextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * Manages endpoint descriptions of the Input Provider component.
 * 
 * @author Mark Geiger
 * @author Doreen Seider
 * @author Adrian Stock
 * @author Tim Rosenbach
 * @author Kathrin Schaffert (added NumericalTextConstraintFloatListener, NumericalTextConstraintIntListener to display info messages)
 */
public class InputProviderEndpointEditDialog extends EndpointEditDialog implements SelectionListener {

    private static final int[] NOTETEXTSIZE = { 75, 10 };

    private static final int SHORTTEXT_MAXLENGTH = 140;

    private static final int DIALOG_WIDTH = 345;

    private static final int DIALOG_HEIGHT = 400;

    private Label label;

    private Text textField;

    private Combo combo;

    private Button selectFromFileSystemButton;

    private Composite confContainer;

    private Composite warningComposite;

    private Boolean warningCompositeCreated = false;

    private NumericalTextConstraintFloatListener floatListener;

    private NumericalTextConstraintIntListener integerListener;

    private TextConstraintListener textLengthListener;

    private Button selectFromProjectButton;

    private Button selectAtStartCheckbox;

    private boolean isInProject;

    public InputProviderEndpointEditDialog(Shell parentShell, EndpointActionType actionType,
        ComponentInstanceProperties configuration, EndpointType direction, String id, boolean isStatic, Image icon,
        EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected void createEndpointSettings(Composite parent) {
        super.createEndpointSettings(parent);
        comboDataType.addSelectionListener(this);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {}

    @Override
    public void widgetSelected(SelectionEvent event) {

        removeListeners();

        initialize();
    }

    @Override
    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        confContainer = container;

        createValueArea();

        createFileSelectionArea();

        createListeners();

        initialize();

        initializeWithValue(value);

        return textField;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
    }

    private void initializeWithValue(String value) {
        if (metadataValues.containsKey(InputProviderComponentConstants.META_FILESOURCETYPE)
            && metadataValues.get(InputProviderComponentConstants.META_FILESOURCETYPE)
                .equals(InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART)) {
            selectAtStartCheckbox.setSelection(true);
            setSelectedAtStart();
        } else {
            textField.setText(value);
        }
        if (combo != null) {
            combo.select(combo.indexOf(value));
        }
    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        if (combo != null) {
            combo.setBounds(textField.getBounds());
        }
    }

    private void initialize() {

        if (!selectAtStartCheckbox.getSelection()) {
            textField.setText("");
            textField.setEnabled(true);
        }

        String dataType = comboDataType.getText();

        setTextFieldOrComboVisible(true);
        // enabled to enter value at workflow start for all data types
        setDataTypeSelected(true);

        if (dataType.equals(DataType.Integer.getDisplayName())) {
            textField.addVerifyListener(integerListener);
        } else if (dataType.equals(DataType.Float.getDisplayName())) {
            textField.addVerifyListener(floatListener);
        } else if (dataType.equals(DataType.FileReference.getDisplayName())
            || dataType.equals(DataType.DirectoryReference.getDisplayName())) {
            if (!selectAtStartCheckbox.getSelection()) {
                setFileOrDirectorySelected(true);
            }
        } else if (dataType.equals(DataType.ShortText.getDisplayName())) {
            textField.addVerifyListener(textLengthListener);
        } else if (dataType.equals(DataType.Boolean.getDisplayName())) {
            if (combo == null) {
                createBooleanCombo();
            }
            setTextFieldOrComboVisible(false);
            if (!selectAtStartCheckbox.getSelection()) {
                textField.setText(combo.getText());
            }
            combo.setEnabled(!selectAtStartCheckbox.getSelection());
        }
    }

    
    
    private void setTextFieldOrComboVisible(boolean textFieldVisible) {
        textField.setVisible(textFieldVisible);
        if (combo != null) {
            combo.setVisible(!textFieldVisible);
        }
    }

    private void createValueArea() {
        label = new Label(confContainer, SWT.NONE);
        label.setText(Messages.value);

        textField = new Text(confContainer, SWT.BORDER);
        textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        textField.setVisible(false);
    }

    private void createFileSelectionArea() {

        if ((PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput()
            .toString().split("/")).length == 1) {
            isInProject = false;
        } else {
            isInProject = true;
        }
        Composite fileSelectionComposite = new Composite(confContainer, SWT.NONE);
        fileSelectionComposite.setLayout(new GridLayout(2, true));
        GridData g = new GridData(GridData.FILL, GridData.FILL, true, true);
        g.horizontalSpan = 2;
        fileSelectionComposite.setLayoutData(g);

        g = new GridData();
        g.horizontalSpan = 2;
        g.grabExcessHorizontalSpace = false;
        selectAtStartCheckbox = new Button(fileSelectionComposite, SWT.CHECK);
        selectAtStartCheckbox.setLayoutData(g);
        selectAtStartCheckbox.setText(Messages.chooseAtWorkflowStart);
        selectAtStartCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                setSelectedAtStart();
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });

        selectFromProjectButton = new Button(fileSelectionComposite, SWT.PUSH);
        selectFromProjectButton.setText(Messages.selectFromProject);
        g = new GridData(GridData.CENTER, GridData.CENTER, false, false);
        selectFromProjectButton.setLayoutData(g);
        selectFromProjectButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {

                IResource resource = null;
                if (comboDataType.getText().equals(DataType.DirectoryReference.getDisplayName())) {
                    resource = PropertyTabGuiHelper.selectDirectoryFromActiveProject(confContainer.getShell(),
                        Messages.selectDirectory, Messages.selectDirectoryFromProject);
                } else if (isInProject) {
                    resource = PropertyTabGuiHelper.selectFileFromActiveProject(confContainer.getShell(),
                        Messages.selectFile, Messages.selectFileFromProject);
                }
                if (resource != null) {
                    textField.setText(resource.getFullPath().makeRelative().toPortableString());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });
        selectFromFileSystemButton = new Button(fileSelectionComposite, SWT.PUSH);
        selectFromFileSystemButton.setText(Messages.selectFromFileSystem);
        selectFromFileSystemButton.setLayoutData(g);
        selectFromFileSystemButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                String path;
                if (comboDataType.getText().equals(DataType.DirectoryReference.getDisplayName())) {
                    DirectoryDialog dialog = new DirectoryDialog(confContainer.getShell(), SWT.OPEN);
                    dialog.setText(Messages.selectDirectory);
                    dialog.setMessage(Messages.selectDirectoryFromFileSystem);
                    checkIfPathExists(dialog, textField.getText());
                    path = dialog.open();
                } else {
                    FileDialog dialog = new FileDialog(confContainer.getShell(), SWT.OPEN);

                    dialog.setText(Messages.selectFile);
                    checkIfPathExists(dialog, textField.getText());
                    path = dialog.open();
                }
                if (path != null) {
                    textField.setText(path);
                }
            }

            private void checkIfPathExists(Dialog dialog, String text) {
                Path isThisPathExisting = Paths.get(text);
                if (Files.exists(isThisPathExisting, LinkOption.NOFOLLOW_LINKS)) {
                    if (dialog instanceof DirectoryDialog) {
                        ((DirectoryDialog) dialog).setFilterPath(isThisPathExisting.toString());
                    } else {

                        // Type FileDialog
                        if (isThisPathExisting.getParent() != null) {
                            ((FileDialog) dialog).setFilterPath(isThisPathExisting.getParent().toString());

                        } else {
                            // The path value is empty. This would open the last opened FileDialog if we not
                            // set the root path.
                            File[] paths = File.listRoots();
                            if (paths[0].getPath() != null) {
                                ((FileDialog) dialog).setFilterPath(paths[0].getPath());
                            } else {
                                return;
                            }

                        }

                    }

                    return;
                }

                if (isThisPathExisting.getParent() != null) {
                    checkIfPathExists(dialog, isThisPathExisting.getParent().toString());
                } else {
                    return;
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {}
        });
        setFileOrDirectorySelected(false);

        if (!isInProject) {
            createWorkflowNotInRCEWarning();
        }
    }

    private void createWorkflowNotInRCEWarning() {

        warningComposite = new Composite(confContainer, SWT.NONE);
        warningComposite.setLayout(new GridLayout(2, false));

        GridData g = new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_BEGINNING, true, true);
        g.horizontalSpan = 2;
        warningComposite.setLayoutData(g);

        Label noteLabel = new Label(warningComposite, SWT.TOP);
        noteLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
        noteLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        g = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        g.horizontalAlignment = GridData.FILL;
        g.widthHint = NOTETEXTSIZE[1];
        g.heightHint = NOTETEXTSIZE[0];
        g.grabExcessHorizontalSpace = true;

        Text noteText = new Text(warningComposite, SWT.WRAP | SWT.READ_ONLY);
        noteText.setEditable(false);
        noteText.setEnabled(false);
        noteText.setLayoutData(g);
        noteText.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW));
        noteText.setText(
            "Select from project is not available because the component is in a workflow outside your current workspace.");

        warningCompositeCreated = true;
    }

    private void setSelectedAtStart() {
        boolean selectAtStart = selectAtStartCheckbox.getSelection();
        textField.setEnabled(!selectAtStart);
        if (selectAtStart) {
            textField.setText("-");
        } else {
            textField.setText("");
        }
        String dataType = comboDataType.getText();
        if (dataType.equals(DataType.FileReference.toString())
            || dataType.equals(DataType.DirectoryReference.toString())) {
            if (isInProject) {
                selectFromProjectButton.setEnabled(!selectAtStart);
            } else {
                warningComposite.setVisible(!selectAtStart);
            }
            selectFromFileSystemButton.setEnabled(!selectAtStart);
        }
        if (dataType.equals(DataType.Boolean.toString())) {
            combo.setEnabled(!selectAtStart);
            if (!selectAtStart) {
                textField.setText(combo.getText());
            }
        }
        if (selectAtStart) {
            metadataValues.put(InputProviderComponentConstants.META_FILESOURCETYPE,
                InputProviderComponentConstants.META_FILESOURCETYPE_ATWORKFLOWSTART);
        } else {
            metadataValues.remove(InputProviderComponentConstants.META_FILESOURCETYPE);
        }
    }

    private void setDataTypeSelected(boolean selected) {
        selectAtStartCheckbox.setEnabled(selected);
        selectFromProjectButton.setEnabled(false);
        selectFromFileSystemButton.setEnabled(false);
        if (!isInProject && warningCompositeCreated) {
            warningComposite.setVisible(!selected);
        }
    }

    private void setFileOrDirectorySelected(boolean selected) {
        selectAtStartCheckbox.setEnabled(selected);
        if (isInProject) {
            selectFromProjectButton.setEnabled(selected);
        } else {
            selectFromProjectButton.setEnabled(!selected);
            if (warningCompositeCreated) {
                warningComposite.setVisible(selected);
            }
        }
        selectFromFileSystemButton.setEnabled(selected);
    }

    private void createBooleanCombo() {
        combo = new Combo(confContainer, SWT.READ_ONLY);
        combo.add(Boolean.TRUE.toString().toLowerCase());
        combo.add(Boolean.FALSE.toString().toLowerCase());
        combo.setText(Boolean.TRUE.toString().toLowerCase());
        combo.setBounds(textField.getBounds());
        combo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                textField.setText(combo.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });
        combo.setVisible(false);
    }

    private void createListeners() {
        floatListener = new NumericalTextConstraintFloatListener(WidgetGroupFactory.ONLY_FLOAT);
        integerListener = new NumericalTextConstraintIntListener(WidgetGroupFactory.ONLY_INTEGER);
        textLengthListener = new TextConstraintListener(SHORTTEXT_MAXLENGTH);
    }

    private void removeListeners() {
        textField.removeVerifyListener(integerListener);
        textField.removeVerifyListener(floatListener);
        textField.removeVerifyListener(textLengthListener);
    }

    @Override
    protected boolean validateMetaDataInputs() {
        return selectAtStartCheckbox.getSelection() || super.validateMetaDataInputs();
    }

    private class NumericalTextConstraintFloatListener extends NumericalTextConstraintListener {

        NumericalTextConstraintFloatListener(int function) {
            super(function);
        }

        @Override
        public void verifyText(VerifyEvent e) {

            super.verifyText(e);

            Text text = (Text) e.getSource();
            String oldS = text.getText();
            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);

            if (newS.endsWith(E) || newS.endsWith(SMALL_E)) {
                return;
            }
            try {
                if ((String.valueOf(Double.parseDouble(newS)).equals("Infinity")
                    || String.valueOf(Double.parseDouble(newS)).equals("-Infinity")) && getMessage().isEmpty()) {
                    updateMessage("The maximum or minimum value for data type Float has been reached.", false);
                }
            } catch (NumberFormatException exception) {
                // nothing to do here
            }
        }

    }

    private class NumericalTextConstraintIntListener extends NumericalTextConstraintListener {

        NumericalTextConstraintIntListener(int function) {
            super(function);
        }

        @Override
        public void verifyText(VerifyEvent e) {

            super.verifyText(e);

            Text text = (Text) e.getSource();
            String oldS = text.getText();
            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);

            try {
                if ((Double.parseDouble(newS) >= Long.MAX_VALUE
                    || Double.parseDouble(newS) <= Long.MIN_VALUE) && getMessage().isEmpty()) {
                    updateMessage("The maximum or minimum value for data type Integer has been reached.", false);
                }
            } catch (NumberFormatException exception) {
                // nothing to do here
            }
        }

    }

}
