/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN_PROPERTIES;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamanagement.FileDataService;
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
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Derby implementation of {@link RemotableMetaDataBackendService}.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
public class DerbyMetaDataBackendServiceImpl implements MetaDataBackendService {

    private static final String FALSE = "FALSE";

    private static final int NOT_MARKED_TO_BE_DELETED = 0;

    private static final int WORKFLOW_RUN_TO_BE_DELETED = 1;

    private static final int FILES_TO_BE_DELETED = 2;

    private static final String TRUE = "true";

    private static final int TIME_TO_WAIT_FOR_RETRY = 5000;

    private static final String INITIALIZATION_TIMEOUT_ERROR_MESSAGE =
        "Initialization timeout reached for meta data database.";

    private static final int INITIALIZATION_TIMEOUT = 30;

    private static final int SHUTDOWN_TIMEOUT = 5;

    private static final int MAX_RETRIES = 5;

    private static final Log LOGGER = LogFactory.getLog(DerbyMetaDataBackendServiceImpl.class);

    private static final String METADATA_DB_NAME = "metadata";

    private final CountDownLatch initializationLatch = new CountDownLatch(1);

    private final DerbyMetaDataBackendOperationsImpl metaDataBackendOperations = new DerbyMetaDataBackendOperationsImpl();

    private SharedPoolDataSource connectionPool;

    private EmbeddedConnectionPoolDataSource connectionPoolDatasource;

    private DerbyMetaDataBackendConfiguration configuration;

    private ConfigurationService configService;

    private volatile FileDataService dataService; // made volatile to ensure thread visibility; other approaches possible

    private final ThreadLocal<PooledConnection> connections = new ThreadLocal<PooledConnection>();

    private final AsyncOrderedExecutionQueue executionQueue = ConcurrencyUtils.getFactory().createAsyncOrderedExecutionQueue(
        AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private TypedDatumSerializer typedDatumSerializer;

    private boolean startedSuccessfully = false;

    private String errorMessage = null;

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
        ConcurrencyUtils.getAsyncTaskService().execute("Database initialization", this::initialize);
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

    protected void bindDataService(FileDataService newService) {
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
     * @param <T> the class to handle with
     * 
     * @author Christian Weiss
     * @author Jan Flink
     * @author Robert Mischke
     * @author Tobias Rodehutskors (catch InterruptedException)
     */
    protected abstract class SafeExecution<T> implements Callable<T> {

        @Override
        public final T call() {

            T result = null;
            try {
                // If the current thread is interrupted before reaching this code,
                // initializationLatch.await() will directly throw an InterruptedException.
                if (!initializationLatch.await(INITIALIZATION_TIMEOUT, TimeUnit.SECONDS)) {
                    LOGGER.error(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                    throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                }
            } catch (InterruptedException e) {
                // All SaveExecution Callables in this class are NOT executed in a separate thread, but instead they are executed by
                // directly calling SaveExecution.call(). To be able to write to the database and exit the whole thread cleanly, we catch
                // the InterruptedException and check again if the latch can be passed. This should be the case in this case. Therefore, a
                // short timeout is no problem.
                try {

                    if (!initializationLatch.await(1, TimeUnit.SECONDS)) {
                        LOGGER.error(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                        throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                    }
                } catch (InterruptedException e1) {
                    throw new RuntimeException(INITIALIZATION_TIMEOUT_ERROR_MESSAGE);
                }
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
                    final long startTimeForAttempt = System.currentTimeMillis();
                    try {
                        result = protectedCall(connection, false);
                        // note: the logged (usually anonymous) class name is not very helpful, but at least allows separation
                        StatsCounter.registerValue("Metadata Backend: successful database call duration (msec)",
                            getClass().getName(),
                            System.currentTimeMillis() - startTimeForAttempt);
                        break;
                    } catch (SQLTransientException e) {
                        // from spec: The subclass of SQLException is thrown in situations where a previously failed operation might
                        // be able to succeed when the operation is retried without any intervention by application-level functionality.
                        if (count == 0) {
                            LOGGER.debug(StringUtils.format("Executing database statement failed (%s). Will retry.", e.getMessage()));
                        }
                        // note: the logged (usually anonymous) class name is not very helpful, but at least allows separation
                        StatsCounter.registerValue(
                            "Metadata Backend: duration of database calls on transient failure (in msec) - will wait and retry",
                            getClass().getName(), System.currentTimeMillis() - startTimeForAttempt);
                        waitForRetry();
                    }
                    count++;
                    if (count >= MAX_RETRIES) {
                        throw new RuntimeException(StringUtils.format("Failed to commit database transaction after %d retries.",
                            MAX_RETRIES));
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
                Long wfRunId =
                    metaDataBackendOperations.addWorkflowRun(workflowTitle, workflowControllerNodeId,
                        workflowDataManagementNodeId,
                        connection, isRetry);
                metaDataBackendOperations.addTimelineInterval(wfRunId, TimelineIntervalType.WORKFLOW_RUN, starttime, null,
                    connection,
                    isRetry);
                return wfRunId;
            }
        };
        return execution.call();
    }

    @Override
    public void addWorkflowFileToWorkflowRun(final Long workflowRunId, final String wfFileReference) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.addWorkflowFileToWorkflowRun(workflowRunId, wfFileReference, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    private void addProperties(final String propertiesTableName, final Long relatedId, final Map<String, String> properties) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.addProperties(propertiesTableName, relatedId, properties, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    public Map<String, Long> addComponentInstances(final Long workflowRunId, final Collection<ComponentInstance> componentInstances) {
        final SafeExecution<Map<String, Long>> execution = new SafeExecution<Map<String, Long>>() {

            @Override
            protected Map<String, Long> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return metaDataBackendOperations.addComponentInstances(workflowRunId, componentInstances, connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    @AllowRemoteAccess
    public Long addComponentRun(final Long componentInstanceDbId, final String nodeId, final Integer count,
        final Long starttime) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long cRunId =
                    metaDataBackendOperations.addComponentRun(componentInstanceDbId, nodeId, count, starttime, connection, isRetry);
                Long wfRunId =
                    metaDataBackendOperations.getWorkflowRunIdByComponentInstanceId(componentInstanceDbId, connection, isRetry);
                metaDataBackendOperations.addTimelineInterval(wfRunId, TimelineIntervalType.COMPONENT_RUN, starttime, cRunId,
                    connection,
                    isRetry);
                return cRunId;
            }
        };
        return execution.call();
    }

    @Override
    @AllowRemoteAccess
    public void setOrUpdateHistoryDataItem(final Long componentRunId, final String historyDataItem) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setOrUpdateHistoryDataItem(componentRunId, historyDataItem, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    public void setOrUpdateTimelineDataItem(final Long workflowRunId, final String timelinDataItem) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setOrUpdateTimelineDataItem(workflowRunId, timelinDataItem, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    public void setWorkflowRunFinished(final Long workflowRunId, final Long endtime, final FinalWorkflowState finalState) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setWorkflowRunEndtime(workflowRunId, endtime, connection, isRetry);
                metaDataBackendOperations.setWorkflowRunFinalState(workflowRunId, finalState, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public void setComponentRunFinished(final Long componentRunId, final Long endtime, final FinalComponentRunState finalState) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setComponentRunFinished(componentRunId, endtime, finalState, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public void setComponentInstanceFinalState(final Long componentInstanceId, final FinalComponentState finalState) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setComponentInstanceFinalState(componentInstanceId, finalState, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions() {
        final SafeExecution<Set<WorkflowRunDescription>> execution = new SafeExecution<Set<WorkflowRunDescription>>() {

            @Override
            protected Set<WorkflowRunDescription> protectedCall(final Connection connection, final boolean isRetry)
                throws SQLException {
                connection.setReadOnly(true);
                return metaDataBackendOperations.getWorkflowRunDescriptions(connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRun getWorkflowRun(final Long workflowRunId) {
        final SafeExecution<WorkflowRun> execution = new SafeExecution<WorkflowRun>() {

            @Override
            protected WorkflowRun protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return metaDataBackendOperations.getWorkflowRun(workflowRunId, connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    public Collection<ComponentRun> getComponentRuns(final Long componentInstanceId) {
        final SafeExecution<Collection<ComponentRun>> execution = new SafeExecution<Collection<ComponentRun>>() {

            @Override
            protected Collection<ComponentRun> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return metaDataBackendOperations.getComponentRuns(componentInstanceId, connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    public Map<String, Long> addEndpointInstances(final Long componentInstanceId,
        final Collection<EndpointInstance> endpointInstances) {
        final SafeExecution<Map<String, Long>> execution = new SafeExecution<Map<String, Long>>() {

            @Override
            protected Map<String, Long> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return metaDataBackendOperations.addEndpointInstances(componentInstanceId, endpointInstances, connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    @AllowRemoteAccess
    public void addInputDatum(final Long componentRunId, final Long typedDatumId, final Long endpointInstanceId,
        final Integer count) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.addEndpointDatum(componentRunId, typedDatumId, endpointInstanceId, count, connection,
                    isRetry);
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
                TypedDatum td = typedDatumSerializer.deserialize(datum);
                Long typedDatumId =
                    metaDataBackendOperations.addTypedDatum(td.getDataType().getShortName(), datum, connection, isRetry);
                return metaDataBackendOperations.addEndpointDatum(componentRunId, typedDatumId, endpointInstanceId, count,
                    connection,
                    isRetry);
            }
        };
        return execution.call();
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
                return metaDataBackendOperations.addTimelineInterval(workflowRunId, intervalType, starttime, relatedComponentId,
                    connection,
                    isRetry);
            }
        };
        return execution.call();
    }

    @Override
    public void setTimelineIntervalFinished(final Long timelineIntervalId, final long endtime) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                metaDataBackendOperations.setTimelineIntervalFinished(timelineIntervalId, endtime, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public WorkflowRunTimline getWorkflowTimeline(final Long workflowRunId) {
        final SafeExecution<WorkflowRunTimline> execution = new SafeExecution<WorkflowRunTimline>() {

            @Override
            protected WorkflowRunTimline protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                String workflowRunName = metaDataBackendOperations.getWorkflowRunName(workflowRunId, connection, isRetry);
                TimelineInterval workflowRunInterval =
                    metaDataBackendOperations.getWorkflowInterval(workflowRunId, connection, isRetry);
                List<ComponentRunInterval> componentRunIntervals =
                    metaDataBackendOperations.getComponentRunIntervals(workflowRunId, connection, isRetry);
                return new WorkflowRunTimline(workflowRunName, workflowRunInterval, componentRunIntervals);
            }
        };
        return execution.call();
    }

    private Map<String, String> getProperties(final String tableName, final Long relatedId) {
        final SafeExecution<Map<String, String>> execution = new SafeExecution<Map<String, String>>() {

            @Override
            protected Map<String, String> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return metaDataBackendOperations.getProperties(tableName, relatedId, connection, isRetry);
            }
        };
        return execution.call();
    }

    @Override
    @AllowRemoteAccess
    public Boolean deleteWorkflowRunFiles(final Long workflowRunId) {
        final SafeExecution<Boolean> execution = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                if (metaDataBackendOperations.isWorkflowFinished(workflowRunId, connection, isRetry)) {
                    metaDataBackendOperations.markDeletion(workflowRunId, FILES_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(StringUtils.format("Marked workflow run id %d to delete files.", workflowRunId));
                    return true;
                }
                LOGGER.debug("Workflow files deletion requested, but workflow " + workflowRunId
                    + " is either not finished yet or has already been deleted.");
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
                if (metaDataBackendOperations.isWorkflowFinished(workflowRunId, connection, isRetry)) {
                    metaDataBackendOperations.markDeletion(workflowRunId, WORKFLOW_RUN_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(StringUtils.format("Marked workflow run id %d to be deleted.", workflowRunId));
                    return true;
                }
                LOGGER.debug("Workflow run deletion requested, but workflow " + workflowRunId
                    + " is either not finished yet or has already been deleted.");
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
                LOGGER.debug(StringUtils.format("Starting to delete workflow run id %d.", workflowRunId));
                Map<Long, Set<String>> keys =
                    metaDataBackendOperations.getDataReferenceBinaryKeys(workflowRunId, connection, isRetry);
                return keys;
            }
        };
        final Map<Long, Set<String>> dataKeys = execution.call();
        if (dataKeys != null) {
            try {
                deleteFiles(dataKeys.values());
            } catch (RemoteOperationException e) {
                throw new RuntimeException("Failed to delete files. ", e);
            }
        }
        final SafeExecution<Boolean> execution2 = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Boolean deleted = metaDataBackendOperations.deleteDataReferences(dataKeys, connection, isRetry);
                LOGGER.debug(StringUtils.format("Deleted data references of workflow run id %d.", workflowRunId));
                return deleted;
            }
        };
        if (execution2.call()) {
            final SafeExecution<Void> execution3 = new SafeExecution<Void>() {

                @Override
                protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                    metaDataBackendOperations.deleteTypedDatums(workflowRunId, connection, isRetry);
                    metaDataBackendOperations.deleteWorkflowRunContent(workflowRunId, connection, isRetry);
                    LOGGER.debug(StringUtils.format("Finished deletion of workflow run id %d.", workflowRunId));
                    return null;
                }
            };
            execution3.call();
        } else {
            LOGGER.warn(StringUtils.format("Could not delete workflow run id %d.", workflowRunId));
        }
    }

    private void deleteWorkflowRunFilesInternal(final Long workflowRunId) {
        final SafeExecution<Map<Long, Set<String>>> execution = new SafeExecution<Map<Long, Set<String>>>() {

            @Override
            protected Map<Long, Set<String>> protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                LOGGER.debug(StringUtils.format("Starting to delete files of workflow run id %d.", workflowRunId));
                Map<Long, Set<String>> keys =
                    metaDataBackendOperations.getDataReferenceBinaryKeys(workflowRunId, connection, isRetry);
                return keys;
            }
        };
        final Map<Long, Set<String>> dataKeys = execution.call();
        if (dataKeys != null) {
            try {
                deleteFiles(dataKeys.values());
            } catch (RemoteOperationException e) {
                throw new RuntimeException("Failed to delete files. ", e);
            }
        }
        final SafeExecution<Boolean> execution2 = new SafeExecution<Boolean>() {

            @Override
            protected Boolean protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                return metaDataBackendOperations.deleteDataReferences(dataKeys, connection, isRetry);
            }
        };
        if (execution2.call()) {
            final SafeExecution<Void> execution3 = new SafeExecution<Void>() {

                @Override
                protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                    metaDataBackendOperations.markDataReferencesDeleted(workflowRunId, connection, isRetry);
                    metaDataBackendOperations.markDeletion(workflowRunId, NOT_MARKED_TO_BE_DELETED, connection, isRetry);
                    LOGGER.debug(StringUtils.format("Finished file deletion of workflow run id %d.",
                        workflowRunId));
                    return null;
                }
            };
            execution3.call();
        }
    }

    private void deleteFiles(Collection<Set<String>> binaryKeys) throws RemoteOperationException {
        for (final Set<String> keySet : binaryKeys) {
            for (final String key : keySet) {
                dataService.deleteReference(key);
            }
        }
    }

    @Override
    public Long addDataReferenceToComponentRun(final Long componentRunId, final DataReference dataReference) {
        final SafeExecution<Long> execution = new SafeExecution<Long>() {

            @Override
            protected Long protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Long dataReferenceId = metaDataBackendOperations.addDataReference(dataReference, connection, isRetry);
                metaDataBackendOperations.addDataReferenceComponentRunRelation(dataReferenceId, componentRunId, connection,
                    isRetry);
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
                Long dataReferenceId = metaDataBackendOperations.addDataReference(dataReference, connection, isRetry);
                metaDataBackendOperations.addDataReferenceComponentInstanceRelation(dataReferenceId, componentInstanceId,
                    connection,
                    isRetry);
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
                Long dataReferenceId = metaDataBackendOperations.addDataReference(dataReference, connection, isRetry);
                metaDataBackendOperations.addDataReferenceWorkflowRunRelation(dataReferenceId, workflowRunId, connection, isRetry);
                return dataReferenceId;
            }
        };
        return execution.call();
    }

    @Override
    public void addBinaryReference(final Long dataReferenceId, final BinaryReference binaryReference) {
        final SafeExecution<Void> execution = new SafeExecution<Void>() {

            @Override
            protected Void protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                Set<Long> brIds = new HashSet<Long>();
                brIds.add(metaDataBackendOperations.addBinaryReference(binaryReference, connection, isRetry));
                metaDataBackendOperations.addDataBinaryReferenceRelations(dataReferenceId, brIds, connection, isRetry);
                return null;
            }
        };
        execution.call();
    }

    @Override
    @AllowRemoteAccess
    public DataReference getDataReference(final String dataReferenceKey) {
        final SafeExecution<DataReference> execution = new SafeExecution<DataReference>() {

            @Override
            protected DataReference protectedCall(final Connection connection, final boolean isRetry) throws SQLException {
                connection.setReadOnly(true);
                return metaDataBackendOperations.getDataReference(dataReferenceKey, connection, isRetry);
            }
        };
        return execution.call();
    }

    private Collection<EndpointData> getEndpointData(Long componentRunId, EndpointType endpointType) {
        // TODO Check if method could be removed.
        // Not needed yet. Endpoint data are currently returned within the workflow run object.
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
    public void addEndpointInstanceProperties(Long endpointInstanceId, Map<String, String> properties) {
        addProperties(TABLE_ENDPOINT_INSTANCE_PROPERTIES, endpointInstanceId, properties);
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

    private void cleanUpDeletion(Connection connection, boolean isRetry) throws SQLException {

        Map<Long, Integer> wfsToBeDeleted = metaDataBackendOperations.getWorkflowRunsToBeDeleted(connection, isRetry);
        if (wfsToBeDeleted != null) {
            for (final Long wfrunId : wfsToBeDeleted.keySet()) {
                switch (wfsToBeDeleted.get(wfrunId)) {
                case WORKFLOW_RUN_TO_BE_DELETED:
                    LOGGER.debug(StringUtils.format("Clean up deletion of workflow run id %d", wfrunId));
                    executionQueue.enqueue(new Runnable() {

                        @Override
                        public void run() {
                            deleteWorkflowRunInternal(wfrunId);
                        }
                    });
                    break;
                case FILES_TO_BE_DELETED:
                    LOGGER.debug(StringUtils.format("Clean up file deletion of workflow run id %d", wfrunId));
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
        }
    }

    private void initialize() {
        createConnectionPool();
        try {
            final Connection connection = connectionPool.getConnection();
            connection.close();
        } catch (SQLException e) {
            errorMessage = "Failed to connect to the database. Most likely reasons: The database is used by another RCE "
                + "instance or the database was not created successfully before then.";
            throw new IllegalStateException("Connecting to data management meta data db failed.", e);
        }
        initializeDatabase();
        initializationLatch.countDown();
    }

    private void initializeDatabase() {
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            DerbyDatabaseSetup.setupDatabase(connection);
            int affectedLines = metaDataBackendOperations.cleanUpWorkflowRunFinalStates(connection, false);
            if (affectedLines > 0) {
                LOGGER.debug(StringUtils.format("Cleaned up corrupted final states of %d workflows.", affectedLines));
            }
            cleanUpDeletion(connection, false);
            connection.commit();
            startedSuccessfully = true;
        } catch (SQLException | RuntimeException e) {
            startedSuccessfully = false;
            errorMessage = e.getMessage();
            // Do not log errorMessage because it will be logged again by the InstanceValidator
            // LOGGER.error(errorMessage);
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
                final Future<Boolean> future = ConcurrencyUtils.getAsyncTaskService().submit(new Callable<Boolean>() {

                    @Override
                    @TaskDescription("Close database connection pool")
                    public Boolean call() throws Exception {
                        executionQueue.cancel(true);
                        if (connectionPool != null) {
                            connectionPool.close();
                        }
                        return true;
                    }
                });
                future.get(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                LOGGER.error("Failed to close connection pool:", cause);
            } catch (InterruptedException e) {
                LOGGER.error("Failed to close connection pool due to interruption:", e);
            } catch (TimeoutException e) {
                LOGGER.error("Failed to close connection pool due to timeout: ", e);
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

    @Override
    public boolean isMetaDataBackendOk() {
        try {
            initializationLatch.await(INITIALIZATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return startedSuccessfully;
    }

    @Override
    public String getMetaDataBackendStartErrorMessage() {
        return errorMessage;
    }
}
