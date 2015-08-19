/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.datamodel.types.internal.BooleanTDImpl;
import de.rcenvironment.core.datamodel.types.internal.DateTimeTDImpl;
import de.rcenvironment.core.datamodel.types.internal.DirectoryReferenceTDImpl;
import de.rcenvironment.core.datamodel.types.internal.EmptyTDImpl;
import de.rcenvironment.core.datamodel.types.internal.FileReferenceTDImpl;
import de.rcenvironment.core.datamodel.types.internal.FloatTDImpl;
import de.rcenvironment.core.datamodel.types.internal.NotAValueTDImpl;
import de.rcenvironment.core.datamodel.types.internal.IntegerTDImpl;
import de.rcenvironment.core.datamodel.types.internal.MatrixTDImpl;
import de.rcenvironment.core.datamodel.types.internal.ShortTextTDImpl;
import de.rcenvironment.core.datamodel.types.internal.SmallTableTDImpl;
import de.rcenvironment.core.datamodel.types.internal.VectorTDImpl;

/**
 * Default {@link TypedDatumFactory} implementation.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class DefaultTypedDatumFactory implements TypedDatumFactory {

    @Override
    public BooleanTD createBoolean(boolean value) {
        return new BooleanTDImpl(value);
    }

    @Override
    public IntegerTD createInteger(long value) {
        return new IntegerTDImpl(value);
    }
    
    @Override
    public FloatTD createFloat(double value) {
        return new FloatTDImpl(value);
    }

    @Override
    public ShortTextTD createShortText(String value) {
        return new ShortTextTDImpl(value);
    }
    
    @Override
    public VectorTD createVector(int dimension) {
        if (dimension < 0) {
            throw new IllegalArgumentException("dimension must greater than 0");
        }
        FloatTD[] values = new FloatTD[dimension];
        for (int i = 0; i < dimension; i++) {
            values[i] = createFloat(0.0);
        }
        return new VectorTDImpl(values);
    }

    @Override
    public VectorTD createVector(FloatTD[] values) {
        if (values == null) {
            throw new NullPointerException();
        }
        return new VectorTDImpl(values);
    }

    @Override
    public MatrixTD createMatrix(int rows, int columns) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("rows and columms must greater than 0");
        }
        FloatTD[][] values = new FloatTD[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                values[i][j] = createFloat(0.0);
            }
        }
        return new MatrixTDImpl(values);
    }

    @Override
    public MatrixTD createMatrix(FloatTD[][] values) {
        if (values == null) {
            throw new NullPointerException();
        }
        return new MatrixTDImpl(values);
    }

    @Override
    public SmallTableTD createSmallTable(int rows, int columns) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("rows and columms must greater than 0");
        }
        TypedDatum[][] tableEntries = new TypedDatum[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                tableEntries[i][j] = new EmptyTDImpl();
            }
        }
        return new SmallTableTDImpl(tableEntries);
    }

    @Override
    public SmallTableTD createSmallTable(TypedDatum[][] tableEntries) {
        if (tableEntries == null) {
            throw new NullPointerException();
        }
        return new SmallTableTDImpl(tableEntries);
    }
    
    @Override
    public DateTimeTD createDateTime(long dataTime) {
        return new DateTimeTDImpl(dataTime);
    }

    @Override
    public FileReferenceTD createFileReference(String reference, String fileName) {
        if (reference == null || fileName == null) {
            throw new NullPointerException();
        }
        return new FileReferenceTDImpl(reference, fileName);
    }
    
    @Override
    public EmptyTD createEmpty() {
        return new EmptyTDImpl();
    }
    
    @Override
    public NotAValueTD createNotAValue() {
        return new NotAValueTDImpl();
    }
    
    @Override
    public NotAValueTD createNotAValue(String identifier) {
        return new NotAValueTDImpl(identifier);
    }
    
    @Override
    public DirectoryReferenceTD createDirectoryReference(String reference, String dirName) {
        if (reference == null || dirName == null) {
            throw new NullPointerException();
        }
        return new DirectoryReferenceTDImpl(reference, dirName);
    }

}
