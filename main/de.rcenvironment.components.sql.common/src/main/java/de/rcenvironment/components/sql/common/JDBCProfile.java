/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import java.io.Serializable;

/**
 * JDBC profile information.
 * 
 * @author Christian Weiss
 */
public class JDBCProfile implements Serializable {

    private static final long serialVersionUID = 3795104345637987043L;

    private String label;

    private String host;

    private String database;

    private String user;

    private String password;

    private JDBCDriverInformation jdbc;

    /**
     * Returns the profile label.
     * 
     * @return the profile label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the profile label.
     * 
     * @param id the profile label
     */
    public void setLabel(String id) {
        this.label = id;
    }

    /**
     * Returns the database host.
     * 
     * @return the database host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the database host.
     * 
     * @param host the database host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the database name.
     * 
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the database name.
     * 
     * @param database the database name
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Returns the user name.
     * 
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user name.
     * 
     * @param user the user name.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * 
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the {@link JDBCDriverInformation}.
     * 
     * @return the {@link JDBCDriverInformation}.
     */
    public JDBCDriverInformation getJdbc() {
        return jdbc;
    }

    /**
     * Sets the {@link JDBCDriverInformation}.
     * 
     * @param jdbc the {@link JDBCDriverInformation}
     */
    public void setJdbc(JDBCDriverInformation jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + label;
    }

}
