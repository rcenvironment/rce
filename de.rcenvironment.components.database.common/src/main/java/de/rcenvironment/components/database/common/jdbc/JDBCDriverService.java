/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.common.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Set;


/**
 * JDBC Driver Service.
 *
 * @author Oliver Seebach
 */
public interface JDBCDriverService {

    /**
     * Return the registered JDBC drivers that are registered at bundle activation.
     * 
     * @return The registered JDBC drivers.
     */
    Set<JDBCDriverInformation> getRegisteredJDBCDrivers();
    
    /**
     * Return the driver for the given url.
     * 
     * @param url The url for the driver.
     * @return the driver for the given url.
     * @throws SQLException if some SQL exception appears
     */
    Driver getDriverForURL(String url) throws SQLException;

    /**
     * Creates and returns a jdbc connection for the given driver url, username and password.
     * 
     * @param url the url
     * @param databaseUser the user
     * @param databasePassword the password
     * @return the jdbc connection
     * @throws SQLException if some SQL exception appears
     */
    Connection getConnectionWithCredentials(String url, String databaseUser, String databasePassword) throws SQLException;
    
}
