/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.formatter;

import java.util.List;

/**
 * Creates a Table, which can be aligned with the formatter class.
 * 
 * @author Adrian Stock
 */
public interface DataTable {

    /**
     * @return Amount of rows of the table.
     */
    int getSizeOfTable();

    /**
     * @return Amount of columns of the table.
     */
    int getSizeOfRow();

    /**
     * @param index of the row you want to receive.
     * @return Row on place of the index.
     */
    String[] getRow(int index);

    /**
     * @param place is important for the charAlignment. You can receive the maximal Amount of characters either before("BEFORE") or
     *        after("AFTER") a specific character. When you need the length or the column, use another expression (e.g. "OVERALL").
     * @return List<Integer> which contains the length of each column (before or after a specific character, when needed).
     */
    List<Integer> getAmountOfSpace(String place);

    /**
     * @return the Character, the columns are aligned at.
     */
    char getAlignmentCharacter();

    /**
     * @return the Alignments for each column.
     */
    Alignments[] getAlignments();

    /**
     * @param newAlignments sets the Alignments for each column.
     */
    void setAlignment(Alignments... newAlignments);

    /**
     * @param alignmentCharacter sets the Character, the columns are aligned at.
     */
    void setAlignmentCharacter(char alignmentCharacter);
}
