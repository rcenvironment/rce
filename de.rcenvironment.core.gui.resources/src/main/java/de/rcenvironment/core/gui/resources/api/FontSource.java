/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.resources.api;

/**
 * An abstract source of {@link Font}s. Added to provide a flexible way for bundles to provide their
 * own font resources, and to make the "extensible enum" pattern possible.
 * 
 * @author Sascha Zur
 */
public interface FontSource {

    /**
     * @return name of the font.
     */
    String getFontName();

    /**
     * 
     * @return Width of the font.
     */
    int getFontWidth();

    /**
     * 
     * @return swt flag.
     */
    int getFontSwtFlag();
}
