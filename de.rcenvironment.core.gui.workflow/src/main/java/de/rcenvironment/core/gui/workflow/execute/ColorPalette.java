/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Class providing colors for the execution wizard. Linux and Windows have different meanings of system colors, therefore RGB values are
 * used.
 * 
 * @author Goekhan Guerkan
 */

public final class ColorPalette {

    private static ColorPalette instance = new ColorPalette();

    private final RGB firstRowRGB = new RGB(255, 255, 240);

    private final RGB secondRowRGB = new RGB(255, 255, 220);

    private boolean colorsDisposed = true;

    /**
     * LIGHT YELLOW - used to separate each row.
     */
    private Color firstRow;

    /**
     * DARK WHITE - used to separate each row.
     */
    private Color secondRow;

    private ColorPalette() {

        initColors();
    }

    /**
     * Creates all Colors.
     */
    private void initColors() {

        firstRow = new Color(Display.getCurrent(), firstRowRGB);
        secondRow = new Color(Display.getCurrent(), secondRowRGB);
        colorsDisposed = false;

    }

    /**
     * Returns Singleton instance of ColorPalette.
     * 
     * @return ColorPalette
     */
    public static ColorPalette getInstance() {
        return instance;
    }

    /**
     * Disposes all Colors.
     */
    public void disposeColors() {

        if (!colorsDisposed) {
            colorsDisposed = true;
            firstRow.dispose();
            secondRow.dispose();
        }

    }

    /**
     * Loads all Colors.
     */
    public void loadColors() {
        if (colorsDisposed) {
            initColors();
        }

    }

    public Color getFirstRowColor() {

        return firstRow;
    }

    public Color getSecondRowColor() {

        return secondRow;
    }

}
