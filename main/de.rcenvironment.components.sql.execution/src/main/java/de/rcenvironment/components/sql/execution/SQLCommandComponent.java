/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.execution;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Component to execute commands in a SQL database.
 * 
 * @author Christian Weiss
 */
public class SQLCommandComponent extends AbstractSQLComponent {

    @Override
    protected void validate() {
        // do nothing
    }

    @Override
    protected void runSql(final String sql) {
        // FIXME - split needs to be sensitive to in-string semicolons
        // furthermore this should be moved to AbstractSQLComponent to be applied to all configured sql statements
        final String[] sqlStatements = sql.split(";");
        for (final String sqlStatement : sqlStatements) {
            if (!sqlStatement.trim().isEmpty()) {
                runSqlStatement(sqlStatement);
            }
        }
    }

    private void runSqlStatement(final String sql) {
        // declare the jdbc assets to be able to close them in the finally-clause
        Connection jdbcConnection = null;
        Statement statement = null;
        // exectue the jdbc stuff
        try {
            jdbcConnection = getConnection();
            // start transaction
            final boolean autoCommit = jdbcConnection.getAutoCommit();
            try {
                jdbcConnection.setAutoCommit(false);
                statement = jdbcConnection.createStatement();
                final boolean hasResultSet = statement.execute(sql);
                ResultSet resultSet = null;
                if (hasResultSet) {
                    resultSet = statement.getResultSet();
                }
                // commit transaction
                jdbcConnection.commit();
                if (resultSet != null) {
                    distributeResults(resultSet);
                }
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

}
