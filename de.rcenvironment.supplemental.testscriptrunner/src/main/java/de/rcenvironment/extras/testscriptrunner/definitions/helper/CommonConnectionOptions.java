/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.definitions.helper;

import java.util.Optional;

/**
 * contains option-keys and default values for connection options viable for at least two connection types.
 * 
 * @author Marlon Schroeter
 */
public class CommonConnectionOptions {


    private boolean autoStartFlag;
    
    private Optional<String> connectionName = Optional.empty();
    
    private String host;
    
    private Optional<Integer> port = Optional.empty();
    
    private int serverNumber;

    private String userName;
    
    private Optional<String> userRole = Optional.empty();

    CommonConnectionOptions() {
        setAutoStart(false);
        setHost(ConnectionOptionConstants.HOST_DEFAULT);
        setServerNumber(0);
        setUserName(ConnectionOptionConstants.USER_NAME_DEFAULT);
    }

    public boolean getAutoStart() {
        return autoStartFlag;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStartFlag = autoStart;
    }

    public Optional<String> getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = Optional.of(connectionName);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Optional<Integer> getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = Optional.of(port);
    }

    public int getServerNumber() {
        return serverNumber;
    }

    public void setServerNumber(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Optional<String> getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = Optional.of(userRole);
    }

}
