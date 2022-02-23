/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Matrix data type. Its cells use the {@link #Float} data type. All row and column indices are
 * zero-based.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface MatrixTD extends TypedDatum {

    /**
     * @param rowIndex zero-based row index of the element
     * @param columnIndex zero-based column index of the element
     * @return {@link FloatTD} of element
     */
    FloatTD getFloatTDOfElement(int rowIndex, int columnIndex);

    /**
     * Sets {@link TypedDatum} for an element.
     * 
     * @param number {@link FloatTD} to set
     * @param rowIndex zero-based row index of the element
     * @param columnIndex zero-based column index of the element
     */
    void setFloatTDForElement(FloatTD number, int rowIndex, int columnIndex);

    /**
     * @return number of rows in the matrix
     */
    int getRowDimension();

    /**
     * @return number of columns in the matrix
     */
    int getColumnDimension();

    /**
     * Returns a new {@link MatrixTD} which is a sub-matrix of this one.
     * 
     * @param endRowIndex ending zero-based row index, exclusive.
     * @param endColumnIndex ending zero-based row index, exclusive.
     * @return new {@link MatrixTD}
     */
    MatrixTD getSubMatrix(int endRowIndex, int endColumnIndex);

    /**
     * Returns a new {@link MatrixTD} which is a sub-matrix of this one.
     * 
     * @param beginRowIndex beginning zero-based row index, inclusive.
     * @param beginColumnIndex beginning zero-based row index, inclusive.
     * @param endRowIndex ending zero-based row index, exclusive.
     * @param endColumnIndex ending zero-based row index, exclusive.
     * @return new {@link MatrixTD}
     */
    MatrixTD getSubMatrix(int beginRowIndex, int beginColumnIndex, int endRowIndex, int endColumnIndex);

    /**
     * Returns a {@link VectorTD} which is a sub-matrix (with columns dimension of 1) of this one.
     * 
     * @param columnIndex zero-based column index
     * @return {@link VectorTD}
     */
    VectorTD getColumnVector(int columnIndex);

    /**
     * Returns a {@link VectorTD} which is a sub-matrix (with row dimension of 1) of this one.
     * 
     * @param rowIndex zero-based column index
     * @return {@link VectorTD}
     */

    VectorTD getRowVector(int rowIndex);

    /**
     * Returns a two-dimensional array of type {@link FloatTD}.
     * 
     * @return two dimensional Array
     */
    FloatTD[][] toArray();
    
    /**
     * @param maxLength maximum length of string representation
     * @return string representation
     */
    String toLengthLimitedString(int maxLength);

}
