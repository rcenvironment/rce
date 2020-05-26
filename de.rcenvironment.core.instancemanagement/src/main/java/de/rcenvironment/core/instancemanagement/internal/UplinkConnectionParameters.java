/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

/**
 * provides a parameter object using the builder pattern to create a more readable passing of parameters used to create an uplink 
 * connection.
 * 
 * @author Marlon Schreter
 */
public class UplinkConnectionParameters {

    private String connectionId;

    private String host;

    private int port;

    private String clientId;

    private boolean gatewayFlag;

    private boolean autoStartFlag;

    private boolean autoRetryFlag;

    private String userName;

    private String password;

    /**
     * builder to build UplinkConnectionParameters objects. 
     */
    public static class UplinkConnectionBuilder {

        private UplinkConnectionParameters parameters = new UplinkConnectionParameters();

        /**
         * @param connectionIdParam connectionId parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder connectionId(String connectionIdParam) {
            parameters.setConnectionId(connectionIdParam);
            return this;
        }

        /**
         * @param hostParam host parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder host(String hostParam) {
            parameters.setHost(hostParam);
            return this;
        }

        /**
         * @param portParam port parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder port(int portParam) {
            parameters.setPort(portParam);
            return this;
        }

        /**
         * @param clientIdParam clientId parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder clientId(String clientIdParam) {
            parameters.setClientId(clientIdParam);
            return this;
        }

        /**
         * @param gatewayFlagParam gatewayFlag parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder gateway(boolean gatewayFlagParam) {
            parameters.setGatewayFlag(gatewayFlagParam);
            return this;
        }

        /**
         * @param autoStartFlagParam autoStartFlag parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder autoStart(boolean autoStartFlagParam) {
            parameters.setAutoStartFlag(autoStartFlagParam);
            return this;
        }

        /**
         * @param autoRetryFlagParam autoRetryFlag parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder autoRetry(boolean autoRetryFlagParam) {
            parameters.setAutoRetryFlag(autoRetryFlagParam);
            return this;
        }

        /**
         * @param userNameParam userName parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder userName(String userNameParam) {
            parameters.setUserName(userNameParam);
            return this;
        }

        /**
         * @param passwordParam password parameter
         * @return builder object to create a fluent interface
         */
        public UplinkConnectionBuilder password(String passwordParam) {
            parameters.setPassword(passwordParam);
            return this;
        }

        /**
         * 
         * @return UplinkConnectionParameters object containing all set parameters 
         */
        public UplinkConnectionParameters build() {
            return this.parameters;
        }
    }
    
    /**
     * 
     * @return UplinkConnectionBuilder object to build UplinkConnectionParameters
     */
    public static UplinkConnectionBuilder builder() {
        return new UplinkConnectionBuilder();
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean getGatewayFlag() {
        return gatewayFlag;
    }

    public void setGatewayFlag(boolean gatewayFlag) {
        this.gatewayFlag = gatewayFlag;
    }

    public boolean getAutoStartFlag() {
        return autoStartFlag;
    }

    public void setAutoStartFlag(boolean autoStartFlag) {
        this.autoStartFlag = autoStartFlag;
    }

    public boolean getAutoRetryFlag() {
        return autoRetryFlag;
    }

    public void setAutoRetryFlag(boolean autoRetryFlag) {
        this.autoRetryFlag = autoRetryFlag;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
