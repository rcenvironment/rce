/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.common;

/**
 * Data class for a database connection.
 *
 * @author Oliver Seebach
 */
public class DatabaseConnection {

    private int id;
    
    private String name;

    private String type;

    private String host;

    private String port;

    private String state;

    private String username;

    private String password;

    private String scheme;

    public DatabaseConnection(int id, String name, String type, String host, String port, String state, String username, String password,
        String scheme) {
        super();
        this.id = id;
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.state = state;
        this.username = username;
        this.password = password;
        this.scheme = scheme;
    }

    public DatabaseConnection() {
        
    }

    public int getId() {
        return id;
    }

    
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

}
