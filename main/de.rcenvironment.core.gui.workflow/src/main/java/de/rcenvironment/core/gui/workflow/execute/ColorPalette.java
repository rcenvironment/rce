/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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

    private final RGB warningRGB = new RGB(255, 0, 0);

    private final RGB firstRowRGB = new RGB(255, 255, 240);

    private final RGB secondRowRGB = new RGB(255, 255, 220);

    private final RGB blackRGB = new RGB(0, 0, 0);

    private boolean colorsDisposed = true;

    /**
     * RED - used when version is wrong or instance is not available.
     */
    private Color warningRED;

    /**
     * LIGHT YELLOW - used to separate each row.
     */
    private Color firstRow;

    /**
     * DARK WHITE - used to separate each row.
     */
    private Color secondRow;

    /**
     * BLACK - used to set the color back to black after warning has finished.
     */
    private Color comboBlack;

    private ColorPalette() {

        initColors();
    }

    /**
     * Creates all Colors.
     */
    private void initColors() {

        warningRED = new Color(Display.getCurrent(), warningRGB);
        firstRow = new Color(Display.getCurrent(), firstRowRGB);
        secondRow = new Color(Display.getCurrent(), secondRowRGB);
        comboBlack = new Color(Display.getCurrent(), blackRGB);

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
            warningRED.dispose();
            firstRow.dispose();
            secondRow.dispose();
            comboBlack.dispose();
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

    public Color getBlackColor() {

        return comboBlack;
    }

    public Color getWarningColor() {

        return warningRED;
    }

}
