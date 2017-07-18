/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.incubator.formatter;

/**
 * Contains the possible alignments within the table.
 *
 * @author Adrian Stock
 */

public enum Alignments {
    /**
     * Is used, when the text shall be aligned on the left hand-side of the column.
     */
    LEFT,
    /**
     * Is used, when the text shall be aligned on the right hand-side of the column.
     */
    RIGHT,
    /**
     * Is used, when the text shall be aligned in the center of the column.
     */
    CENTER,
    /**
     * Is used, when the text shall be aligned in dependency of a specific character.
     */
    CHARALIGNMENT;
}
