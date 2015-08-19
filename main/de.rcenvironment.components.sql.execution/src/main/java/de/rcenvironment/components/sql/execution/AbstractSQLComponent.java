/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.sql.execution;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.components.sql.common.JDBCProfile;
import de.rcenvironment.components.sql.common.JDBCService;
import de.rcenvironment.components.sql.common.SqlComponentConstants;
import de.rcenvironment.components.sql.execution.ResultValueConverter.ResultSetMetaData;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.channel.legacy.VariantArray;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;

/**
 * SQL Database implementation of {@link de.rcenvironment.core.component.model.spi.Component}.
 * 
 * @author Markus Kunde
 * @author Christian Weiss
 */
@SuppressWarnings("deprecation")
//This is a legacy class which will not be adapted to the new Data Types. Thus, the deprecation warnings are suppressed here.
public abstract class AbstractSQLComponent extends AbstractComponent {

    protected static final String META_OUTPUT_PREFIX = "meta.";

    private static TypedDatumService typedDatumService;
    
    private static JDBCService jdbcService;

    private Connection connection;

    private Boolean hasDataInputs;

    private Boolean hasDataOutputs;

    private Set<String> dataOutputs;

    private Boolean hasFullDataOutputs;

    private Set<String> fullDataOutputs;

    private Boolean hasFullDataInputs;

    private Set<String> fullDataInputs;

    private Set<String> metaOutputs;

    public AbstractSQLComponent() {
        super(new CyclicInputConsumptionStrategy(), true);
    }
    
    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        jdbcService = componentContext.getService(JDBCService.class);
        typedDatumService = componentContext.getService(TypedDatumService.class);
        super.start();
    }

    protected abstract void validate();

    protected boolean hasDataInputs() {
        if (hasDataInputs == null) {
            hasDataInputs = hasDataInputs(null);
        }
        return hasDataInputs;
    }

    protected boolean hasDataInputs(final DataType type) {
        boolean result = false;
        for (final String inputName : getInputs(type)) {
            if (!inputName.startsWith(META_OUTPUT_PREFIX)) {
                result = true;
                break;
            }
        }
        return result;
    }

    protected boolean hasFullDataInputs() {
        if (hasFullDataInputs == null) {
            hasFullDataInputs = hasDataInputs(DataType.SmallTable);
        }
        return hasFullDataInputs;
    }

    protected Set<String> getDataInputs(final DataType type) {
        final Set<String> result = new HashSet<>();
        for (final String inputName : getInputs(type)) {
            if (!inputName.startsWith(META_OUTPUT_PREFIX)) {
                result.add(inputName);
            }
        }
        return result;
    }

    protected Set<String> getFullDataInputs() {
        if (fullDataInputs == null) {
            fullDataInputs = new HashSet<>();
            fullDataInputs.addAll(getDataInputs(DataType.SmallTable));
        }
        return fullDataInputs;
    }

    protected boolean hasDataOutputs() {
        if (hasDataOutputs == null) {
            hasDataOutputs = hasDataOutputs(null);
        }
        return hasDataOutputs;
    }

    protected boolean hasDataOutputs(final DataType type) {
        boolean result = false;
        for (final String outputName : getOutputs(type)) {
            if (!outputName.startsWith(META_OUTPUT_PREFIX)) {
                result = true;
                break;
            }
        }
        return result;
    }

    protected boolean hasFullDataOutputs() {
        if (hasFullDataOutputs == null) {
            hasFullDataOutputs = hasDataOutputs(DataType.SmallTable);
        }
        return hasFullDataOutputs;
    }

    protected Set<String> getDataOutputs(final DataType type) {
        final Set<String> result = new HashSet<>();
        for (final String outputName : getOutputs(type)) {
            if (!outputName.startsWith(META_OUTPUT_PREFIX)) {
                result.add(outputName);
            }
        }
        return result;
    }

    protected Set<String> getFullDataOutputs() {
        if (fullDataOutputs == null) {
            fullDataOutputs = new HashSet<>();
            fullDataOutputs.addAll(getDataOutputs(DataType.SmallTable));
        }
        return fullDataOutputs;
    }

    protected Set<String> getDataOutputs() {
        if (dataOutputs == null) {
            dataOutputs = new HashSet<>();
            for (String outputName : getOutputs()) {
                if (outputName.startsWith(META_OUTPUT_PREFIX)) {
                    continue;
                }
                final DataType value = componentContext.getOutputDataType(outputName);
                if (value != DataType.SmallTable) {
                    dataOutputs.add(outputName);
                }
            }
        }
        return dataOutputs;
    }

    protected Set<String> getMetaOutputs() {
        if (metaOutputs == null) {
            metaOutputs = new HashSet<>();
            for (String outputName : getOutputs()) {
                if (!outputName.startsWith(META_OUTPUT_PREFIX)) {
                    continue;
                }
                final DataType value = componentContext.getOutputDataType(outputName);
                if (value != DataType.SmallTable) {
                    metaOutputs.add(outputName);
                }
            }
        }
        return metaOutputs;
    }

    protected JDBCProfile getJdbcProfile() {
        final String profileLabel = componentContext.getConfigurationValue(SqlComponentConstants.METADATA_JDBC_PROFILE_PROPERTY);
        if (profileLabel.trim().isEmpty()) {
            throw new RuntimeException("No JDBC profile label configured.");
        }
        final JDBCProfile profile = jdbcService.getProfileByLabel(profileLabel);
        if (profile == null) {
            throw new RuntimeException(StringUtils.format("JDBC profile with the label '%s' does not exist.", profileLabel));
        }
        return profile;
    }

    protected Connection getConnection() throws SQLException {
        if (connection == null) {
            final JDBCProfile profile = getJdbcProfile();
            connection = jdbcService.getConnection(profile);
            if (connection == null) {
                throw new RuntimeException("Connection could not be retrieved.");
            }
        }
        return connection;
    }

    protected void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Database connection could not be closed. " + e.toString());
            }
            connection = null;
        }
    }

    @Override
    public void startInComponent() throws ComponentException {
        // set static replacement values
        final String dash = "-";
        defaultContext.set(CONTEXT_STATIC, "componentSqlId", componentContext.getExecutionIdentifier().replace(dash, ""));
        defaultContext.set(CONTEXT_STATIC, "workflowSqlId", componentContext.getWorkflowExecutionIdentifier().replace(dash, ""));
        if (hasProperty(SqlComponentConstants.METADATA_TABLE_NAME_PROPERTY)) {
            defaultContext.set(CONTEXT_STATIC, "tableName", getTableName());
        }
        validate();
    }
    
    @Override
    public boolean runInitialInComponent(boolean inputsConnected) {
        sqlSetup();
        if (isSqlInitQueryEnabled()) {
            final String sql = getSqlInitQuery();
            runSqlSafe(sql);
            return true;
        }
        return false;
    }

    @Override
    protected boolean runStepInComponent(Map<String, List<Input>> inputValues) throws ComponentException {
        // get the sql query string (variables are already substituted)
        final String sql = getSqlQuery();
        runSqlSafe(sql);
        return true;
    }

    private void runSqlSafe(final String sql) {
        Integer success = null;
        try {
            runSql(sql);
            success = 1;
        } catch (final RuntimeException e) {
            success = 0;
            logger.warn("Failed to execute SQL statement:", e);
            throw e;
        } finally {
            getContext().set(CONTEXT_RUN, "sqlSuccess", success);
            distributeMetas();
        }
    }
        
    protected abstract void runSql(final String sql);


    protected void sqlSetup() {
        final String sqlStatementsString =
            componentContext.getConfigurationValue(SqlComponentConstants.METADATA_SQL_SETUP_PROPERTY);
        executeSqlStatements(sqlStatementsString);
    }

    protected void sqlCleanup() {
        final String sqlStatementsString =
            componentContext.getConfigurationValue(SqlComponentConstants.METADATA_SQL_CLEANUP_PROPERTY);
        executeSqlStatements(sqlStatementsString);
    }

    protected void sqlDispose() {
        final String sqlStatementsString =
            componentContext.getConfigurationValue(SqlComponentConstants.METADATA_SQL_DISPOSE_PROPERTY);
        executeSqlStatements(sqlStatementsString);
    }

    protected void executeSqlStatements(String sqlStatementsString) {
        if (sqlStatementsString == null || sqlStatementsString.isEmpty()) {
            return;
        }
        sqlStatementsString = replace(sqlStatementsString);
        final String[] sqlStatements = sqlStatementsString.split(";");
        // declare the jdbc assets to be able to close them in the finally-clause
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        // exectue the jdbc stuff
        try {
            jdbcConnection = getConnection();
            final boolean autoCommit = jdbcConnection.getAutoCommit();
            jdbcConnection.setAutoCommit(false);
            boolean commited = false;
            try {
                statement = jdbcConnection.createStatement();
                for (final String sql : sqlStatements) {
                    final String sqlClean = sql.trim();
                    if (!sqlClean.isEmpty()) {
                        statement.addBatch(sqlClean);
                    }
                }
                statement.executeBatch();
                jdbcConnection.commit();
                commited = true;
            } finally {
                if (!commited) {
                    jdbcConnection.rollback();
                }
                jdbcConnection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            logger.error("SQL Exception occured. " + e.toString());
            throw new RuntimeException(e.toString());
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("ResultSet or Statement could not be closed properly. " + e.toString());
            }
        }
    }

    protected String getSqlQuery() {
        return getSqlQuery(true);
    }
    
    protected String getSqlQuery(final boolean replace) {
        String result;
        result = componentContext.getConfigurationValue(SqlComponentConstants.METADATA_SQL_PROPERTY);
        if (result != null && replace) {
            result = replace(result);
        }
        return result;
    }
    
    @Override
    protected boolean isSqlInitQueryEnabled() {
        final boolean result = Boolean.valueOf(componentContext.getConfigurationValue(SqlComponentConstants.METADATA_DO_SQL_INIT_PROPERTY));
        return result;
    }

    protected String getSqlInitQuery() {
        return getSqlInitQuery(true);
    }

    protected String getSqlInitQuery(final boolean replace) {
        String result;
        result = componentContext.getConfigurationValue(SqlComponentConstants.METADATA_SQL_INIT_PROPERTY);
        if (result != null && replace) {
            result = replace(result);
        }
        return result;
    }

    protected String getTableName() {
        return getTableName(true);
    }

    protected String getTableName(final boolean replace) {
        String result;
        result = componentContext.getConfigurationValue(SqlComponentConstants.METADATA_TABLE_NAME_PROPERTY);
        if (result != null && replace) {
            result = replace(result);
        }
        return result;
    }
    
    @Override
    public void tearDown(FinalComponentState state) {
        switch (state) {
        case FINISHED:
            closeConnection();
        case CANCELLED:
            sqlCleanup();
        default:
            break;
        }
        super.tearDown(state);
    }

    @Override
    public void dispose() {
        sqlDispose();
        closeConnection();
    }

    protected void distributeResults(ResultSet resultSet) throws SQLException {
        final ComponentContext componentContext = getComponentContext();
        if (!hasDataOutputs() && !hasFullDataOutputs()) {
            return;
        }
        final ResultSetMetaData metaData = ResultSetMetaData.parse(resultSet);
        /*
         * Convert the WHOLE ResultSet to a list of TypedValue arrays.
         * This needs to be changed if the primary use case is iteration over the ResultSet
         */
        final Iterable<TypedValue[]> rows = ResultValueConverter.convertToTypedValueListIterator(resultSet);
        /*
         * If full data outputs exist, the whole result set has to be stored in a collection.
         */
        final boolean storeFullRowList = hasFullDataOutputs();
        List<TypedValue[]> fullRowList = null;
        if (storeFullRowList) {
            fullRowList = new LinkedList<TypedValue[]>();
        }
        // determine the meta outputs
        if (hasDataOutputs()) {
            /*
             * Iterate over each row and write the values to the output.
             */
            final Set<String> dataOuts = getDataOutputs();
            // determine the relevant columns which are those who are mapped to an output with equal
            // name
            final Map<String, Integer> relevantColumns = new HashMap<String, Integer>();
            for (int columnIndex = 0; columnIndex < metaData.columnCount; ++columnIndex) {
                final String columnName = metaData.columnLabels.get(columnIndex);
                // output with equal name indicates a relevant column
                if (dataOuts.contains(columnName)) {
                    relevantColumns.put(columnName, columnIndex);
                }
            }
            // only iterate over the rows if relevant columns exist or meta outputs exist
            if (!relevantColumns.isEmpty() || !getMetaOutputs().isEmpty()) {
                // iterate over the rows
                for (final TypedValue[] row : rows) {
                    // write the value of each relevant column into the output with equal name
                    for (final Map.Entry<String, Integer> relevantColumn : relevantColumns.entrySet()) {
                        final String columnName = relevantColumn.getKey();
                        final int columnIndex = relevantColumn.getValue();
                        final TypedDatum value = DataTypeConverter.convert(row[columnIndex], typedDatumService);
                        componentContext.writeOutput(columnName, value);
                    }
                    // store the row in the full row list if necessary
                    if (storeFullRowList) {
                        fullRowList.add(row);
                    }
                }
            }
        } else if (storeFullRowList) {
            // store the row in the full row list if necessary
            // iterate over the rows
            for (final TypedValue[] row : rows) {
                fullRowList.add(row);
            }
        }
        if (hasFullDataOutputs()) {
            final VariantArray value = ResultValueConverter.convertToVariantArray("VariantArray", fullRowList, metaData.columnCount);
            for (final String outputName : getFullDataOutputs()) {
                componentContext.writeOutput(outputName, DataTypeConverter.convert(value, typedDatumService));
            }
        }
    }

    protected void distributeMetas() {
        for (String variableName : getMetaOutputs()) {
            final Serializable variableValue = getVariableValue(variableName.substring(META_OUTPUT_PREFIX.length()), Serializable.class);
            if (variableValue != null) {
                componentContext.writeOutput(variableName, DataTypeConverter.convert(variableValue,
                    componentContext.getOutputDataType(variableName),
                    typedDatumService));
            } else {
                logger.warn("Cannot discover any value for insertion into outputchannel: " + variableName + "");
            }
        }
    }

}
