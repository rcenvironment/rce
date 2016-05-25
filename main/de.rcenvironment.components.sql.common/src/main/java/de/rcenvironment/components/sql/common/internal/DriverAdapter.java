/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common.internal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A driver adapter to forward requests to another {@link Driver} implementation.
 * 
 * @author Christian Weiss
 */
public class DriverAdapter implements Driver {

    private final Driver driver;

    public DriverAdapter(final Driver driver) {
        this.driver = driver;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return driver.connect(url, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    /**
     * This method is necessary for Java 7 compatibility. It MUST NOT have an @Override annotation,
     * otherwise it will break on Java 6. -- misc_ro
     * 
     * @see java.sql.Driver#getParentLogger()
     * 
     * @return nothing; this method will always throw an exception (see parent JavaDoc)
     * @throws SQLFeatureNotSupportedException always thrown to signal that java.util.logging is not
     *         used
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // see java.sql.Driver#getParentLogger()
        throw new SQLFeatureNotSupportedException("Not using java.util.logging");
    }

}
