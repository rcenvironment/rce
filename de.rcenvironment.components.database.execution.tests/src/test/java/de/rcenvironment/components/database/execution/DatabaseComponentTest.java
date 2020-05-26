/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.execution;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverService;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.testutils.ComponentContextMock;
import de.rcenvironment.core.component.testutils.ComponentTestWrapper;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Component tests for {@link DatabaseComponent}..
 *
 * @author Oliver Seebach
 */
public class DatabaseComponentTest {

    private static final String ESCAPED_SLASH = "\"";

    private static final String CONNECTOR_NAME = "ConnectorName";

    private static final String ANY_URL = "AnyURL";

    private static final String SELECT_SOMETHING = "SELECT something";

    private static final String DEF_OUTPUT_NAME = "out";

    private static final String DEF_STATEMENT_NAME = "some name";

    /**
     * Expected fails.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ComponentTestWrapper component;

    private ComponentContextMock context;

    private TypedDatumFactory typedDatumFactory;

    private Set<JDBCDriverInformation> createJDBCDriverInformationSetMock() {
        // JDBC INFORMATION
        JDBCDriverInformation driverInfoMock = EasyMock.createNiceMock(JDBCDriverInformation.class);
        EasyMock.expect(driverInfoMock.getDisplayName()).andReturn(CONNECTOR_NAME).anyTimes();
        EasyMock.expect(driverInfoMock.getUrlScheme()).andReturn(ANY_URL).anyTimes();
        EasyMock.replay(driverInfoMock);
        Set<JDBCDriverInformation> jdbcInformationsSet = new HashSet<JDBCDriverInformation>();
        jdbcInformationsSet.add(driverInfoMock);
        return jdbcInformationsSet;
    }

    private Set<JDBCDriverInformation> createEmptyJDBCDriverInformationSetMock() {
        // JDBC INFORMATION
        JDBCDriverInformation driverInfoMock = EasyMock.createNiceMock(JDBCDriverInformation.class);
        EasyMock.expect(driverInfoMock.getDisplayName()).andReturn(CONNECTOR_NAME).anyTimes();
        EasyMock.expect(driverInfoMock.getUrlScheme()).andReturn(ANY_URL).anyTimes();
        EasyMock.replay(driverInfoMock);
        Set<JDBCDriverInformation> jdbcInformationsSet = new HashSet<JDBCDriverInformation>();
        return jdbcInformationsSet;
    }

    private PreparedStatement createPreparedStatementMock(ResultSet resultSetMock) throws SQLException {
        PreparedStatement preparedStatementMock = EasyMock.createStrictMock(PreparedStatement.class);
        EasyMock.expect(preparedStatementMock.executeQuery()).andStubReturn(resultSetMock);
        EasyMock.expect(preparedStatementMock.isClosed()).andReturn(false);
        preparedStatementMock.close();
        EasyMock.expectLastCall();
        EasyMock.replay(preparedStatementMock);
        return preparedStatementMock;
    }

    private Statement createStatementMock(ResultSet resultSetMock) throws SQLException {
        Statement statementMock = EasyMock.createStrictMock(Statement.class);
        EasyMock.expect(statementMock.getResultSet()).andStubReturn(resultSetMock);
        EasyMock.replay(statementMock);
        return statementMock;
    }

    private Connection createConnectionFailsToCreateStatementMock(Statement statementMock, PreparedStatement preparedStatementMock)
        throws SQLException {
        Connection connectionMock = EasyMock.createStrictMock(Connection.class);
        EasyMock.expect(connectionMock.prepareStatement(EasyMock.anyObject(String.class))).andStubThrow(
            EasyMock.createNiceMock(SQLException.class));
        return connectionMock;
    }

    private Connection createConnectionMock(Statement statementMock, PreparedStatement preparedStatementMock) throws SQLException {
        Connection connectionMock = EasyMock.createStrictMock(Connection.class);
        EasyMock.expect(connectionMock.createStatement()).andStubReturn(statementMock);
        EasyMock.expect(connectionMock.prepareStatement(EasyMock.anyObject(String.class))).andStubReturn(preparedStatementMock);
        EasyMock.expect(connectionMock.getAutoCommit()).andStubReturn(false);
        EasyMock.expect(connectionMock.isClosed()).andStubReturn(false);
        connectionMock.setAutoCommit(EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(connectionMock.isClosed()).andReturn(false).anyTimes();
        connectionMock.close();
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(connectionMock.setSavepoint(EasyMock.anyObject(String.class))).andStubReturn(
            EasyMock.createNiceMock(Savepoint.class));
        connectionMock.commit();
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(connectionMock.isClosed()).andReturn(true).anyTimes();
        connectionMock.releaseSavepoint(EasyMock.anyObject(Savepoint.class));
        EasyMock.expectLastCall().anyTimes();
        connectionMock.setAutoCommit(EasyMock.anyBoolean());
        EasyMock.expectLastCall();
        connectionMock.close();
        EasyMock.expectLastCall();
        EasyMock.expect(connectionMock.isClosed()).andReturn(true).anyTimes();
        EasyMock.replay(connectionMock);
        return connectionMock;
    }

    private ResultSet createResultSetMock(int rows, int cols) throws SQLException {
        ResultSetMetaData resultSetMetaDataMock = EasyMock.createStrictMock(ResultSetMetaData.class);
        EasyMock.expect(resultSetMetaDataMock.getColumnCount()).andReturn(cols);
        EasyMock.replay(resultSetMetaDataMock);

        ResultSet resultSetMock = EasyMock.createStrictMock(ResultSet.class);
        // ------------
        // CHECK IF RESULT SET IS EMPTY
        EasyMock.expect(resultSetMock.isBeforeFirst()).andReturn(true);
        EasyMock.expect(resultSetMock.next()).andReturn(true).once();
        resultSetMock.beforeFirst();
        EasyMock.expectLastCall();

        // ------------
        // GET SIZE OF RESULT SET
        EasyMock.expect(resultSetMock.getMetaData()).andStubReturn(resultSetMetaDataMock);
        EasyMock.expect(resultSetMock.next()).andReturn(true).times(rows);
        EasyMock.expect(resultSetMock.next()).andReturn(false).once();
        resultSetMock.beforeFirst();
        EasyMock.expectLastCall().anyTimes();

        // ------------
        // HANDLE ACTUAL RESULT SET
        EasyMock.expect(resultSetMock.getObject(EasyMock.anyInt())).andStubReturn(true);
        EasyMock.expect(resultSetMock.getRow()).andStubReturn(3);
        EasyMock.expect(resultSetMock.getBoolean(EasyMock.anyInt())).andStubReturn(true);

        EasyMock.expect(resultSetMock.next()).andReturn(true).times(5);
        EasyMock.expect(resultSetMock.next()).andReturn(false).once();

        // ------------
        // CLOSE DOWN
        EasyMock.expect(resultSetMock.isClosed()).andReturn(false).anyTimes();
        resultSetMock.close();
        EasyMock.expectLastCall().anyTimes();

        EasyMock.replay(resultSetMock);
        return resultSetMock;
    }

    private void setArbitraryConnectionConfigurations() {
        context.setConfigurationValue(DatabaseComponentConstants.DATABASE_CONNECTOR, CONNECTOR_NAME);
        context.setConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_PHRASE, "SomePassword");
        context.setConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_USER, "SomeUser");
    }

    private JDBCDriverService createJDBCDriverServiceMock(Connection connectionMock, Set<JDBCDriverInformation> jdbcInformationsSet)
        throws SQLException {
        JDBCDriverService jdbcDriverServiceMock = EasyMock.createStrictMock(JDBCDriverService.class);
        EasyMock.expect(
            jdbcDriverServiceMock.getConnectionWithCredentials(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
                EasyMock.anyObject(String.class))).andStubReturn(connectionMock);
        EasyMock.expect(jdbcDriverServiceMock.getRegisteredJDBCDrivers()).andReturn(jdbcInformationsSet).anyTimes();
        EasyMock.replay(jdbcDriverServiceMock);
        return jdbcDriverServiceMock;
    }

    /**
     * Common setup.
     *
     * @throws IOException e
     * @throws SQLException se
     */
    @Before
    public void setUp() throws IOException, SQLException {
        context = new ComponentContextMock();
        component = new ComponentTestWrapper(new DatabaseComponent(), context);
        typedDatumFactory = context.getService(TypedDatumService.class).getFactory();

    }

    /**
     * Common cleanup.
     */
    @After
    public void tearDown() {
        component.tearDown(Component.FinalComponentState.FINISHED);
        component.dispose();
    }

    private String convertParametersToKey(int index, String name, String statement, boolean willWriteToOutput, String outputToWriteTo) {
        String key = "";
        if (name != null) {
            name = ESCAPED_SLASH + name + ESCAPED_SLASH;
        }
        if (statement != null) {
            statement = ESCAPED_SLASH + statement + ESCAPED_SLASH;
        }
        if (outputToWriteTo != null) {
            outputToWriteTo = ESCAPED_SLASH + outputToWriteTo + ESCAPED_SLASH;
        }
        key =
            "[{\"index\": " + String.valueOf(index) + " ,\"name\":" + name + ",\"statement\":" + statement
                + ",\"willWriteToOutput\":" + willWriteToOutput + ",\"outputToWriteTo\":" + outputToWriteTo + "}]";
        return key;
    }

    private void setUpValidContext() throws SQLException {

        context.addSimulatedOutput("Success", "Success", DataType.Boolean, false, new HashMap<String, String>());

        // Add Service to context
        context
            .addService(
                JDBCDriverService.class,
                createJDBCDriverServiceMock(
                    createConnectionMock(createStatementMock(createResultSetMock(5, 5)),
                        createPreparedStatementMock(createResultSetMock(5, 5))), createJDBCDriverInformationSetMock()));

        setArbitraryConnectionConfigurations();
    }

    /**
     * A simple test that runs through with an arbitrary valid setup.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testRunThroughSimple() throws SQLException, ComponentException {

        setUpValidContext();

        context.addSimulatedOutput(DEF_OUTPUT_NAME, DEF_OUTPUT_NAME, DataType.SmallTable, true, null);
        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, "SELECT * FROM myTable;", true, DEF_OUTPUT_NAME);
        context.setConfigurationValue((DatabaseComponentConstants.DB_STATEMENTS_KEY), key);

        component.start();
    }
    
    /**
     * A test that checks if null statements are handled correctly.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testValidationOfNullStatement() throws ComponentException {
        context.setConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY, null);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("error occured while loading statements");
        component.start();
    }
    
    /**
     * A test that checks if invalid statements are handled correctly.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testValidationOfInvalidStatement() throws ComponentException {
        context.setConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY, "rubbish");

        thrown.expect(ComponentException.class);
        thrown.expectMessage("Failed to parse SQL statement");
        component.start();
    }
    
    /**
     * A test that checks if invalid writing to output settings are handled correctly.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testWriteToOutputButNullDefined() throws ComponentException {
        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, SELECT_SOMETHING, true, null);
        context.setConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY, key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("is configured to write to an output but no output is selected");
        component.start();
    }
    
    /**
     * A test that checks if invalid writing to output settings are handled correctly.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testWriteToOutputButEmptyDefined() throws ComponentException {
        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, SELECT_SOMETHING, true, "");
        context.setConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY, key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("is configured to write to an output but no output is selected");
        component.start();
    }
    
    /**
     * A test that checks how missing jdbc drivers are handled.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testNoJDBCDriverRegistered() throws ComponentException, SQLException {

        context
            .addService(
                JDBCDriverService.class,
                createJDBCDriverServiceMock(
                    createConnectionFailsToCreateStatementMock(createStatementMock(createResultSetMock(5, 5)),
                        createPreparedStatementMock(createResultSetMock(5, 5))), createEmptyJDBCDriverInformationSetMock()));

        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, SELECT_SOMETHING, false, null);
        context.setConfigurationValue(DatabaseComponentConstants.DB_STATEMENTS_KEY, key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("Failed to establish");
        component.start();
    }

    /**
     * A simple test that uses invalid statement type.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testInvalidStatementBehavior() throws SQLException, ComponentException {

        setUpValidContext();

        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, "CREATE TABLE test ()", false, "");
        context.setConfigurationValue((DatabaseComponentConstants.DB_STATEMENTS_KEY), key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("allowed query types");
        component.start();
    }

    /**
     * A simple test where statement creation fails.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testConnectionFailureBehavior() throws SQLException, ComponentException {

        context
            .addService(
                JDBCDriverService.class,
                createJDBCDriverServiceMock(
                    createConnectionFailsToCreateStatementMock(createStatementMock(createResultSetMock(5, 5)),
                        createPreparedStatementMock(createResultSetMock(5, 5))), createJDBCDriverInformationSetMock()));

        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, "SELECT * FROM table WHERE something", false, "");
        context.setConfigurationValue((DatabaseComponentConstants.DB_STATEMENTS_KEY), key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("Failed to establish database connection");
        component.start();

    }

    /**
     * A simple test that has writing to an output selected but not output activated.
     * 
     * @throws SQLException an exception more on database site.
     * @throws ComponentException an exception by the component.
     */
    @Test
    public void testWriteToOutputButNoOutputSelectedBehavior() throws SQLException, ComponentException {
        setUpValidContext();

        String key = convertParametersToKey(0, DEF_STATEMENT_NAME, "SELECT * FROM myTable;", true, "");
        context.setConfigurationValue((DatabaseComponentConstants.DB_STATEMENTS_KEY), key);

        thrown.expect(ComponentException.class);
        thrown.expectMessage("but no output is selected");
        component.start();
    }

    // /**
    // * ................................
    // *
    // * @throws SQLException
    // */
    // @Test
    // public void simpleStatementTest() throws ComponentException, SQLException {
    // // JDBC INFORMATION
    // JDBCDriverInformation driverInfoMock = EasyMock.createNiceMock(JDBCDriverInformation.class);
    // EasyMock.expect(driverInfoMock.getDisplayName()).andReturn("ConnectorName").anyTimes();
    // EasyMock.expect(driverInfoMock.getUrlScheme()).andReturn("AnyURL").anyTimes();
    // EasyMock.replay(driverInfoMock);
    // HashSet<JDBCDriverInformation> jdbcInformationsSet = new HashSet<JDBCDriverInformation>();
    // jdbcInformationsSet.add(driverInfoMock);
    //
    // int COL_COUNT = 5;
    // int ROW_COUNT = 5;
    // ResultSetMetaData resultSetMetaDataMock = EasyMock.createStrictMock(ResultSetMetaData.class);
    // EasyMock.expect(resultSetMetaDataMock.getColumnCount()).andReturn(COL_COUNT);
    // EasyMock.replay(resultSetMetaDataMock);
    //
    // ResultSet resultSetMock = EasyMock.createStrictMock(ResultSet.class);
    // // ------------
    // // CHECK IF RESULT SET IS EMPTY
    // EasyMock.expect(resultSetMock.isBeforeFirst()).andReturn(true);
    // EasyMock.expect(resultSetMock.next()).andReturn(true).once();
    // resultSetMock.beforeFirst();
    // EasyMock.expectLastCall();
    //
    // // ------------
    // // GET SIZE OF RESULT SET
    // EasyMock.expect(resultSetMock.getMetaData()).andStubReturn(resultSetMetaDataMock);
    // EasyMock.expect(resultSetMock.next()).andReturn(true).times(ROW_COUNT);
    // EasyMock.expect(resultSetMock.next()).andReturn(false).once();
    // resultSetMock.beforeFirst();
    // EasyMock.expectLastCall().anyTimes();
    //
    // // ------------
    // // HANDLE ACTUAL RESULT SET
    // EasyMock.expect(resultSetMock.getObject(EasyMock.anyInt())).andStubReturn(new Boolean(true));
    // EasyMock.expect(resultSetMock.getRow()).andStubReturn(3);
    // EasyMock.expect(resultSetMock.getBoolean(EasyMock.anyInt())).andStubReturn(true);
    //
    // EasyMock.expect(resultSetMock.next()).andReturn(true).times(5);
    // EasyMock.expect(resultSetMock.next()).andReturn(false).once();
    //
    // // ------------
    // // CLOSE DOWN
    // EasyMock.expect(resultSetMock.isClosed()).andReturn(false).anyTimes();
    // resultSetMock.close();
    // EasyMock.expectLastCall().anyTimes();
    //
    // EasyMock.replay(resultSetMock);
    //
    // Statement statementMock = EasyMock.createStrictMock(Statement.class);
    // EasyMock.expect(statementMock.getResultSet()).andStubReturn(resultSetMock);
    // EasyMock.replay(statementMock);
    //
    // PreparedStatement preparedStatementMock = EasyMock.createStrictMock(PreparedStatement.class);
    // EasyMock.expect(preparedStatementMock.executeQuery()).andStubReturn(resultSetMock);
    // EasyMock.expect(preparedStatementMock.isClosed()).andReturn(false);
    // preparedStatementMock.close();
    // EasyMock.expectLastCall();
    // EasyMock.replay(preparedStatementMock);
    //
    // Connection connectionMock = EasyMock.createStrictMock(Connection.class);
    // EasyMock.expect(connectionMock.createStatement()).andStubReturn(statementMock);
    // EasyMock.expect(connectionMock.prepareStatement(EasyMock.anyObject(String.class))).andStubReturn(preparedStatementMock);
    // EasyMock.expect(connectionMock.getAutoCommit()).andStubReturn(false);
    // EasyMock.expect(connectionMock.isClosed()).andStubReturn(false);
    // connectionMock.setAutoCommit(EasyMock.anyBoolean());
    // EasyMock.expectLastCall().anyTimes();
    // EasyMock.expect(connectionMock.isClosed()).andReturn(false).anyTimes();
    // connectionMock.close();
    // EasyMock.expectLastCall().anyTimes();
    // EasyMock.expect(connectionMock.setSavepoint(EasyMock.anyObject(String.class))).andStubReturn(
    // EasyMock.createNiceMock(Savepoint.class));
    // connectionMock.commit();
    // EasyMock.expectLastCall().anyTimes();
    // EasyMock.expect(connectionMock.isClosed()).andReturn(true).anyTimes();
    // connectionMock.releaseSavepoint(EasyMock.anyObject(Savepoint.class));
    // EasyMock.expectLastCall().anyTimes();
    // connectionMock.setAutoCommit(EasyMock.anyBoolean());
    // EasyMock.expectLastCall();
    // connectionMock.close();
    // EasyMock.expectLastCall();
    // EasyMock.expect(connectionMock.isClosed()).andReturn(true).anyTimes();
    // EasyMock.replay(connectionMock);
    //
    // // Service that returns Connection
    // JDBCDriverService jdbcDriverServiceMock = EasyMock.createStrictMock(JDBCDriverService.class);
    // EasyMock.expect(
    // jdbcDriverServiceMock.getConnectionWithCredentials(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
    // EasyMock.anyObject(String.class))).andStubReturn(connectionMock);
    // EasyMock.expect(jdbcDriverServiceMock.getRegisteredJDBCDrivers()).andReturn(jdbcInformationsSet).anyTimes();
    // EasyMock.replay(jdbcDriverServiceMock);
    //
    // // Add Service to context
    // context.addService(JDBCDriverService.class, jdbcDriverServiceMock);
    // context.setConfigurationValue(DatabaseComponentConstants.DATABASE_CONNECTOR, "ConnectorName");
    // context.setConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_PHRASE, "SomePassword");
    // context.setConfigurationValue(DatabaseComponentConstants.CONFIG_KEY_AUTH_USER, "SomeUser");
    //
    // context.addSimulatedOutput("out", "out", DataType.SmallTable, true, null);
    // context.addSimulatedOutput("Success", "Success", DataType.Boolean, false, null);
    // String key = convertParametersToKey(0, "some name", "SELECT * FROM myTable;", true, "out");
    // context.setConfigurationValue((DatabaseComponentConstants.DB_STATEMENTS_KEY), key);
    //
    // component.start();
    //
    // assertEquals(true, context.getCapturedOutput("out").toString().contains("true"));
    // }

}
