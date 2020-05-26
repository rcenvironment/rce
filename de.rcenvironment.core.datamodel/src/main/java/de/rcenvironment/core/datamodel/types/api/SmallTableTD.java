/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * A table that is small enough that it can be held in RAM at typical heap sizes. Each cell has its
 * individual data type. Valid cell data types are defined by the {@link #isAllowedAsCellType()}
 * method. All row and column indices are zero-based.
 * 
 * TODO design: define exact size limit
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface SmallTableTD extends TypedDatum {

    /**
     * @param rowIndex zero-based row index of the cell
     * @param columnIndex zero-based column index of the cell
     * @return {@link TypedDatum} of the cell or <code>null</code> if no such cell exists
     */
    TypedDatum getTypedDatumOfCell(int rowIndex, int columnIndex);

    /**
     * Sets {@link TypedDatum} for cell.
     * 
     * @param typedDatum {@link TypedDatum} to set
     * @param rowIndex zero-based row index of the cell
     * @param columnIndex zero-based column index of the cell
     */
    void setTypedDatumForCell(TypedDatum typedDatum, int rowIndex, int columnIndex);

    /**
     * @return number of rows of the table
     */
    int getRowCount();

    /**
     * @return number columns of the table
     */
    int getColumnCount();

    /**
     * Returns a new {@link SmallTableTD} which is a sub-table of this one.
     * 
     * @param endRowIndex ending zero-based row index, exclusive.
     * @param endColumnIndex ending zero-based column index, exclusive.
     * @return sub-table
     */
    SmallTableTD getSubTable(int endRowIndex, int endColumnIndex);

    /**
     * Returns a new {@link SmallTableTD} which is a sub-table of this one.
     * 
     * @param beginRowIndex beginning zero-based row index, inclusive.
     * @param beginColumnIndex beginning zero-based column index, inclusive.
     * @param endRowIndex ending zero-based row index, exclusive.
     * @param endColumnIndex ending zero-based column index, exclusive.
     * @return sub-table
     */
    SmallTableTD getSubTable(int beginRowIndex, int beginColumnIndex, int endRowIndex, int endColumnIndex);
    
    /**
     * @return table entries as array
     */
    TypedDatum[][] toArray();
    
    /**
     * @param maxLength maximum length of string representation
     * @return string representation
     */
    String toLengthLimitedString(int maxLength);
}
