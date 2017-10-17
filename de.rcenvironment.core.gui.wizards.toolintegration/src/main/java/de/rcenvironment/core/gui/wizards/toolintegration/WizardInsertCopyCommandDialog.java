/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for inserting a copy command in pre/post script.
 * 
 * @author Sascha Zur
 */
public class WizardInsertCopyCommandDialog extends Dialog {

    private static final String REQUIRED = " (required)";

    private static final String FILE = "File";

    private static final String SLASH = "/";

    private static final String DIRECTORY_PREFIX = "Directory: ";

    private static final String INPUT_PREFIX = "Input ";

    private static final String TARGET_STRING = "*TARGET*";

    private static final String SOURCE_STRING = "*SOURCE*";

    private static final String SHUTIL_COPYTREE_STRING = StringUtils
        .format("shutil.copytree(\"%s\", \"%s\")", SOURCE_STRING, TARGET_STRING);

    private static final String SHUTIL_COPY_STRING = StringUtils.format("shutil.copy(\"%s\", \"%s\")", SOURCE_STRING, TARGET_STRING);

    private static final int SOURCE = 0;

    private static final int TARGET = 1;

    private Button fileToFileButton;

    private Button dirButton;

    private String command;

    private final List<String> inputs;

    private final String[] directories;

    private Button fileToDirButton;

    private Button[] predefOptionButtons;

    private Button[] customOptionButtons;

    private Combo[] predefCombos;

    private Text[] predefTexts;

    private Text[] customTexts;

    private Button[] customChooseButtons;

    private Label previewCommandLabel;

    private Label[] slashLabel;

    private CLabel validationLabel;

    public WizardInsertCopyCommandDialog(Shell parentShell, List<String> endpointNames, String[] directories) {
        super(parentShell);
        this.inputs = endpointNames;
        this.directories = directories;
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE
            | getDefaultOrientation());
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.copyDialogShellText);
        command = SHUTIL_COPY_STRING;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        GridData g = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        container.setLayoutData(g);
        Composite typeContainer = new Composite(container, SWT.NONE);
        typeContainer.setLayout(new GridLayout(5, false));
        Label sourceType = new Label(typeContainer, SWT.NONE);
        sourceType.setText(Messages.copy);
        GridData labelData = new GridData();
        labelData.horizontalSpan = 1;
        sourceType.setLayoutData(labelData);
        fileToFileButton = new Button(typeContainer, SWT.RADIO);
        fileToFileButton.setText(Messages.copyFileToFile);
        GridData fileData = new GridData();
        fileToFileButton.setLayoutData(fileData);
        fileToFileButton.setSelection(true);
        fileToDirButton = new Button(typeContainer, SWT.RADIO);
        fileToDirButton.setText(Messages.copyFileToDir);
        GridData fileToDirData = new GridData();
        fileToDirButton.setLayoutData(fileToDirData);

        dirButton = new Button(typeContainer, SWT.RADIO);
        dirButton.setText(Messages.copyDirToDir);
        GridData dirData = new GridData();
        dirData.horizontalSpan = 2;
        dirButton.setLayoutData(dirData);

        predefOptionButtons = new Button[2];
        customOptionButtons = new Button[2];
        predefCombos = new Combo[2];
        predefTexts = new Text[2];
        slashLabel = new Label[2];
        customTexts = new Text[2];
        customChooseButtons = new Button[2];

        Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        buildUpElements(SOURCE, container);
        Label separator3 = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        separator3.setVisible(false);
        buildUpElements(TARGET, container);

        Label separator2 = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label previewLabel = new Label(container, SWT.NONE);
        previewLabel.setText(Messages.insertCommand);
        previewCommandLabel = new Label(container, SWT.NONE);
        previewCommandLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        validationLabel = new CLabel(container, SWT.NONE);
        validationLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        validationLabel.setVisible(false);
        validationLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));

        return container;
    }

    private Text buildUpElements(int type, Composite container) {
        Label label = new Label(container, SWT.NONE);
        if (type == 0) {
            label.setText(Messages.source);
        } else {
            label.setText(Messages.target);
        }
        GridData data = new GridData();
        label.setLayoutData(data);

        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new GridLayout(5, false));
        GridData compData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        compData.horizontalIndent = 10;
        composite.setLayoutData(compData);

        Button predefPathButton = new Button(composite, SWT.RADIO);
        predefPathButton.setText(Messages.predefinedPath);
        GridData pathButtonData = new GridData();
        pathButtonData.horizontalSpan = 5;
        predefPathButton.setLayoutData(pathButtonData);
        predefPathButton.setSelection(true);

        predefOptionButtons[type] = predefPathButton;

        Combo combo = new Combo(composite, SWT.READ_ONLY);
        GridData comboData = new GridData();
        fillCombo(combo, type);
        combo.setLayoutData(comboData);
        predefCombos[type] = combo;

        Label slash = new Label(composite, SWT.NONE);
        slash.setText(SLASH);
        GridData slashData = new GridData();
        slashData.widthHint = 5;
        slash.setLayoutData(slashData);
        slashLabel[type] = slash;
        Text predefText = new Text(composite, SWT.BORDER);
        GridData predefTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        predefTextData.horizontalSpan = 3;
        predefText.setLayoutData(predefTextData);
        predefText.setMessage(Messages.subfolderOrFilename);
        predefTexts[type] = predefText;

        Button customPathButton = new Button(composite, SWT.RADIO);
        customPathButton.setText(Messages.customPath);
        GridData customButtonData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        customButtonData.horizontalSpan = 5;
        customPathButton.setLayoutData(customButtonData);
        customOptionButtons[type] = customPathButton;

        Text text = new Text(composite, SWT.BORDER);
        GridData sourceTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        sourceTextData.horizontalSpan = 4;
        text.setLayoutData(sourceTextData);
        text.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                updateSelectionAndValidateInput();
            }
        });
        customTexts[type] = text;
        Button selectionButton = new Button(composite, SWT.PUSH);
        selectionButton.setText(" ... ");
        selectionButton.addSelectionListener(new FileDirButtonChooser(text));
        customChooseButtons[type] = selectionButton;

        return text;
    }

    private void fillCombo(Combo combo, int type) {
        String selection = combo.getText();
        combo.removeAll();
        for (String input : inputs) {
            if (input.startsWith(FILE) && type == SOURCE && (fileToFileButton.getSelection() || fileToDirButton.getSelection())) {
                combo.add(INPUT_PREFIX + input);
            } else if (input.startsWith(FILE) && type == TARGET && fileToFileButton.getSelection()) {
                combo.add(INPUT_PREFIX + input);
            }
        }
        for (String dir : directories) {
            combo.add(DIRECTORY_PREFIX + dir);
        }
        if (selection != null && combo.indexOf(selection) >= 0) {
            combo.select(combo.indexOf(selection));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        installListeners();
        updateSelectionAndValidateInput();
    }

    private void installListeners() {
        UpdateListener ul = new UpdateListener();
        fileToFileButton.addSelectionListener(ul);
        fileToDirButton.addSelectionListener(ul);
        dirButton.addSelectionListener(ul);
        for (int i = 0; i < 2; i++) {
            predefOptionButtons[i].addSelectionListener(ul);
            customOptionButtons[i].addSelectionListener(ul);
            predefCombos[i].addSelectionListener(ul);
            customChooseButtons[i].addSelectionListener(ul);
        }
        for (int i = 0; i < 2; i++) {
            predefTexts[i].addModifyListener(ul);
            customTexts[i].addModifyListener(ul);
        }
    }

    protected void updateSelectionAndValidateInput() {
        for (int i = 0; i < 2; i++) {
            customTexts[i].setEnabled(!predefOptionButtons[i].getSelection());
            customChooseButtons[i].setEnabled(!predefOptionButtons[i].getSelection());
            predefCombos[i].setEnabled(predefOptionButtons[i].getSelection());
            predefTexts[i].setEnabled(predefOptionButtons[i].getSelection());
            if (predefCombos[i].getText() != null && predefCombos[i].getText().startsWith(INPUT_PREFIX + FILE)) {
                predefTexts[i].setVisible(false);
                slashLabel[i].setVisible(false);
            } else {
                predefTexts[i].setVisible(true);
                slashLabel[i].setVisible(true);
            }

        }
        if (fileToFileButton.getSelection()) {
            predefTexts[SOURCE].setMessage(Messages.subfolderOrFilename + REQUIRED);
            predefTexts[TARGET].setMessage(Messages.subfolderOrFilename);
        } else if (fileToDirButton.getSelection()) {
            predefTexts[SOURCE].setMessage(Messages.subfolderOrFilename + REQUIRED);
            predefTexts[TARGET].setMessage(Messages.subfolderOrFilename.substring(0, Messages.subfolderOrFilename.indexOf(SLASH)));
        } else if (dirButton.getSelection()) {
            predefTexts[SOURCE].setMessage(Messages.subfolderOrFilename.substring(0, Messages.subfolderOrFilename.indexOf(SLASH)));
            predefTexts[TARGET].setMessage(Messages.subfolderOrFilename.substring(0, Messages.subfolderOrFilename.indexOf(SLASH))
                + REQUIRED);
        }
        boolean isSourceValid = true;
        List<String> validationMessages = new LinkedList<>();

        if (predefOptionButtons[SOURCE].getSelection()) {
            if (predefCombos[SOURCE].getText() == null || predefCombos[SOURCE].getText().isEmpty()) {
                isSourceValid = false;
                validationMessages.add("Relative source path not valid.");
            }
            if (fileToFileButton.getSelection() || fileToDirButton.getSelection()) {
                if (predefTexts[SOURCE].getText() == null || predefTexts[SOURCE].getText().isEmpty()
                    && !predefCombos[SOURCE].getText().startsWith(INPUT_PREFIX + FILE)) {
                    isSourceValid = false;
                    validationMessages.add("Relative source file not valid.");
                }
            }
        } else {
            if (customTexts[SOURCE].getText() == null || customTexts[SOURCE].getText().isEmpty()) {
                isSourceValid = false;
                validationMessages.add("Absolute source path not valid.");
            } else if (!(new File(customTexts[SOURCE].getText())).isAbsolute()) {
                isSourceValid = false;
                validationMessages.add("Source path is not absolute.");
            }
        }

        boolean isTargetValid = true;
        if (predefOptionButtons[TARGET].getSelection()) {
            if (predefCombos[TARGET].getText() == null || predefCombos[TARGET].getText().isEmpty()) {
                isTargetValid = false;
                validationMessages.add("Relative target path not valid.");
            }
            if (dirButton.getSelection()) {
                if (predefTexts[TARGET].getText() == null || predefTexts[TARGET].getText().isEmpty()) {
                    isTargetValid = false;
                    validationMessages.add("Relative target directory not valid.");

                }
            }
        } else {
            if (customTexts[TARGET].getText() == null || customTexts[TARGET].getText().isEmpty()) {
                isTargetValid = false;
                validationMessages.add("Absolute target path not valid.");
            } else if (!(new File(customTexts[TARGET].getText())).isAbsolute()) {
                isTargetValid = false;
                validationMessages.add("Target path is not absolute.");
            }
        }
        if (!validationMessages.isEmpty()) {
            validationLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            validationLabel.setText(validationMessages.get(0));
            validationLabel.setVisible(true);
            validationLabel.pack();
        } else {
            validationLabel.setVisible(false);
        }
        boolean isValid = isSourceValid && isTargetValid;
        buildCommand(isValid);
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    private void buildCommand(boolean isValid) {
        if (dirButton.getSelection()) {
            command = SHUTIL_COPYTREE_STRING;
        } else {
            command = SHUTIL_COPY_STRING;
        }
        if (predefOptionButtons[SOURCE].getSelection()) {
            String slash = SLASH;
            if (predefCombos[SOURCE].getText() != null && predefCombos[SOURCE].getText().startsWith(INPUT_PREFIX + FILE)) {
                slash = "";
            }
            command =
                command.replace(SOURCE_STRING, getPlaceholder(predefCombos[SOURCE].getText()) + slash + predefTexts[SOURCE].getText());
        } else {
            command = command.replace(SOURCE_STRING, customTexts[SOURCE].getText());
        }

        if (predefOptionButtons[TARGET].getSelection()) {
            String slash = SLASH;
            if (predefCombos[TARGET].getText() != null && predefCombos[TARGET].getText().startsWith(INPUT_PREFIX + FILE)) {
                slash = "";
            }
            command =
                command.replace(TARGET_STRING, getPlaceholder(predefCombos[TARGET].getText()) + slash + predefTexts[TARGET].getText());
        } else {
            command = command.replace(TARGET_STRING, customTexts[TARGET].getText());
        }
        if (isValid) {
            previewCommandLabel.setText(command);
        } else {
            previewCommandLabel.setText("-");
        }

    }

    private String getPlaceholder(String selection) {
        if (selection != null && !selection.isEmpty()) {
            String name = selection.substring(selection.indexOf(":") + 2);
            String insertString = "${%s:%s}";
            if (selection.startsWith(INPUT_PREFIX)) {
                insertString = StringUtils.format(insertString, "in", name);
            } else if (selection.startsWith(DIRECTORY_PREFIX)) {
                int i = 0;
                while (!name.equals(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES[i])) {
                    i++;
                }
                if (i < ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER.length) {
                    insertString = StringUtils.format(insertString, "dir", ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[i]);
                }
            }
            return insertString;
        }
        return "";
    }

    public String getCopyCommand() {
        return command;
    }

    /**
     * Listener for updating the dialog.
     * 
     * @author Sascha Zur
     */
    private class UpdateListener implements SelectionListener, ModifyListener {

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (arg0.getSource().equals(fileToDirButton) || arg0.getSource().equals(fileToFileButton)
                || arg0.getSource().equals(dirButton)) {
                fillCombo(predefCombos[SOURCE], SOURCE);
                fillCombo(predefCombos[TARGET], TARGET);
            }
            updateSelectionAndValidateInput();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            updateSelectionAndValidateInput();
        }
    }

    /**
     * Opens the correct dialog for the file or directory choose buttons.
     * 
     * @author Sascha Zur
     */
    private class FileDirButtonChooser implements SelectionListener {

        private final Text text;

        FileDirButtonChooser(Text text) {
            this.text = text;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            String result = "";
            if (fileToFileButton.getSelection() || fileToDirButton.getSelection() && arg0.getSource().equals(customChooseButtons[SOURCE])) {
                FileDialog f = new FileDialog(getShell());
                result = f.open();

            } else {
                DirectoryDialog d = new DirectoryDialog(getShell());
                result = d.open();
            }
            if (result != null) {
                text.clearSelection();
                text.setText(result.replaceAll("\\\\", "/"));
                text.forceFocus();

            }
        }
    }
}
