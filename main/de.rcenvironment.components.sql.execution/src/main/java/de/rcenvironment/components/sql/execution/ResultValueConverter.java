/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.sql.execution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.utils.common.channel.legacy.VariantArray;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;


/**
 * Util class to convert values of jdcb {@link ResultSet}s into {@link TypedValue}s.
 *
 * @author Christian Weiss
 */
@SuppressWarnings("deprecation")
//This is a legacy class which will not be adapted to the new Data Types. Thus, the deprecation warnings are suppressed here.
/* default */ class ResultValueConverter {
    
    protected ResultValueConverter() {
        // do nothing
    }

    /**
     * Converts a jdbc {@link ResultSet} into a list of {@link TypedValue} arrays.
     * 
     * @param resultSet the {@link ResultSet} to read the result from
     * @return a list of {@link TypedValue} arrays
     * @throws SQLException in case an sql exception occurs
     */
    public static List<TypedValue[]> convertToTypedValueList(final ResultSet resultSet) throws SQLException {
        // create the result list
        final List<TypedValue[]> result = new LinkedList<TypedValue[]>();
        final Iterable<TypedValue[]> rows = convertToTypedValueListIterator(resultSet);
        for (final TypedValue[] row : rows) {
            result.add(row);
        }
        return result;
    }

//    /**
//     * Converts a jdbc {@link ResultSet} into a list of {@link TypedValue} arrays.
//     * 
//     * @param resultSet the {@link ResultSet} to read the result from
//     * @return a list of {@link TypedValue} arrays
//     * @throws SQLException in case an sql exception occurs
//     */
//    public static List<TypedValue[]> convertToTypedValueList(final ResultSet resultSet) throws SQLException {
//        // create the result list
//        final List<TypedValue[]> result = new LinkedList<TypedValue[]>();
//        // determine the column count
//        final int columnCount = resultSet.getMetaData().getColumnCount();
//        // determine and store the column type for each column
//        final int[] columnTypes = new int[columnCount];
//        for (int col = 0; col < columnCount; col++) {
//            columnTypes[col] = resultSet.getMetaData().getColumnType(col + 1);
//        }
//        // iterate over the result set and convert the values
//        while (resultSet.next()) {
//            final TypedValue[] row = new TypedValue[columnCount];
//            for (int columnNumber = 1; columnNumber <= columnCount; columnNumber++) {
//                final int columnIndex = columnNumber - 1;
//                switch(columnTypes[columnIndex]) {
//                case Types.INTEGER:
//                case Types.TINYINT:
//                case Types.SMALLINT:
//                    final Integer integerValue = resultSet.getInt(columnNumber);
//                    row[columnIndex] = new TypedValue(integerValue);
//                    break;
//                case Types.BIGINT:
//                    final Long longValue = resultSet.getLong(columnNumber);
//                    row[columnIndex] = new TypedValue(longValue);
//                    break;
//                case Types.DOUBLE:
//                case Types.FLOAT:
//                case Types.REAL:
//                case Types.NUMERIC:
//                case Types.DECIMAL:
//                    final Double doubleValue = resultSet.getDouble(columnNumber);
//                    row[columnIndex] = new TypedValue(doubleValue);
//                    break;
//                case Types.BOOLEAN:
//                case Types.BIT:
//                    Boolean booleanValue = resultSet.getBoolean(columnNumber);
//                    row[columnIndex] = new TypedValue(booleanValue);
//                    break;
//                case Types.VARCHAR:
//                    String valueVarChar = resultSet.getString(columnNumber);
//                    row[columnIndex] = new TypedValue(valueVarChar);
//                    break;
//                default:
//                    String valueDefault = resultSet.getString(columnNumber);
//                    row[columnIndex] = new TypedValue(valueDefault);
//                    break;
//                }
//            }
//            result.add(row);
//        }
//        // return the result list
//        return result;
//    }

    /**
     * Converts a jdbc {@link ResultSet} into a 2 dimensional {@link TypedValue} array.
     * 
     * @param resultSet the {@link ResultSet} to read the result from
     * @return a 2 dimensional {@link TypedValue} array
     * @throws SQLException in case an sql exception occurs
     */
    public static TypedValue[][] convertToTypedValue2D(final ResultSet resultSet) throws SQLException {
        // determine the result list
        final List<TypedValue[]> rows = convertToTypedValueList(resultSet);
        // determine the column count
        final int columnCount = resultSet.getMetaData().getColumnCount();
        // create the result structure
        final TypedValue[][] result = new TypedValue[rows.size()][columnCount];
        // transfer the result values into the result structure
        for (int row = 0; row < rows.size(); row++) {
            for (int column = 0; column < columnCount; column++) {
                result[row][column] = rows.get(row)[column];
            }
        }
        // return the result structure
        return result;
    }

    /**
     * Converts a jdbc {@link ResultSet} into a list of {@link TypedValue} arrays.
     * 
     * @param resultSet the {@link ResultSet} to read the result from
     * @return a list of {@link TypedValue} arrays
     * @throws SQLException in case an sql exception occurs
     */
    public static Iterable<TypedValue[]> convertToTypedValueListIterator(final ResultSet resultSet) throws SQLException {
        return new ResultSetTypedValueArrayIterator(resultSet);
    }

    /**
     * Converts a jdbc {@link ResultSet} into a {@link VariantArray}.
     * 
     * @param name the name of the {@link VariantArray}
     * @param resultSet the {@link ResultSet} to read the result from
     * @return a {@link VariantArray}
     * @throws SQLException in case an sql exception occurs
     */
    public static VariantArray convertToVariantArray(final String name, final ResultSet resultSet) throws SQLException {
        // determine the result list
        final List<TypedValue[]> rows = convertToTypedValueList(resultSet);
        return convertToVariantArray(name, rows, resultSet);
    }

    /**
     * Converts a list of {@link TypedValue}s to a {@link VariantArray}.
     * 
     * @param name the name of the {@link VariantArray}
     * @param rows the list of {@link TypedValue}s
     * @param resultSet the base {@link ResultSet}
     * @return a {@link VariantArray}
     * @throws SQLException in case an sql exception occurs
     */
    public static VariantArray convertToVariantArray(final String name, final List<TypedValue[]> rows, final ResultSet resultSet)
        throws SQLException {
        // determine the column count
        final int columnCount = resultSet.getMetaData().getColumnCount();
        // create the result instance
        final VariantArray result = convertToVariantArray(name, rows, columnCount);             
        // return the result instance
        return result;
    }

    /**
     * Converts a list of {@link TypedValue}s to a {@link VariantArray}.
     * 
     * @param name the name of the {@link VariantArray}
     * @param resultSet the base {@link ResultSet}
     * @param rows the list of {@link TypedValue}s
     * @return a {@link VariantArray}
     * @throws SQLException in case an sql exception occurs
     */
    public static VariantArray convertToVariantArray(final String name, final List<TypedValue[]> rows, final int columnCount)
        throws SQLException {
        // determine the row count
        final int rowCount = rows.size();
        // create the result instance
        final VariantArray result = new VariantArray(name, rowCount, columnCount);                
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                result.setValue(rows.get(row)[col], row, col);
            }
        }
        // return the result instance
        return result;
    }
    
    /**
     * Data container for pre-calculated metadata for a {@link ResultSet}.
     *
     * @author Christian Weiss
     */
    public static final class ResultSetMetaData {
        
        /** The column count. */
        public final int columnCount;
        
        /** The column types. */
        public final int[] columnTypes;

        /** The column labels. */
        public final List<String> columnLabels;
        
        private ResultSetMetaData(final ResultSet resultSet) {
            try {
                // determine the column count
                columnCount = resultSet.getMetaData().getColumnCount();
                // determine the column type for each column
                columnTypes = new int[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    columnTypes[col] = resultSet.getMetaData().getColumnType(col + 1);
                }
                // determine the list of column labels
                final List<String> columnLabelsModifiableList = new ArrayList<String>(columnCount);
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    final int columnNumber = columnIndex + 1;
                    columnLabelsModifiableList.add(columnIndex, resultSet.getMetaData().getColumnLabel(columnNumber));
                }
                columnLabels = Collections.unmodifiableList(columnLabelsModifiableList);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to parse ResultSet MetaData: " + e.toString());
            }
        }
        
        /**
         * Parses the given {@link ResultSet} and extracts the metadata.
         * 
         * @param resultSet the {@link ResultSet} to gather the metadata from
         * @return the pre-calculated metadata in a {@link ResultSetMetaData} data container
         */
        public static ResultSetMetaData parse(final ResultSet resultSet) {
            return new ResultSetMetaData(resultSet);
        }
        
    }

    /**
     * {@link Iterable} implementation extracting the {@link TypedValue} arrays of a {@link ResultSet} (iteratively).
     *
     * @author Christian Weiss
     */
    private static final class ResultSetTypedValueArrayIterator implements Iterable<TypedValue[]> {

        private final ResultSetMetaData metaData;

        private final ResultSet resultSet;
        
        /** Guard next() to be invoked illegally - necessary as hasNext() actually does what next() implies (moving one row forward). */
        private boolean illegalNextGuard = true;

        ResultSetTypedValueArrayIterator(final ResultSet resultSet) {
            this.resultSet = resultSet;
            metaData = new ResultSetMetaData(resultSet);
        }

        @Override
        public Iterator<TypedValue[]> iterator() {
            return new Iterator<TypedValue[]>() {

                @Override
                public boolean hasNext() {
                    try {
                        final boolean result = resultSet.next();
                        illegalNextGuard = false;
                        return result;
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to check whether ResultSet contains more lines: " + e.toString());
                    }
                }

                @Override
                public TypedValue[] next() {
                    if (illegalNextGuard) {
                        throw new RuntimeException("hasNext() has not been invoked or next() has been invoked more then once");
                    }
                    illegalNextGuard = true;
                    try {
                        final TypedValue[] row = new TypedValue[metaData.columnCount];
                        for (int columnNumber = 1; columnNumber <= metaData.columnCount; columnNumber++) {
                            final int columnIndex = columnNumber - 1;
                            switch(metaData.columnTypes[columnIndex]) {
                            case Types.INTEGER:
                            case Types.TINYINT:
                            case Types.SMALLINT:
                                final Integer integerValue = resultSet.getInt(columnNumber);
                                row[columnIndex] = new TypedValue(integerValue);
                                break;
                            case Types.BIGINT:
                                final Long longValue = resultSet.getLong(columnNumber);
                                row[columnIndex] = new TypedValue(longValue);
                                break;
                            case Types.DOUBLE:
                            case Types.FLOAT:
                            case Types.REAL:
                            case Types.NUMERIC:
                            case Types.DECIMAL:
                                final Double doubleValue = resultSet.getDouble(columnNumber);
                                row[columnIndex] = new TypedValue(doubleValue);
                                break;
                            case Types.BOOLEAN:
                            case Types.BIT:
                                Boolean booleanValue = resultSet.getBoolean(columnNumber);
                                row[columnIndex] = new TypedValue(booleanValue);
                                break;
                            case Types.VARCHAR:
                                String valueVarChar = resultSet.getString(columnNumber);
                                row[columnIndex] = new TypedValue(valueVarChar);
                                break;
                            case Types.TIMESTAMP:
                                Timestamp valueTimestamp = resultSet.getTimestamp(columnNumber);
                                row[columnIndex] = new TypedValue(valueTimestamp);
                                break;
                            default:
                                String valueDefault = resultSet.getString(columnNumber);
                                row[columnIndex] = new TypedValue(valueDefault);
                                break;
                            }
                        }
                        return row;
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to retrieve next row from ResultSet: " + e.toString());
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
            };
        }
    }

}
