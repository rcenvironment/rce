/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.api;

import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Vector data type. Its cells use the {@link #Float} data type. All row indices are zero-based.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface VectorTD extends TypedDatum {

    /**
     * @param rowIndex zero-based row index of the element
     * @return {@link FloatTD} of the element
     */
    FloatTD getFloatTDOfElement(int rowIndex);

    /**
     * Sets {@link TypedDatum} for an element.
     * 
     * @param number {@link FloatTD} to set
     * @param rowIndex zero-based row index of the element
     */
    void setFloatTDForElement(FloatTD number, int rowIndex);

    /**
     * @return number of rows in the matrix
     */
    int getRowDimension();

    /**
     * Returns a new {@link VectorTD} which is a sub-vector of this one.
     * 
     * @param endRowIndex ending zero-based row index, exclusive.
     * @return new {@link VectorTD}
     */
    VectorTD getSubVector(int endRowIndex);

    /**
     * Returns a new {@link VectorTD} which is a sub-vector of this one.
     * 
     * @param beginRowIndex beginning zero-based row index, inclusive.
     * @param endRowIndex ending zero-based row index, exclusive.
     * @return new {@link VectorTD}
     */
    VectorTD getSubVector(int beginRowIndex, int endRowIndex);

    /**
     * Returns a new {@link FloatTD} array that represents the vector.
     * 
     * @return new {@link FloatTD} array
     */
    FloatTD[] toArray();
    
    /**
     * @param maxLength maximum length of string representation
     * @return string representation
     */
    String toLengthLimitedString(int maxLength);

}
