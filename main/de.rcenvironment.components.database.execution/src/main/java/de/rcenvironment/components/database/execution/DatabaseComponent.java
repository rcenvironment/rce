/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.execution;

import java.io.IOException;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

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
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Database component execution class.
 *
 * @author Oliver Seebach
 */
@LocalExecutionOnly
public class DatabaseComponent extends DefaultComponent {

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

    private List inputOrder = new ArrayList<>();

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

        jdbcDriverService = componentContext.getService(JDBCDriverService.class);
        typedDatumService = componentContext.getService(TypedDatumService.class);

    }

    @Override
    public void start() throws ComponentException {
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
        checkStatementValidity();

        Map<String, TypedDatum> inputValues = new HashMap<>();
        if (componentContext != null && componentContext.getInputsWithDatum() != null) {
            for (String inputName : componentContext.getInputsWithDatum()) {
                inputValues.put(inputName, componentContext.readInput(inputName));
            }
        }

        String statementsString = componentContext.getConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY);
        if (statementsString != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JavaType javaType = mapper.getTypeFactory().constructCollectionType(List.class, DatabaseStatement.class);
                databaseStatements = mapper.readValue(statementsString, javaType);
            } catch (IOException e) {
                throw new ComponentException("Failed to parse SQL statements while initializing execution.", e);
            }
        } else {
            throw new ComponentException("An error occured while loading statements from configuration.");
        }

        String statementPart = " statement";
        if (databaseStatements.size() > 1) {
            statementPart = " statements";
        }
        componentContext.getLog().componentInfo("Executing " + databaseStatements.size() + statementPart + ".");
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
        componentContext.writeOutput("Success", typedDatumService.getFactory().createBoolean(true));
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
            throw new ComponentException("Failed to establish connection because no JDBC driver is registered. "
                + "Please make sure the subfolder '.../configuration/jdbc' in your "
                + "installation directory contains the desired driver file");
        }

        String urlScheme = "";
        for (JDBCDriverInformation driverInformation : jdbcDriverService.getRegisteredJDBCDrivers()) {
            if (databaseConnector.equals(driverInformation.getDisplayName())) {
                urlScheme = driverInformation.getUrlScheme();
                break;
            }
        }
        if (urlScheme.isEmpty()) {
            throw new ComponentException("Failed to establish connection because no JDBC driver for the selected connector was found. "
                + "Please make sure the subfolder '.../extras/database_connectors' in your "
                + "installation directory contains the desired driver file");
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
            distributeResults(databaseStatement, resultSet);
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

    private void checkStatementValidity() throws ComponentException {
        List<DatabaseStatement> statementsToValidate = new ArrayList<>();
        // read in statements
        String statementsString = componentContext.getConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY);
        if (statementsString != null) {
            ObjectMapper mapper = new ObjectMapper();
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
                "The statement ' " + statement.getName()
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

    private TypedDatum convertResultSetToTypedDatum(ResultSet resultSet, String outputToWriteTo)
        throws SQLException, ComponentException {
        // Determine resultSet's size
        int columnCount = 0;
        int rowCount = 0;
        try {
            columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                rowCount++;
            }
            resultSet.beforeFirst();
        } catch (SQLException e1) {
            throw new ComponentException("Failed to parse result set from database", e1);
        }
        componentContext.getLog().componentInfo("Processing result set with " + rowCount + " row(s) and " + columnCount + " column(s).");
        DataType dataType = componentContext.getOutputDataType(outputToWriteTo);
        TypedDatumFactory tdFactory = typedDatumService.getFactory();
        if (columnCount == 0 || rowCount == 0) {
            // ################ 0 x 0 ######################
            throw new ComponentException("Received empty result set although a result was expected.");
        } else if (columnCount == 1 && rowCount == 1) {
            // ################ 1 x 1 ######################
            if (dataType == DataType.Float) {
                resultSet.next();
                if (resultSet.getObject(1) instanceof Float || resultSet.getObject(1) instanceof Double) {
                    TypedDatum result = tdFactory.createFloat(resultSet.getDouble(1));
                    return result;
                }
                if (resultSet.getObject(1) instanceof Integer) {
                    TypedDatum result = tdFactory.createInteger(resultSet.getInt(1));
                    return result;
                } else {
                    throw new ComponentException("Failed to convert result set to single float value.");
                }
            } else if (dataType == DataType.Integer) {
                resultSet.next();
                if (resultSet.getObject(1) instanceof Integer) {
                    TypedDatum result = tdFactory.createInteger(resultSet.getInt(1));
                    return result;
                } else {
                    throw new ComponentException("Failed to convert result set to single integer value.");
                }
            } else if (dataType == DataType.ShortText) {
                resultSet.next();
                if (resultSet.getObject(1) instanceof String) {
                    TypedDatum result = tdFactory.createShortText(resultSet.getString(1));
                    return result;
                } else {
                    throw new ComponentException("Failed to convert result set to single short text value.");
                }
            } else if (dataType == DataType.Boolean) {
                resultSet.next();
                if (resultSet.getObject(1) instanceof Boolean) {
                    TypedDatum result = tdFactory.createBoolean(resultSet.getBoolean(1));
                    return result;
                } else {
                    throw new ComponentException("Failed to convert result set to single boolean value.");
                }
            } else if (dataType == DataType.SmallTable) {
                SmallTableTD smallTableTD = tdFactory.createSmallTable(1, 1);
                // fill table with data
                try {
                    while (resultSet.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            if (resultSet.getObject(i) instanceof String) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD
                                    .setTypedDatumForCell(tdFactory.createShortText(resultSet.getString(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Integer) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createInteger(resultSet.getInt(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Float) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getFloat(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Boolean) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createBoolean(resultSet.getBoolean(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Double) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getDouble(i)), rowInTable, colInTable);
                            } else {
                                throw new ComponentException("Error when filling the output " + outputToWriteTo + " of type small table. "
                                    + "The given data type is currently not supported.");
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new ComponentException("Failed to distribute result set. Database response: " + e.getMessage());
                }
                return smallTableTD;
            }
        } else if (columnCount > 1 || rowCount > 1) {
            // ################ n x n ######################
            if (dataType == DataType.Float || dataType == DataType.Integer
                || dataType == DataType.ShortText || dataType == DataType.Boolean) {
                throw new ComponentException("Result set contains of several rows and/or columns "
                    + "and cannot be written into selected datatype.");
            } else if (dataType == DataType.SmallTable) {
                SmallTableTD smallTableTD = tdFactory.createSmallTable(rowCount, columnCount);
                // fill table with data
                try {
                    while (resultSet.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            if (resultSet.getObject(i) instanceof String) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD
                                    .setTypedDatumForCell(tdFactory.createShortText(resultSet.getString(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Integer) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createInteger(resultSet.getInt(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Float) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getFloat(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Boolean) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createBoolean(resultSet.getBoolean(i)), rowInTable, colInTable);
                            } else if (resultSet.getObject(i) instanceof Double) {
                                int rowInTable = resultSet.getRow() - 1;
                                int colInTable = i - 1;
                                smallTableTD.setTypedDatumForCell(tdFactory.createFloat(resultSet.getDouble(i)), rowInTable, colInTable);
                            } else {
                                throw new ComponentException("Error when filling the output " + outputToWriteTo + " of type small table. "
                                    + "The given data type is currently not supported.");
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new ComponentException("Failed to distribute result set. Database response: " + e.getMessage());
                }
                return smallTableTD;
            }
        }
        return null;
    }

    protected void distributeResults(DatabaseStatement statement, ResultSet resultSet) throws SQLException, ComponentException {
        TypedDatum convertedTypedDatum = convertResultSetToTypedDatum(resultSet, statement.getOutputToWriteTo());
        if (convertedTypedDatum != null) {
            componentContext.writeOutput(statement.getOutputToWriteTo(), convertedTypedDatum);
        } else {
            componentContext.getLog().componentError("Failed to convert the database result set into to the given output.");
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
