/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.sql.execution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.sql.common.ColumnType;
import de.rcenvironment.components.sql.common.InputMapping;
import de.rcenvironment.components.sql.common.InputMapping.ColumnMapping;
import de.rcenvironment.components.sql.common.InputMode;
import de.rcenvironment.components.sql.common.SqlComponentConstants;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;


/**
 * Component to write data into a SQL database.
 *
 * @author Christian Weiss
 */
public class SQLWriterComponent extends AbstractSQLComponent {

    private InputMode inputMode;
    
    private int[] dataIndexs;
    
    @Override
    protected void validate() {
        // do nothing
    }

    @Override
    protected void runSql(final String sql) {
        if (!InputMode.BLOCK.equals(getInputMode())) {
            throw new UnsupportedOperationException();
        }
        runSqlBlockMode(sql);
    }

    protected void runSqlBlockMode(final String sql) {
        // declare the jdbc assets to be able to close them in the finally-clause
        Connection jdbcConnection = null;
        PreparedStatement statement = null;
        // exectue the jdbc stuff
        try {
            jdbcConnection = getConnection();
            // start transaction
            final boolean autoCommit = jdbcConnection.getAutoCommit();
            try {
                jdbcConnection.setAutoCommit(false);
                statement = jdbcConnection.prepareStatement(sql);
                // get the input
                final Map<String, SmallTableTD> input = extractFullDataInputs();
                // iterate over the rows
                for (final String inputName : input.keySet()) {
                    statement.clearParameters();

                    final TypedDatum[][] data = input.get(inputName).toArray();
                    
                    for (int row = 0; row < data.length; row++) {
                        for (int col = 0; col < data[0].length; col++) {
                            TypedDatum td = data[row][col];
                            switch (td.getDataType()) {
                            case ShortText:
                                String valueString = ((ShortTextTD) td).getShortTextValue();
                                statement.setObject(col + 1, valueString);
                                break;
                            case Integer:
                                Long valueInt = ((IntegerTD) td).getIntValue();
                                statement.setObject(col + 1, valueInt);
                                break;
                            case Float:
                                Double valueDouble = ((FloatTD) td).getFloatValue();
                                statement.setObject(col + 1, valueDouble);
                                break;
                            case Boolean:
                                Boolean valueBool = ((BooleanTD) td).getBooleanValue();
                                statement.setObject(col + 1, valueBool);
                                break;
                            case DateTime:
                                Date valueDateTime = ((DateTimeTD) td).getDateTime();
                                statement.setObject(col + 1, valueDateTime);
                                break;
                            default:
                                break;
                            }
                        }
                        statement.execute();
                    }
                }
                // commit transaction
                jdbcConnection.commit();
            } catch (SQLException e) {
                jdbcConnection.rollback();
                throw e;
            } finally {
                jdbcConnection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            logger.error("SQL Exception occured:", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("Statement could not be closed properly:", e);
            }
        }
    }

    protected Map<String, SmallTableTD> extractFullDataInputs() {
        final Map<String, SmallTableTD> result = new HashMap<String, SmallTableTD>();
        for (final String inputName : getFullDataInputs()) {
            final SmallTableTD inputData = getVariableValue(inputName, SmallTableTD.class);
            result.put(inputName, inputData);
        }
        return result;
    }
    
    @Override
    protected String getSqlQuery() {
        final String result;
        if (InputMode.BLOCK.equals(getInputMode())) {
            result = createInsertPreparedStatement();
        } else {
            result = super.getSqlQuery();
        }
        return result;
    }

    private boolean isCreateTableEnabled() {
        final String key = SqlComponentConstants.METADATA_CREATE_TABLE;
        final Boolean createTableValue = Boolean.valueOf(componentContext.getConfigurationValue(key));
        final boolean createTableEnabled = createTableValue != null && createTableValue;
        return createTableEnabled;
    }

    private boolean isDropTableEnabled() {
        final String key = SqlComponentConstants.METADATA_DROP_TABLE;
        final Boolean createTableValue = Boolean.valueOf(componentContext.getConfigurationValue(key));
        final boolean createTableEnabled = createTableValue != null && createTableValue;
        return createTableEnabled;
    }

    @Override
    protected void sqlSetup() {
        super.sqlSetup();
        if (isCreateTableEnabled()) {
            String sql = createCreateTableStatement();
            sql = replace(sql);
            executeSqlStatements(sql);
        }
    }
    
    @Override
    protected void sqlDispose() {
        super.sqlDispose();
        // only drop a table, if the table has been created by this component
        if (isCreateTableEnabled() && isDropTableEnabled()) {
            final String sql = createDropTableStatement();
            executeSqlStatements(sql);
        }
    }

    private String createCreateTableStatement() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ");
        builder.append(componentContext.getConfigurationValue(SqlComponentConstants.METADATA_TABLE_NAME_PROPERTY));
        builder.append("(");
        final InputMapping inputMapping = getInputMapping();
        for (int index = 0; index < inputMapping.size(); ++index) {
            final ColumnMapping columnMapping = inputMapping.get(index);
            final String name = columnMapping.getName();
            if (name == null || name.isEmpty() || ColumnType.IGNORE.equals(columnMapping.getType())) {
                continue;
            }
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(name);
            builder.append(" ");
            final int sqlType = columnMapping.getType().getSqlType();
            final String sqlTypeString;
            switch (sqlType) {
            case java.sql.Types.VARCHAR:
                sqlTypeString = "VARCHAR(255)";
                break;
            case Types.INTEGER:
                sqlTypeString = "INTEGER";
                break;
            case Types.DOUBLE:
                sqlTypeString = "DOUBLE";
                break;
            case Types.CLOB:
                sqlTypeString = "LONGTEXT";
                break;
            default:
                throw new RuntimeException("Unsupported sql type.");
            }
            builder.append(sqlTypeString);
        }
        builder.append(")");
        return builder.toString();
    }

    private String createInsertPreparedStatement() {
        final StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ");
        builder.append(componentContext.getConfigurationValue(SqlComponentConstants.METADATA_TABLE_NAME_PROPERTY));
        final InputMapping inputMapping = getInputMapping();
        final StringBuilder valuesBuilder = new StringBuilder();
        builder.append("(");
        valuesBuilder.append("(");
        final List<Integer> dataIndexsList = new LinkedList<Integer>();
        for (int index = 0; index < inputMapping.size(); ++index) {
            final ColumnMapping columnMapping = inputMapping.get(index);
            if (ColumnType.IGNORE.equals(columnMapping.getType())) {
                continue;
            }
            if (index > 0) {
                builder.append(", ");
                valuesBuilder.append(", ");
            }
            builder.append(columnMapping.getName());
            valuesBuilder.append("?");
            dataIndexsList.add(index);
        }
        valuesBuilder.append(")");
        builder.append(")");
        builder.append(" VALUES ");
        builder.append(valuesBuilder);
        dataIndexs = new int[dataIndexsList.size()];
        for (int index = 0; index < dataIndexs.length; ++index) {
            dataIndexs[index] = dataIndexsList.get(index);
        }
        String result = builder.toString();
        result = replace(result);
        return result;
    }

    private String createDropTableStatement() {
        final StringBuilder builder = new StringBuilder();
        builder.append("DROP TABLE ");
        builder.append(componentContext.getConfigurationValue(SqlComponentConstants.METADATA_TABLE_NAME_PROPERTY));
        String result = builder.toString();
        result = replace(result);
        return result;
    }
    
    private InputMapping getInputMapping() {
        final String key = SqlComponentConstants.METADATA_INPUT_MAPPING;
        final String value = componentContext.getConfigurationValue(key);
        final InputMapping inputMapping;
        if (value != null) {
            inputMapping = InputMapping.deserialize(value);
        } else {
            inputMapping = new InputMapping();
        }
        return inputMapping;
    }
    
    private InputMode getInputMode() {
        if (inputMode == null) {
            final String key = SqlComponentConstants.METADATA_INPUT_MODE;
            inputMode = InputMode.valueOf(componentContext.getConfigurationValue(key));
        }
        return inputMode;
    }

}
