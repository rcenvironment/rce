/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.incubator.formatter;

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
