/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.extras.testscriptrunner.definitions.helper;

import java.util.Optional;

/**
 * contains option-keys and default values for connection options viable only for regular ssh types.
 * 
 * @author Marlon Schroeter
 */
public final class SSHConnectionOptions {
    
    private Optional<String> displayName = Optional.empty();
    
    private CommonConnectionOptions commonOptions = new CommonConnectionOptions();
    
    private SSHConnectionOptions() {
        setUserName("remote_access_user");
    }

    /**
     * builder class.
     * @author Marlon Schroeter
     */
    public static class Builder {

        private SSHConnectionOptions options;
        
        Builder() {
            options = new SSHConnectionOptions();
        }

        /**
         * @param connenctionNameValue connection name value
         * @return builder object to create a fluent interface
         */
        public Builder connectionName(String connenctionNameValue) {
            options.setConnectionName(connenctionNameValue);
            return this;
        }

        /**
         * @param displayNameValue display name value
         * @return builder object to create a fluent interface
         */
        public Builder displayName(String displayNameValue) {
            options.setDisplayName(displayNameValue);
            return this;
        }

        /**
         * @param hostValue host value
         * @return builder object to create a fluent interface
         */
        public Builder host(String hostValue) {
            options.setHost(hostValue);
            return this;
        }

        /**
         * @param portValue port value
         * @return builder object to create a fluent interface
         */
        public Builder port(int portValue) {
            options.setPort(portValue);
            return this;
        }

        /**
         * @param serverNumberValue server number value
         * @return builder object to create a fluent interface
         */
        public Builder serverNumber(int serverNumberValue) {
            options.setServerNumber(serverNumberValue);
            return this;
        }

        /**
         * @param userNameValue user Name value
         * @return builder object to create a fluent interface
         */
        public Builder userName(String userNameValue) {
            options.setUserName(userNameValue);
            return this;
        }

        /**
         * @param userRoleValue user role value
         * @return builder object to create a fluent interface
         */
        public Builder userRole(String userRoleValue) {
            options.setUserRole(userRoleValue);
            return this;
        }
        
        /**
         * @return build object
         */
        public SSHConnectionOptions build() {
            return options;
        }
    }

    private void setConnectionName(String connectionName) {
        commonOptions.setConnectionName(connectionName);
    }

    private void setDisplayName(String displayName) {
        this.displayName = Optional.of(displayName);
    }

    private void setHost(String host) {
        commonOptions.setHost(host);
    }

    private void setPort(int port) {
        commonOptions.setPort(port);
    }

    private void setServerNumber(int serverNumber) {
        commonOptions.setServerNumber(serverNumber);
    }
    
    private void setUserName(String userName) {
        commonOptions.setUserName(userName);
    }
    
    private void setUserRole(String userRole) {
        commonOptions.setUserRole(userRole);
    }

    /**
     * @return builder object used to build connection options
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public Optional<String> getConnectionName() {
        return commonOptions.getConnectionName();
    }
    
    public Optional<String> getDisplayName() {
        return this.displayName;
    }
    
    public Optional<Integer> getPort() {
        return commonOptions.getPort();
    }
    
    public String getHost() {
        return commonOptions.getHost();
    }

    public String getUserName() {
        return commonOptions.getUserName();
    }
    
    public Optional<String> getUserRole() {
        return commonOptions.getUserRole();
    }
    
    public int getServerNumber() {
        return commonOptions.getServerNumber();
    }

}
