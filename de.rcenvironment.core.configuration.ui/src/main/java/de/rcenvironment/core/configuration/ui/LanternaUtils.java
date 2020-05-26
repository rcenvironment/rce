/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.ui;

import org.apache.commons.lang3.text.WordUtils;

import com.googlecode.lanterna.gui.Action;
import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.component.Button;
import com.googlecode.lanterna.gui.component.Label;
import com.googlecode.lanterna.gui.component.Panel;
import com.googlecode.lanterna.gui.dialog.MessageBox;
import com.googlecode.lanterna.gui.layout.LayoutParameter;

/**
 * Collection of methods that might be useful in a Lanterna UI.
 * 
 * @author Tobias Brieden
 */
public final class LanternaUtils {

    /**
     * This field exists to enforce a dependency to the LayoutParameter class. Otherwise, the associated bundle import might get removed,
     * which would result in a NoClassDefFound error at runtime.
     */
    private static LayoutParameter lp;

    // note: assuming screen width of 80 characters; can it be different?
    private static final int WORD_WRAPPING_MAX_LINE_LENGTH = 60;

    private static final String DIALOG_TITLE_SUCCESS = "Success";

    private static final String DIALOG_TITLE_ERROR = "Error";

    private LanternaUtils() {}

    /**
     * Returns the same string, but wrapped after 60 characters.
     * 
     * @param input The input string which should be wrapped.
     * @return The wrapped string.
     */
    public static String applyWordWrapping(String input) {
        return applyWordWrapping(input, WORD_WRAPPING_MAX_LINE_LENGTH);
    }

    /**
     * Returns the same string, but wrapped.
     * 
     * @param width The width after which the wrapping should be applied.
     * @param input The input string which should be wrapped.
     * @return The wrapped string.
     */
    public static String applyWordWrapping(String input, int width) {
        return WordUtils.wrap(input, width, "\n", true); // true = break long words
    }

    /**
     * Creates a panel with two buttons named "Ok" and "Cancel" and given actions.
     * 
     * @param okAction The action to link with the "Ok"-button.
     * @param cancelAction The action to link with the "Cancel"-button.
     * @return The created Panel.
     */
    public static Panel createOkCancelButtonPanel(final Action okAction, final Action cancelAction) {

        Button buttonOk = new Button("Ok", okAction);
        Button buttonCancel = new Button("Cancel", cancelAction);
        Panel buttonPanel = new Panel(Panel.Orientation.HORISONTAL);
        // apparently, this is the standard way to do this; see the ActionListDialog() constructor
        // TODO improve by calculating indentation width?
        buttonPanel.addComponent(new Label("                   "));
        buttonPanel.addComponent(buttonOk);
        buttonPanel.addComponent(buttonCancel);
        return buttonPanel;
    }

    /**
     * Displays a modal dialog indicating success.
     * 
     * @param guiScreen The gui screen of the UI.
     * @param message The message to display.
     */
    public static void showSuccessMessageBox(GUIScreen guiScreen, final String message) {
        MessageBox.showMessageBox(guiScreen, DIALOG_TITLE_SUCCESS, applyWordWrapping(message));
    }

    /**
     * Displays a modal dialog indicating an error.
     * 
     * @param guiScreen The gui screen of the UI.
     * @param message The message to display.
     */
    public static void showErrorMessageBox(GUIScreen guiScreen, String message) {
        MessageBox.showMessageBox(guiScreen, DIALOG_TITLE_ERROR, applyWordWrapping(message));
    }

}
