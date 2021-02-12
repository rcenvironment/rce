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
 * contains option-keys and default values for connection options viable only for uplink connection types.
 * 
 * @author Marlon Schroeter
 */
public final class UplinkConnectionOptions {

    private boolean autoRetryFlag;
    
    private String clientId;

    private boolean gatewayFlag;
    
    private String password;

    private CommonConnectionOptions commonOptions = new CommonConnectionOptions();

    private UplinkConnectionOptions() {
        setAutoRetry(false);
        setGateway(false);
        setUserName(ConnectionOptionConstants.USER_NAME_DEFAULT);
        setPassword(ConnectionOptionConstants.USER_NAME_DEFAULT);
    }

    /**
     * builder class.
     * @author Marlon Schroeter
     */
    public static class Builder {
        
        private UplinkConnectionOptions options;

        Builder() {
            options = new UplinkConnectionOptions();
        }

        /**
         * 
         * @param autoRetryValue auto retry flag
         * @return builder object to create a fluent interface
         */
        public Builder autoRetry(boolean autoRetryValue) {
            options.setAutoRetry(autoRetryValue);
            return this;
        }

        /**
         * 
         * @param autoStartValue auto start flag
         * @return builder object to create a fluent interface
         */
        public Builder autoStart(boolean autoStartValue) {
            options.setAutoStart(autoStartValue);
            return this;
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
         * @param clientIdValue client ID value
         * @return builder object to create a fluent interface
         */
        public Builder clientId(String clientIdValue) {
            options.setClientId(clientIdValue);
            return this;
        }

        /**
         * 
         * @param gatewayValue gateway flag value
         * @return builder object to create a fluent interface
         */
        public Builder gateway(boolean gatewayValue) {
            options.setGateway(gatewayValue);
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
         * @param passwordValue password value
         * @return builder object to create a fluent interface
         */
        public Builder password(String passwordValue) {
            options.setPassword(passwordValue);
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
         * @param userNameValue user name value
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
        public UplinkConnectionOptions build() {
            return options;
        }
    }

    private void setAutoRetry(boolean autoRetry) {
        this.autoRetryFlag = autoRetry;
    }

    private void setAutoStart(boolean autoStart) {
        commonOptions.setAutoStart(autoStart);
    }

    private void setClientId(String clientId) {
        this.clientId = clientId;
    }

    private void setConnectionName(String connectionName) {
        commonOptions.setConnectionName(connectionName);
    }

    private void setGateway(boolean gateway) {
        this.gatewayFlag = gateway;
    }

    private void setHost(String host) {
        commonOptions.setHost(host);
    }

    private void setPassword(String password) {
        this.password = password;
    }

    private void setPort(int port) {
        commonOptions.setPort(port);
    }
    
    private void setUserName(String userName) {
        commonOptions.setUserName(userName);
    }

    private void setUserRole(String userRole) {
        commonOptions.setUserRole(userRole);
    }
    
    private void setServerNumber(int serverNumber) {
        commonOptions.setServerNumber(serverNumber);
    }

    /**
     * @return builder object used to build connection options
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean getAutoRetry() {
        return autoRetryFlag;
    }

    public boolean getAutoStart() {
        return commonOptions.getAutoStart();
    }

    public String getClientId() {
        return clientId;
    }
    
    public Optional<String> getConnectionName() {
        return commonOptions.getConnectionName();
    }
    
    public boolean getGateway() {
        return gatewayFlag;
    }
    
    public String getHost() {
        return commonOptions.getHost();
    }

    public String getPassword() {
        return password;
    }
    
    public Optional<Integer> getPort() {
        return commonOptions.getPort();
    }
    
    public int getServerNumber() {
        return commonOptions.getServerNumber();
    }

    public String getUserName() {
        return commonOptions.getUserName();
    }

    public Optional<String> getUserRole() {
        return commonOptions.getUserRole();
    }
}
