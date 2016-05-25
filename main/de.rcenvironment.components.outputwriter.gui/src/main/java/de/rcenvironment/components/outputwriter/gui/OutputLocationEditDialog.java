/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.components.outputwriter.common.OutputLocation;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants;
import de.rcenvironment.components.outputwriter.common.OutputWriterComponentConstants.HandleExistingFile;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.workflow.executor.properties.WhitespaceShowListener;

/**
 * 
 * Edit Dialog for an OutputLocation.
 *
 * @author Brigitte Boden
 */

public class OutputLocationEditDialog extends Dialog {

    private static final int GROUPS_MIN_WIDTH = 550;

    private static final int HEADER_HEIGHT = 30;

    private static final int FORMAT_HEIGHT = 50;

    private static final char[] FORBIDDEN_CHARS = new char[] { '/', '\\', ':',
        '*', '?', '\"', '>', '<', '|' };

    private static final int MINUS_ONE = -1;

    private static final String COLON = ":";

    private String chosenFilename;

    private String chosenFolderForSaving;

    private HandleExistingFile chosenHandle;

    private List<String> chosenInputSet;

    private String chosenHeader;

    private String chosenFormatString;

    private String title;

    private final Set<String> paths;

    private final List<String> possibleInputs;

    private final List<String> inputsSelectedByOthers;

    private final List<String> otherOutputLocationFileNamesWithPaths;

    private Combo formatPlaceholderCombo;

    private Combo headerPlaceholderCombo;

    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell parent Shell
     * @param title
     * @param configuration the containing endpoint manager
     */
    public OutputLocationEditDialog(Shell parentShell, String title,
        Set<String> paths, List<String> possibleInputs, List<String> inputsSelectedByOthers,
        List<String> otherOutputLocationFileNamesWithPaths) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.title = title;
        this.paths = paths;
        this.possibleInputs = possibleInputs;
        Collections.sort(this.possibleInputs);
        this.inputsSelectedByOthers = inputsSelectedByOthers;
        this.otherOutputLocationFileNamesWithPaths = otherOutputLocationFileNamesWithPaths;

        chosenFilename = "";
        chosenFolderForSaving = OutputWriterComponentConstants.ROOT_DISPLAY_NAME;
        chosenHandle = OutputWriterComponentConstants.DEFAULT_HANDLE_EXISTING_FILE;
        chosenInputSet = new ArrayList<String>();
        chosenHeader = "";
        chosenFormatString = "";
    }

    public String getChosenFilename() {
        return chosenFilename;
    }

    public HandleExistingFile getChosenHandle() {
        return chosenHandle;
    }

    public List<String> getChosenInputSet() {
        return chosenInputSet;
    }

    public String getChosenHeader() {
        return chosenHeader;
    }

    public String getChosenFormatString() {
        return chosenFormatString;
    }

    public String getChosenFolderForSaving() {
        return chosenFolderForSaving;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        GridData g = new GridData(GridData.FILL_HORIZONTAL);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);

        createFileAndFolderSettings(container);

        Group inputsGroup = new Group(container, SWT.LEFT);
        GridData gr = new GridData(GridData.FILL_BOTH);
        gr.widthHint = container.getSize().x;
        inputsGroup.setLayoutData(gr);
        gr.grabExcessVerticalSpace = true;
        gr.minimumWidth = GROUPS_MIN_WIDTH;
        inputsGroup.setText(Messages.groupTitleInputs);

        RowLayout rowlayout = new RowLayout();
        inputsGroup.setLayout(rowlayout);
        boolean listEmpty = true;
        for (final String input : possibleInputs) {
            if (!inputsSelectedByOthers.contains(input)) {
                listEmpty = false;
                final Button item = new Button(inputsGroup, SWT.CHECK);
                item.setText(input);
                if (chosenInputSet.contains(input)) {
                    item.setSelection(true);
                }
                item.addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        if (item.getSelection()) {
                            chosenInputSet.add(input);
                        } else {
                            chosenInputSet.remove(input);
                        }
                        validateInput();
                        refreshPlaceholders();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent arg0) {
                        widgetSelected(arg0);
                    }
                });
            }
        }

        if (listEmpty) {
            final Label dummyLabel = new Label(inputsGroup, SWT.NONE);
            dummyLabel.setText("");
            final Label infoLabel = new Label(inputsGroup, SWT.NONE);
            infoLabel.setText(Messages.emptyInputTable);
        }

        createFormatSection(container);

        return container;
    }

    protected void createFileAndFolderSettings(Composite container) {

        Group configGroup = new Group(container, SWT.CENTER);
        GridData g = new GridData(GridData.FILL_HORIZONTAL);
        configGroup.setLayoutData(g);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.FILL_HORIZONTAL;
        g.minimumWidth = GROUPS_MIN_WIDTH;
        configGroup.setText(Messages.groupTitleTargetFile);
        configGroup.setLayout(new GridLayout(2, false));

        final Label fileNameLabel = new Label(configGroup, SWT.NONE);
        fileNameLabel.setText(Messages.outputLocFilename + COLON);
        final Text fileName = new Text(configGroup, SWT.SINGLE | SWT.BORDER);
        fileName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        fileName.setText(chosenFilename);
        fileName.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        fileName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                chosenFilename = fileName.getText();
                validateInput();
            }
        });

        new Label(configGroup, SWT.NONE).setText("");

        final Composite placeholderComp = new Composite(configGroup, SWT.NONE);
        placeholderComp.setLayout(new GridLayout(2, false));
        placeholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        Combo placeholderCombo =
            OutputWriterGuiUtils.createPlaceholderCombo(placeholderComp, OutputWriterComponentConstants.WORDLIST_OUTPUT);
        OutputWriterGuiUtils.createPlaceholderInsertButton(placeholderComp, placeholderCombo, fileName);

        new Label(configGroup, SWT.NONE).setText(Messages.targetFolder + COLON);
        final Combo directoryCombo = new Combo(configGroup, SWT.READ_ONLY);
        directoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        if (!paths.contains(OutputWriterComponentConstants.ROOT_DISPLAY_NAME)) {
            directoryCombo.add(OutputWriterComponentConstants.ROOT_DISPLAY_NAME);
        }
        for (String path : paths) {
            directoryCombo.add(path);
        }
        int index = MINUS_ONE;
        for (String item : directoryCombo.getItems()) {
            if (item.equals(chosenFolderForSaving)) {
                index = directoryCombo.indexOf(item);
            }
        }
        if (index >= 0) {
            directoryCombo.select(index);
        } else {
            directoryCombo.select(0);
        }
        chosenFolderForSaving = directoryCombo.getText();
        new Label(configGroup, SWT.NONE).setText(Messages.subFolder + COLON);
        final Text additionalFolder = new Text(configGroup, SWT.SINGLE | SWT.BORDER);
        additionalFolder.setMessage(Messages.optionalMessage);
        additionalFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        additionalFolder.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                if (!((Text) arg0.getSource()).getText().isEmpty()) {
                    chosenFolderForSaving =
                        OutputWriterComponentConstants.ROOT_DISPLAY_NAME + File.separator + ((Text) arg0.getSource()).getText();
                } else {
                    chosenFolderForSaving = OutputWriterComponentConstants.ROOT_DISPLAY_NAME;
                }
                validateInput();
            }
        });

        new Label(configGroup, SWT.NONE).setText("");
        new Label(configGroup, SWT.NONE).setText(Messages.onlyOneSubfolderMessage);
        new Label(configGroup, SWT.NONE).setText("");

        Composite dirPlaceholderComposite = new Composite(configGroup, SWT.NONE);
        dirPlaceholderComposite.setLayout(new GridLayout(2, false));
        dirPlaceholderComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        final Combo dirPlaceholderCombo =
            OutputWriterGuiUtils.createPlaceholderCombo(dirPlaceholderComposite, OutputWriterComponentConstants.WORDLIST_OUTPUT);
        final Button dirInsertButton =
            OutputWriterGuiUtils.createPlaceholderInsertButton(dirPlaceholderComposite, dirPlaceholderCombo, additionalFolder);

        if (directoryCombo.getSelectionIndex() > 0) {
            additionalFolder.setEnabled(false);
            dirPlaceholderCombo.setEnabled(false);
            dirInsertButton.setEnabled(false);
        }

        directoryCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                chosenFolderForSaving = "" + ((Combo) arg0.getSource()).getText();
                additionalFolder.setEnabled(((Combo) arg0.getSource()).getText().equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
                dirPlaceholderCombo.setEnabled(((Combo) arg0.getSource()).getText()
                    .equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
                dirInsertButton.setEnabled(((Combo) arg0.getSource()).getText().equals(OutputWriterComponentConstants.ROOT_DISPLAY_NAME));
                validateInput();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
                validateInput();
            }
        });
        additionalFolder.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));

    }

    protected void createFormatSection(Composite container) {

        Group configGroup = new Group(container, SWT.CENTER);
        GridData g = new GridData(GridData.FILL_HORIZONTAL);
        configGroup.setLayoutData(g);
        g.grabExcessHorizontalSpace = true;
        g.horizontalAlignment = GridData.BEGINNING;
        g.minimumWidth = GROUPS_MIN_WIDTH;
        configGroup.setText(Messages.groupTitleFormat);
        configGroup.setLayout(new GridLayout(2, false));

        final Label headerLabel = new Label(configGroup, SWT.NONE);
        headerLabel.setText(Messages.header + COLON + "\n" + Messages.headerMessage);
        final StyledText header = new StyledText(configGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData headerGridData = new GridData(GridData.FILL_BOTH);
        headerGridData.heightHint = HEADER_HEIGHT;
        header.setLayoutData(headerGridData);
        header.setText(chosenHeader);
        header.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                chosenHeader = header.getText();
                validateInput();
            }
        });

        WhitespaceShowListener headerWhitespaceListener = new WhitespaceShowListener();
        headerWhitespaceListener.setScriptingText(header);
        header.addPaintListener(headerWhitespaceListener);
        headerWhitespaceListener.setOn(true);
        headerWhitespaceListener.drawStyledText();

        new Label(configGroup, SWT.NONE).setText("");

        final Composite headerPlaceholderComp = new Composite(configGroup, SWT.NONE);
        headerPlaceholderComp.setLayout(new GridLayout(2, false));
        headerPlaceholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        headerPlaceholderCombo = OutputWriterGuiUtils.createPlaceholderCombo(headerPlaceholderComp, new String[0]);
        OutputWriterGuiUtils.createPlaceholderInsertButton(headerPlaceholderComp, headerPlaceholderCombo, header);

        final Label formatStringLabel = new Label(configGroup, SWT.NONE);
        formatStringLabel.setText(Messages.format + COLON + "\n" + Messages.formatMessage);
        final StyledText formatString = new StyledText(configGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData formatGridData = new GridData(GridData.FILL_BOTH);
        formatGridData.heightHint = FORMAT_HEIGHT;
        formatString.setLayoutData(formatGridData);
        formatString.setText(chosenFormatString);
        // formatString.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        formatString.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                chosenFormatString = formatString.getText();
                validateInput();
            }
        });

        WhitespaceShowListener formatWhitespaceListener = new WhitespaceShowListener();
        formatWhitespaceListener.setScriptingText(formatString);
        formatString.addPaintListener(formatWhitespaceListener);
        formatWhitespaceListener.setOn(true);
        formatWhitespaceListener.drawStyledText();

        new Label(configGroup, SWT.NONE).setText("");

        final Composite formatPlaceholderComp = new Composite(configGroup, SWT.NONE);
        formatPlaceholderComp.setLayout(new GridLayout(2, false));
        formatPlaceholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        formatPlaceholderCombo = OutputWriterGuiUtils.createPlaceholderCombo(formatPlaceholderComp, new String[0]);
        OutputWriterGuiUtils.createPlaceholderInsertButton(formatPlaceholderComp, formatPlaceholderCombo, formatString);

        refreshPlaceholders();

        new Label(configGroup, SWT.NONE).setText(Messages.handleExisting + COLON);
        final Combo existingFileCombo = new Combo(configGroup, SWT.READ_ONLY);
        existingFileCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        String[] handlingOptions = { Messages.handleAppend, Messages.handleAutoRename, Messages.handleOverride };
        existingFileCombo.setItems(handlingOptions);

        // Select correct item
        switch (chosenHandle) {
        case APPEND:
            existingFileCombo.select(existingFileCombo.indexOf(Messages.handleAppend));
            break;
        case OVERRIDE:
            existingFileCombo.select(existingFileCombo.indexOf(Messages.handleOverride));
            break;
        case AUTORENAME:
            existingFileCombo.select(existingFileCombo.indexOf(Messages.handleAutoRename));
            break;
        default:
            existingFileCombo.select(existingFileCombo.indexOf(Messages.handleAppend));
            LogFactory.getLog(getClass()).error("Handling existing files: Option can not be selected.");
        }

        existingFileCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (existingFileCombo.getText().equals(Messages.handleAppend)) {
                    chosenHandle = HandleExistingFile.APPEND;
                } else if (existingFileCombo.getText().equals(Messages.handleAutoRename)) {
                    chosenHandle = HandleExistingFile.AUTORENAME;
                } else if (existingFileCombo.getText().equals(Messages.handleOverride)) {
                    chosenHandle = HandleExistingFile.OVERRIDE;
                } else {
                    chosenHandle = HandleExistingFile.APPEND;
                    LogFactory.getLog(getClass()).error("Handling existing files: Option does not exist.");
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        new Label(configGroup, SWT.NONE).setText("");
        new Label(configGroup, SWT.NONE).setText(Messages.previousIterationMessage);
        new Label(configGroup, SWT.NONE).setText("");
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }

    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);

        validateInput();
    }

    protected void validateInput() {

        // Check if input fields are empty
        boolean isValid = !chosenFilename.isEmpty();

        isValid = isValid && !otherOutputLocationFileNamesWithPaths.contains(chosenFolderForSaving + File.separator + chosenFilename);

        List<String> forbiddenFilenames = Arrays.asList(OutputWriterComponentConstants.PROBLEMATICFILENAMES_WIN);
        isValid = isValid && !forbiddenFilenames.contains(chosenFilename.toUpperCase());

        // enable/disable "ok"
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);
    }

    /**
     * 
     * Creates selectable placeholders for header and formatString.
     *
     */
    protected void refreshPlaceholders() {
        List<String> placeholderList = new ArrayList<String>();
        List<String> headerPlaceholderList = new ArrayList<String>();

        headerPlaceholderList.add(OutputWriterComponentConstants.PH_LINEBREAK);
        headerPlaceholderList.add(OutputWriterComponentConstants.PH_TIMESTAMP);
        headerPlaceholderList.add(OutputWriterComponentConstants.PH_EXECUTION_COUNT);

        Collections.sort(chosenInputSet);

        for (String input : chosenInputSet) {
            placeholderList.add(OutputWriterComponentConstants.PH_PREFIX + input + OutputWriterComponentConstants.PH_SUFFIX);

            // Placeholder for input name is currently not needed
            /*
             * placeholderList.add(OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME +
             * OutputWriterComponentConstants.PH_DELIM + input + OutputWriterComponentConstants.PH_SUFFIX);
             * headerPlaceholderList.add(OutputWriterComponentConstants.PH_PREFIX + OutputWriterComponentConstants.INPUTNAME +
             * OutputWriterComponentConstants.PH_DELIM + input + OutputWriterComponentConstants.PH_SUFFIX);
             */
        }
        placeholderList.add(OutputWriterComponentConstants.PH_LINEBREAK);
        placeholderList.add(OutputWriterComponentConstants.PH_TIMESTAMP);
        placeholderList.add(OutputWriterComponentConstants.PH_EXECUTION_COUNT);

        formatPlaceholderCombo.setItems((String[]) placeholderList.toArray(new String[placeholderList.size()]));
        formatPlaceholderCombo.select(0);

        headerPlaceholderCombo.setItems((String[]) headerPlaceholderList.toArray(new String[headerPlaceholderList.size()]));
        headerPlaceholderCombo.select(0);
    }

    /**
     * Initialize values in dialog fields for editing.
     * 
     * @param out Output Location to edit
     */
    public void initializeValues(OutputLocation out) {

        chosenFilename = out.getFilename();
        chosenFolderForSaving = out.getFolderForSaving();
        chosenFormatString = out.getFormatString();
        //Copy list to prevent side effects.
        chosenInputSet = new ArrayList<String>(out.getInputs());
        chosenHandle = out.getHandleExistingFile();
        chosenHeader = out.getHeader();

    }

}
