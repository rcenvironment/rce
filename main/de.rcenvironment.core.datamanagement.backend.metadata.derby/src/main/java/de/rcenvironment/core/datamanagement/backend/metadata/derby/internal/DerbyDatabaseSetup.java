/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BINARY_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_RUN_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATA_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DB_VERSION;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTINSTANCE_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_DATAREFERENCE_BINARYREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_BINARY_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_DATA_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_DB_VERSION_INFO;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TIMELINE_INTERVAL;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TYPED_DATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_RUNS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_TIMELINE_INTERVALS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_COMPONENTRUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_TYPEDDATUM;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.datamanagement.commons.MetaDataConstants;

/**
 * Derby meta data db setup.
 * 
 * @author Jan Flink
 */
public abstract class DerbyDatabaseSetup {

    private static final String CURRENT_DB_VERSION = "6.1";

    private static final int QUERY_EXECUTION_TIMEOUT = 600000;

    private static final int MAX_RETRIES = 3;

    private static final Log LOGGER = LogFactory.getLog(DerbyMetaDataBackendServiceImpl.class);

    private static final String WHERE = " WHERE ";

    private static final String APO = "'";

    private static final String AND = " AND ";

    private static final String SELECT = " SELECT ";

    protected static void setupDatabase(final Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        if (!tableExists(statement, MetaDataConstants.TABLE_DB_VERSION_INFO)) {
            // if DB version is 0 - table version info does not exist - update to version 6.1
            if (tableExists(statement, MetaDataConstants.TABLE_WORKFLOW_RUN)) {
                LOGGER.debug("Updating database: v 0 --> v 6.1");
                createTableDBVersionInfo(connection, CURRENT_DB_VERSION);
                deleteViews(connection);
                createViews(connection);
            } else {
                // set up a fresh database
                LOGGER.debug("Setting up database.");
                createTableDBVersionInfo(connection, CURRENT_DB_VERSION);
                createTables(connection);
                createIndexes(connection);
                createViews(connection);
            }
        } else if (!tableExists(statement, MetaDataConstants.TABLE_WORKFLOW_RUN)
            || !tableExists(statement, MetaDataConstants.TABLE_WORKFLOW_RUN_PROPERTIES)
            || !tableExists(statement, MetaDataConstants.TABLE_TIMELINE_INTERVAL)
            || !tableExists(statement, MetaDataConstants.TABLE_COMPONENT_INSTANCE)
            || !tableExists(statement, MetaDataConstants.TABLE_COMPONENT_INSTANCE_PROPERTIES)
            || !tableExists(statement, MetaDataConstants.TABLE_COMPONENT_RUN)
            || !tableExists(statement, MetaDataConstants.TABLE_COMPONENT_RUN_PROPERTIES)
            || !tableExists(statement, MetaDataConstants.TABLE_DATA_REFERENCE)
            || !tableExists(statement, MetaDataConstants.TABLE_BINARY_REFERENCE)
            || !tableExists(statement, MetaDataConstants.TABLE_ENDPOINT_INSTANCE)
            || !tableExists(statement, MetaDataConstants.TABLE_TYPED_DATUM)
            || !tableExists(statement, MetaDataConstants.TABLE_ENDPOINT_DATA)
            || !tableExists(statement, MetaDataConstants.REL_COMPONENTINSTANCE_DATAREFERENCE)
            || !tableExists(statement, MetaDataConstants.REL_WORKFLOWRUN_DATAREFERENCE)
            || !tableExists(statement, MetaDataConstants.REL_COMPONENTRUN_DATAREFERENCE)
            || !tableExists(statement, MetaDataConstants.REL_DATAREFERENCE_BINARYREFERENCE)
            || !viewExists(statement, MetaDataConstants.VIEW_COMPONENT_RUNS)
            || !viewExists(statement, MetaDataConstants.VIEW_COMPONENT_TIMELINE_INTERVALS)
            || !viewExists(statement, MetaDataConstants.VIEW_ENDPOINT_DATA)
            || !viewExists(statement, MetaDataConstants.VIEW_WORKFLOWRUN_COMPONENTRUN)
            || !viewExists(statement, MetaDataConstants.VIEW_WORKFLOWRUN_DATAREFERENCE)
            || !viewExists(statement, MetaDataConstants.VIEW_WORKFLOWRUN_TYPEDDATUM)) {
            throw new RuntimeException("Unknown DB state!");
        }
        statement.close();
        LOGGER.debug(String.format("Database version is %s", getDBVersion(connection)));
    }

    private static void deleteViews(final Connection connection) {
        final Runnable task = new SQLRunnable(MAX_RETRIES) {

            @Override
            protected void sqlRun() throws SQLTransientConnectionException {
                try {
                    deleteView(VIEW_COMPONENT_RUNS, connection);
                    deleteView(VIEW_COMPONENT_TIMELINE_INTERVALS, connection);
                    deleteView(VIEW_ENDPOINT_DATA, connection);
                    deleteView(VIEW_WORKFLOWRUN_DATAREFERENCE, connection);
                    deleteView(VIEW_WORKFLOWRUN_TYPEDDATUM, connection);
                    deleteView(VIEW_WORKFLOWRUN_COMPONENTRUN, connection);
                } catch (SQLException e) {
                    if (e instanceof SQLTransientConnectionException) {
                        throw (SQLTransientConnectionException) e;
                    }
                    throw new RuntimeException("Failed to delete views in data management meta data db.", e);
                }
            }

            @Override
            protected void handleSQLException(SQLException sqlException) {
                throw new RuntimeException("Failed to delete views.", sqlException);
            }

        };
        task.run();
    }

    private static void deleteView(String viewName, Connection connection) throws SQLException {
        String sql = "DROP VIEW %s";
        Statement stmt = connection.createStatement();
        if (viewExists(stmt, viewName)) {
            stmt.execute(String.format(sql, viewName));
            LOGGER.debug(String.format("Deleted view %s", viewName));
        }
        stmt.close();
    }

    private static void createTableDBVersionInfo(final Connection connection, final String dbVersion) {
        final Runnable task = new SQLRunnable(MAX_RETRIES) {

            @Override
            protected void sqlRun() throws SQLTransientConnectionException {
                try {
                    createTable(TABLE_DB_VERSION_INFO, connection);
                    setDBVersion(dbVersion, connection);
                } catch (SQLException e) {
                    if (e instanceof SQLTransientConnectionException) {
                        throw (SQLTransientConnectionException) e;
                    }
                    throw new RuntimeException("Failed to create table in data management meta data db.", e);
                }
            }

            @Override
            protected void handleSQLException(SQLException sqlException) {
                throw new RuntimeException("Failed to create version information table.", sqlException);
            }

        };
        task.run();
    }

    private static void setDBVersion(String dbVersion, Connection connection) {
        try {
            String sql = "INSERT INTO " + TABLE_DB_VERSION_INFO + " VALUES('%s')";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(String.format(sql, dbVersion));
            stmt.close();
            LOGGER.info(String.format("Set database version to %s", dbVersion));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set database version.", e);
        }
    }

    private static String getDBVersion(Connection connection) {
        try {
            String version = "unknown";
            String sql = SELECT + DB_VERSION + " FROM " + TABLE_DB_VERSION_INFO;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(String.format(sql));
            if (rs != null && rs.next()) {
                version = rs.getString(DB_VERSION);
                rs.close();
            }
            stmt.close();
            return version;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database version.", e);
        }
    }

    private static boolean tableExists(Statement statement, String tablename) throws SQLException {
        boolean isExistentTable = false;
        ResultSet rs = null;
        try {
            final String sql = SELECT + " tablename FROM SYS.SYSTABLES" //
                + WHERE + "tablename = " + APO + tablename + APO + AND + "tabletype = " + APO + "T" + APO;
            statement.setQueryTimeout(QUERY_EXECUTION_TIMEOUT);
            rs = statement.executeQuery(sql);
            isExistentTable = rs.next();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e = null;
            }
        }
        return isExistentTable;
    }

    private static boolean viewExists(Statement statement, String viewname) throws SQLException {
        boolean isExistentView = false;
        ResultSet rs = null;
        try {
            final String sql = SELECT + " tablename FROM SYS.SYSTABLES" //
                + WHERE + "tablename = " + APO + viewname + APO + AND + "tabletype = " + APO + "V" + APO;
            statement.setQueryTimeout(QUERY_EXECUTION_TIMEOUT);
            rs = statement.executeQuery(sql);
            isExistentView = rs.next();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e = null;
            }
        }
        return isExistentView;
    }

    private static void createIndexes(final Connection connection) {
        final Runnable task = new SQLRunnable(MAX_RETRIES) {

            @Override
            protected void sqlRun() throws SQLTransientConnectionException {
                createIndexesTrial(connection);
            }

            @Override
            protected void handleSQLException(SQLException sqlException) {
                throw new RuntimeException("Failed to create indexes.", sqlException);
            }

        };
        task.run();
    }

    private static void createIndexesTrial(Connection connection) throws SQLTransientConnectionException {
        try {
            createIndex(TABLE_DATA_REFERENCE, DATA_REFERENCE_KEY, connection);
            createIndex(TABLE_TIMELINE_INTERVAL, COMPONENT_RUN_ID, connection);
            createIndex(TABLE_BINARY_REFERENCE, BINARY_REFERENCE_KEY, connection);
            createIndex(TABLE_ENDPOINT_DATA, COMPONENT_RUN_ID, connection);
        } catch (SQLException e) {
            if (e instanceof SQLTransientConnectionException) {
                throw (SQLTransientConnectionException) e;
            }
            throw new RuntimeException("Failed to create indexes in meta data db.", e);
        }
    }

    private static void createIndex(String tableName, String columnName, Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = String.format("CREATE INDEX INDEX_%s_%s ON %s (%s)", tableName, columnName, tableName, columnName);
        LOGGER.debug(String.format("Creating index 'INDEX_%s_%s'", tableName, columnName));
        stmt.executeUpdate(sql);
        stmt.close();
    }

    private static void createTables(final Connection connection) {
        final Runnable task = new SQLRunnable(MAX_RETRIES) {

            @Override
            protected void sqlRun() throws SQLTransientConnectionException {
                createTablesTrial(connection);
            }

            @Override
            protected void handleSQLException(SQLException sqlException) {
                throw new RuntimeException("Failed to create tables.", sqlException);
            }

        };
        task.run();
    }

    private static void createTablesTrial(final Connection connection) throws SQLTransientConnectionException {
        try {
            createTable(TABLE_WORKFLOW_RUN, connection);
            createTable(TABLE_WORKFLOW_RUN_PROPERTIES, connection);
            createTable(TABLE_COMPONENT_INSTANCE, connection);
            createTable(TABLE_COMPONENT_INSTANCE_PROPERTIES, connection);
            createTable(TABLE_COMPONENT_RUN, connection);
            createTable(TABLE_COMPONENT_RUN_PROPERTIES, connection);
            createTable(TABLE_TIMELINE_INTERVAL, connection);
            createTable(TABLE_DATA_REFERENCE, connection);
            createTable(TABLE_BINARY_REFERENCE, connection);
            createTable(TABLE_ENDPOINT_INSTANCE, connection);
            createTable(TABLE_TYPED_DATUM, connection);
            createTable(TABLE_ENDPOINT_DATA, connection);
            createTable(REL_COMPONENTRUN_DATAREFERENCE, connection);
            createTable(REL_COMPONENTINSTANCE_DATAREFERENCE, connection);
            createTable(REL_WORKFLOWRUN_DATAREFERENCE, connection);
            createTable(REL_DATAREFERENCE_BINARYREFERENCE, connection);
        } catch (SQLException e) {
            if (e instanceof SQLTransientConnectionException) {
                throw (SQLTransientConnectionException) e;
            }
            throw new RuntimeException("Failed to create tables in data management meta data db.", e);
        }
    }

    private static void createTable(final String tableName, final Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = null;
        LOGGER.debug(String.format("Creating table '%s'", tableName));

        if (!tableExists(stmt, tableName)) {
            switch (tableName) {
            case TABLE_DB_VERSION_INFO:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableDbVersionInfo();
                break;
            case TABLE_WORKFLOW_RUN:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableWorkflowRun();
                break;
            case TABLE_WORKFLOW_RUN_PROPERTIES:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableWorkflowRunProperties();
                break;
            case TABLE_TIMELINE_INTERVAL:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableTimelineInterval();
                break;
            case TABLE_COMPONENT_INSTANCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableComponentInstance();
                break;
            case TABLE_COMPONENT_INSTANCE_PROPERTIES:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableComponentInstanceProperties();
                break;
            case TABLE_COMPONENT_RUN:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableComponentRun();
                break;
            case TABLE_COMPONENT_RUN_PROPERTIES:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableComponentRunProperties();
                break;
            case TABLE_TYPED_DATUM:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableTypedDatum();
                break;
            case TABLE_ENDPOINT_INSTANCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableEndpointInstance();
                break;
            case TABLE_ENDPOINT_DATA:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableEndpointData();
                break;
            case TABLE_DATA_REFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableDataReference();
                break;
            case TABLE_BINARY_REFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlTableBinaryReference();
                break;
            case REL_COMPONENTRUN_DATAREFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlRelationComponentRunDataReference();
                break;
            case REL_DATAREFERENCE_BINARYREFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlRelationDataReferenceBinaryReference();
                break;
            case REL_COMPONENTINSTANCE_DATAREFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlRelationComponentInstanceDataReference();
                break;
            case REL_WORKFLOWRUN_DATAREFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlRelationWorkflowRunDataReference();
                break;
            default:
                break;
            }
        }
        if (sql != null) {
            stmt.executeUpdate(sql);
        }
        stmt.close();
    }

    private static void createViews(final Connection connection) {
        final Runnable task = new SQLRunnable(MAX_RETRIES) {

            @Override
            protected void sqlRun() throws SQLTransientConnectionException {
                createViewsTrial(connection);
            }

            @Override
            protected void handleSQLException(SQLException sqlException) {
                throw new RuntimeException("Failed to create views.", sqlException);
            }

        };
        task.run();
    }

    private static void createViewsTrial(final Connection connection) throws SQLTransientConnectionException {
        try {
            createView(VIEW_COMPONENT_RUNS, connection);
            createView(VIEW_ENDPOINT_DATA, connection);
            createView(VIEW_COMPONENT_TIMELINE_INTERVALS, connection);
            createView(VIEW_WORKFLOWRUN_COMPONENTRUN, connection);
            createView(VIEW_WORKFLOWRUN_DATAREFERENCE, connection);
            createView(VIEW_WORKFLOWRUN_TYPEDDATUM, connection);
        } catch (SQLException e) {
            if (e instanceof SQLTransientConnectionException) {
                throw (SQLTransientConnectionException) e;
            }
            throw new RuntimeException("Failed to create views in data management meta data db.", e);
        }
    }

    private static void createView(final String viewName, final Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        String sql = null;
        LOGGER.debug(String.format("Creating view '%s'", viewName));

        if (!viewExists(stmt, viewName)) {
            switch (viewName) {
            case VIEW_COMPONENT_RUNS:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewComponentRuns();
                break;
            case VIEW_ENDPOINT_DATA:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewEndpointData();
                break;
            case VIEW_COMPONENT_TIMELINE_INTERVALS:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewComponentTimelineIntervals();
                break;
            case VIEW_WORKFLOWRUN_COMPONENTRUN:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewWorkflowRunComponentRun();
                break;
            case VIEW_WORKFLOWRUN_DATAREFERENCE:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewWorkflowRunDataReference();
                break;
            case VIEW_WORKFLOWRUN_TYPEDDATUM:
                sql = DerbyDatabaseSetupSqlStatements.getSqlViewWorkflowRunTypedDatum();
                break;
            default:
                break;
            }
        }
        if (sql != null) {
            stmt.executeUpdate(sql);
        }
        stmt.close();
    }

    /**
     * Manages the retries for sql statements.
     * 
     * @author Christian Weiss
     */
    private abstract static class SQLRunnable implements Runnable {

        private final int maxAttempts;

        private SQLRunnable() {
            this(3);
        }

        private SQLRunnable(final int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        @Override
        public final void run() {
            SQLException exception;
            int attemptCount = 0;
            do {
                ++attemptCount;
                exception = null;
                try {
                    sqlRun();
                    break;
                } catch (SQLTransientConnectionException e) {
                    exception = e;
                    waitForRetry();
                }
            } while (attemptCount <= maxAttempts);
            if (exception != null) {
                handleSQLException(exception);
            }
        }

        private void waitForRetry() {
            LOGGER.info("Waiting half o a second to retry SQL statement execution");
            final int halfOfASecond = 500;
            try {
                Thread.sleep(halfOfASecond);
            } catch (InterruptedException e) {
                LOGGER.warn("Waiting for retrying a failed SQL statement was interupted");
            }
        }

        protected abstract void sqlRun() throws SQLTransientConnectionException;

        protected abstract void handleSQLException(final SQLException sqlException);

    }

}
