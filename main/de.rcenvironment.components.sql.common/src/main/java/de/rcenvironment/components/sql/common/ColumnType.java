/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import java.io.Serializable;
import java.sql.Types;

/**
 * Column types.
 * 
 * @author Christian Weiss
 */
public enum ColumnType {
    /** Ignore. */
    IGNORE(Messages.columnTypeIgnoreLabel, Types.NULL, null),
    /** Long. */
    INTEGER(Messages.columnTypeIntegerLabel, Types.INTEGER, Integer.class),
    /** Double. */
    DOUBLE(Messages.columnTypeDoubleLabel, Types.DOUBLE, Double.class),
    /** String. */
    STRING(Messages.columnTypeStringLabel, Types.VARCHAR, String.class),
    /** Clob. */
    CLOB(Messages.columnTypeClobLabel, Types.CLOB, String.class);

    private final String label;
    
    private final int sqlType;

    private final Class<? extends Serializable> javaType;

    ColumnType(final String label, final int sqlType, final Class<? extends Serializable> type) {
        this.label = label;
        this.sqlType = sqlType;
        this.javaType = type;
    }

    public String getLabel() {
        return label;
    }
    
    public int getSqlType() {
        return sqlType;
    }

    public Class<? extends Serializable> getJavaType() {
        return javaType;
    }

    /**
     * Returns the {@link ColumnType} with the specified label.
     * 
     * @param label the label
     * @return the {@link ColumnType} with the specified label
     */
    public static ColumnType valueOfLabel(final String label) {
        final ColumnType[] values = values();
        for (int index = 0; index < values.length; index++) {
            final ColumnType columnType = values[index];
            if (columnType.label.equals(label)) {
                return columnType;
            }
        }
        throw new IllegalArgumentException("No item with the specified label.");
    }

    /**
     * Returns the {@link ColumnType} with the specified type.
     * 
     * @param type the type
     * @return the {@link ColumnType} with the specified type
     */
    public static ColumnType valueOfJavaType(final Class<? extends Serializable> type) {
        final ColumnType[] values = values();
        for (int index = 0; index < values.length; index++) {
            final ColumnType columnType = values[index];
            if (columnType.javaType.equals(type)) {
                return columnType;
            }
        }
        throw new IllegalArgumentException("No item with the specified type.");
    }

    /**
     * Returns the labels of all values.
     * 
     * @return the labels
     */
    public static String[] labelValues() {
        final ColumnType[] values = values();
        final String[] result = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = values[index].label;
        }
        return result;
    }

}
