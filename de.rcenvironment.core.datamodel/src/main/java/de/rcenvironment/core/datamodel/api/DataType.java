/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.api;

import de.rcenvironment.core.datamodel.types.api.BigTableTD;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.InternalTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.StructuredDataTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Defines the data types of RCE 3.0.0 and later.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public enum DataType {

    /**
     * A string of limited length. The rationale for the length restriction is to avoid RAM shortage
     * from handling arbitrary-sized data as strings. Large string data should be handled via
     * {@link #FileReference} instead, using temporary files or direct data management upload if
     * needed.
     * 
     * TODO design: define exact size limit
     */
    ShortText("Short Text", "STxt", ShortTextTD.class),

    /**
     * A container for a boolean value.
     */
    Boolean("Boolean", "Bool", BooleanTD.class),

    /**
     * A container for a 64 bit signed integer.
     */
    Integer("Integer", "Int", IntegerTD.class),

    /**
     * A container for a double-precision (64 bit) float.
     */
    Float("Float", "Flt", FloatTD.class),

    /**
     * Vector data type. Its cells use the {@link #Float} data type.
     */
    Vector("Vector", "Vctr", VectorTD.class),

    /**
     * Matrix data type. Its cells use the {@link #Float} data type.
     */
    Matrix("Matrix", "Mtrx", MatrixTD.class),

    /**
     * A table that is small enough that it can be held in RAM at typical heap sizes. Each cell has
     * its individual data type. Valid cell data types are defined by the
     * {@link #isAllowedAsCellType()} method.
     * 
     * TODO design: define exact size limit
     */
    SmallTable("Small Table", "SmlT", SmallTableTD.class),

    /**
     * A container for a timezone-independent timestamp.
     * 
     * TODO design: add timezone metadata?
     */
    DateTime("Date/Time", "Date", DateTimeTD.class),

    /**
     * From the user perspective, this data type represents a file and provides access to its
     * content, an (optional?) file name and possibly other file metadata. Technically, the file
     * content will typically be represented as a data management reference.
     */
    FileReference("File", "File", FileReferenceTD.class), // TODO keep or remove "...Reference"?

    /**
     * From the user perspective, this data type represents a directory of files. The technical
     * representation has not been defined yet, but will typically be based on one or more data
     * management references.
     * 
     * TODO design: internal representation? type API?
     */
    DirectoryReference("Directory", "Dir", DirectoryReferenceTD.class), // TODO keep/remove
                                                                        // "...Reference"?

    /**
     * A container for an empty value.
     */
    Empty("Empty", "Epty", EmptyTD.class),

    /**
     * A table of arbitrary size that is partially loaded into RAM on access. Each cell has its
     * individual data type. Valid cell data types are defined by the {@link #isAllowedAsCellType()}
     * method.
     */
    BigTable("Big Table", "BigT", BigTableTD.class),

    /**
     * A complex data type with JSON-like features.
     * 
     * TODO design: details & API
     */
    StructuredData("Structured Data", "SDat", StructuredDataTD.class),
    
    /**
     * Sent if no output can be computed due to invalid inputs or component crash. Used in loops.
     */
    NotAValue("Not-a-value", "Ind", NotAValueTD.class),
    
    /**
     * Used for workflow control purposes.
     */
    Internal("Internal", "Intern", InternalTD.class);
    
    private String displayName;

    private String shortName;

    private Class<? extends TypedDatum> tdClass;

    DataType(String displayName, String shortName, Class<? extends TypedDatum> tdClass) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.tdClass = tdClass;
    }

    public Class<? extends TypedDatum> getTDClass() {
        return tdClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    /**
     * @return true if this type can be a cell type of {@link #SmallTable} or {@link #BigTable}.
     */
    public boolean isValidCellType() {
        return this == DataType.Boolean || this == Integer || this == Float || this == DateTime || this == ShortText;
    }

    /**
     * Returns the {@link DataType} associated with the given {@link TypedDatum} Java interface.
     * 
     * @param inputClass the {@link TypedDatum} class
     * @return the associated {@link DataType} enum value
     */
    public static DataType byTDClass(Class<? extends TypedDatum> inputClass) {
        for (DataType type : values()) {
            if (type.tdClass == inputClass) {
                return type;
            }
        }
        throw new IllegalArgumentException("No match for class " + inputClass);
    }

    /**
     * Returns the {@link DataType} associated with the given name.
     * 
     * @param shortName the short name of the class
     * @return the associated {@link DataType} enum value
     */
    public static DataType byShortName(String shortName) {
        for (DataType type : values()) {
            if (shortName.equals(type.getShortName())) {
                return type;
            }
        }
        throw new IllegalArgumentException(StringUtils.format("No matching data type for given short name found: '%s'", shortName));
    }

    /**
     * Returns the {@link DataType} associated with the given name.
     * 
     * @param displayName the short name of the class
     * @return the associated {@link DataType} enum value
     */
    public static DataType byDisplayName(String displayName) {
        for (DataType type : values()) {
            if (displayName.equals(type.getDisplayName())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No match for classname " + displayName);
    }

    @Override
    public String toString() {
        return displayName; // preliminary; change as necessary
    }
}
