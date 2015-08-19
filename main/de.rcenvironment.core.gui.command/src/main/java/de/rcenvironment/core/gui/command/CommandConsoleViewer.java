/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.command;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import de.rcenvironment.core.command.api.CommandExecutionResult;
import de.rcenvironment.core.command.api.CommandExecutionService;
import de.rcenvironment.core.command.spi.AbstractInteractiveCommandConsole;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * A visual command console for executing RCE commands.
 * 
 * @author Marc Stammerjohann
 * @author Doreen Seider (command history tweaks)
 * @author Robert Mischke (fixed #10886)
 */
public class CommandConsoleViewer extends ViewPart {

    private static final String RCEPROMPT = "rce>";

    private static final int RCEPROMPTLENGTH = RCEPROMPT.length();

    private static final String KEYCOMMAND = "UsedCommands";

    private static final String SAVEDCOMMANDCOUNTER = "SavedCommandCounter";

    private static final Color PROMPT_COLOR_BLUE = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

    /** Constant. Command History is limited to 30 entries. */
    private static final int COMMAND_LIMIT = 30;

    /** Used to insert text in new line. It is not allowed to insert text behind this position. */
    private int caretLinePosition;

    /** Used to extract text from styled text. */
    private int currentLine;

    private Action clearConsoleAction;

    private Action copyAction;

    private Action pasteAction;

    private StyledText styledtext;

    private CommandConsoleOutputAdapter textOutputReceiver;

    private CommandExecutionService commandExecutionService;

    private PersistentSettingsService persistentSettingsService;

    private final List<String> usedCommands = new ArrayList<String>();

    private final String lineBreak = "\n";

    /** starts at -1. */
    private int commandPosition = 0 - 1;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * An {@link Action} to clear the displayed text in the console.
     */
    private final class ClearConsoleAction extends Action {

        public ClearConsoleAction(String clearConsoleActionContextMenuLabel) {
            super(clearConsoleActionContextMenuLabel);
        }

        public void run() {
            currentLine = 0;
            styledtext.selectAll();
            insertText("");
            insertRCEPrompt();
            setSelection(caretLinePosition);
            clearConsoleAction.setEnabled(false);
        }
    }

    /**
     * An {@link Action} to paste text in the console.
     */
    private final class PasteAction extends Action {

        public PasteAction(String text) {
            super(text);
        }

        public void run() {
            // if multiple lines are in the clipboard, extract the first one and put it back to the clipboard so that only this line will be
            // pasted
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            try {
                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String clipboardText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    if (clipboardText.contains("\n")) {
                        clipboardText = clipboardText.substring(0, clipboardText.indexOf("\n"));
                        StringSelection stringSelection = new StringSelection(clipboardText);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                    }
                }
            } catch (UnsupportedFlavorException | IOException e) {
                LogFactory.getLog(getClass()).debug("Error when checking the clipboad for multi-line content: " + e.getMessage());
            }

            styledtext.paste();
            String line = getLineWithoutRCEPROMPT(currentLine);
            setStyledRange(caretLinePosition, line.length());
            if (!line.isEmpty()) {
                clearConsoleAction.setEnabled(true);
            }
        }
    }

    /**
     * An {@link Action} to copy text of the console.
     */
    private final class CopyAction extends Action {

        protected CopyAction(String text) {
            super(text);
        }

        public void run() {
            styledtext.copy();
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        /* Layout Section */
        GridLayout gridLayout = new GridLayout(1, false);
        parent.setLayout(gridLayout);
        GridData gridDataLabel = new GridData(GridData.FILL_HORIZONTAL);
        GridData gridDataStyledText = new GridData(GridData.FILL_BOTH);

        /* Label Section */
        Label label = new Label(parent, SWT.BORDER);
        label.setText(Messages.defaultLabelText);
        label.setLayoutData(gridDataLabel);

        /* Text Section */
        styledtext = new StyledText(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        styledtext.setLayoutData(gridDataStyledText);

        /* Listener Section */
        styledtext.addKeyListener(new CommandKeyListener());
        styledtext.addTraverseListener(new CommandTraverseListener());
        styledtext.addMouseListener(new CommandMouseListener());
        styledtext.addVerifyKeyListener(new CommandVerifyKeyListener());

        insertRCEPrompt();

        makeActions();
        hookContextMenu();
        contributeToActionBars();
        registerServices();
        getCommandHistory();

        textOutputReceiver = new CommandConsoleOutputAdapter();
    }

    /** Register {@link CommandExecutionService} and {@link PersistentSettingsService}. */
    private void registerServices() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        commandExecutionService = serviceRegistryAccess.getService(CommandExecutionService.class);
        persistentSettingsService = serviceRegistryAccess.getService(PersistentSettingsService.class);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                CommandConsoleViewer.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(styledtext);
        styledtext.setMenu(menu);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(clearConsoleAction);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(copyAction);
        manager.add(pasteAction);
        manager.add(new Separator());
        manager.add(clearConsoleAction);
    }

    private void makeActions() {
        clearConsoleAction = new ClearConsoleAction(Messages.clearConsoleActionContextMenuLabel);
        clearConsoleAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.CLEARCONSOLE_16));
        clearConsoleAction.setEnabled(false);

        copyAction = new CopyAction(Messages.copyActionContextMenuLabel);
        copyAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.COPY_16));
        pasteAction = new PasteAction(Messages.pasteActionContextMenuLabel);
        pasteAction.setImageDescriptor(ImageManager.getInstance().getImageDescriptor(StandardImages.PASTE_16));
    }

    @Override
    public void setFocus() {
        styledtext.setFocus();
    }

    /** Retrieve commands from persistent service. */
    private void getCommandHistory() {
        String commands = persistentSettingsService.readStringValue(KEYCOMMAND);
        if (commands != null) {
            String[] commandSplit = StringUtils.splitAndUnescape(commands);
            for (int i = 0; i < commandSplit.length; i++) {
                addUsedCommand(commandSplit[i]);
            }
        }
    }

    /**
     * Save command with persistent service. Used as History. Commands are available after restart.
     * 
     * @param command to be saved
     */
    private void saveCommand(String command) {
        String savedCommands = persistentSettingsService.readStringValue(KEYCOMMAND);
        String savedCommandCounter = persistentSettingsService.readStringValue(SAVEDCOMMANDCOUNTER);
        String escapeAndConcat = null;
        int savedCounter;
        if (savedCommandCounter == null) {
            savedCounter = 0;
        } else {
            savedCounter = Integer.parseInt(savedCommandCounter);
        }
        if (savedCounter < COMMAND_LIMIT) {
            // saves new command
            if (savedCommands == null) {
                escapeAndConcat = StringUtils.escapeAndConcat(command);
            } else {
                String[] splitAndUnescape = splitAndUnescapeCommand(savedCommands, command, savedCounter);
                escapeAndConcat = StringUtils.escapeAndConcat(splitAndUnescape);
            }
            persistentSettingsService.saveStringValue(SAVEDCOMMANDCOUNTER, "" + ++savedCounter);
        } else {
            // if limit of saving commands is reached, last command will be removed
            String[] splitAndUnescape = splitAndRemoveLast(savedCommands, command);
            escapeAndConcat = StringUtils.escapeAndConcat(splitAndUnescape);
        }
        persistentSettingsService.saveStringValue(KEYCOMMAND, escapeAndConcat);
    }

    /** Splits saved commands and adds new command to be saved. */
    private String[] splitAndUnescapeCommand(String savedCommands, String command, int savedCounter) {
        String[] splitAndUnescape = new String[savedCounter + 1];
        String[] commandSplit = StringUtils.splitAndUnescape(savedCommands);
        for (int i = 0; i < commandSplit.length; i++) {
            splitAndUnescape[i] = commandSplit[i];
        }
        splitAndUnescape[splitAndUnescape.length - 1] = command;
        return splitAndUnescape;
    }

    /** removes last command. */
    private String[] splitAndRemoveLast(String savedCommands, String command) {
        String[] splitAndUnescape = StringUtils.splitAndUnescape(savedCommands);
        for (int i = 0; i < splitAndUnescape.length - 1; i++) {
            splitAndUnescape[i] = splitAndUnescape[i + 1];
        }
        splitAndUnescape[splitAndUnescape.length - 1] = command;
        return splitAndUnescape;
    }

    /** Changes output text color to black. */
    private void setStyledRange(int styledstart, int styledlength) {
        StyleRange styleRange = new StyleRange();
        styleRange.start = styledstart;
        styleRange.length = styledlength;
        styleRange.foreground = PROMPT_COLOR_BLUE;
        styledtext.setStyleRange(styleRange);
    }

    private void addUsedCommand(String command) {
        usedCommands.remove(command);
        int size = usedCommands.size();
        if (size == COMMAND_LIMIT) {
            usedCommands.remove(size - 1);
        }
        usedCommands.add(0, command);
    }

    /** Display all used commands. */
    private void displayUsedCommands() {
        // reverse order to display commands chronologically
        List<String> usedCommandsToDisplay = new ArrayList<>(usedCommands);
        Collections.reverse(usedCommandsToDisplay);
        for (String command : usedCommandsToDisplay) {
            insertTextWithLineBreak(command);
            selectNewLine(command.length());
        }
        insertRCEPrompt();
        increaseCurrentLine();
    }

    /** Insert RCE Prompt in the current line. */
    private void insertRCEPrompt() {
        insertText(RCEPROMPT);
        setCaretLinePosition(getCurrentCaretLocation() + RCEPROMPTLENGTH);
        setSelection(caretLinePosition);
        setStyledRange(caretLinePosition - RCEPROMPTLENGTH, RCEPROMPTLENGTH);
    }

    /**
     * Display a command from a given String without line break.
     * 
     * @param command to be displayed
     */
    private void displayCommand(String command) {
        String line = getLineWithoutRCEPROMPT(currentLine);
        if (line.isEmpty()) {
            setSelection(caretLinePosition);
        } else {
            setSelectionRange(caretLinePosition, line.length());
        }
        insertText(command);
        setStyledRange(caretLinePosition, command.length());
        setSelection(caretLinePosition + command.length());
    }

    /**
     * Display a messages from a given String with a line break.
     * 
     * @param message to be displayed
     */
    private void displayMessage(String message) {
        int selected = message.length();
        String line = getLine(currentLine);
        if (line.isEmpty()) {
            insertTextWithLineBreak(message);
            selectNewLine(selected);
        } else {
            int caretOffsetLine = getCurrentCaretLocation();
            setSelectionRange(caretLinePosition - RCEPROMPTLENGTH, line.length());
            insertTextWithLineBreak(message);

            int currentCaretLocation = getCurrentCaretLocation();
            insertText(line);
            setStyledRange(currentCaretLocation, line.length());
            selectLineAndUpdateCaretPosition(currentCaretLocation + RCEPROMPTLENGTH);
            // select same caret location as it was before
            setSelection(caretOffsetLine + selected + 1);
        }
    }

    /** displays commands, when arrow up is pressed. */
    private void displayCommands() {
        if (commandPosition + 1 < usedCommands.size()) {
            commandPosition++;
            displayCommand(usedCommands.get(commandPosition));
        }
    }

    /** displays commands (reverse), when arrow down is pressed. */
    private void displayCommandsReverse() {
        if (commandPosition < usedCommands.size() && commandPosition > 0) {
            commandPosition--;
            displayCommand(usedCommands.get(commandPosition));
        }
    }

    /** Reset command position to -1. */
    private void resetCommandPosition() {
        this.commandPosition = 0 - 1;
    }

    private void setSelection(int start) {
        styledtext.setSelection(start);
    }

    private void setSelectionRange(int start, int length) {
        styledtext.setSelectionRange(start, length);
    }

    private void insertText(String text) {
        styledtext.insert(text);
    }

    private void insertTextWithLineBreak(String text) {
        styledtext.insert(text + lineBreak);
        increaseCurrentLine();
    }

    private void selectNewLine(int selection) {
        setSelection(getCurrentCaretLocation() + selection + 1);
    }

    private void selectLineAndUpdateCaretPosition(int selection) {
        setSelection(selection);
        // change caretLinePosition to the new line
        setCaretLinePosition(getCurrentCaretLocation());
    }

    private void setCaretLinePosition(int caretLinePosition) {
        this.caretLinePosition = caretLinePosition;
    }

    private void increaseCurrentLine() {
        currentLine++;
    }

    private int getCurrentCaretLocation() {
        return styledtext.getCaretOffset();
    }

    private String getLine(int line) {
        try {
            return styledtext.getLine(line);
        } catch (IllegalArgumentException e) {
            log.error("Invalid line " + line + " requested; line count is " + styledtext.getLineCount(), e);
            return "";
        }
    }

    private String getLineWithoutRCEPROMPT(int line) {
        return getLine(line).replaceFirst(RCEPROMPT, "");
    }

    /**
     * If a key (between 'a' and 'z', '0' and '9') is pressed, caret selects the command line (where the latest RCEPROMPT is). If command
     * line contains text, caret is set at the end.
     */
    private void moveCaretToCommandLine() {
        String line = getLineWithoutRCEPROMPT(currentLine);
        setSelection(caretLinePosition + line.length());
    }

    /** A {@link KeyListener} to react on pressed or released key's. */
    private class CommandKeyListener implements KeyListener {

        public void keyReleased(KeyEvent keyEvent) {
            // disable clearConsoleAction, if after Backspace/Delete the first line is empty
            if (keyEvent.keyCode == SWT.BS || keyEvent.keyCode == SWT.DEL) {
                if (getLineWithoutRCEPROMPT(0).isEmpty()) {
                    clearConsoleAction.setEnabled(false);
                }
            }
        }

        public void keyPressed(KeyEvent keyEvent) {
            if (!getLineWithoutRCEPROMPT(0).isEmpty()) {
                clearConsoleAction.setEnabled(true);
            }
            if (keyEvent.keyCode == SWT.CR) {
                String command = getLineWithoutRCEPROMPT(currentLine);
                if (command.equals(Messages.historyUsedCommand)) {
                    displayUsedCommands();
                } else {
                    resetCommandPosition();
                    addUsedCommand(command);
                    setCaretLinePosition(caretLinePosition + command.length());
                    increaseCurrentLine();
                    insertRCEPrompt();
                    // run command in separate thread to keep the UI responsive
                    SharedThreadPool.getInstance().execute(new ExecuteCommand(command));
                }
            }
            if (!getLineWithoutRCEPROMPT(currentLine).isEmpty() && getCurrentCaretLocation() > caretLinePosition) {
                // set new character to prompt color
                setStyledRange(getCurrentCaretLocation() - 1, 1);
            }
        }
    }

    /** A {@link TraverseListener} to change the keys behavior. (Return, Arrow Up and Arrow Down) */
    private class CommandTraverseListener implements TraverseListener {

        @Override
        public void keyTraversed(TraverseEvent event) {
            switch (event.detail) {
            case SWT.TRAVERSE_RETURN:
                executeReturn(event);
                break;
            case SWT.TRAVERSE_ARROW_PREVIOUS:
                if (event.keyCode == SWT.ARROW_UP) {
                    disableEvent(event);
                    displayCommands();
                    if (!getLineWithoutRCEPROMPT(currentLine).isEmpty()) {
                        clearConsoleAction.setEnabled(true);
                    }
                }
                break;
            case SWT.TRAVERSE_ARROW_NEXT:
                if (event.keyCode == SWT.ARROW_DOWN) {
                    disableEvent(event);
                    displayCommandsReverse();
                }
                break;
            default:
                break;
            }
        }

        private void disableEvent(TraverseEvent event) {
            event.doit = true;
            event.detail = SWT.TRAVERSE_NONE;
        }

        /**
         * "Return" is enabled: if the selected line has text. <br>
         * "Return" is disabled: if the selected line is empty or the selected line is above the command
         * 
         * @param event
         */
        private void executeReturn(TraverseEvent event) {
            String line = getLineWithoutRCEPROMPT(currentLine);
            int currentCaretLocation = getCurrentCaretLocation();
            if (currentCaretLocation == caretLinePosition && line.isEmpty()) {
                disableEvent(event);
            } else if (currentCaretLocation < caretLinePosition) {
                disableEvent(event);
            } else {
                if (!line.isEmpty()) {
                    // setSelection(styledtext.getCaretOffset() + (line.length() - (styledtext.getCaretOffset() -
                    // caretPosition)));
                    setSelection(styledtext.getCaretOffset() + line.length());
                }
            }
        }
    }

    /**
     * A {@link VerifyKeyListener} to change the keys behavior.<br>
     * Changes the behavior of the key, if the current caret position is lower than the {@link CommandConsoleViewer#caretLinePosition}.
     */
    private class CommandVerifyKeyListener implements VerifyKeyListener {

        // keys are always enable, even above the caretLinePosition
        private final List<Integer> enabledEvents = new ArrayList<Integer>();

        {
            enabledEvents.add(SWT.ARROW_LEFT);
            enabledEvents.add(SWT.ARROW_RIGHT);
            enabledEvents.add(SWT.PAGE_UP);
            enabledEvents.add(SWT.PAGE_DOWN);
            enabledEvents.add(SWT.HOME);
            enabledEvents.add(SWT.END);
        }

        @Override
        public void verifyKey(VerifyEvent event) {
            int currentCaretLocation = getCurrentCaretLocation();
            int selectionRangeStart = styledtext.getSelectionRange().x;
            boolean emptyLine = getLine(currentLine).isEmpty();
            // disable all key actions if caret location or selection range start is above caret line position or if line is empty
            if (emptyLine || (caretLinePosition > currentCaretLocation || caretLinePosition > selectionRangeStart)) {
                if (!enabledEvents.contains(event.keyCode)) {
                    if (event.stateMask == 0
                        && ((event.keyCode >= 'a' && event.keyCode <= 'z') || (event.keyCode >= '0' && event.keyCode <= '9'))) {
                        moveCaretToCommandLine();
                    } else {
                        event.doit = false;
                    }
                }
                // enable a some key (combinations)
                // allow copy with CTRL + C
                if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {
                    event.doit = true;
                }
            } else {
                if (event.keyCode == SWT.BS) {
                    checkBackspace(event, currentCaretLocation);
                } else if (event.keyCode == SWT.DEL) {
                    checkDelete(event, currentCaretLocation);
                } else if ((event.stateMask == SWT.CTRL && event.keyCode == 'v')
                    || (event.stateMask == SWT.SHIFT && event.keyCode == SWT.INSERT)) {
                    // key event of 'paste' are using the 'pasteAction'
                    event.doit = false;
                    pasteAction.run();
                }
            }
        }

        /**
         * "Backspace" is enabled: if the current command line has text. <br>
         * "Backspace" is disabled: if the caret is at the beginning of the line.
         * 
         * @param event
         * @param caretOffset
         */
        private void checkBackspace(VerifyEvent event, int caretOffset) {
            String line = getLineWithoutRCEPROMPT(currentLine);
            if (caretLinePosition < caretOffset && !line.isEmpty()) {
                event.doit = true;
            } else {
                event.doit = false;
            }
        }

        /**
         * "Delete" is enabled: if the current command line has text. <br>
         * 
         * @param event
         * @param caretOffset
         */
        private void checkDelete(VerifyEvent event, int caretOffset) {
            String line = getLineWithoutRCEPROMPT(currentLine);
            if (!line.isEmpty()) {
                event.doit = true;
            } else {
                event.doit = false;
            }
        }
    }

    /**
     * A {@link MouseListener} to set enable settings of Copy and Paste.
     */
    private class CommandMouseListener implements MouseListener {

        @Override
        public void mouseDown(MouseEvent event) {
            int currentCaretLocation = getCurrentCaretLocation();
            if (caretLinePosition > currentCaretLocation) {
                pasteAction.setEnabled(false);
            } else {
                pasteAction.setEnabled(true);
            }
            if (!styledtext.getSelectionText().isEmpty()) {
                copyAction.setEnabled(true);
            } else {
                copyAction.setEnabled(false);
            }
        }

        @Override
        public void mouseDoubleClick(MouseEvent event) {}

        @Override
        public void mouseUp(MouseEvent event) {}

    }

    /** Thread to execute the command. */
    private class ExecuteCommand implements Runnable {

        private String command;

        private Display display = Display.getDefault();

        private int line;

        private volatile boolean writing;

        public ExecuteCommand(String command) {
            this.command = command;
            // this.line = ++currentLine;
            this.writing = true;
        }

        @Override
        @TaskDescription("Execute Command")
        public void run() {
            saveCommand(command);
            List<String> tokens = getTokens();
            Future<CommandExecutionResult> asyncExecMultiCommand =
                commandExecutionService.asyncExecMultiCommand(tokens, textOutputReceiver, "command console");
            // waitWithDelay();
            // waitForExecToFinish(asyncExecMultiCommand);
        }

        /** waiting for the execution to finish, while text is displayed user cannot enter new commands. */
        private void waitWithDelay() {
            int sleepTime = 5;
            try {
                while (writing) {
                    Thread.sleep(sleepTime);
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            // TODO this line can throw an "Index out of bounds" exception; check before this code is being used again
                            if (styledtext.isDisposed() || (writing && getLineWithoutRCEPROMPT(line++).isEmpty())) {
                                writing = false;
                            }
                        }
                    });
                }
                display.asyncExec(new DisplayText(RCEPROMPT));
            } catch (InterruptedException e) {
                log.error("Exception while waiting for console output", e);
            }
        }

        /** waiting until the execution of the command is finished. */
        private void waitForExecToFinish(Future<CommandExecutionResult> asyncExecMultiCommand) {
            try {
                CommandExecutionResult commandExecutionResult = asyncExecMultiCommand.get();
                if (commandExecutionResult == CommandExecutionResult.DEFAULT || commandExecutionResult == CommandExecutionResult.ERROR) {
                    display.asyncExec(new DisplayText(RCEPROMPT));
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception while waiting for console command to complete", e);
            }
        }

        private List<String> getTokens() {
            final List<String> tokens = new LinkedList<String>();
            String[] arguments = command.split(" ");
            for (int i = 0; i < arguments.length; i++) {
                tokens.add(arguments[i]);
            }
            return tokens;
        }
    }

    /** Thread to interact with the swt styledtext. */
    private class DisplayText implements Runnable {

        private String text;

        public DisplayText(String text) {
            this.text = text;
        }

        @Override
        @TaskDescription("Display Text")
        public void run() {
            if (text.equals(RCEPROMPT)) {
                insertRCEPrompt();
            } else {
                displayMessage(text);
            }
        }
    }

    /** Class for handling command input and printing out the output. */
    private class CommandConsoleOutputAdapter extends AbstractInteractiveCommandConsole {

        private Display display = Display.getDefault();

        public CommandConsoleOutputAdapter() {
            super(commandExecutionService);
        }

        /** Adds an Output to the console. */
        public void addOutput(String line) {
            if (line.contains("\n")) {
                while (line.contains("\n")) {
                    // displays the string before the line break
                    display.asyncExec(new DisplayText(line.substring(0, line.indexOf("\n"))));
                    line = line.substring(line.indexOf("\n") + 1);
                }
                if (!line.equals("")) {
                    display.asyncExec(new DisplayText(line));
                }
            } else {
                display.asyncExec(new DisplayText(line));
            }
        }
    }
}
