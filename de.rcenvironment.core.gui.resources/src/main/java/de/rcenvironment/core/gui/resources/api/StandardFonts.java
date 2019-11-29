/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

import org.eclipse.swt.SWT;

/**
 * List of standard fonts in RCE.
 * 
 * @author Sascha Zur
 */
public enum StandardFonts implements FontSource {
    /**
     * Standard font for all console like text fields.
     */
    CONSOLE_TEXT_FONT("Courier", 10, SWT.NORMAL);

    private final String fontName;

    private final int width;

    private final int swtFlag;

    StandardFonts(String fontName, int width, int swtFlag) {
        this.fontName = fontName;
        this.width = width;
        this.swtFlag = swtFlag;
    }

    @Override
    public String getFontName() {
        return fontName;
    }

    @Override
    public int getFontWidth() {
        return width;
    }

    @Override
    public int getFontSwtFlag() {
        return swtFlag;
    }
}
