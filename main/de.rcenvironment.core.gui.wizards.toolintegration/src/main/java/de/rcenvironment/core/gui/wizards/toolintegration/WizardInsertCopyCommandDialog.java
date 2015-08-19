/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;

/**
 * A dialog for inserting a copy command in pre/post script.
 * 
 * @author Sascha Zur
 */
public class WizardInsertCopyCommandDialog extends Dialog {

    private static final int MINIMUM_TEXT_WIDTH = 200;

    private static final String DIRECTORY_PREFIX = "Directory: ";

    private static final String PROPERTY_PREFIX = "Property: ";

    private static final String OUTPUT_PREFIX = "Output: ";

    private static final String INPUT_PREFIX = "Input: ";

    private static final String TARGET_STRING = "*TARGET*";

    private static final String SOURCE_STRING = "*SOURCE*";

    private static final String SHUTIL_COPYTREE_STRING = String.format("shutil.copytree(%s, %s)", SOURCE_STRING, TARGET_STRING);

    private static final String SHUTIL_COPY_STRING = String.format("shutil.copy(%s, %s)", SOURCE_STRING, TARGET_STRING);

    private static final String QUOTE = "\"";

    private Button fileButton;

    private Button dirButton;

    private String command;

    private Text sourceText;

    private Text targetText;

    private final String[] inputs;

    private final String[] outputs;

    private final String[] props;

    private final String[] directories;

    private final Map<String, Object> properties;

    public WizardInsertCopyCommandDialog(Shell parentShell, String[] inputs, String[] outputs, String[] props, String[] directories,
        Map<String, Object> properties) {
        super(parentShell);
        this.inputs = inputs;
        this.outputs = outputs;
        this.props = props;
        this.directories = directories;
        this.properties = properties;
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE
            | getDefaultOrientation());
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Insert copy command");
        command = SHUTIL_COPY_STRING;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        GridData g = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        container.setLayoutData(g);
        Composite typeContainer = new Composite(container, SWT.NONE);
        typeContainer.setLayout(new GridLayout(4, false));
        Label sourceType = new Label(typeContainer, SWT.NONE);
        sourceType.setText("Type to copy: ");
        GridData labelData = new GridData();
        sourceType.setLayoutData(labelData);
        fileButton = new Button(typeContainer, SWT.RADIO);
        fileButton.setText("File");
        GridData fileData = new GridData();
        fileButton.setLayoutData(fileData);
        fileButton.setSelection(true);
        dirButton = new Button(typeContainer, SWT.RADIO);
        dirButton.setText("Directory");
        GridData dirData = new GridData();
        dirData.horizontalSpan = 2;
        dirButton.setLayoutData(dirData);

        Label sourceLabel = new Label(container, SWT.NONE);
        sourceLabel.setText("Source:");
        GridData sourceData = new GridData();
        sourceData.horizontalIndent = 5;
        sourceLabel.setLayoutData(sourceData);
        Composite sourceComposite = new Composite(container, SWT.NONE);
        sourceComposite.setLayout(new GridLayout(4, false));
        GridData sourceCompData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        sourceComposite.setLayoutData(sourceCompData);
        sourceText = new Text(sourceComposite, SWT.BORDER);
        GridData sourceTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        sourceText.setLayoutData(sourceTextData);
        sourceText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                validateInput();
            }
        });

        Combo sourceCombo = new Combo(sourceComposite, SWT.READ_ONLY);
        GridData sourceComboData = new GridData();
        fillCombo(sourceCombo);
        sourceCombo.setLayoutData(sourceComboData);
        Button sourceInsertButton = new Button(sourceComposite, SWT.PUSH);
        sourceInsertButton.setText(Messages.insertButtonLabel);
        sourceInsertButton.addSelectionListener(new InsertButtonListener(sourceText, sourceCombo));
        Button sourceSelectionButton = new Button(sourceComposite, SWT.PUSH);
        sourceSelectionButton.setText(" ... ");
        sourceSelectionButton.addSelectionListener(new FileDirButtonChooser(sourceText));

        Label targetLabel = new Label(container, SWT.NONE);
        targetLabel.setText("Target:");
        GridData targetData = new GridData();
        targetData.horizontalIndent = 5;
        targetLabel.setLayoutData(targetData);
        Composite targetComp = new Composite(container, SWT.NONE);
        targetComp.setLayout(new GridLayout(4, false));
        GridData targetCompData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        targetComp.setLayoutData(targetCompData);
        targetText = new Text(targetComp, SWT.BORDER);
        GridData targetTextData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        targetTextData.minimumWidth = MINIMUM_TEXT_WIDTH;
        targetText.setLayoutData(targetTextData);
        targetText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                validateInput();
            }
        });

        Combo targetCombo = new Combo(targetComp, SWT.READ_ONLY);
        GridData targetComboData = new GridData();
        targetCombo.setLayoutData(targetComboData);
        fillCombo(targetCombo);
        Button targetInsertButton = new Button(targetComp, SWT.PUSH);
        targetInsertButton.setText(Messages.insertButtonLabel);
        targetInsertButton.addSelectionListener(new InsertButtonListener(targetText, targetCombo));
        Button targetSelectionButton = new Button(targetComp, SWT.PUSH);
        targetSelectionButton.setText(" ... ");
        targetSelectionButton.addSelectionListener(new FileDirButtonChooser(targetText));
        updateInitValues();
        return container;
    }

    private void fillCombo(Combo sourceCombo) {
        for (String input : inputs) {
            sourceCombo.add(INPUT_PREFIX + input);
        }
        for (String output : outputs) {
            sourceCombo.add(OUTPUT_PREFIX + output);
        }
        for (String prop : props) {
            sourceCombo.add(PROPERTY_PREFIX + prop);
        }
        for (String dir : directories) {
            sourceCombo.add(DIRECTORY_PREFIX + dir);
        }
    }

    private void updateInitValues() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        super.create();
        validateInput();
        installListeners();
    }

    private void installListeners() {
        SelectionListener sl = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (fileButton.getSelection()) {
                    command = SHUTIL_COPY_STRING;
                } else {
                    command = SHUTIL_COPYTREE_STRING;
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        };
        fileButton.addSelectionListener(sl);
        dirButton.addSelectionListener(sl);

    }

    @Override
    protected void okPressed() {
        String sourceString = sourceText.getText();
        String targetString = targetText.getText();
        if (sourceString != null && !sourceString.isEmpty()) {
            command = command.replace(SOURCE_STRING, QUOTE + sourceString + QUOTE);
        }
        if (targetString != null && !targetString.isEmpty()) {
            command = command.replace(TARGET_STRING, QUOTE + targetString + QUOTE);
        }
        super.okPressed();
    }

    protected void validateInput() {

        boolean isValid = true;
        if (sourceText.getText() == null || sourceText.getText().isEmpty()
            || targetText.getText() == null || targetText.getText().isEmpty()) {
            isValid = false;
        }
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    public String getCopyCommand() {
        return command;
    }

    /**
     * Opens the correct dialog for the file or directory choose buttons.
     * 
     * @author Sascha Zur
     */
    private class FileDirButtonChooser implements SelectionListener {

        private final Text text;

        public FileDirButtonChooser(Text text) {
            this.text = text;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (fileButton.getSelection()) {
                FileDialog f = new FileDialog(getShell());
                String result = f.open();
                if (result != null) {
                    text.clearSelection();
                    text.insert(result);
                    text.forceFocus();

                }
            } else {
                DirectoryDialog d = new DirectoryDialog(getShell());
                String result = d.open();
                if (result != null) {
                    text.clearSelection();
                    text.insert(result);
                    text.forceFocus();
                }
            }
        }

    }

    /**
     * Listener for insert buttons.
     * 
     * @author Sascha Zur
     */
    private class InsertButtonListener implements SelectionListener {

        private final Text insertText;

        private final Combo sourceCombo;

        public InsertButtonListener(Text insertText, Combo sourceCombo) {
            this.insertText = insertText;
            this.sourceCombo = sourceCombo;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            String selection = sourceCombo.getText();
            if (selection != null && !selection.isEmpty()) {
                String name = selection.substring(selection.indexOf(":") + 2);
                String insertString = "${%s:%s}";
                if (selection.startsWith(INPUT_PREFIX)) {
                    insertString = String.format(insertString, "in", name);
                } else if (selection.startsWith(OUTPUT_PREFIX)) {
                    insertString = String.format(insertString, "out", name);
                } else if (selection.startsWith(PROPERTY_PREFIX) && properties != null) {

                    for (String propTabName : properties.keySet()) {
                        Map<String, Object> proptab = (Map<String, Object>) properties.get(propTabName);
                        for (String propkey : proptab.keySet()) {
                            if (proptab.get(propkey) instanceof Map<?, ?>) {
                                Map<String, String> property = (Map<String, String>) proptab.get(propkey);
                                if (property.get(ToolIntegrationConstants.KEY_PROPERTY_DISPLAYNAME).equals(name)) {
                                    insertString = String.format(insertString, "prop", propkey);
                                }
                            }
                        }
                    }
                } else if (selection.startsWith(DIRECTORY_PREFIX)) {
                    int i = 0;
                    while (!name.equals(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES[i])) {
                        i++;
                    }
                    if (i < ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER.length) {
                        insertString = String.format(insertString, "dir", ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[i]);
                    }
                }
                insertText.insert(insertString);
                insertText.forceFocus();
            }
        }
    }
}
