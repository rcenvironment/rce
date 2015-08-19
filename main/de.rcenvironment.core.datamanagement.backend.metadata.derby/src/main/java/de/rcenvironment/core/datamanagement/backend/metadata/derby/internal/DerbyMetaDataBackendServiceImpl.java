/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BIG_VALUE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BINARY_REFERENCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BINARY_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_INSTANCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_INSTANCE_NAME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_RUN_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPRESSION;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.CONTROLLER_NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COUNTER;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATAMANAGEMENT_NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATA_REFERENCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATA_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.ENDPOINT_INSTANCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.ENDTIME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.FINAL_STATE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.HISTORY_DATA_ITEM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.NAME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REFERENCES_DELETED;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTINSTANCE_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_DATAREFERENCE_BINARYREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REVISION;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.STARTTIME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_BINARY_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_DATA_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TIMELINE_INTERVAL;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TYPED_DATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TIMELINE_DATA_ITEM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TIMELINE_INTERVAL_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TO_BE_DELETED;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TYPE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TYPED_DATUM_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VALUE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_RUNS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_TIMELINE_INTERVALS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_COMPONENTRUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_TYPEDDATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.WORKFLOW_RUN_ID;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.dbcp.datasources.SharedPoolDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamanagement.DataService;
import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.ComponentRunInterval;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.TimelineInterval;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunTimline;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Derby implementation of {@link MetaDataBackendService}.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
public class DerbyMetaDataBackendServiceImpl implements MetaDataBackendService {

    private static final String FALSE = "FALSE";

    private static final String SINGE_QOUTE = "'";

    private static final String NOT_EQUAL = " != ";

    private static final int NOT_MARKED_TO_BE_DELETED = 0;

    private static final int WORKFLOW_RUN_TO_BE_DELETED = 1;

    private static final int FILES_TO_BE_DELETED = 2;

    private static final int MAX_VALUE_LENGTH = 32672;

    private static final String TRUE = "true";

    private static final String BRACKET_STRING_PLACEHOLDER = "(%s)";

    private static final String GROUP_BY = " GROUP BY ";

    private static final int TIME_TO_WAIT_FOR_RETRY = 5000;

    private static final String DB_PREFIX = "APP.";

    private static final String STRING_PLACEHOLDER = "%s";

    private static final String PLACEHOLDER_FOUR_VALUES = "(?,?,?,?)";

    private static final String PLACEHOLDER_THREE_VALUES = "(?,?,?)";

    private static final String PLACEHOLDER_TWO_VALUES = "(?,?)";

    private static final String QMARK = " ? ";

    private static final String INITIALIZATION_TIMEOUT_ERROR_MESSAGE =
        "Initialization timeout reached for meta data database.";

    private static final int INITIALIZATION_TIMEOUT = 30;

    private static final int MAX_RETRIES = 5;

    private static final Log LOGGER = LogFactory.getLog(DerbyMetaDataBackendServiceImpl.class);

    private static final String WHERE = " WHERE ";

    private static final String EQUAL = " = ";

    private static final String COMMA = " , ";

    private static final String AND = " AND ";

    private static final String SELECT = " SELECT ";

    private static final String FROM = " FROM ";

    private static final String ORDER_BY = " ORDER BY ";

    private static final String DELETE_FROM = " DELETE FROM ";

    private static final String INSERT_INTO = " INSERT INTO ";

    private static final String UPDATE = " UPDATE ";

    private static final String SET = " SET ";

    private static final String METADATA_DB_NAME = "metadata";

    private static final String VALUES = " VALUES ";

    private static final String DESCENDING = " DESC ";

    private static final String DOT = ".";

    private static final String INNER_JOIN = " INNER JOIN ";

    private static final String ON = " ON ";

    private static final String SELECT_ALL = " SELECT * ";

    private static final String IN = " IN ";

    private static final String IS_NULL = " IS NULL ";

    private final CountDownLatch initializationLatch = new CountDownLatch(1);

    private SharedPoolDataSource connectionPool;

    private EmbeddedConnectionPoolDataSource connectionPoolDatasource;

    private DerbyMetaDataBackendConfiguration configuration;

    private ConfigurationService configService;

    private TypedDatumSerializer typedDatumSerializer;

    private volatile DataService dataService; // made volatile to ensure thread visibility; other approaches possible

    private final ThreadLocal<PooledConnection> connections = new ThreadLocal<PooledConnection>();

    private final AsyncOrderedExecutionQueue executionQueue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED,
        SharedThreadPool.getInstance());

    protected void activate(BundleContext context) throws IOException {
        File storageRootDir = configService.getConfigurablePath(ConfigurablePathId.PROFILE_DATA_MANAGEMENT);
        File metaDataDirectory = new File(storageRootDir, METADATA_DB_NAME);
        System.setProperty("derby.stream.error.file", new File(storageRootDir, "derby.log").getAbsolutePath());
        System.setProperty("derby.locks.waitTimeout", "30");
        System.setProperty("derby.locks.deadlockTimeout", "20");
        System.setProperty("derby.system.bootAll", TRUE);
        System.setProperty("derby.storage.pageCacheSize", "20000");
        System.setProperty("derby.storage.rowLocking", TRUE);
        System.setProperty("derby.locks.escalationThreshold", "500000");
        // Properties set for debugging
        System.setProperty("derby.language.logQueryPlan", FALSE);
        System.setProperty("derby.locks.monitor", FALSE);
        System.setProperty("derby.locks.deadlockTrace", FALSE);
        // note: disabled old configuration loading for 6.0.0 as it is not being used anyway
        // configuration = configService.getConfiguration(context.getBundle().getSymbolicName(), DerbyMetaDataBackendConfiguration.class);
        // TODO using default values until reworked or removed
        configuration = new DerbyMetaDataBackendConfiguration();
        if (configuration.getDatabaseURL().equals("")) {
            configuration.setDatabaseUrl(metaDataDirectory.getAbsolutePath()); // despite the name, does not actually accept file URIs
            LOGGER.debug("Initializing Derby meta data backend in " + metaDataDirectory);
        } else {
            // note: the "else" path was not doing anything before; at least log if this happens
            LOGGER.warn("Unexpected state: Database URL already defined");
        }
        SharedThreadPool.getInstance().execute(new Runnable() {

            @Override
            @TaskDescription(value = "Database initialization")
            public void run() {
                initialize();
            }
        });
    }

    protected void deactivate() {
        try {
            if (!initializationLatch.await(INITIALIZATION_TIMEOUT, TimeUnit.SECONDS)) {
                LOGGER.error(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE, e);
        }
        shutDown();
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        configService = newConfigurationService;
    }

    protected void bindTypedDatumService(TypedDatumService newService) {
        typedDatumSerializer = newService.getSerializer();
    }

    protected void bindDataService(DataService newService) {
        dataService = newService;
    }

    protected Connection getConnection() {
        PooledConnection result = connections.get();
        try {
            if (result != null && result.isClosed()) {
                result = null;
            }
        } catch (SQLException e) {
            result = null;
        }
        if (result == null) {
            try {
                final Connection connection = connectionPool.getConnection();
                final PooledConnectionInvocationHandler handler = new PooledConnectionInvocationHandler(connection);
                final PooledConnection pooledConnection = (PooledConnection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { PooledConnection.class }, handler);
                connections.set(pooledConnection);
                result = pooledConnection;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to retrieve connection from connection pool:", e);
            } catch (NullPointerException e) {
                LOGGER.warn("Unable to get database connection. Connection pool already shut down.");
            }
        }
        if (result != null) {
            result.increment();
        }
        return result;
    }

    /**
     * Template for safe executions.
     * 
     * @author Christian Weiss
     * @author Jan Flink
     * @author Robert Mischke
     */
    protected abstract class SafeExecution<T> implements Callable<T> {

        @Override
        public final T call() {
            T result = null;
            try {
                if (!initializationLatch.await(INITIALIZATION_TIMEOUT, TimeUnit.SECONDS)) {
                    LOGGER.error(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                    throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE, e);
            }
            final Connection connection = getConnection();
            if (connection == null) {
                throw new RuntimeException("Failed to get database connection.");
            }
            try {
                connection.setAutoCommit(false);
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                int count = 0;
                while (true) {
                    try {
                        result = protectedCall(connection, false);
                        break;
                    } catch (SQLTransientException e) {
                        // from spec: The subclass of SQLException is thrown in situations where a previoulsy failed operation might
                        // be able to succeed when the operation is retried without any intervention by application-level functionality.
                        if (count == 0) {
                            LOGGER.debug(String.format("Executing database statement failed (%s). Will retry.", e.getMessage()));
                        }
                        waitForRetry();
                    }
                    count++;
                    if (count >= MAX_RETRIES) {
                        throw new RuntimeException(String.format("Failed to commit database transaction after %d retries.", MAX_RETRIES));
                    }
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    LOGGER.error("Failed to rollback database transaction", e1);
                }
                throw new RuntimeException("Failed to safely execute:", e);
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.error("Failed to close database connection", e);
                }
            }
        }

        protected abstract T protectedCall(Connection connection, boolean isRetry) throws SQLException;
    }

    @Override
    public Long addWorkflowRun(final String workflowTitle, final String workflowControllerNodeId,
        final String workflowDataManagementNodeId, final Long starttime) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long wfRunId = addWorkflowRun(workflowTitle, workflowControllerNodeId, workflowDataManagementNodeId,
                    connection, isRetry);
                addTimelineInterval(wfRunId, TimelineIntervalType.WORKFLOW_RUN, starttime, null, connection, isRetry);
                return wfRunId;
            }
        };
        return execution.call();
    }

    private Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId,
        String workflowDataManagementNodeId, Connection connection, Boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_WORKFLOW_RUN + "("
            + NAME + COMMA + CONTROLLER_NODE_ID + COMMA + DATAMANAGEMENT_NODE_ID + COMMA + TO_BE_DELETED + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, workflowTitle);
        stmt.setString(2, workflowControllerNodeId);
        stmt.setString(3, workflowDataManagementNodeId);
        stmt.setInt(4, NOT_MARKED_TO_BE_DELETED);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    private void addProperties(final String propertiesTableName, final Long relatedId, final Map<String, String> properties) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                addProperties(propertiesTableName, relatedId, properties, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void addProperties(String propertiesTableName, Long relatedId, Map<String, String> properties, Connection connection,
        boolean isRetry) throws SQLException {
        String sql = String.format(INSERT_INTO + DB_PREFIX + STRING_PLACEHOLDER + "("
            + WORKFLOW_RUN_ID + COMMA + KEY + COMMA + VALUE + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES, propertiesTableName);
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (String key : properties.keySet()) {
            stmt.setLong(1, relatedId);
            stmt.setString(2, key);
            stmt.setString(3, properties.get(key));
            stmt.execute();
        }
        stmt.close();
    }

    @Override
    public Map<String, Long> addComponentInstances(final Long workflowRunId, final Collection<ComponentInstance> componentInstances) {
        final SafeExecution<Map<String, Long>> execution = new SafeExecution<Map<String, Long>>() {

            @Override
            protected Map<String, Long> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return addComponentInstances(workflowRunId, componentInstances, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances,
        Connection connection, boolean isRetry) throws SQLException {
        Map<String, Long> result = new HashMap<String, Long>();
        String sql = INSERT_INTO + DB_PREFIX + TABLE_COMPONENT_INSTANCE + "("
            + WORKFLOW_RUN_ID + COMMA + COMPONENT_ID + COMMA + COMPONENT_INSTANCE_NAME + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs;
        for (ComponentInstance ci : componentInstances) {
            stmt.setLong(1, workflowRunId);
            stmt.setString(2, ci.getComponentID());
            stmt.setString(3, ci.getComponentInstanceName());
            stmt.execute();
            rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                result.put(ci.getComponentExecutionID(), rs.getLong(1));
                rs.close();
            }
        }
        stmt.close();
        return result;
    }

    @Override
    @AllowRemoteAccess
    public Long addComponentRun(final Long componentInstanceId, final String nodeId, final Integer count,
        final Long starttime) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long cRunId = addComponentRun(componentInstanceId, nodeId, count, starttime, connection, isRetry);
                Long wfRunId = getWorkflowRunIdByComponentInstanceId(componentInstanceId, connection, isRetry);
                addTimelineInterval(wfRunId, TimelineIntervalType.COMPONENT_RUN, starttime, cRunId, connection, isRetry);
                return cRunId;
            }
        };
        return execution.call();
    }

    private Long getWorkflowRunIdByComponentInstanceId(Long componentInstanceId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + INNER_JOIN + TABLE_COMPONENT_INSTANCE + ON + TABLE_COMPONENT_INSTANCE + DOT
                + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, componentInstanceId);
        ResultSet rs = stmt.executeQuery();
        Long id = null;
        if (rs != null && rs.next()) {
            id = rs.getLong(WORKFLOW_RUN_ID);
            rs.close();
        }
        stmt.close();
        return id;
    }

    private Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime, Connection connection,
        boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_COMPONENT_RUN + "("
            + COMPONENT_INSTANCE_ID + COMMA + NODE_ID + COMMA + COUNTER + COMMA + REFERENCES_DELETED + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, componentInstanceId);
        stmt.setString(2, nodeId);
        stmt.setInt(3, count);
        stmt.setBoolean(4, false);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    @Override
    @AllowRemoteAccess
    public void setOrUpdateHistoryDataItem(final Long componentRunId, final String historyDataItem) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setOrUpdateHistoryDataItem(componentRunId, historyDataItem, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_RUN + SET + HISTORY_DATA_ITEM + EQUAL + QMARK
            + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, historyDataItem);
        stmt.setLong(2, componentRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    public void setOrUpdateTimelineDataItem(final Long workflowRunId, final String timelinDataItem) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setOrUpdateTimelineDataItem(workflowRunId, timelinDataItem, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setOrUpdateTimelineDataItem(Long workflowRunId, String timelinDataItem, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + TIMELINE_DATA_ITEM + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, timelinDataItem);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    public void setWorkflowRunFinished(final Long workflowRunId, final Long endtime, final FinalWorkflowState finalState) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setWorklfowRunEndtime(workflowRunId, endtime, connection, isRetry);
                setWorkflowRunFinalState(workflowRunId, finalState, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setWorklfowRunEndtime(Long workflowRunId, Long endtime, Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET + ENDTIME + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, workflowRunId);
        stmt.setString(3, TimelineIntervalType.WORKFLOW_RUN.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    private void setWorkflowRunFinalState(Long workflowRunId, FinalWorkflowState finalState, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + FINAL_STATE + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, finalState.toString());
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    @AllowRemoteAccess
    public void setComponentRunFinished(final Long componentRunId, final Long endtime) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setComponentRunFinished(componentRunId, endtime, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setComponentRunFinished(Long componentRunId, Long endtime, Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET + ENDTIME + EQUAL + QMARK
            + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, componentRunId);
        stmt.setString(3, TimelineIntervalType.COMPONENT_RUN.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    @AllowRemoteAccess
    public void setComponentInstanceFinalState(final Long componentInstanceId, final FinalComponentState finalState) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setComponentInstanceFinalState(componentInstanceId, finalState, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState, Connection connection,
        boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_INSTANCE + SET + FINAL_STATE + EQUAL + QMARK
            + WHERE + COMPONENT_INSTANCE_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, finalState.toString());
        stmt.setLong(2, componentInstanceId);
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    @AllowRemoteAccess
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() {
        final SafeExecution<Set<WorkflowRunDescription>> execution = new SafeExecution<Set<WorkflowRunDescription>>() {

            @Override
            protected Set<WorkflowRunDescription> protectedCall(final Connection connection, final boolean isRetry)
                throws SQLException {
                connection.setReadOnly(true);
                return getWorkflowRunDescriptions(connection, isRetry);
            }
        };
        return execution.call();
    }

    private Map<Long, Integer> getDataReferenceCounts(Connection connection, boolean isRetry) throws SQLException {
        String sql =
            SELECT + " COUNT(*) AS " + COUNTER + COMMA + WORKFLOW_RUN_ID + FROM + VIEW_WORKFLOWRUN_DATAREFERENCE + GROUP_BY
                + WORKFLOW_RUN_ID;
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        Map<Long, Integer> counts = new HashMap<Long, Integer>();
        if (rs != null) {
            while (rs.next()) {
                counts.put(rs.getLong(WORKFLOW_RUN_ID), rs.getInt(COUNTER));
            }
            rs.close();
        }
        stmt.close();
        return counts;
    }

    private Set<WorkflowRunDescription> getWorkflowRunDescriptions(Connection connection, boolean isRetry) throws SQLException {
        Map<Long, Integer> counts = getDataReferenceCounts(connection, isRetry);
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + NAME + COMMA
                + TABLE_WORKFLOW_RUN + DOT + CONTROLLER_NODE_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + DATAMANAGEMENT_NODE_ID + COMMA
                + TABLE_WORKFLOW_RUN + DOT + TO_BE_DELETED + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA + TABLE_WORKFLOW_RUN
                + DOT + FINAL_STATE + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + INNER_JOIN + DB_PREFIX + TABLE_TIMELINE_INTERVAL + ON
                + TABLE_TIMELINE_INTERVAL + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_TIMELINE_INTERVAL + DOT + TYPE + EQUAL + QMARK + AND + TO_BE_DELETED + NOT_EQUAL + QMARK
                + ORDER_BY + STARTTIME + DESCENDING;
        Set<WorkflowRunDescription> results = new HashSet<WorkflowRunDescription>();
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, TimelineIntervalType.WORKFLOW_RUN.toString());
        stmt.setInt(2, WORKFLOW_RUN_TO_BE_DELETED);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                FinalWorkflowState finalState = null;
                if (rs.getString(FINAL_STATE) != null) {
                    finalState = FinalWorkflowState.valueOf(rs.getString(FINAL_STATE));
                }
                Long wfRunId = rs.getLong(WORKFLOW_RUN_ID);
                boolean markedForDeletion = rs.getInt(TO_BE_DELETED) != NOT_MARKED_TO_BE_DELETED;
                Boolean hasDataReferences = counts.get(wfRunId) != null && counts.get(wfRunId) > 0;
                results.add(new WorkflowRunDescription(wfRunId, rs.getString(NAME), rs.getString(CONTROLLER_NODE_ID)
                    .trim(), rs.getString(DATAMANAGEMENT_NODE_ID).trim(), rs.getTimestamp(STARTTIME).getTime(), endtime, finalState,
                    hasDataReferences, markedForDeletion));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRun getWorkflowRun(final Long workflowRunId) {
        final SafeExecution<WorkflowRun> execution = new SafeExecution<WorkflowRun>() {

            @Override
            protected WorkflowRun protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return getWorkflowRun(workflowRunId, connection, isRetry);
            }
        };
        return execution.call();
    }

    private WorkflowRun getWorkflowRun(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + NAME + COMMA
                + TABLE_WORKFLOW_RUN + DOT + CONTROLLER_NODE_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + DATAMANAGEMENT_NODE_ID + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA + TABLE_WORKFLOW_RUN
                + DOT + FINAL_STATE + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + INNER_JOIN + DB_PREFIX + TABLE_TIMELINE_INTERVAL + ON
                + TABLE_TIMELINE_INTERVAL + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + EQUAL + QMARK + AND + TABLE_TIMELINE_INTERVAL + DOT + TYPE + EQUAL
                + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, TimelineIntervalType.WORKFLOW_RUN.toString());
        WorkflowRun workflowRun = null;
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            // end time might be NULL, avoid NPE
            Long endtime = null;
            if (rs.getTimestamp(ENDTIME) != null) {
                endtime = rs.getTimestamp(ENDTIME).getTime();
            }
            // final state might be NULL, avoid NPE
            FinalWorkflowState finalState = null;
            if (rs.getString(FINAL_STATE) != null) {
                finalState = FinalWorkflowState.valueOf(rs.getString(FINAL_STATE));
            }
            Long wfRunId = rs.getLong(WORKFLOW_RUN_ID);
            workflowRun =
                new WorkflowRun(wfRunId, rs.getString(NAME), rs.getString(CONTROLLER_NODE_ID).trim(),
                    rs.getString(DATAMANAGEMENT_NODE_ID).trim(), rs.getTimestamp(STARTTIME).getTime(),
                    endtime, finalState, null, null);
            rs.close();
        }
        stmt.close();
        Map<Long, Set<EndpointData>> endpointData = new HashMap<Long, Set<EndpointData>>();
        String sqlEndpointData =
            SELECT_ALL + FROM + DB_PREFIX + VIEW_ENDPOINT_DATA
                + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtEndpointData =
            connection.prepareStatement(sqlEndpointData);
        stmtEndpointData.setLong(1, workflowRunId);
        ResultSet rsEndpointData = stmtEndpointData.executeQuery();
        if (rsEndpointData != null) {
            while (rsEndpointData.next()) {
                String value = rsEndpointData.getString(VALUE);
                if (value == null) {
                    value = rsEndpointData.getString(BIG_VALUE);
                }
                Long id = rsEndpointData.getLong(COMPONENT_RUN_ID);
                if (endpointData.get(id) == null) {
                    endpointData.put(id, new HashSet<EndpointData>());
                }
                endpointData.get(id).add(
                    new EndpointData(new EndpointInstance(rsEndpointData.getString(NAME), EndpointType
                        .valueOf(rsEndpointData
                            .getString("ENDPOINT_TYPE"))),
                        rsEndpointData.getInt(COUNTER), value));
            }
            rsEndpointData.close();
        }
        stmtEndpointData.close();
        String sqlComponentRuns = SELECT_ALL + FROM + DB_PREFIX + VIEW_COMPONENT_RUNS + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtComponentRuns =
            connection.prepareStatement(sqlComponentRuns, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmtComponentRuns.setLong(1, workflowRunId);
        ResultSet rsComponentRuns = stmtComponentRuns.executeQuery();
        if (rsComponentRuns != null) {
            while (rsComponentRuns.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rsComponentRuns.getTimestamp(ENDTIME) != null) {
                    endtime = rsComponentRuns.getTimestamp(ENDTIME).getTime();
                }
                ComponentInstance ci =
                    new ComponentInstance(rsComponentRuns.getString(COMPONENT_ID), rsComponentRuns.getString(COMPONENT_INSTANCE_NAME),
                        rsComponentRuns.getString(FINAL_STATE));
                Long crId = rsComponentRuns.getLong(COMPONENT_RUN_ID);
                ComponentRun cr =
                    new ComponentRun(rsComponentRuns.getLong(COMPONENT_RUN_ID), rsComponentRuns.getString(NODE_ID).trim(),
                        rsComponentRuns.getInt(COUNTER), rsComponentRuns.getTimestamp(
                            STARTTIME).getTime(), endtime,
                        rsComponentRuns.getString(HISTORY_DATA_ITEM), rsComponentRuns.getBoolean(REFERENCES_DELETED));

                cr.setEndpointData(endpointData.get(crId));
                workflowRun.addComponentRun(ci, cr);
            }
            rsComponentRuns.close();
        }
        stmtComponentRuns.close();
        return workflowRun;
    }

    @Override
    public Collection<ComponentRun> getComponentRuns(final Long componentInstanceId) {
        final SafeExecution<Collection<ComponentRun>> execution = new SafeExecution<Collection<ComponentRun>>() {

            @Override
            protected Collection<ComponentRun> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return getComponentRuns(componentInstanceId, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Collection<ComponentRun> getComponentRuns(Long componentInstanceId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = SELECT + COMPONENT_RUN_ID + COMMA + COMPONENT_INSTANCE_ID + COMMA + NODE_ID + COMMA
            + COUNTER + COMMA + STARTTIME + COMMA + ENDTIME + COMMA + HISTORY_DATA_ITEM + COMMA + REFERENCES_DELETED
            + FROM + DB_PREFIX + TABLE_COMPONENT_RUN + WHERE + COMPONENT_INSTANCE_ID + EQUAL + QMARK + ORDER_BY + STARTTIME + DESCENDING;
        Collection<ComponentRun> results = new TreeSet<ComponentRun>();
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, componentInstanceId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                results.add(new ComponentRun(rs.getLong(COMPONENT_RUN_ID), rs.getLong(COMPONENT_INSTANCE_ID), rs.getString(NODE_ID).trim(),
                    rs.getInt(COUNTER), rs.getTimestamp(STARTTIME).getTime(),
                    endtime, rs.getString(HISTORY_DATA_ITEM), rs.getBoolean(REFERENCES_DELETED)));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    @Override
    public Map<String, Long> addEndpointInstances(final Long componentInstanceId,
        final Collection<EndpointInstance> endpointInstances) {
        final SafeExecution<Map<String, Long>> execution = new SafeExecution<Map<String, Long>>() {

            @Override
            protected Map<String, Long> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return addEndpointInstances(componentInstanceId, endpointInstances, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances,
        Connection connection,
        boolean isRetry) throws SQLException {
        Map<String, Long> result = new HashMap<String, Long>();
        String sql = INSERT_INTO + DB_PREFIX + TABLE_ENDPOINT_INSTANCE + "("
            + COMPONENT_INSTANCE_ID + COMMA + NAME + COMMA + TYPE + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs;
        for (EndpointInstance ei : endpointInstances) {
            stmt.setLong(1, componentInstanceId);
            stmt.setString(2, ei.getEndpointName());
            stmt.setString(3, ei.getEndpointType().name());
            stmt.execute();
            rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                result.put(ei.getEndpointName(), rs.getLong(1));
            }
            if (rs != null) {
                rs.close();
            }
        }
        stmt.close();
        return result;
    }

    @Override
    @AllowRemoteAccess
    public void addInputDatum(final Long componentRunId, final Long typedDatumId, final Long endpointInstanceId,
        final Integer count) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                addEndpointDatum(componentRunId, typedDatumId, endpointInstanceId, count, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public Long addOutputDatum(final Long componentRunId, final Long endpointInstanceId, final String datum, final Integer count) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long typedDatumId = addTypedDatum(typedDatumSerializer.deserialize(datum), connection, isRetry);
                return addEndpointDatum(componentRunId, typedDatumId, endpointInstanceId, count, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Long addEndpointDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count, Connection connection,
        boolean isRetry) throws SQLException {
        String endpointSql = INSERT_INTO + DB_PREFIX + TABLE_ENDPOINT_DATA + "("
            + COMPONENT_RUN_ID + COMMA + TYPED_DATUM_ID + COMMA + ENDPOINT_INSTANCE_ID + COMMA + COUNTER + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(endpointSql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, componentRunId);
        stmt.setLong(2, typedDatumId);
        stmt.setLong(3, endpointInstanceId);
        stmt.setInt(4, count);
        stmt.executeUpdate();
        Long endpointDataId = getGeneratedKey(stmt);
        stmt.close();
        return endpointDataId;
    }

    private Long addTypedDatum(TypedDatum datum, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_TYPED_DATUM + "("
            + TYPE + COMMA + STRING_PLACEHOLDER + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        String value = typedDatumSerializer.serialize(datum);
        String valueColumn;
        if (value.length() <= MAX_VALUE_LENGTH) {
            valueColumn = VALUE;
        } else {
            valueColumn = BIG_VALUE;
        }
        PreparedStatement stmt = connection.prepareStatement(String.format(sql, valueColumn), Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, datum.getDataType().getShortName());
        stmt.setString(2, value);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    @Override
    public Long addTimelineInterval(final Long workflowRunId, final TimelineIntervalType intervalType, final long starttime,
        final Long relatedComponentId) {
        if (intervalType.equals(TimelineIntervalType.WORKFLOW_RUN)
            || intervalType.equals(TimelineIntervalType.COMPONENT_RUN)) {
            throw new IllegalArgumentException("Called using internal TimelineIntervalType:" + intervalType.name());
        }
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime,
        Long relatedComponentId, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_TIMELINE_INTERVAL + "("
            + WORKFLOW_RUN_ID + COMMA + TYPE + COMMA + STARTTIME + COMMA + COMPONENT_RUN_ID + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, intervalType.toString());
        stmt.setTimestamp(3, new Timestamp(starttime));
        if (relatedComponentId != null) {
            stmt.setLong(4, relatedComponentId);
        } else {
            stmt.setNull(4, java.sql.Types.BIGINT);
        }
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    private Long getGeneratedKey(PreparedStatement stmt) throws SQLException {
        ResultSet rs = stmt.getGeneratedKeys();
        Long id = null;
        if (rs != null && rs.next()) {
            id = rs.getLong(1);
            rs.close();
        }
        return id;
    }

    @Override
    public void setTimelineIntervalFinished(final Long timelineIntervalId, final long endtime) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                setTimelineIntervalFinished(timelineIntervalId, endtime, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void setTimelineIntervalFinished(Long timelineIntervalId, long endtime, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET
            + ENDTIME + EQUAL + QMARK
            + WHERE + TIMELINE_INTERVAL_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, timelineIntervalId);
        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRunTimline getWorkflowTimeline(final Long workflowRunId) {
        final SafeExecution<WorkflowRunTimline> execution = new SafeExecution<WorkflowRunTimline>() {

            @Override
            protected WorkflowRunTimline protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                String workflowRunName = getWorkflowRunName(workflowRunId, connection, isRetry);
                TimelineInterval workflowRunInterval = getWorkflowInterval(workflowRunId, connection, isRetry);
                List<ComponentRunInterval> componentRunIntervals = getComponentRunIntervals(workflowRunId, connection, isRetry);
                return new WorkflowRunTimline(workflowRunName, workflowRunInterval, componentRunIntervals);
            }
        };
        return execution.call();
    }

    private List<ComponentRunInterval> getComponentRunIntervals(Long workflowRunId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql =
            SELECT + STARTTIME + COMMA + ENDTIME + COMMA + TYPE + COMMA + COMPONENT_ID + COMMA + COMPONENT_INSTANCE_NAME
                + FROM + DB_PREFIX + VIEW_COMPONENT_TIMELINE_INTERVALS
                + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        List<ComponentRunInterval> results = new ArrayList<ComponentRunInterval>();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                results.add(new ComponentRunInterval(rs.getString(COMPONENT_ID), rs.getString(COMPONENT_INSTANCE_NAME),
                    TimelineIntervalType.valueOf(rs.getString(TYPE)), rs.getTimestamp(STARTTIME).getTime(), endtime));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    private TimelineInterval getWorkflowInterval(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + STARTTIME + COMMA + ENDTIME + FROM + DB_PREFIX + TABLE_TIMELINE_INTERVAL
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK
            + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, TimelineIntervalType.WORKFLOW_RUN.toString());
        ResultSet rs = stmt.executeQuery();
        TimelineInterval ti = null;
        if (rs != null && rs.next()) {
            // end time might be NULL, avoid NPE
            Long endtime = null;
            if (rs.getTimestamp(ENDTIME) != null) {
                endtime = rs.getTimestamp(ENDTIME).getTime();
            }
            ti = new TimelineInterval(TimelineIntervalType.WORKFLOW_RUN, rs.getTimestamp(STARTTIME).getTime(), endtime);
        }
        if (rs != null) {
            rs.close();
        }
        stmt.close();
        return ti;
    }

    private String getWorkflowRunName(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + NAME + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            String name = rs.getString(NAME);
            rs.close();
            stmt.close();
            return name;
        }
        stmt.close();
        return null;
    }

    private Map<String, String> getProperties(final String tableName, final Long relatedId) {
        final SafeExecution<Map<String, String>> execution = new SafeExecution<Map<String, String>>() {

            @Override
            protected Map<String, String> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return getProperties(tableName, relatedId, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Map<String, String> getProperties(String tableName, Long relatedId, Connection connection, boolean isRetry)
        throws SQLException {
        String relatedIdColumn;
        switch (tableName) {
        case TABLE_COMPONENT_RUN_PROPERTIES:
            relatedIdColumn = COMPONENT_RUN_ID;
            break;
        case TABLE_WORKFLOW_RUN_PROPERTIES:
            relatedIdColumn = WORKFLOW_RUN_ID;
            break;
        default:
            relatedIdColumn = null;
        }
        String sql = String.format(SELECT + KEY + COMMA + VALUE
            + FROM + DB_PREFIX + STRING_PLACEHOLDER + WHERE + STRING_PLACEHOLDER + EQUAL + QMARK, tableName, relatedIdColumn);
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, relatedId);
        Map<String, String> results = new HashMap<String, String>();
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                results.put(rs.getString(KEY), rs.getString(VALUE));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRunFiles(final Long workflowRunId) {
        final SafeExecution<Boolean> execution = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                if (isWorkflowFinished(workflowRunId, connection, isRetry)) {
                    markDeletion(workflowRunId, FILES_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(String.format("Marked workflow run id %d to delete files.", workflowRunId));
                    return true;
                }
                LOGGER.warn("Workflow files deletion requested, but workflow " + workflowRunId + " is not finished yet");
                return false;
            }
        };
        Boolean markedDeletion = execution.call();
        if (markedDeletion) {
            executionQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    deleteWorkflowRunFilesInternal(workflowRunId);
                }
            });
        }
        return markedDeletion;
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRun(final Long workflowRunId) {
        final SafeExecution<Boolean> execution = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                if (isWorkflowFinished(workflowRunId, connection, isRetry)) {
                    markDeletion(workflowRunId, WORKFLOW_RUN_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(String.format("Marked workflow run id %d to be deleted.", workflowRunId));
                    return true;
                }
                LOGGER.warn("Workflow run deletion requested, but workflow " + workflowRunId + " is not finished yet");
                return false;
            }
        };
        Boolean markedDeletion = execution.call();
        if (markedDeletion) {
            executionQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    deleteWorkflowRunInternal(workflowRunId);
                }
            });
        }
        return markedDeletion;
    }

    private void deleteWorkflowRunInternal(final Long workflowRunId) {
        final SafeExecution<Map<Long, Set<String>>> execution = new SafeExecution<Map<Long, Set<String>>>() {

            @Override
            protected Map<Long, Set<String>> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                LOGGER.debug(String.format("Starting to delete workflow run id %d.", workflowRunId));
                Map<Long, Set<String>> keys = getDataReferenceBinaryKeys(workflowRunId, connection, isRetry);
                return keys;
            }
        };
        final Map<Long, Set<String>> dataKeys = execution.call();
        if (dataKeys != null) {
            deleteFiles(dataKeys.values());
        }
        final SafeExecution<Boolean> execution2 = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Boolean deleted = deleteDataReferences(dataKeys, connection, isRetry);
                return deleted;
            }
        };
        if (execution2.call()) {
            final SafeExecution<Void> execution3 = new SafeExecution<Void>() {

                @Override
                protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                    deleteTypedDatums(workflowRunId, connection, isRetry);
                    deleteWorkflowRunContent(workflowRunId, connection, isRetry);
                    LOGGER.debug(String.format("Finished deletion of workflow run id %d.", workflowRunId));
                    return null;
                }
            };
            execution3.call();
        }
    }

    private void deleteWorkflowRunFilesInternal(final Long workflowRunId) {
        final SafeExecution<Map<Long, Set<String>>> execution = new SafeExecution<Map<Long, Set<String>>>() {

            @Override
            protected Map<Long, Set<String>> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                LOGGER.debug(String.format("Starting to delete files of workflow run id %d.", workflowRunId));
                Map<Long, Set<String>> keys = getDataReferenceBinaryKeys(workflowRunId, connection, isRetry);
                return keys;
            }
        };
        final Map<Long, Set<String>> dataKeys = execution.call();
        if (dataKeys != null) {
            deleteFiles(dataKeys.values());
        }
        final SafeExecution<Boolean> execution2 = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return deleteDataReferences(dataKeys, connection, isRetry);
            }
        };
        if (execution2.call()) {
            final SafeExecution<Void> execution3 = new SafeExecution<Void>() {

                @Override
                protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                    markDataReferencesDeleted(workflowRunId, connection, isRetry);
                    markDeletion(workflowRunId, NOT_MARKED_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(String.format("Finished file deletion of workflow run id %d.",
                        workflowRunId));
                    return null;
                }
            };
            execution3.call();
        }
    }

    private void markDeletion(Long workflowRunId, Integer type, Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + TO_BE_DELETED + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, type);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    private void markDataReferencesDeleted(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sqlComponentRunIds =
            SELECT + COMPONENT_RUN_ID + FROM + VIEW_WORKFLOWRUN_COMPONENTRUN + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtComponentRunIds = connection.prepareStatement(sqlComponentRunIds);
        stmtComponentRunIds.setLong(1, workflowRunId);
        ResultSet rs = stmtComponentRunIds.executeQuery();
        List<Long> crIds = new ArrayList<Long>();
        if (rs != null) {
            while (rs.next()) {
                crIds.add(rs.getLong(COMPONENT_RUN_ID));
            }
            rs.close();
        }
        stmtComponentRunIds.close();
        if (crIds.size() > 0) {
            String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_RUN + SET + REFERENCES_DELETED + EQUAL + QMARK
                + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK;
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (Long id : crIds) {
                stmt.setBoolean(1, true);
                stmt.setLong(2, id);
                stmt.executeUpdate();
            }
            stmt.close();
        }
    }

    private boolean isWorkflowFinished(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + FINAL_STATE + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            Boolean finished = rs.getString(FINAL_STATE) != null;
            rs.close();
            stmt.close();
            return finished;
        }
        stmt.close();
        return false;
    }

    private void deleteFiles(Collection<Set<String>> binaryKeys) {
        for (final Set<String> keySet : binaryKeys) {
            for (final String key : keySet) {
                dataService.deleteReference(key);
            }
        }
    }

    private void deleteTypedDatums(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + TYPED_DATUM_ID + FROM + DB_PREFIX + VIEW_WORKFLOWRUN_TYPEDDATUM
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        Set<Long> typedDatumIds = new HashSet<Long>();
        if (rs != null) {
            while (rs.next()) {
                typedDatumIds.add(rs.getLong(TYPED_DATUM_ID));
            }
            rs.close();
        }
        stmt.close();
        String sqlEndpointData = DELETE_FROM + DB_PREFIX + TABLE_ENDPOINT_DATA + WHERE + TYPED_DATUM_ID + EQUAL + QMARK;
        String sqlTypedDatum = DELETE_FROM + DB_PREFIX + TABLE_TYPED_DATUM + WHERE + TYPED_DATUM_ID + EQUAL + QMARK;

        PreparedStatement stmtEndpointData = connection.prepareStatement(sqlEndpointData);
        PreparedStatement stmtTypedDatum = connection.prepareStatement(sqlTypedDatum);
        for (Long id : typedDatumIds) {
            stmtEndpointData.setLong(1, id);
            stmtTypedDatum.setLong(1, id);
            stmtEndpointData.execute();
            stmtTypedDatum.execute();
        }
        stmtEndpointData.close();
        stmtTypedDatum.close();
    }

    private Boolean deleteDataReferences(Map<Long, Set<String>> dataReferenceKeys,
        Connection connection, boolean isRetry)
        throws SQLException {
        if (dataReferenceKeys.isEmpty()) {
            return true;
        }
        String sqlRelBinaryDataRef =
            DELETE_FROM + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelCompRunDataRef =
            DELETE_FROM + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelCompInstanceDataRef =
            DELETE_FROM + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelWorkflowRunDataRef =
            DELETE_FROM + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlBinaryRef =
            DELETE_FROM + DB_PREFIX + TABLE_BINARY_REFERENCE + WHERE + BINARY_REFERENCE_KEY + EQUAL + QMARK;
        String sqlDataRef = DELETE_FROM + DB_PREFIX + TABLE_DATA_REFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        PreparedStatement stmtRelBinaryDataRef = connection.prepareStatement(sqlRelBinaryDataRef);
        PreparedStatement stmtRelCompRunDataRef = connection.prepareStatement(sqlRelCompRunDataRef);
        PreparedStatement stmtRelCompInstanceDataRef = connection.prepareStatement(sqlRelCompInstanceDataRef);
        PreparedStatement stmtRelWorkflowRunDataRef = connection.prepareStatement(sqlRelWorkflowRunDataRef);
        PreparedStatement stmtBinaryRef = connection.prepareStatement(sqlBinaryRef);
        PreparedStatement stmtDataRef = connection.prepareStatement(sqlDataRef);
        for (Long id : dataReferenceKeys.keySet()) {
            stmtRelBinaryDataRef.setLong(1, id);
            stmtRelBinaryDataRef.execute();
            stmtRelCompRunDataRef.setLong(1, id);
            stmtRelCompRunDataRef.execute();
            stmtRelCompInstanceDataRef.setLong(1, id);
            stmtRelCompInstanceDataRef.execute();
            stmtRelWorkflowRunDataRef.setLong(1, id);
            stmtRelWorkflowRunDataRef.execute();
            for (String key : dataReferenceKeys.get(id)) {
                stmtBinaryRef.setString(1, key);
                stmtBinaryRef.execute();
            }
            stmtDataRef.setLong(1, id);
            stmtDataRef.execute();
        }
        stmtRelBinaryDataRef.close();
        stmtRelCompRunDataRef.close();
        stmtRelCompInstanceDataRef.close();
        stmtRelWorkflowRunDataRef.close();
        stmtBinaryRef.close();
        stmtDataRef.close();
        return true;
    }

    private Boolean deleteWorkflowRunContent(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {

        String sqlTimelineInt = DELETE_FROM + DB_PREFIX + TABLE_TIMELINE_INTERVAL + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtTimelineInt = connection.prepareStatement(sqlTimelineInt);
        stmtTimelineInt.setLong(1, workflowRunId);
        stmtTimelineInt.execute();
        stmtTimelineInt.close();
        String sqlCompInstIds =
            SELECT + COMPONENT_INSTANCE_ID + FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        String sqlCompRunIds =
            SELECT + TABLE_COMPONENT_RUN + DOT + COMPONENT_RUN_ID + FROM + DB_PREFIX + TABLE_COMPONENT_RUN + COMMA + DB_PREFIX
                + TABLE_COMPONENT_INSTANCE + WHERE + TABLE_COMPONENT_RUN + DOT + COMPONENT_INSTANCE_ID + EQUAL
                + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID + AND + TABLE_COMPONENT_INSTANCE + DOT + WORKFLOW_RUN_ID
                + EQUAL + QMARK;

        String sqlcompRunProp =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_RUN_PROPERTIES + WHERE + COMPONENT_RUN_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompRunProp =
            connection.prepareStatement(String.format(sqlcompRunProp, String.format(sqlCompRunIds, sqlCompInstIds)));
        stmtCompRunProp.setLong(1, workflowRunId);
        stmtCompRunProp.execute();
        stmtCompRunProp.close();
        String sqlcompInstProp =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE_PROPERTIES + WHERE + COMPONENT_INSTANCE_ID + IN
                + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompInstProp = connection.prepareStatement(String.format(sqlcompInstProp, sqlCompInstIds));
        stmtCompInstProp.setLong(1, workflowRunId);
        stmtCompInstProp.execute();
        stmtCompInstProp.close();
        String sqlEndpointInst =
            DELETE_FROM + DB_PREFIX + TABLE_ENDPOINT_INSTANCE + WHERE + COMPONENT_INSTANCE_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtEndpointInst = connection.prepareStatement(String.format(sqlEndpointInst, sqlCompInstIds));
        stmtEndpointInst.setLong(1, workflowRunId);
        stmtEndpointInst.execute();
        stmtEndpointInst.close();
        String sqlCompRun =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_RUN + WHERE + COMPONENT_INSTANCE_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompRun = connection.prepareStatement(String.format(sqlCompRun, sqlCompInstIds));
        stmtCompRun.setLong(1, workflowRunId);
        stmtCompRun.execute();
        stmtCompRun.close();
        String sqlCompInst = DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtCompInst = connection.prepareStatement(sqlCompInst);
        stmtCompInst.setLong(1, workflowRunId);
        stmtCompInst.execute();
        stmtCompInst.close();
        String sqlWorkflowRunProp =
            DELETE_FROM + DB_PREFIX + TABLE_WORKFLOW_RUN_PROPERTIES + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtWorkflowRunProp = connection.prepareStatement(sqlWorkflowRunProp);
        stmtWorkflowRunProp.setLong(1, workflowRunId);
        stmtWorkflowRunProp.execute();
        stmtWorkflowRunProp.close();
        String sqlWorkflowRun = DELETE_FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtWorkflowRun = connection.prepareStatement(sqlWorkflowRun);
        stmtWorkflowRun.setLong(1, workflowRunId);
        int affectedLines = stmtWorkflowRun.executeUpdate();
        stmtWorkflowRun.close();
        return affectedLines == 1;
    }

    private Long addDataReference(final DataReference dataReference, final Connection connection, final boolean isRetry)
        throws SQLException {
        Long dataReferenceId =
            addDataReference(dataReference.getDataReferenceKey(), dataReference.getNodeIdentifier().getIdString(),
                connection, isRetry);
        Set<Long> binaryReferenceIds = addBinaryReferences(dataReference.getBinaryReferences(), connection, isRetry);
        addDataBinaryReferenceRelations(dataReferenceId, binaryReferenceIds, connection, isRetry);
        return dataReferenceId;
    }

    @Override
    public Long addDataReferenceToComponentRun(final Long componentRunId, final DataReference dataReference) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long dataReferenceId = addDataReference(dataReference, connection, isRetry);
                addDataReferenceComponentRunRelation(dataReferenceId, componentRunId, connection, isRetry);
                return dataReferenceId;
            }

        };
        return execution.call();
    }

    @Override
    public Long addDataReferenceToComponentInstance(final Long componentInstanceId, final DataReference dataReference) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long dataReferenceId = addDataReference(dataReference, connection, isRetry);
                addDataReferenceComponentInstanceRelation(dataReferenceId, componentInstanceId, connection, isRetry);
                return dataReferenceId;
            }
        };
        return execution.call();
    }

    @Override
    public Long addDataReferenceToWorkflowRun(final Long workflowRunId, final DataReference dataReference) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long dataReferenceId = addDataReference(dataReference, connection, isRetry);
                addDataReferenceWorkflowRunRelation(dataReferenceId, workflowRunId, connection, isRetry);
                return dataReferenceId;
            }
        };
        return execution.call();
    }

    private void addDataReferenceWorkflowRunRelation(Long dataReferenceId, Long workflowRunId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_WORKFLOWRUN_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + WORKFLOW_RUN_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    private void addDataReferenceComponentInstanceRelation(Long dataReferenceId, Long componentInstanceId, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_COMPONENTINSTANCE_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + COMPONENT_INSTANCE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, componentInstanceId);
        stmt.executeUpdate();
        stmt.close();
    }

    private void addDataReferenceComponentRunRelation(Long dataReferenceId, Long componentRunId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + COMPONENT_RUN_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, componentRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    private void addDataBinaryReferenceRelations(Long dataReferenceId, Set<Long> binaryReferenceIds,
        Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + BINARY_REFERENCE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (Long id : binaryReferenceIds) {
            stmt.setLong(1, dataReferenceId);
            stmt.setLong(2, id);
            stmt.executeUpdate();
            stmt.clearParameters();
        }
        stmt.close();
    }

    private Set<Long> addBinaryReferences(Set<BinaryReference> binaryReferences, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_BINARY_REFERENCE + "("
            + BINARY_REFERENCE_KEY + COMMA + COMPRESSION + COMMA + REVISION + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        Set<Long> ids = new HashSet<Long>();
        for (BinaryReference br : binaryReferences) {
            stmt.setString(1, br.getBinaryReferenceKey());
            stmt.setString(2, br.getCompression().toString());
            stmt.setString(3, br.getRevision());
            stmt.executeUpdate();
            ids.add(getGeneratedKey(stmt));
            stmt.clearParameters();
        }
        stmt.close();
        return ids;
    }

    private Long addDataReference(String dataReferenceKey, String nodeIdentifier, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_DATA_REFERENCE + "("
            + DATA_REFERENCE_KEY + COMMA + NODE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, dataReferenceKey);
        stmt.setString(2, nodeIdentifier);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    @Override
    public void addBinaryReference(final Long dataReferenceId, final BinaryReference binaryReference) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Set<Long> brIds = new HashSet<Long>();
                brIds.add(addBinaryReference(binaryReference, connection, isRetry));
                addDataBinaryReferenceRelations(dataReferenceId, brIds, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private Long addBinaryReference(BinaryReference binaryReference, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_BINARY_REFERENCE + "("
            + BINARY_REFERENCE_KEY + COMMA + COMPRESSION + COMMA + REVISION + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, binaryReference.getBinaryReferenceKey());
        stmt.setString(2, binaryReference.getCompression().toString());
        stmt.setString(3, binaryReference.getRevision());
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    @Override
    @AllowRemoteAccess
    public DataReference getDataReference(final String dataReferenceKey) {
        final SafeExecution<DataReference> execution = new SafeExecution<DataReference>() {

            @Override
            protected DataReference protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return getDataReference(dataReferenceKey, connection, isRetry);
            }
        };
        return execution.call();
    }

    private DataReference getDataReference(String dataReferenceKey, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + DATA_REFERENCE_ID + COMMA + NODE_ID + FROM + DB_PREFIX + TABLE_DATA_REFERENCE
            + WHERE + DATA_REFERENCE_KEY + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, dataReferenceKey);
        ResultSet rs = stmt.executeQuery();
        Long dataRefId = null;
        String dataRefNodeId = null;
        if (rs != null && rs.next()) {
            dataRefId = rs.getLong(DATA_REFERENCE_ID);
            dataRefNodeId = rs.getString(NODE_ID).trim();
            rs.close();
        }
        stmt.close();
        if (dataRefId == null) {
            return null;
        }
        sql = SELECT + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_KEY + COMMA + TABLE_BINARY_REFERENCE + DOT + COMPRESSION + COMMA
            + TABLE_BINARY_REFERENCE + DOT + REVISION
            + FROM + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + INNER_JOIN + DB_PREFIX + TABLE_BINARY_REFERENCE + ON
            + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_ID + EQUAL + REL_DATAREFERENCE_BINARYREFERENCE + DOT + BINARY_REFERENCE_ID
            + WHERE + REL_DATAREFERENCE_BINARYREFERENCE + DOT + DATA_REFERENCE_ID + EQUAL + QMARK;
        stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataRefId);
        rs = stmt.executeQuery();
        DataReference dataRef = null;
        Set<BinaryReference> binaryReferences = new HashSet<BinaryReference>();
        if (rs != null) {
            while (rs.next()) {
                binaryReferences.add(new BinaryReference(rs.getString(BINARY_REFERENCE_KEY).trim(), CompressionFormat.valueOf(rs
                    .getString(COMPRESSION)), rs
                    .getString(REVISION)));
            }
            dataRef = new DataReference(dataReferenceKey, NodeIdentifierFactory.fromNodeId(dataRefNodeId), binaryReferences);
            rs.close();
        }
        stmt.close();
        return dataRef;
    }

    private Map<Long, Set<String>> getDataReferenceBinaryKeys(Long workflowRunId, Connection connection, boolean isRetry)
        throws SQLException {
        // TODO optimize initialization of key map
        String sqlDataRefIds = SELECT + DATA_REFERENCE_ID + FROM + DB_PREFIX + VIEW_WORKFLOWRUN_DATAREFERENCE
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        String sqlBinaryRefs =
            SELECT + DATA_REFERENCE_ID + COMMA + BINARY_REFERENCE_KEY + FROM + DB_PREFIX + TABLE_BINARY_REFERENCE
                + INNER_JOIN + REL_DATAREFERENCE_BINARYREFERENCE + ON + REL_DATAREFERENCE_BINARYREFERENCE + DOT + BINARY_REFERENCE_ID
                + EQUAL + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_ID
                + WHERE + DATA_REFERENCE_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtDataRefIds = connection.prepareStatement(sqlDataRefIds);
        stmtDataRefIds.setLong(1, workflowRunId);
        ResultSet rs = stmtDataRefIds.executeQuery();
        Map<Long, Set<String>> keys = new HashMap<Long, Set<String>>();
        if (rs != null) {
            while (rs.next()) {
                keys.put(rs.getLong(DATA_REFERENCE_ID), new HashSet<String>());
            }
            rs.close();
        }
        stmtDataRefIds.close();
        PreparedStatement stmtBinaryRefs;
        if (keys.keySet().size() > 0) {
            stmtBinaryRefs = connection.prepareStatement(String.format(sqlBinaryRefs, sqlDataRefIds));
            stmtBinaryRefs.setLong(1, workflowRunId);
            rs = stmtBinaryRefs.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    keys.get(rs.getLong(DATA_REFERENCE_ID)).add(rs.getString(BINARY_REFERENCE_KEY).trim());
                }
                rs.close();
            }
            stmtBinaryRefs.close();
        }
        return keys;
    }

    private Collection<EndpointData> getEndpointData(Long componentRunId, EndpointType endpointType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addWorkflowRunProperties(Long workflowRunId, Map<String, String> properties) {
        addProperties(TABLE_WORKFLOW_RUN_PROPERTIES, workflowRunId, properties);
    }

    @Override
    public void addComponentInstanceProperties(Long componentInstanceId, Map<String, String> properties) {
        addProperties(TABLE_COMPONENT_INSTANCE_PROPERTIES, componentInstanceId, properties);
    }

    @Override
    public void addComponentRunProperties(Long componentRunId, Map<String, String> properties) {
        addProperties(TABLE_COMPONENT_RUN_PROPERTIES, componentRunId, properties);
    }

    @Override
    public Collection<EndpointData> getInputData(Long componentRunId) {
        return getEndpointData(componentRunId, EndpointType.INPUT);
    }

    @Override
    public Collection<EndpointData> getOutputData(Long componentRunId) {
        return getEndpointData(componentRunId, EndpointType.OUTPUT);
    }

    @Override
    public Map<String, String> getWorkflowRunProperties(Long workflowRunId) {
        return getProperties(TABLE_WORKFLOW_RUN_PROPERTIES, workflowRunId);
    }

    @Override
    public Map<String, String> getComponentRunProperties(Long componentRunId) {
        return getProperties(TABLE_COMPONENT_RUN_PROPERTIES, componentRunId);
    }

    @Override
    public void addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime) {
        addTimelineInterval(workflowRunId, intervalType, starttime, null);
    }

    private void initialize() {
        createConnectionPool();
        try {
            final Connection connection = connectionPool.getConnection();
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Connecting to data management meta data db failed.", e);
        }
        initializeDatabase();
        initializationLatch.countDown();
    }

    private void cleanUpWorkflowRunFinalStates(Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + TABLE_WORKFLOW_RUN + SET + FINAL_STATE + EQUAL + SINGE_QOUTE + STRING_PLACEHOLDER + SINGE_QOUTE
            + WHERE + FINAL_STATE + IS_NULL;
        Statement stmt = connection.createStatement();
        int affectedLines = stmt.executeUpdate(String.format(sql, FinalWorkflowState.CORRUPTED));
        LOGGER.debug(String.format("Cleaned up corrupted final states of %d workflows.", affectedLines));
        stmt.close();
    }

    private void cleanUpDeletion(Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + WORKFLOW_RUN_ID + COMMA + TO_BE_DELETED + FROM + TABLE_WORKFLOW_RUN
            + WHERE + TO_BE_DELETED + NOT_EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, NOT_MARKED_TO_BE_DELETED);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                final long wfrunId = rs.getLong(WORKFLOW_RUN_ID);
                int type = rs.getInt(TO_BE_DELETED);
                switch (type) {
                case WORKFLOW_RUN_TO_BE_DELETED:
                    LOGGER.debug(String.format("Clean up deletion of workflow run id %d", wfrunId));
                    executionQueue.enqueue(new Runnable() {

                        @Override
                        public void run() {
                            deleteWorkflowRunInternal(wfrunId);
                        }
                    });
                    break;
                case FILES_TO_BE_DELETED:
                    LOGGER.debug(String.format("Clean up file deletion of workflow run id %d", wfrunId));
                    executionQueue.enqueue(new Runnable() {

                        @Override
                        public void run() {
                            deleteWorkflowRunFilesInternal(wfrunId);
                        }
                    });
                    break;
                default:
                    break;
                }
            }
            rs.close();
        }
        stmt.close();
    }

    private void initializeDatabase() {
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            DerbyDatabaseSetup.setupDatabase(connection);
            cleanUpWorkflowRunFinalStates(connection, false);
            cleanUpDeletion(connection, false);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            LOGGER.error("Failed to initialize database:", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    e1 = null;
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.error("Failed to close connection:", e);
                }
            }
        }
    }

    private void createConnectionPool() {
        connectionPoolDatasource = new EmbeddedConnectionPoolDataSource();
        connectionPoolDatasource.setDatabaseName(configuration.getDatabaseURL());
        connectionPoolDatasource.setCreateDatabase("create");
        connectionPool = new SharedPoolDataSource();
        connectionPool.setConnectionPoolDataSource(connectionPoolDatasource);
        connectionPool.setDefaultAutoCommit(false);
        connectionPool.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        final int noLimit = -1;
        connectionPool.setMaxActive(noLimit);
        connectionPool.setDefaultReadOnly(false);
        LOGGER.debug("Start data management meta data db: " + configuration.getDatabaseURL());
    }

    private void shutDown() {
        if (connectionPool != null) {
            /*
             * Catching Exception is not allowed due to CheckStyle, thus this quirky Executor-construction is used to shut down the
             * connection pool.
             */
            try {
                final Future<Boolean> future = SharedThreadPool.getInstance().submit(new Callable<Boolean>() {

                    @Override
                    @TaskDescription("Close database connection pool")
                    public Boolean call() throws Exception {
                        executionQueue.cancel(true);
                        connectionPool.close();
                        return true;
                    }
                });
                future.get(3, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                LOGGER.error("Failed to close connection pool:", cause);
            } catch (InterruptedException e) {
                LOGGER.error("Failed to close connection pool due to interruption:", e);
            } catch (TimeoutException e) {
                LOGGER.error("Failed to close connection pool due to timeout:", e);
            } finally {
                connectionPool = null;
            }
        }
        connectionPoolDatasource = new EmbeddedConnectionPoolDataSource();
        connectionPoolDatasource.setDatabaseName(configuration.getDatabaseURL());
        connectionPoolDatasource.setShutdownDatabase("shutdown");
        try {
            connectionPoolDatasource.getConnection();
        } catch (SQLException e) {
            // when Derby shuts down a database, it throws an SQLException with an SQLState of 08006
            if (!e.getSQLState().equals("08006")) {
                LOGGER.error("Failed to shut down database:", e);
            }
        }
    }

    private void waitForRetry() {
        LOGGER.debug("Waiting 5 seconds to retry SQL statement execution");
        try {
            Thread.sleep(TIME_TO_WAIT_FOR_RETRY);
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting for retrying a failed SQL statement was interupted");
        }
    }
}
