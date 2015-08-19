/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.internal.TypedDatumServiceImpl;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

/**
 * Implementation of {@link VectorTD}.
 * 
 * @author Doreen Seider
 */
public class VectorTDImpl extends AbstractTypedDatum implements VectorTD {

    private static final String COMMA = ",";

    private static final String OUT_OF_RANGE_ERROR_MESSAGE = "index out of range: ";
    
    private MatrixTD matrix;

    public VectorTDImpl(FloatTD[] vectorEntries) {
        super(DataType.Vector);
        this.matrix = new MatrixTDImpl(toTwoDimensionalArray(vectorEntries));
    }
    
    @Override
    public FloatTD getFloatTDOfElement(int rowIndex) {
        return matrix.getFloatTDOfElement(rowIndex, 0);
    }

    @Override
    public void setFloatTDForElement(FloatTD number, int rowIndex) {
        matrix.setFloatTDForElement(number, rowIndex, 0);
    }

    @Override
    public int getRowDimension() {
        return matrix.getRowDimension();
    }

    @Override
    public VectorTD getSubVector(int endRowIndex) {
        if (endRowIndex < 0 || endRowIndex >= getRowDimension()) {
            throw new IllegalArgumentException(OUT_OF_RANGE_ERROR_MESSAGE + endRowIndex);
        }
        TypedDatumConverter converter = new TypedDatumServiceImpl().getConverter();
        try {
            return converter.castOrConvert(matrix.getSubMatrix(endRowIndex, 1), VectorTD.class);            
        } catch (DataTypeException e) {
            // should not happen
            LogFactory.getLog(getClass()).error("conversion failed", e);
            return null;
        }
    }

    @Override
    public VectorTD getSubVector(int beginRowIndex, int endRowIndex) {
        if (beginRowIndex < 0 || beginRowIndex >= getRowDimension()) {
            throw new IllegalArgumentException(OUT_OF_RANGE_ERROR_MESSAGE + beginRowIndex);
        } else if (endRowIndex < 0 || endRowIndex >= getRowDimension()) {
            throw new IllegalArgumentException(OUT_OF_RANGE_ERROR_MESSAGE + endRowIndex);
        } else if (beginRowIndex > endRowIndex) {
            throw new IllegalArgumentException("begin index must lower than end index: " + beginRowIndex + " - " + endRowIndex);
        }
        TypedDatumConverter converter = new TypedDatumServiceImpl().getConverter();
        try {
            return converter.castOrConvert(matrix.getSubMatrix(beginRowIndex, 0, endRowIndex, 1), VectorTD.class);            
        } catch (DataTypeException e) {
            // should not happen
            LogFactory.getLog(getClass()).error("conversion failed", e);
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof VectorTD) {
            VectorTD other = (VectorTD) obj;
            return matrix.equals(new MatrixTDImpl(toTwoDimensionalArray(other.toArray())));            
        }
        return false;
    }

    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public FloatTD[] toArray() {
        FloatTD[][] matrixEntries = matrix.toArray();
        FloatTD[] vectorEntries = new FloatTD[getRowDimension()];
        for (int i = 0; i < getRowDimension(); i++) {
            vectorEntries[i] = matrixEntries[i][0];
        }
        return vectorEntries;
    }
    
    private FloatTD[][] toTwoDimensionalArray(FloatTD[] vector) {
        FloatTD[][] matrixEntries = new FloatTD[vector.length][1];
        for (int i = 0; i < vector.length; i++) {
            matrixEntries[i][0] = vector[i];
        }
        return matrixEntries;
    }

    @Override
    public String toLengthLimitedString(int maxLength) {
        int dimensions = 0;
        String text = "[";
        String formattedLabel = "";

        for (FloatTD f : toArray()) {
            // remove comma for integers
            String floatValue = toPrettyString(f.getFloatValue());
            text += floatValue;
            text += COMMA;
        }
        // remove last comma
        text = text.substring(0, text.length() - 1);
        
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength - 4);
            text = text.substring(0, text.lastIndexOf(COMMA));
            text += COMMA;
            text += "...";
        }
        text += "]";
        formattedLabel += text;
        String dimensionsText = " (" + dimensions + "-dim)";
        formattedLabel += dimensionsText;
        
        return formattedLabel;
    }
    
    @Override
    public String toString() {
        String fullContent = "";
        for (FloatTD f : toArray()) {
            // remove comma for integers
            String floatValue = toPrettyString(f.getFloatValue());
            fullContent += floatValue;
            fullContent += ", ";
        }
        // remove last comma and whitespace
        fullContent = fullContent.substring(0, fullContent.length() - 2);
        return fullContent;
    }
    
    protected static String toPrettyString(double d) {
        int i = (int) d;
        if (d == i) {
            return String.valueOf(i);
        } else {
            return String.valueOf(d);
        }
    }

}
