/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.helper;

/**
 * provides a parameter object using the builder pattern to create a more readable passing of parameters used to create an regular 
 * connection.
 * 
 * @author Marlon Schreter
 */
public class RegularConnectionParameters {

    private String connectionId;

    private String host;

    private int port;

    private boolean autoStartFlag;

    private int autoRetryInitDelay;
    
    private int autoRetryMaxDelay; 
    
    private float autoRetryDelayMultiplier;

    /**
     * builder to build RegularConnectionParameters objects. 
     */
    public static class RegularConnectionBuilder {

        private RegularConnectionParameters parameters = new RegularConnectionParameters();

        /**
         * @param connectionIdParam connectionId parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder connectionId(String connectionIdParam) {
            parameters.setConnectionId(connectionIdParam);
            return this;
        }

        /**
         * @param hostParam host parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder host(String hostParam) {
            parameters.setHost(hostParam);
            return this;
        }

        /**
         * @param portParam port parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder port(int portParam) {
            parameters.setPort(portParam);
            return this;
        }

        /**
         * @param autoStartFlagParam autoStartFlag parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder autoStartFlag(boolean autoStartFlagParam) {
            parameters.setAutoStartFlag(autoStartFlagParam);
            return this;
        }

        /**
         * @param autoRetryInitDelayParam autoRetryInitDelay parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder autoRetryInitDelay(int autoRetryInitDelayParam) {
            parameters.setAutoRetryInitDelay(autoRetryInitDelayParam);
            return this;
        }

        /**
         * @param autoRetryMaxDelayParam autoRetryMaxDelay parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder autoRetryMaxDelay(int autoRetryMaxDelayParam) {
            parameters.setAutoRetryMaxDelay(autoRetryMaxDelayParam);
            return this;
        }

        /**
         * @param autoRetryDelayMultiplierParam autoRetryMaxDelay parameter
         * @return builder object to create a fluent interface
         */
        public RegularConnectionBuilder autoRetryDelayMultiplier(float autoRetryDelayMultiplierParam) {
            parameters.setAutoRetryDelayMultiplier(autoRetryDelayMultiplierParam);
            return this;
        }

        /**
         * @return RegularConnectionParameters object containing all set parameters 
         */
        public RegularConnectionParameters build() {
            return this.parameters;
        }
    }
    
    /**
     * 
     * @return RegularConnectionBuilder object to build RegularConnectionParameters
     */
    public static RegularConnectionBuilder builder() {
        return new RegularConnectionBuilder();
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
    
    public boolean isAutoStart() {
        return autoStartFlag;
    }

    public void setAutoStartFlag(boolean autoStartFlag) {
        this.autoStartFlag = autoStartFlag;
    }

    public int getAutoRetryInitDelay() {
        return autoRetryInitDelay;
    }

    public void setAutoRetryInitDelay(int autoRetryInitDelay) {
        this.autoRetryInitDelay = autoRetryInitDelay;
    }

    public int getAutoRetryMaxDelay() {
        return autoRetryMaxDelay;
    }

    public void setAutoRetryMaxDelay(int autoRetryMaxDelay) {
        this.autoRetryMaxDelay = autoRetryMaxDelay;
    }

    public float getAutoRetryDelayMultiplier() {
        return autoRetryDelayMultiplier;
    }

    public void setAutoRetryDelayMultiplier(float autoRetryDelayMultiplier) {
        this.autoRetryDelayMultiplier = autoRetryDelayMultiplier;
    }
}
