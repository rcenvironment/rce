/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.command;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import de.rcenvironment.core.gui.resources.api.FontManager;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardFonts;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.common.ClipboardHelper;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A visual command console for executing RCE commands.
 * 
 * @author Marc Stammerjohann
 * @author Doreen Seider (command history tweaks)
 * @author Robert Mischke (fixed #10886)
 * @author Sascha Zur
 */
public class CommandConsoleViewer extends ViewPart {

    private static final String RCEPROMPT = "rce>";

    private static final int RCEPROMPTLENGTH = RCEPROMPT.length();

    private static final Color PROMPT_COLOR_BLUE = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

    private static final int MAXIMUM_PASTE_LENGTH = 10000;

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

    // replacing the line break with the platform-independent one results in errors (invalid
    // argument) in #setSelection(int start);
    // investigate if "\n" causes issues
    private final String lineBreak = "\n";

    private final String platformIndependentLineBreak = System.lineSeparator();

    /** starts at -1. */
    private int commandPosition = 0 - 1;

    private final Log log = LogFactory.getLog(getClass());

    private CommandHandler commandService;

    private List<String> usedCommands;

    /**
     * An {@link Action} to clear the displayed text in the console.
     */
    private final class ClearConsoleAction extends Action {

        ClearConsoleAction(String clearConsoleActionContextMenuLabel) {
            super(clearConsoleActionContextMenuLabel);
        }

        @Override
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

        PasteAction(String text) {
            super(text);
        }

        @Override
        public void run() {
            // if multiple lines are in the clipboard, extract the first one and put it back to the
            // clipboard so that only this line will be
            String content = ClipboardHelper.getContentAsStringOrNull();
            if (content == null) {
                return;
            }

            final int contentLength = content.length();
            if (contentLength < MAXIMUM_PASTE_LENGTH) {
                if (content.contains(platformIndependentLineBreak)) {
                    content = content.replaceAll(platformIndependentLineBreak, " ");
                }

                ClipboardHelper.setContent(content);

                styledtext.paste();
                String line = getLineWithoutRCEPROMPT(currentLine);
                setStyledRange(caretLinePosition, line.length());
                if (!line.isEmpty()) {
                    clearConsoleAction.setEnabled(true);
                }
            } else {
                Display.getCurrent().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        String warningMessage = "The text could not be pasted because it is too long. "
                            + "Its length is " + contentLength + " characters but the maximum allowed length is "
                            + MAXIMUM_PASTE_LENGTH + " characters.";
                        MessageDialog.open(MessageDialog.ERROR, null, "Warning", warningMessage, SWT.NONE);
                    }
                });
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

        @Override
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
        styledtext.setFont(FontManager.getInstance().getFont(StandardFonts.CONSOLE_TEXT_FONT));
        styledtext.addModifyListener(new CommandModifyListener());
        insertRCEPrompt();

        makeActions();
        hookContextMenu();
        contributeToActionBars();
        registerServices();
        commandService = new CommandHandler();
        usedCommands = commandService.getUsedCommands();

        textOutputReceiver = new CommandConsoleOutputAdapter();
    }

    /** Register {@link CommandExecutionService}. */
    private void registerServices() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        commandExecutionService = serviceRegistryAccess.getService(CommandExecutionService.class);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            @Override
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

    /** Changes output text color to black. */
    private void setStyledRange(int styledstart, int styledlength) {
        StyleRange styleRange = new StyleRange();
        styleRange.start = styledstart;
        styleRange.length = styledlength;
        styleRange.foreground = PROMPT_COLOR_BLUE;
        styledtext.setStyleRange(styleRange);
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

    /**
     * As soon as the text is modified this listener ensures that the clearConsoleAction is enabled, if the text contains anything else than
     * the command prompt.
     */
    private class CommandModifyListener implements ModifyListener {

        private boolean containsContent() {
            for (int i = 0; i <= currentLine; i++) {
                if (!getLineWithoutRCEPROMPT(i).isEmpty()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            if (clearConsoleAction != null && !clearConsoleAction.isEnabled() && containsContent()) {
                clearConsoleAction.setEnabled(true);
            }
        }
    }

    /** A {@link KeyListener} to react on pressed or released key's. */
    private class CommandKeyListener implements KeyListener {

        @Override
        public void keyReleased(KeyEvent keyEvent) {
            // disable clearConsoleAction, if after Backspace/Delete the first line is empty
            if (keyEvent.keyCode == SWT.BS || keyEvent.keyCode == SWT.DEL) {
                if (getLineWithoutRCEPROMPT(0).isEmpty()) {
                    clearConsoleAction.setEnabled(false);
                }
            }

            if (keyEvent.keyCode == SWT.ESC) {
                styledtext.setSelection(styledtext.getCharCount() - getLineWithoutRCEPROMPT(currentLine).length(),
                    styledtext.getCharCount());
                insertText("");
                setSelection(caretLinePosition);
            }
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {

            String command = getLineWithoutRCEPROMPT(currentLine);

            // It can happen that keyEvent.keyCode == SWT.CR but keyEvent.character != '\r'. This is for example the case if you press <^>
            // once on your keyboard and <RETURN> directly after that. In this case two KeyEvents are passed to this method. Both KeyEvents
            // have keyEvent.keyCode == SWT.CR but one will also have keyEvent.character == '^'. This is due to the construction of some
            // special characters which require two key presses, e.g. diacritical versions of characters like the circumflex on top of the
            // letter e.
            if ((keyEvent.keyCode == SWT.CR || keyEvent.keyCode == SWT.KEYPAD_CR) && keyEvent.character == '\r') {
                if (command.isEmpty()) {
                    // If the prompt is selected and the selection ends at the current caret position, there occured an exception.
                    // To quick fix it, if this kind of selection is done, pressing return won't enter a new empty line.
                    if (styledtext.getSelectionCount() == 0) {
                        increaseCurrentLine();
                        insertRCEPrompt();
                    }
                } else {

                    if (command.equals(Messages.historyUsedCommand)) {
                        displayUsedCommands();
                        return;
                    } else if (command.equals("clear")) {

                        clearConsoleAction.run();
                        return;

                    } else {
                        setSelection(styledtext.getCaretOffset() + command.length());

                        resetCommandPosition();
                        // addUsedCommand(command);
                        setCaretLinePosition(caretLinePosition + command.length());
                        increaseCurrentLine(); // TODO problematic
                        insertRCEPrompt();
                        // run command in separate thread to keep the UI responsive
                        ConcurrencyUtils.getAsyncTaskService().execute("Execute Command", new ExecuteCommand(command));
                    }
                }
            } else if (keyEvent.keyCode == SWT.HOME) {
                setSelection(caretLinePosition); // POS1/HOME moves caret behind prompt of last command line
            } else if (keyEvent.keyCode == SWT.END) {
                setSelection(caretLinePosition + command.length()); // END moves caret behind command of the last of command line
            }

            if (!command.isEmpty() && getCurrentCaretLocation() > caretLinePosition) {
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
            case SWT.TRAVERSE_TAB_NEXT:
                if (event.keyCode == SWT.TAB) {
                    disableEvent(event);
                    /*
                    String command = getLineWithoutRCEPROMPT(currentLine);
                    String[] completions = commandExecutionService.getCommandCompleter().findCompletion(command);
                    
                    if (completions.length == 1) {
                        completeLine(command, completions[0]);
                        
                    } else if (completions.length > 1){
                        showCompletions(command, completions);
                        
                    }
                    */
                }
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
            if (currentCaretLocation < caretLinePosition) {
                disableEvent(event);
            } else {
                if (!line.isEmpty()) {
                    // setSelection(styledtext.getCaretOffset() + (line.length() -
                    // (styledtext.getCaretOffset() -
                    // caretPosition)));
                    setSelection(styledtext.getCaretOffset() + line.length());
                }
            }
        }
        
        private void completeLine(String command, String completion) {
            
            String[] words = command.split(" ");
            String lastWord = words[words.length - 1];
            
            setSelection(styledtext.getCaretOffset() + command.length());
            
            if (completion.startsWith(lastWord)) {
                
                int lastWordLength = command.length() - (command.lastIndexOf(' ') + 1);
                completion = completion.substring(lastWordLength);
                
            } else if (!command.endsWith(" ")) {
                completion = " " + completion;
                
            }
            
            insertText(completion);
            setSelection(styledtext.getCaretOffset() + completion.length());
            
        }
        
        private void showCompletions(String command, String[] completions) {
            setSelection(styledtext.getCaretOffset() + command.length());
            
            String text = completions[0];
            
            for (int i = 1; i < completions.length; i++) {
                text += "\t" + completions[i];
            }
            
            insertTextWithLineBreak("\n" + text);
            increaseCurrentLine();
            
            setSelection(styledtext.getCaretOffset() + text.length() + 2);
            
            insertRCEPrompt();
            insertText(command);
            setSelection(styledtext.getCaretOffset() + command.length());
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
            // disable all key actions if caret location or selection range start is above caret
            // line position or if line is empty
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

        private final String command;

        private final Display display = Display.getDefault();

        private int line;

        private volatile boolean writing;

        ExecuteCommand(String command) {
            this.command = command;
            // this.line = ++currentLine;
            this.writing = true;
        }

        @Override
        @TaskDescription("Execute Command")
        public void run() {

            commandService.saveCommand(command);
            commandService.addUsedCommand(command);

            List<String> tokens = getTokens();
            Future<CommandExecutionResult> asyncExecMultiCommand =
                commandExecutionService.asyncExecMultiCommand(tokens, textOutputReceiver, "command console");
            // waitWithDelay();
            // waitForExecToFinish(asyncExecMultiCommand);
        }

        /**
         * waiting for the execution to finish, while text is displayed user cannot enter new commands.
         */
        private void waitWithDelay() {
            int sleepTime = 5;
            try {
                while (writing) {
                    Thread.sleep(sleepTime);
                    display.asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            // TODO this line can throw an "Index out of bounds" exception; check
                            // before this code is being used again
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
            for (String argument : arguments) {
                tokens.add(argument);
            }
            return tokens;
        }
    }

    /** Thread to interact with the swt styledtext. */
    private class DisplayText implements Runnable {

        private final String text;

        DisplayText(String text) {
            this.text = text;
        }

        @Override
        @TaskDescription("Display Text")
        public void run() {
            if (styledtext.isDisposed()) {
                return;
            }
            if (text.equals(RCEPROMPT)) {
                insertRCEPrompt();
            } else {
                displayMessage(text);
            }                
        }
    }

    /** Class for handling command input and printing out the output. */
    private class CommandConsoleOutputAdapter extends AbstractInteractiveCommandConsole {

        private final Display display = Display.getDefault();

        CommandConsoleOutputAdapter() {
            super(commandExecutionService);
        }

        /** Adds an Output to the console. */
        @Override
        public void addOutput(String line) {
            if (display.isDisposed()) {
                return;
            }
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
