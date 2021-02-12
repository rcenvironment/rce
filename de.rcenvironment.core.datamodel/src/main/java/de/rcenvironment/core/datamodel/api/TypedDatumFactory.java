/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

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

/**
 * Factory for {@link TypedDatum} instances.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Doreen Seider
 */
public interface TypedDatumFactory {

    /**
     * @param value that should be contained.
     * @return a {@link BooleanTD} value container
     */
    BooleanTD createBoolean(boolean value);

    /**
     * @param value that should be contained.
     * @return a {@link IntegerTD} value container
     */
    IntegerTD createInteger(long value);

    /**
     * @param value that should be contained.
     * @return a {@link FloatTD} value container
     */
    FloatTD createFloat(double value);

    /**
     * @param value of the short text.
     * @return an empty {@link ShortTextTD} value container
     * 
     */
    ShortTextTD createShortText(String value);
    
    /**
     * @param dimension of the new vector
     * @return an empty {@link VectorTD} instance
     */
    VectorTD createVector(int dimension);
    
    /**
     * @param values Given vector entries
     * @return a {@link VectorTD} instance
     */
    VectorTD createVector(FloatTD[] values);

    /**
     * @param column of the matrix
     * @param row of the matrix
     * @return an column x row dimensioned matrix
     */
    MatrixTD createMatrix(int column, int row);

    /**
     * @param values predefined values for the matrix
     * @return a {@link MatrixTD} instance
     */
    MatrixTD createMatrix(FloatTD[][] values);
    
    /**
     * @param rows row dimension
     * @param columns column dimension
     * @return an empty {@link SmallTableTD} instance
     */
    SmallTableTD createSmallTable(int rows, int columns);

    /**
     * @param tableEntries Given table entries
     * @return a {@link SmallTableTD} instance
     */
    SmallTableTD createSmallTable(TypedDatum[][] tableEntries);
    
    /**
     * @param dateTime the datum has to represent
     * @return an empty {@link DateTimeTD} value container
     */
    DateTimeTD createDateTime(long dateTime);
    
    /**
     * @param reference file reference
     * @param fileName name of the file
     * @return an {@link FileReferenceTD} instance
     */
    FileReferenceTD createFileReference(String reference, String fileName);
    
    /**
     * @param reference directory reference
     * @param dirName of the directory reference
     * @return an {@link DirectoryReferenceTD} instance
     */
    DirectoryReferenceTD createDirectoryReference(String reference, String dirName);
    
    /**
     * @return an empty value.
     */
    EmptyTD createEmpty();
    
    /**
     * Like {@link #createNotAValue(NotAValueTD.Cause)} called with {@link Cause.InvalidInputs}.
     * @return an undefined value (NaV)
     */
    NotAValueTD createNotAValue();
    
    /**
     * @param cause {@link Cause} why the {@link NotAValueTD} was sent
     * @return an undefined value (NaV)
     */
    NotAValueTD createNotAValue(NotAValueTD.Cause cause);
    
    /**
     * Like {@link #createNotAValue(String, NotAValueTD.Cause)} called with {@link Cause.InvalidInputs}.
     * @param identifier identifier of the {@link NotAValueTD} instance
     * @return an undefined value (NaV)
     */
    NotAValueTD createNotAValue(String identifier);
    
    /**
     * @param identifier identifier of the {@link NotAValueTD} instance
     * @param cause {@link Cause} why the {@link NotAValueTD} was sent
     * @return an undefined value (NaV)
     */
    NotAValueTD createNotAValue(String identifier, NotAValueTD.Cause cause);

}
