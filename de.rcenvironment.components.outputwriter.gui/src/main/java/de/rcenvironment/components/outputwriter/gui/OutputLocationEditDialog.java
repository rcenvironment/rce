/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import de.rcenvironment.components.outputwriter.common.OutputWriterValidatorHelper;
import de.rcenvironment.core.gui.utils.incubator.AlphanumericalTextContraintListener;
import de.rcenvironment.core.gui.workflow.executor.properties.WhitespaceShowListener;

/**
 * 
 * Edit Dialog for an OutputLocation.
 *
 * @author Brigitte Boden
 * @author Dominik Schneider
 * @author Kathrin Schaffert (#17016, #14895)
 */

public class OutputLocationEditDialog extends Dialog {

    private static final char SPACE = ' ';

    private static final String SEMICOLON = ";";

    private static final String LINE_SEP = System.getProperty("line.separator");

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

    private List<String> generalHeaderPlaceholderList;

    private List<String> generalFormatPlaceholderList;

    private CCombo formatPlaceholderCombo;

    private CCombo headerPlaceholderCombo;

    private WarningErrorLabel warningLabel;

    /**
     * Dialog for creating or editing an endpoint.
     * 
     * @param parentShell   parent Shell
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
        chosenInputSet = new ArrayList<>();
        chosenHeader = "";
        chosenFormatString = "";

        generalHeaderPlaceholderList = new ArrayList<>();
        generalFormatPlaceholderList = new ArrayList<>();
        generalHeaderPlaceholderList.add(OutputWriterComponentConstants.PH_LINEBREAK);
        generalHeaderPlaceholderList.add(OutputWriterComponentConstants.PH_TIMESTAMP);
        generalHeaderPlaceholderList.add(OutputWriterComponentConstants.PH_EXECUTION_COUNT);

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
        GridData g = new GridData(GridData.FILL_BOTH);
        g.horizontalAlignment = GridData.CENTER;
        container.setLayoutData(g);

        createFileAndFolderSettings(container);

        Group inputsGroup = new Group(container, SWT.LEFT);
        GridData gr = new GridData(GridData.FILL_HORIZONTAL);
        inputsGroup.setLayoutData(gr);
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
                        refreshPlaceholders();
                        refreshFormatPlaceholderList();
                        updateWarningLabel();
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
        g.minimumWidth = GROUPS_MIN_WIDTH;
        configGroup.setText(Messages.groupTitleTargetFile);
        configGroup.setLayout(new GridLayout(2, false));

        final Label fileNameLabel = new Label(configGroup, SWT.NONE);
        fileNameLabel.setText(Messages.outputLocFilename + COLON);
        final Text fileName = new Text(configGroup, SWT.SINGLE | SWT.BORDER);
        fileName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        fileName.setText(chosenFilename);
        fileName.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));
        fileName.addModifyListener(ignoredEvent -> {
            chosenFilename = fileName.getText();
            setOKButtonActivation();
        });

        new Label(configGroup, SWT.NONE).setText("");

        final Composite placeholderComp = new Composite(configGroup, SWT.NONE);
        GridLayout placeholderCompLayout = new GridLayout(2, false);
        placeholderCompLayout.marginWidth = 0;
        placeholderComp.setLayout(placeholderCompLayout);
        placeholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        CCombo placeholderCombo =
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
        additionalFolder.addModifyListener(event -> {
            if (!((Text) event.getSource()).getText().isEmpty()) {
                chosenFolderForSaving =
                    OutputWriterComponentConstants.ROOT_DISPLAY_NAME + File.separator + ((Text) event.getSource()).getText();
            } else {
                chosenFolderForSaving = OutputWriterComponentConstants.ROOT_DISPLAY_NAME;
            }
            setOKButtonActivation();
        });

        new Label(configGroup, SWT.NONE).setText("");
        new Label(configGroup, SWT.NONE).setText(Messages.onlyOneSubfolderMessage);
        new Label(configGroup, SWT.NONE).setText("");

        Composite dirPlaceholderComp = new Composite(configGroup, SWT.NONE);
        GridLayout dirPlaceholderCompLayout = new GridLayout(2, false);
        dirPlaceholderCompLayout.marginWidth = 0;
        dirPlaceholderComp.setLayout(dirPlaceholderCompLayout);
        dirPlaceholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        final CCombo dirPlaceholderCombo =
            OutputWriterGuiUtils.createPlaceholderCombo(dirPlaceholderComp, OutputWriterComponentConstants.WORDLIST_OUTPUT);
        final Button dirInsertButton =
            OutputWriterGuiUtils.createPlaceholderInsertButton(dirPlaceholderComp, dirPlaceholderCombo, additionalFolder);

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
                setOKButtonActivation();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
                setOKButtonActivation();
            }
        });
        additionalFolder.addListener(SWT.Verify, new AlphanumericalTextContraintListener(FORBIDDEN_CHARS));

    }

    protected void createFormatSection(Composite container) {
        Group configGroup = new Group(container, SWT.CENTER);
        GridData g = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        configGroup.setLayoutData(g);
        g.minimumWidth = GROUPS_MIN_WIDTH;
        configGroup.setText(Messages.groupTitleFormat);
        GridLayout configGroupLayout = new GridLayout(2, false);
        configGroup.setLayout(configGroupLayout);

        final Label headerLabel = new Label(configGroup, SWT.NONE);
        headerLabel.setText(Messages.header + COLON + "\n" + Messages.headerMessage);
        final StyledText header = new StyledText(configGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData headerGridData = new GridData(GridData.FILL_HORIZONTAL);
        headerGridData.heightHint = HEADER_HEIGHT;
        header.setLayoutData(headerGridData);
        header.setText(chosenHeader);
        header.addModifyListener(event -> {
            chosenHeader = header.getText();
            setOKButtonActivation();
            updateWarningLabel();
        });

        // prevents adding linebreaks via enter
        header.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (!header.getText().isEmpty()) {
                    if (e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
                        header.setText(cleanCarriageReturn(header.getText()));
                        header.setSelection(header.getText().length());
                    }
                }
            }
        });

        WhitespaceShowListener headerWhitespaceListener = new WhitespaceShowListener(header);
        header.addPaintListener(headerWhitespaceListener);
        headerWhitespaceListener.setEnabled(true);
        headerWhitespaceListener.drawStyledText();

        new Label(configGroup, SWT.NONE).setText("");

        final Composite headerPlaceholderComp = new Composite(configGroup, SWT.NONE);
        GridLayout headerPlaceholderCompLayout = new GridLayout(2, false);
        headerPlaceholderCompLayout.marginWidth = 0;
        headerPlaceholderComp.setLayout(headerPlaceholderCompLayout);
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
        formatString.addModifyListener(ignoredEvent -> {
            chosenFormatString = formatString.getText();
            setOKButtonActivation();
            updateWarningLabel();
        });

        // prevents adding linebreaks via enter
        formatString.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (formatString.getText().length() > 0) {
                    if (e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
                        formatString.setText(cleanCarriageReturn(formatString.getText()));
                        formatString.setSelection(formatString.getText().length());
                    }
                }
            }
        });

        WhitespaceShowListener formatWhitespaceListener = new WhitespaceShowListener(formatString);
        formatString.addPaintListener(formatWhitespaceListener);
        formatWhitespaceListener.setEnabled(true);
        formatWhitespaceListener.drawStyledText();

        new Label(configGroup, SWT.NONE).setText("");

        final Composite formatPlaceholderComp = new Composite(configGroup, SWT.NONE);
        GridLayout formatPlaceholderCompLayout = new GridLayout(2, false);
        formatPlaceholderCompLayout.marginWidth = 0;
        formatPlaceholderComp.setLayout(formatPlaceholderCompLayout);
        formatPlaceholderComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        formatPlaceholderCombo = OutputWriterGuiUtils.createPlaceholderCombo(formatPlaceholderComp, new String[0]);
        OutputWriterGuiUtils.createPlaceholderInsertButton(formatPlaceholderComp, formatPlaceholderCombo, formatString);

        refreshPlaceholders();
        refreshFormatPlaceholderList();

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

        createWarningLabel(configGroup);
    }

    @Override
    public void create() {
        super.create();
        // dialog title
        getShell().setText(title);

        setOKButtonActivation();
        updateWarningLabel();
    }

    protected Boolean validateTargetFileName() {

        // Check if input fields are empty
        boolean isValid = !chosenFilename.isEmpty();

        isValid = isValid && !otherOutputLocationFileNamesWithPaths.contains(chosenFolderForSaving + File.separator + chosenFilename);

        List<String> forbiddenFilenames = Arrays.asList(OutputWriterComponentConstants.PROBLEMATICFILENAMES_WIN);
        isValid = isValid && !forbiddenFilenames.contains(chosenFilename.toUpperCase());

        // enable/disable "ok"
        getButton(IDialogConstants.OK_ID).setEnabled(isValid);

        return isValid;
    }

    /**
     * 
     * Creates selectable placeholders for header and formatString.
     *
     */
    protected void refreshPlaceholders() {
        List<String> placeholderList = new ArrayList<>();
        List<String> headerPlaceholderList = new ArrayList<>();

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
        // Copy list to prevent side effects.
        chosenInputSet = new ArrayList<>(out.getInputs());
        chosenHandle = out.getHandleExistingFile();
        chosenHeader = out.getHeader();

    }

    private String cleanCarriageReturn(String tmp) {
        tmp = tmp.replaceAll("\r", "");
        tmp = tmp.replaceAll("\n", "");
        tmp = tmp.replace(Matcher.quoteReplacement(OutputWriterComponentConstants.PH_LINEBREAK),
            Matcher.quoteReplacement(OutputWriterComponentConstants.PH_LINEBREAK + LINE_SEP));
        return tmp;
    }

    private void refreshFormatPlaceholderList() {
        generalFormatPlaceholderList = new ArrayList<String>();
        generalFormatPlaceholderList.add(OutputWriterComponentConstants.PH_LINEBREAK);
        generalFormatPlaceholderList.add(OutputWriterComponentConstants.PH_TIMESTAMP);
        generalFormatPlaceholderList.add(OutputWriterComponentConstants.PH_EXECUTION_COUNT);

        for (String input : chosenInputSet) {
            String inputPlaceholder = OutputWriterComponentConstants.PH_PREFIX + input + OutputWriterComponentConstants.PH_SUFFIX;
            if (!generalFormatPlaceholderList.contains(inputPlaceholder)) {
                generalFormatPlaceholderList.add(inputPlaceholder);
            }
        }

    }

    private void setOKButtonActivation() {
        final List<String> headerValidationErrors = OutputWriterValidatorHelper.getValidationErrors(chosenHeader);
        final List<String> formatValidationErrors = OutputWriterValidatorHelper.getValidationErrors(chosenFormatString);

        final boolean headerValidationFailed = !headerValidationErrors.isEmpty();
        final boolean formatValidationFailed = !formatValidationErrors.isEmpty();
        final boolean targetFileNameValidationFailed = !validateTargetFileName();
        final boolean validationFailed = headerValidationFailed || formatValidationFailed || targetFileNameValidationFailed;
        final Button okButton = getButton(OK);
        okButton.setEnabled(!validationFailed);
    }

    private void updateWarningLabel() {
        warningLabel.clearWarnings();
        warningLabel.clearErrors();

        final List<String> headerValidationErrors = OutputWriterValidatorHelper.getValidationErrors(chosenHeader);
        final List<String> formatValidationErrors = OutputWriterValidatorHelper.getValidationErrors(chosenFormatString);

        final boolean headerValidationFailed = !headerValidationErrors.isEmpty();
        final boolean formatValidationFailed = !formatValidationErrors.isEmpty();

        if (!headerValidationFailed) {
            final StringBuilder warningBuilder = new StringBuilder();
            warningBuilder.append("Contains unknown placeholder: ");
            final List<String> headerValidationWarnings =
                OutputWriterValidatorHelper.getValidationWarnings(warningBuilder, chosenHeader, generalHeaderPlaceholderList);

            if (!headerValidationWarnings.isEmpty()) {
                final StringBuilder warningMessageBuilder = new StringBuilder();
                warningMessageBuilder.append("Header section:\n");
                warningMessageBuilder.append(String.join(SEMICOLON + SPACE, headerValidationWarnings));

                warningLabel.addWarning(warningMessageBuilder.toString());
            }
        } else {
            final StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append("Header section:\n");
            errorMessageBuilder.append(String.join(SEMICOLON + SPACE, headerValidationErrors));

            warningLabel.addError(errorMessageBuilder.toString());
        }

        if (!formatValidationFailed) {
            final StringBuilder warningBuilder = new StringBuilder();
            warningBuilder.append("Contains unknown placeholder: ");
            final List<String> formatValidationWarnings =
                OutputWriterValidatorHelper.getValidationWarnings(warningBuilder, chosenFormatString, generalFormatPlaceholderList);

            if (!formatValidationWarnings.isEmpty()) {
                final StringBuilder warningMessageBuilder = new StringBuilder();
                warningMessageBuilder.append("Format section:\n");
                warningMessageBuilder.append(String.join(SEMICOLON + SPACE, formatValidationWarnings));

                warningLabel.addWarning(warningMessageBuilder.toString());
            }
        } else {
            final StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append("Format section:\n");
            errorMessageBuilder.append(String.join(SEMICOLON + SPACE, formatValidationErrors));
            warningLabel.addError(errorMessageBuilder.toString());
        }
    }

    private void createWarningLabel(Group configGroup) {
        new Label(configGroup, SWT.NONE).setText("");
        warningLabel = new WarningErrorLabel(configGroup, SWT.NONE);
    }

}
