/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.headless.textui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.DefaultBackgroundRenderer;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.dialog.DialogButtons;
import com.googlecode.lanterna.gui.dialog.DialogResult;
import com.googlecode.lanterna.gui.dialog.MessageBox;
import com.googlecode.lanterna.terminal.Terminal;

/**
 * @author Tobias Brieden
 */
public class QuestionDialogTextUI {

    private static final String BACKGROUND_MESSAGE = "Error during startup...";

    private GUIScreen guiScreen;

    private String dialogTitle;

    private String dialogQuestion;

    private Terminal terminal;

    private final Log log = LogFactory.getLog(getClass());

    public QuestionDialogTextUI(String dialogTitle, String dialogQuestion, Terminal terminal) {
        this.dialogTitle = dialogTitle;
        this.dialogQuestion = dialogQuestion;
        this.terminal = terminal;
    }

    public QuestionDialogTextUI(String dialogTitle, String dialogQuestion) {
        this.dialogTitle = dialogTitle;
        this.dialogQuestion = dialogQuestion;
    }

    /**
     * Displays the question to the user and returns the answer.
     * 
     * @return <code>true</code>, if the user confirmed the question; <code>false</code>, otherwise.
     */
    public boolean run() {
        if (terminal != null) {
            guiScreen = TerminalFacade.createGUIScreen(terminal);
        } else {
            guiScreen = TerminalFacade.createGUIScreen();
        }

        if (guiScreen == null) {
            log.error("Failed to initialize text-mode UI; terminating");
            throw new IllegalStateException();

        }
        guiScreen.setBackgroundRenderer(new DefaultBackgroundRenderer(BACKGROUND_MESSAGE));
        guiScreen.getScreen().startScreen();

        DialogResult confirmation = MessageBox.showMessageBox(guiScreen, dialogTitle, dialogQuestion, DialogButtons.YES_NO);

        guiScreen.getScreen().stopScreen();

        return (confirmation == DialogResult.YES);
    }
}
