/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
     * Returns whether the given {@link TypedDatum} is allowed as cell type.
     * 
     * @param typedDatum The given {@link TypedDatum} to check.
     * @return true, if the {@link TypedDatum}s data type is allowed as cell type, otherwise false.
     * 
     * @deprecated method is under review and is likely to be removed in 8.0 (see https://mantis.sc.dlr.de/view.php?id=13788)
     */
    @Deprecated
    boolean isAllowedAsCellType(TypedDatum typedDatum);
    
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
