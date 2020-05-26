/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * InvocationHandler for pooled connections.
 * 
 * @author Christian Weiss
 */
final class PooledConnectionInvocationHandler implements InvocationHandler {

    private final Connection connection;

    private int count = 0;

    PooledConnectionInvocationHandler(final Connection connection) {
        this.connection = connection;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        boolean execute = true;
        if (method.getName().equals("increment")) {
            count++;
            execute = false;
        } else if (method.getName().equals("decrement")) {
            count--;
            execute = false;
        } else if (method.getName().equals("close")) {
            count--;
            if (count > 0) {
                execute = false;
            }
        }
        if (count == 0) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close connection.", e);
            }
        }
        if (execute) {
            try {
                return method.invoke(connection, args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(StringUtils.format("Failed to execute database query. %s", e.getMessage()));
            }
        }
        return null;
    }
}
