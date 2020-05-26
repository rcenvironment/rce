/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.execution;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.DatabaseComponentHistoryDataItem;
import de.rcenvironment.components.database.common.DatabaseStatement;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverService;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Database component execution class.
 *
 * @author Oliver Seebach
 */
@LocalExecutionOnly
public class DatabaseComponent extends DefaultComponent {

    private static final String ERROR_FILLING_SMALL_TABLE = "Error when filling the output '%s' of type small table.";
    
    private static final String FULL_STOP = ".";

    private static final String SAVEPOINT = "RCEdatabaseTrancactionSavepoint";

    private static final int MINUS_ONE = -1;

    private static final String SEMICOLON = ";";

    private static final String JDBC = "jdbc";

    private static final String SLASH = "/";

    private static final String DOUBLE_SLASH = "//";

    private static final String COLON = ":";

    private static TypedDatumService typedDatumService;

    private static JDBCDriverService jdbcDriverService;

    protected final Log logger = LogFactory.getLog(getClass());

    private Savepoint transactionSavepoint;

    private List<Object> inputOrder = new ArrayList<>();

    private ComponentContext componentContext;

    private List<DatabaseStatement> databaseStatements;

    private Connection jdbcConnection = null;

    private DatabaseComponentHistoryDataItem databaseWorkflowDataItem;

    private void initializeNewWorkflowDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            databaseWorkflowDataItem = new DatabaseComponentHistoryDataItem(DatabaseComponentConstants.COMPONENT_ID);
        }
    }

    private void writeFinalWorkflowDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(databaseWorkflowDataItem);
        }
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {

        jdbcDriverService = componentContext.getService(JDBCDriverService.class);
        typedDatumService = componentContext.getService(TypedDatumService.class);

        super.start();
        if (componentContext.getInputs().isEmpty()) {
            runDatabaseComponent();
        }
    }

    @Override
    public void processInputs() throws ComponentException {
        runDatabaseComponent();
        super.processInputs();
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }

    private void runDatabaseComponent() throws ComponentException {

        initializeNewWorkflowDataItem();
        databaseStatements = parseAndValidateStatements();

        Map<String, TypedDatum> inputValues = new HashMap<>();
        if (componentContext != null && componentContext.getInputsWithDatum() != null) {
            for (String inputName : componentContext.getInputsWithDatum()) {
                inputValues.put(inputName, componentContext.readInput(inputName));
            }
        }

        String statementPart = " statement";
        if (databaseStatements.size() > 1) {
            statementPart = " statements";
        }
        componentContext.getLog().componentInfo("Executing " + databaseStatements.size() + statementPart + FULL_STOP);
        boolean autoCommit = false;
        try {
            try {
                // GET CONNECTION AND SET SAVEPOINT FOR POTENTIAL ROLLBACK
                if (jdbcConnection == null || jdbcConnection.isClosed()) {
                    jdbcConnection = getConnection();
                }
                autoCommit = jdbcConnection.getAutoCommit();
                jdbcConnection.setAutoCommit(false);
                transactionSavepoint = jdbcConnection.setSavepoint(SAVEPOINT);
            } catch (SQLException e) {
                throw new ComponentException("Failed to initialize database connection. Database response: " + e.getMessage());
            }

            // PREPARE AND EXECUTE STATEMENTS
            for (DatabaseStatement databaseStatement : databaseStatements) {
                if (!databaseStatement.getStatement().isEmpty()) {
                    boolean statementTypeSupported = statementTypeIsSupportedGeneral(databaseStatement.getStatement());
                    if (statementTypeSupported) {
                        boolean statementContainsSmalltablePlaceholder =
                            statementContainsSmalltablePlaceholder(databaseStatement.getStatement(), inputValues);

                        // WITHOUT SMALLTABLE INPUTS
                        if (!statementContainsSmalltablePlaceholder) {
                            // replace placeholders except for small tables
                            String preparedStatementString = replaceStringAndFillInputOrder(databaseStatement.getStatement(), inputValues);
                            databaseStatement.setStatement(preparedStatementString);
                            try {
                                runSqlStatementUsingPreparedStatement(databaseStatement);
                            } catch (SQLException e) {
                                throw new ComponentException("Failure during execution of database statement '"
                                    + databaseStatement.getName() + "' (" + databaseStatement.getStatement() + "). Database response: "
                                    + e.getMessage());
                            } finally {
                                inputOrder.clear();
                            }
                        } else {
                            // WITH SMALLTABLE INPUTS
                            if (statementTypeIsSupportedForSmalltable(databaseStatement.getStatement())) {
                                // replace placeholders except for small tables
                                String preparedStatementString =
                                    replaceStringAndFillInputOrder(databaseStatement.getStatement(), inputValues);
                                databaseStatement.setStatement(preparedStatementString);
                                try {
                                    runSqlStatementUsingPreparedStatementForSmalltables(databaseStatement, inputValues);
                                } catch (SQLException e) {
                                    throw new ComponentException("Failure during execution of database statement '"
                                        + databaseStatement.getName() + "' (" + databaseStatement.getStatement() + "). Database response: "
                                        + e.getMessage());
                                } finally {
                                    inputOrder.clear();
                                }
                            } else {
                                componentContext.getLog().componentWarn("Database query '" + databaseStatement.getName()
                                    + "' could not be matched to the allowed query types that use small table inputs. "
                                    + "The query will be skipped. Currently 'Insert' is supported for small table inputs.");
                            }
                        }

                    } else {
                        componentContext.getLog().componentWarn("Database query '" + databaseStatement.getStatement()
                            + "' could not be matched to the allowed query types and will be skipped. "
                            + "Currently 'Select', 'Insert', 'Update' and 'Delete' are supported.");
                    }
                } else {
                    componentContext.getLog().componentWarn(
                        "Database query with name '" + databaseStatement.getName() + "' is empty and will be skipped.");
                }
            }
            // COMMIT TRANSACTION
            try {
                jdbcConnection.commit();
            } catch (SQLException e) {
                throw new ComponentException("Failed to commit transaction. Database response: " + e.getMessage());
            }
        } catch (ComponentException e) {
            try {
                jdbcConnection.rollback(transactionSavepoint);
            } catch (SQLException e2) {
                throw new ComponentException("Failed to rollback database. " + e2.getMessage());
            }
            throw e;
        } finally {
            if (jdbcConnection == null) {
                String host = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_HOST);
                String port = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_PORT);
                String scheme = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_SCHEME);
                throw new ComponentException(
                    "Failed to establish database connection. Is the database, username and password correctly defined? "
                        + "You entered -> Host: '" + host + "'; Port: '" + port + "'; Default Scheme: '" + scheme + "'. "
                        + "Note that username and password are not stated here for security reasons.");
            } else {
                try {
                    if (!jdbcConnection.isClosed()) {
                        if (transactionSavepoint != null) {
                            jdbcConnection.releaseSavepoint(transactionSavepoint);
                        }
                        jdbcConnection.setAutoCommit(autoCommit);
                        jdbcConnection.close();
                    }
                } catch (SQLException e) {
                    throw new ComponentException("Failed to release database resources. Database response: " + e.getMessage());
                }
            }
        }

        // WRITE SUCCESS OUTPUT AND WORKFLOW DATA ITEMS
        componentContext.writeOutput(DatabaseComponentConstants.OUTPUT_NAME_SUCCESS, typedDatumService.getFactory().createBoolean(true));
        writeFinalWorkflowDataItem();
    }

    private String buildInsertPlaceholderWithSize(int size) {
        String placeholder = " (";
        for (int i = 0; i < size; i++) {
            placeholder += "?";
            placeholder += ",";
        }
        placeholder = placeholder.substring(0, placeholder.length() - 1); // remove last ","
        placeholder += ") ";
        return placeholder;
    }

    /**
     * Return a configured database connection.
     * 
     * @return the database connection
     * @throws SQLException Thrown when the connection could not be established.
     * @throws ComponentException Thrown when the driver cannot be loaded.
     */
    public Connection getConnection() throws SQLException, ComponentException {

        final String databaseHost = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_HOST);
        final String databasePort = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_PORT);
        String databaseConnector = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_CONNECTOR);
        final String databaseScheme = componentContext.getConfigurationValue(DatabaseComponentConstants.DATABASE_SCHEME);
        final String databaseUser = componentContext.getConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_USER);
        String databasePassword = componentContext.getConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_PHRASE);

        if (jdbcDriverService.getRegisteredJDBCDrivers().isEmpty()) {
            String noRegisteredDriversWarning = "Failed to establish connection because no JDBC driver is registered. "
                + "Please make sure the subfolder '.../configuration/jdbc' in your "
                + "installation directory contains the desired driver file";
            componentContext.getLog().componentError(noRegisteredDriversWarning);
            throw new ComponentException(noRegisteredDriversWarning);
        }

        String urlScheme = "";
        for (JDBCDriverInformation driverInformation : jdbcDriverService.getRegisteredJDBCDrivers()) {
            if (databaseConnector.equals(driverInformation.getDisplayName())) {
                urlScheme = driverInformation.getUrlScheme();
                break;
            }
        }
        if (urlScheme.isEmpty()) {
            String urlSchemeEmptyWarning = "Failed to establish connection because no JDBC driver for the selected connector was found. "
                + "Please make sure the subfolder '.../extras/database_connectors' in your "
                + "installation directory contains the desired driver file";
            componentContext.getLog().componentError(urlSchemeEmptyWarning);
            throw new ComponentException(urlSchemeEmptyWarning);
        }

        String url = JDBC + COLON + urlScheme + COLON + DOUBLE_SLASH + databaseHost + COLON + databasePort + SLASH + databaseScheme;

        Connection connection = null;
        if (databasePassword.isEmpty()) {
            databasePassword = null;
        }

        connection = jdbcDriverService.getConnectionWithCredentials(url, databaseUser, databasePassword);

        return connection;
    }

    // NO SMALLTABLE INPUT
    private void runSqlStatementUsingPreparedStatement(DatabaseStatement databaseStatement) throws SQLException, ComponentException {
        // declare the jdbc assets to be able to close them in the finally-clause
        PreparedStatement preparedStatement = null;
        String sqlStatement = databaseStatement.getStatement();

        if (!sqlStatement.endsWith(SEMICOLON)) {
            sqlStatement += SEMICOLON;
        }
        preparedStatement = jdbcConnection.prepareStatement(sqlStatement);
        if (!inputOrder.isEmpty()) {
            for (int i = 1; i < inputOrder.size() + 1; i++) {
                if (inputOrder.get(i - 1) instanceof String) {
                    String inputToSet = (String) inputOrder.get(i - 1);
                    preparedStatement.setString(i, inputToSet);
                } else if (inputOrder.get(i - 1) instanceof Long) {
                    Integer inputToSet = Integer.valueOf(((Long) inputOrder.get(i - 1)).intValue());
                    preparedStatement.setInt(i, inputToSet);
                } else if (inputOrder.get(i - 1) instanceof Float) {
                    Float inputToSet = (Float) inputOrder.get(i - 1);
                    preparedStatement.setFloat(i, inputToSet);
                } else if (inputOrder.get(i - 1) instanceof Double) {
                    Double inputToSet = (Double) inputOrder.get(i - 1);
                    preparedStatement.setDouble(i, inputToSet);
                } else if (inputOrder.get(i - 1) instanceof Boolean) {
                    Boolean inputToSet = (Boolean) inputOrder.get(i - 1);
                    preparedStatement.setBoolean(i, inputToSet);
                }
            }
        }
        String effectiveQuery = preparedStatement.toString().substring(preparedStatement.toString().indexOf(COLON) + 2);

        if (databaseWorkflowDataItem != null) {
            databaseWorkflowDataItem.addDatabaseStatementHistoryData(
                databaseStatement.getIndex(), databaseStatement.getName(), sqlStatement, effectiveQuery);
        }

        componentContext.getLog().componentInfo("Sending query '" + effectiveQuery + "' to database.");
        ResultSet resultSet = null;
        boolean isManipulation =
            (effectiveQuery.toLowerCase().startsWith(DatabaseComponentConstants.INSERT)
                || effectiveQuery.toLowerCase().startsWith(DatabaseComponentConstants.UPDATE)
                || effectiveQuery.toLowerCase().startsWith(DatabaseComponentConstants.DELETE));
        if (isManipulation) {
            preparedStatement.executeUpdate();
        } else {
            resultSet = preparedStatement.executeQuery();
        }

        // check if result set is not empty
        boolean hasResultSet = false;
        if (resultSet != null) {
            if (resultSet.isBeforeFirst()) {
                if (resultSet.next()) {
                    hasResultSet = true;
                }
                resultSet.beforeFirst();
            }
        }

        if (resultSet != null && hasResultSet && databaseStatement.isWillWriteToOutput()
            && !databaseStatement.getOutputToWriteTo().isEmpty()) {
            distributeResults(databaseStatement.getOutputToWriteTo(), resultSet);
            if (!resultSet.isClosed()) {
                resultSet.close();
            }
        }
        if (!preparedStatement.isClosed()) {
            preparedStatement.close();
        }

    }

    // WITH SMALLTABLE INPUT
    private void runSqlStatementUsingPreparedStatementForSmalltables(DatabaseStatement databaseStatement,
        Map<String, TypedDatum> inputValues) throws SQLException, ComponentException {
        // Set small table to be considered and make sure there exists only one
        SmallTableTD smallTableToHandle = null;
        String smallTablePlaceholder = "";
        for (String key : inputValues.keySet()) {
            TypedDatum td = inputValues.get(key);
            String possiblePlaceholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, key);
            if (td instanceof SmallTableTD && databaseStatement.getStatement().contains(possiblePlaceholder)) {
                if (smallTableToHandle == null) {
                    smallTableToHandle = (SmallTableTD) td;
                    smallTablePlaceholder = possiblePlaceholder;
                } else {
                    throw new ComponentException("Placeholder for small table input '" + key + "' is just allowed once per statement.");
                }
            }
        }
        if (smallTableToHandle == null) {
            throw new ComponentException("Could not find a small table in statement.");
        }

        PreparedStatement preparedStatement = null;
        for (int row = 0; row < smallTableToHandle.getRowCount(); row++) {
            String sqlStatement = databaseStatement.getStatement();
            List<String> smallTableReplacements = new ArrayList<>();
            for (int col = 0; col < smallTableToHandle.getColumnCount(); col++) {
                String value = "";
                TypedDatum cell = smallTableToHandle.getTypedDatumOfCell(row, col);
                DataType dataType = cell.getDataType();
                if (dataType == DataType.Float) {
                    FloatTD floatTD = (FloatTD) cell;
                    value = String.valueOf(floatTD.getFloatValue());
                } else if (dataType == DataType.Integer) {
                    IntegerTD integerTD = (IntegerTD) cell;
                    value = String.valueOf(integerTD.getIntValue());
                } else if (dataType == DataType.ShortText) {
                    ShortTextTD shortTextTD = (ShortTextTD) cell;
                    value = shortTextTD.getShortTextValue();
                } else if (dataType == DataType.Boolean) {
                    BooleanTD booleanTD = (BooleanTD) cell;
                    value = String.valueOf(booleanTD.getBooleanValue());
                } else if (dataType == DataType.Empty) {
                    value = ""; // set explicitly to make clear what is set
                }
                smallTableReplacements.add(value);
            }
            // memorize where placeholder was and remove
            int placeholdersLocation = sqlStatement.indexOf(smallTablePlaceholder);
            sqlStatement =
                sqlStatement.substring(0, placeholdersLocation)
                    + sqlStatement.substring(placeholdersLocation + smallTablePlaceholder.length());
            // insert (?,?,?,? ... ) with according length
            String replace = buildInsertPlaceholderWithSize(smallTableToHandle.getColumnCount());
            sqlStatement = sqlStatement.substring(0, placeholdersLocation) + replace + sqlStatement.substring(placeholdersLocation);
            if (!sqlStatement.trim().endsWith(SEMICOLON)) {
                sqlStatement += SEMICOLON;
            }
            preparedStatement = jdbcConnection.prepareStatement(sqlStatement);
            int index = 1; // insertion is 1 based
            for (String insertion : smallTableReplacements) {
                preparedStatement.setString(index, insertion);
                index++;
            }
            String effectiveQuery = preparedStatement.toString().substring(preparedStatement.toString().indexOf(COLON) + 2);

            if (databaseWorkflowDataItem != null) {
                databaseWorkflowDataItem.addDatabaseStatementHistoryData(
                    databaseStatement.getIndex(), databaseStatement.getName(), sqlStatement, effectiveQuery);
            }

            preparedStatement.executeUpdate();
        }
        if (preparedStatement == null) {
            throw new ComponentException("Error while executing Insert statement. "
                + "Maybe the small table input is empty?");
        }
    }

    private List<DatabaseStatement> parseAndValidateStatements() throws ComponentException {
        List<DatabaseStatement> statementsToValidate = new ArrayList<>();
        // read in statements
        String statementsString = componentContext.getConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY);
        if (statementsString != null) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            try {
                JavaType javaType = mapper.getTypeFactory().constructCollectionType(List.class, DatabaseStatement.class);
                statementsToValidate = mapper.readValue(statementsString, javaType);
            } catch (IOException e) {
                throw new ComponentException("Failed to parse SQL statements while initializing execution.", e);
            }
        } else {
            throw new ComponentException("An error occured while loading statements from configuration.");
        }

        // check output to write to is really set
        for (DatabaseStatement statement : statementsToValidate) {
            String exceptionMessage =
                "The statement '" + statement.getName()
                    + "' is configured to write to an output but no output is selected.";
            if (statement.isWillWriteToOutput()) {
                if (statement.getOutputToWriteTo() == null) {
                    throw new ComponentException(exceptionMessage);
                } else if (statement.getOutputToWriteTo().isEmpty()) {
                    throw new ComponentException(exceptionMessage);
                }
            }
            boolean statementTypeSupported = statementTypeIsSupportedGeneral(statement.getStatement());
            if (!statementTypeSupported) {
                String errorMessage = "Statement '" + statement.getName() + "'" + " (" + statement.getStatement()
                    + ") could not be matched to the allowed query types. "
                    + "Currently 'Select', 'Insert', 'Update' and 'Delete' are supported.";
                throw new ComponentException(errorMessage);
            }
        }
        componentContext.getLog().componentInfo("Statements validation successfully passed.");
        return statementsToValidate;
    }

    private boolean statementTypeIsSupportedGeneral(String statement) {
        for (String allowedStatement : Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_GENERAL)) {
            if (statement.toLowerCase().startsWith(allowedStatement)) {
                return true;
            }
        }
        return false;
    }

    private boolean statementTypeIsSupportedForSmalltable(String statement) {
        for (String allowedStatement : Arrays.asList(DatabaseComponentConstants.STATEMENT_PREFIX_WHITELIST_SMALLTABLE)) {
            if (statement.toLowerCase().startsWith(allowedStatement)) {
                return true;
            }
        }
        return false;
    }

    private boolean statementContainsSmalltablePlaceholder(String databaseStatement, Map<String, TypedDatum> inputValues) {
        for (String key : inputValues.keySet()) {
            if (inputValues.get(key).getDataType() == DataType.SmallTable) {
                String possiblePlaceholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, key);
                if (databaseStatement.contains(possiblePlaceholder)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeFinalWorkflowDataItem();
    }

    /**
     * This method is recursively called and has 2 purposes: 1. All "RCE" placeholders (${...}) are replaced by placeholders for the
     * prepared statement 2. The order of the input values is stored. This is important for the replacement with the actual values. Note
     * that the order has to be reset/cleared per component run.
     * 
     * @param originalStatement The original statement as entered in the UI OR the statement with partial replacements during the recursive
     *        call
     * @param inputValues The actual input values for the current component run
     * @return The statement where placeholders have been replaced by question marks.
     */
    private String replaceStringAndFillInputOrder(String originalStatement, Map<String, TypedDatum> inputValues) {
        String currentStatement = originalStatement;
        Map<Integer, String> tempOccuranceToInputMapping = new HashMap<>();
        for (String inputName : inputValues.keySet()) {
            if (inputValues.get(inputName).getDataType() == DataType.ShortText
                || inputValues.get(inputName).getDataType() == DataType.Float
                || inputValues.get(inputName).getDataType() == DataType.Integer
                || inputValues.get(inputName).getDataType() == DataType.Boolean) {
                String possiblePlaceholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, inputName);
                int firstIndex = currentStatement.indexOf(possiblePlaceholder);
                if (firstIndex != MINUS_ONE) {
                    // add just if an occurance was found.
                    tempOccuranceToInputMapping.put(firstIndex, inputName);
                }
            }
        }
        if (tempOccuranceToInputMapping.size() > 0) {
            int minIndex = findMin(tempOccuranceToInputMapping.keySet());
            String firstAppearingInputName = tempOccuranceToInputMapping.get(minIndex);
            TypedDatum firstAppearingTypedDatum = inputValues.get(firstAppearingInputName);
            if (firstAppearingTypedDatum.getDataType() == DataType.ShortText) {
                String stringToAdd = ((ShortTextTD) inputValues.get(firstAppearingInputName)).getShortTextValue();
                inputOrder.add(stringToAdd);
            } else if (firstAppearingTypedDatum.getDataType() == DataType.Float) {
                double floatToAdd = ((FloatTD) inputValues.get(firstAppearingInputName)).getFloatValue();
                inputOrder.add(floatToAdd);
            } else if (firstAppearingTypedDatum.getDataType() == DataType.Integer) {
                long integerToAdd = ((IntegerTD) inputValues.get(firstAppearingInputName)).getIntValue();
                inputOrder.add(integerToAdd);
            } else if (firstAppearingTypedDatum.getDataType() == DataType.Boolean) {
                Boolean booleanToAdd = ((BooleanTD) inputValues.get(firstAppearingInputName)).getBooleanValue();
                inputOrder.add(booleanToAdd);
            }

            // TODO review if this is the proper approach, recheck about .replaceFirst(...) -- seeb_ol, November 2015
            String possiblePlaceholder = StringUtils.format(DatabaseComponentConstants.INPUT_PLACEHOLDER_PATTERN, firstAppearingInputName);
            String replacement = "?";
            String replacedStatement =
                currentStatement.substring(0, minIndex) + replacement + currentStatement.substring(minIndex + possiblePlaceholder.length());

            currentStatement = replaceStringAndFillInputOrder(replacedStatement, inputValues);
        }
        return currentStatement;
    }

    private int findMin(Set<Integer> indices) {
        int currentMin = Integer.MAX_VALUE;
        for (int index : indices) {
            if (index < currentMin) {
                currentMin = index;
            }
        }
        return currentMin;
    }

    private int determineResultSetsRowCount(ResultSet resultSet) throws ComponentException {
        int rowCount = 0;
        try {
            while (resultSet.next()) {
                rowCount++;
            }
            resultSet.beforeFirst();
        } catch (SQLException e1) {
            throw new ComponentException("Failed to determine result set's row count.", e1);
        }
        return rowCount;
    }

    private int determineResultSetsColumnCount(ResultSet resultSet) throws ComponentException {
        try {
            return resultSet.getMetaData().getColumnCount();
        } catch (SQLException e1) {
            throw new ComponentException("Failed to determine result set's column count.", e1);
        }
    }

    private TypedDatum convertResultSetToTypedDatum(ResultSet resultSet, String outputToWriteTo)
        throws SQLException, ComponentException {
        // Determine resultSet's size
        int columnCount = determineResultSetsColumnCount(resultSet);
        int rowCount = determineResultSetsRowCount(resultSet);
        componentContext.getLog().componentInfo("Processing result set with " + rowCount + " row(s) and " + columnCount + " column(s).");
        DataType dataType = componentContext.getOutputDataType(outputToWriteTo);
        TypedDatumFactory tdFactory = typedDatumService.getFactory();
        TypedDatum result = null;

        if (columnCount == 0 || rowCount == 0) {
            // ################ 0 x 0 ######################
            throw new ComponentException(
                "The database returned an empty result set although writing the result to an output was activated.");
        } else if (columnCount == 1 && rowCount == 1) {
            // ################ 1 x 1 ######################
            if (dataType == DataType.SmallTable) {
                result = convertResultSetToSmallTableTD(rowCount, columnCount, outputToWriteTo, resultSet);
            } else {
                resultSet.next();
                if (dataType == DataType.Float) {
                    if (resultSet.getObject(1) instanceof Float || resultSet.getObject(1) instanceof Double) {
                        result = tdFactory.createFloat(resultSet.getDouble(1));
                    } else if (resultSet.getObject(1) instanceof Integer || resultSet.getObject(1) instanceof Long) {
                        result = tdFactory.createFloat(resultSet.getLong(1));
                    } else {
                        throw new ComponentException("Failed to convert result set to single float value.");
                    }
                } else if (dataType == DataType.Integer) {
                    if (resultSet.getObject(1) instanceof Integer || resultSet.getObject(1) instanceof Long) {
                        result = tdFactory.createInteger(resultSet.getLong(1));
                    } else {
                        throw new ComponentException("Failed to convert result set to single integer value.");
                    }
                } else if (dataType == DataType.ShortText) {
                    if (resultSet.getObject(1) instanceof String) {
                        result = tdFactory.createShortText(resultSet.getString(1));
                    } else {
                        throw new ComponentException("Failed to convert result set to single short text value.");
                    }
                } else if (dataType == DataType.Boolean) {
                    if (resultSet.getObject(1) instanceof Boolean) {
                        result = tdFactory.createBoolean(resultSet.getBoolean(1));
                    } else {
                        throw new ComponentException("Failed to convert result set to single boolean value.");
                    }
                }
            }
        } else if (columnCount > 1 || rowCount > 1) {
            // ################ n x n ######################
            if (dataType == DataType.SmallTable) {
                result = convertResultSetToSmallTableTD(rowCount, columnCount, outputToWriteTo, resultSet);
            } else if (dataType == DataType.Float || dataType == DataType.Integer
                || dataType == DataType.ShortText || dataType == DataType.Boolean) {
                throw new ComponentException("The result set contains " + rowCount + " rows and " + columnCount + " columns "
                    + "and thus cannot be written into selected datatype " + dataType.getDisplayName() + FULL_STOP);
            } else {
                throw new ComponentException("The output's datatype is " + dataType.getDisplayName()
                    + " and is currently not supported for the component.");
            }
        }
        return result;
    }

    /**
     * Converts a given result set to a small table typed datum of the given size.
     * 
     * @param rowCount The rows of the result set.
     * @param columnCount The columns of the result set.
     * @param outputToWriteTo The output name to write to.
     * @param resultSet The result set.
     * @param tdFactory The typed datum factory.
     * @return
     * @throws ComponentException
     */
    private SmallTableTD convertResultSetToSmallTableTD(int rowCount, int columnCount, String outputToWriteTo, ResultSet resultSet)
        throws ComponentException {
        TypedDatumFactory tdFactory = typedDatumService.getFactory();
        SmallTableTD smallTableTD = tdFactory.createSmallTable(rowCount, columnCount);
        // fill table with data
        try {
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    int rowInTable = resultSet.getRow() - 1;
                    int colInTable = i - 1;
                    if (resultSet.getObject(i) instanceof String) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createShortText(resultSet.getString(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof Integer) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createInteger(resultSet.getInt(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof Long) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createInteger(resultSet.getLong(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof Float) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getFloat(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof Double) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getDouble(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof Boolean) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createBoolean(resultSet.getBoolean(i)), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) == null) {
                        smallTableTD.setTypedDatumForCell(tdFactory.createEmpty(), rowInTable, colInTable);
                    } else if (resultSet.getObject(i) instanceof BigDecimal) {
                        throw new ComponentException(StringUtils.format(ERROR_FILLING_SMALL_TABLE, outputToWriteTo)
                            + "Note that currently no internal data type represents 'big decimal' values.");
                    } else if (resultSet.getObject(i) instanceof BigInteger) {
                        throw new ComponentException(StringUtils.format(ERROR_FILLING_SMALL_TABLE, outputToWriteTo)
                            + "Note that currently no internal data type represents 'big integer' values.");
                    } else {
                        throw new ComponentException(StringUtils.format(ERROR_FILLING_SMALL_TABLE, outputToWriteTo)
                            + "The given data type '" + resultSet.getObject(i).getClass().getName() + "' is currently not supported.");
                    }
                    // Datetime currently not supported in DB component - seeb_ol, April 2016
                    // else if (resultSet.getObject(i) instanceof Timestamp) {
                    // smallTableTD.setTypedDatumForCell(tdFactory.createDateTime((resultSet.getTimestamp(i).getTime())), rowInTable,
                    // colInTable);
                    // }
                }
            }
        } catch (SQLException e) {
            throw new ComponentException("Failed to distribute result set. Database response: " + e.getMessage());
        }
        return smallTableTD;
    }

    protected void distributeResults(String outputToWriteTo, ResultSet resultSet) throws SQLException, ComponentException {
        TypedDatum convertedTypedDatum = convertResultSetToTypedDatum(resultSet, outputToWriteTo);
        if (convertedTypedDatum != null) {
            componentContext.writeOutput(outputToWriteTo, convertedTypedDatum);
        } else {
            componentContext.getLog().componentError(
                "Failed to convert the database result set into to the given output " + outputToWriteTo + FULL_STOP);
        }
    }

    @Override
    public void dispose() {
        closeConnection();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        switch (state) {
        case FINISHED:
            closeConnection();
        default:
            break;
        }
        super.tearDown(state);
    }

    protected void closeConnection() {
        if (jdbcConnection != null) {
            try {
                if (!jdbcConnection.isClosed()) {
                    jdbcConnection.close();
                }
            } catch (SQLException e) {
                componentContext.getLog().componentError("Database connection could not be closed. " + e.getMessage());
            }
            jdbcConnection = null;
        }
    }

}
