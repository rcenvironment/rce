/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.types.internal;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.internal.TypedDatumServiceImpl;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

/**
 * Implementation of {@link MatrixTD}.
 * 
 * @author Doreen Seider
 */
public class MatrixTDImpl extends AbstractTypedDatum implements MatrixTD {

    private SmallTableTD table;

    public MatrixTDImpl(FloatTD[][] matrixEntries) {
        super(DataType.Matrix);
        this.table = new SmallTableTDImpl(matrixEntries);
    }

    @Override
    public FloatTD getFloatTDOfElement(int rowIndex, int columnIndex) {
        return (FloatTD) table.getTypedDatumOfCell(rowIndex, columnIndex);
    }

    @Override
    public void setFloatTDForElement(FloatTD number, int rowIndex, int columnIndex) {
        table.setTypedDatumForCell(number, rowIndex, columnIndex);
    }

    @Override
    public int getRowDimension() {
        return table.getRowCount();
    }

    @Override
    public int getColumnDimension() {
        return table.getColumnCount();
    }

    @Override
    public MatrixTD getSubMatrix(int endRowIndex, int endColumnIndex) {
        TypedDatumConverter converter = new TypedDatumServiceImpl().getConverter();
        try {
            return converter.castOrConvert(table.getSubTable(endRowIndex, endColumnIndex), MatrixTD.class);
        } catch (DataTypeException e) {
            // should not happen
            LogFactory.getLog(getClass()).error("conversion failed", e);
            return null;
        }
    }

    @Override
    public MatrixTD getSubMatrix(int beginRowIndex, int beginColumnIndex, int endRowIndex, int endColumnIndex) {
        TypedDatumConverter converter = new TypedDatumServiceImpl().getConverter();
        try {
            return converter.castOrConvert(table.getSubTable(beginRowIndex, beginColumnIndex,
                endRowIndex, endColumnIndex), MatrixTD.class);
        } catch (DataTypeException e) {
            // should not happen
            LogFactory.getLog(getClass()).error("conversion failed", e);
            return null;
        }
    }

    @Override
    public VectorTD getColumnVector(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnDimension()) {
            throw new IllegalArgumentException("index out of range: " + columnIndex);
        }
        FloatTD[][] matrixEntries = toArray();
        FloatTD[] vectorEntries = new FloatTD[getRowDimension()];
        for (int i = 0; i < getRowDimension(); i++) {
            vectorEntries[i] = matrixEntries[i][columnIndex];
        }
        return new VectorTDImpl(vectorEntries);
    }

    @Override
    public VectorTD getRowVector(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getRowDimension()) {
            throw new IllegalArgumentException("index out of range: " + rowIndex);
        }
        FloatTD[][] matrixEntries = toArray();
        FloatTD[] vectorEntries = new FloatTD[getColumnDimension()];
        for (int i = 0; i < getColumnDimension(); i++) {
            vectorEntries[i] = matrixEntries[rowIndex][i];
        }
        return new VectorTDImpl(vectorEntries);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof MatrixTD) {
            MatrixTD other = (MatrixTD) obj;
            return table.equals(new SmallTableTDImpl(other.toArray()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return table.hashCode();
    }

    @Override
    public FloatTD[][] toArray() {
        TypedDatum[][] tableAsArray = table.toArray();
        FloatTD[][] resultArray = new FloatTD[getRowDimension()][getColumnDimension()];
        for (int i = 0; i < getRowDimension(); i++) {
            System.arraycopy(tableAsArray[i], 0, resultArray[i], 0, getColumnDimension());
        }
        return resultArray;
    }

    @Override
    public String toLengthLimitedString(int maxLength) {
        int rows = 0;
        int columns = 0;
        String text = "[";
        String formattedLabel = "";

        VectorTD firstRow = getRowVector(0);
        rows = getRowDimension();
        columns = getColumnDimension();
        for (FloatTD f : firstRow.toArray()) {
            text += f.toString();
            text += ",";
        }
            // remove last comma
        text = text.substring(0, text.length() - 1);
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength) + " ...";
        }
        text += "]";
        text += "...";
        formattedLabel += text;
        String dimensionsText = " (" + columns + "x" + rows + ")";
        formattedLabel += dimensionsText;
        return formattedLabel;
    }

    @Override
    public String toString() {
        String fullContent = "";

        for (int i = 0; i < getRowDimension(); i++) {
            // fill fullcontent
            VectorTD row = getRowVector(i);
            for (FloatTD f : row.toArray()) {
                // remove comma for integers
                String floatValue = VectorTDImpl.toPrettyString(f.getFloatValue());
                fullContent += floatValue;
                fullContent += ", ";
            }
            fullContent = fullContent.substring(0, fullContent.length() - 2);
            fullContent += "\r\n";
        }

        return fullContent;
    }

}
