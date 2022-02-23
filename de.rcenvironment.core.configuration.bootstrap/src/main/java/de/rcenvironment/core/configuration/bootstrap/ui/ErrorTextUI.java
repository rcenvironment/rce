/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.gui.DefaultBackgroundRenderer;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.terminal.Terminal;

import de.rcenvironment.core.configuration.ui.LanternaUtils;

/**
 * @author Tobias Brieden
 */
public class ErrorTextUI {

    private static final String BACKGROUND_MESSAGE = "Error during startup...";

    private GUIScreen guiScreen;

    private String errorMessage;

    private Terminal terminal;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * If you would like the error message to be wrapped, consider using {@link LanternaUtils#applyWordWrapping(String)}.
     * 
     * @param errorMessage The error message to be displayed. Will not be wrapped.
     * @param terminal The terminal to use to display the message. If this is null, a new terminal will be created and used.
     */
    public ErrorTextUI(String errorMessage, Terminal terminal) {
        this.errorMessage = errorMessage;
        this.terminal = terminal;
    }

    /**
     * Convenience constructor, equivalent to calling ErrorTextUI(errorMessage, null).
     * 
     * @param errorMessage The error message to be displayed. Will not be wrapped.
     * @see #ErrorTextUI(String, Terminal)
     */
    public ErrorTextUI(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Main method of the interactive configuration shell.
     */
    public void run() {
        if (terminal != null) {
            guiScreen = TerminalFacade.createGUIScreen(terminal);
        } else {
            guiScreen = TerminalFacade.createGUIScreen();
        }

        if (guiScreen == null) {
            log.error("Failed to initialize text-mode UI; terminating");
            return;

        }
        guiScreen.setBackgroundRenderer(new DefaultBackgroundRenderer(BACKGROUND_MESSAGE));
        guiScreen.getScreen().startScreen();

        LanternaUtils.showErrorMessageBox(guiScreen, errorMessage);

        guiScreen.getScreen().stopScreen();
    }
}
