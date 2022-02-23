/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.helper;

import java.util.Optional;

/**
 * contains option-keys and default values for connection options viable only for regular connection types.
 * 
 * @author Marlon Schroeter
 */
public class RegularConnectionOptions {

    private int autoRetryInitialDelay;

    private int autoRetryMaxDelay;

    private float autoRetryDelayMultiplier;
    
    private boolean isRelay;

    private CommonConnectionOptions commonOptions = new CommonConnectionOptions();

    RegularConnectionOptions() {
        setAutoRetryInitialDelay(ConnectionOptionConstants.AUTO_RETRY_INIT_DELAY_DEFAULT);
        setAutoRetryMaxDelay(ConnectionOptionConstants.AUTO_RETRY_MAX_DELAY_DEFAULT);
        setAutoRetryDelayMultiplier(ConnectionOptionConstants.AUTO_RETRY_DELAY_MULTIPLIER_DEFAULT);
        setRelay(false);
    }

    /**
     * builder class.
     * 
     * @author Marlon Schroeter
     */
    public static class Builder {

        private RegularConnectionOptions options = new RegularConnectionOptions();

        Builder() {
            options = new RegularConnectionOptions();
        }

        /**
         * @param autoStartValue auto start value
         * @return builder object to create a fluent interface
         */
        public Builder autoStart(boolean autoStartValue) {
            options.setAutoStart(autoStartValue);
            return this;
        }

        /**
         * @param autoRetryDelayMultiplierValue auto retry delay multiplier value
         * @return builder object to create a fluent interface
         */
        public Builder autoRetryDelayMultiplier(float autoRetryDelayMultiplierValue) {
            options.setAutoRetryDelayMultiplier(autoRetryDelayMultiplierValue);
            return this;
        }

        /**
         * @param autoRetryInitialDelayValue initial auto retry delay value
         * @return builder object to create a fluent interface
         */
        public Builder autoRetryInitialDelay(int autoRetryInitialDelayValue) {
            options.setAutoRetryInitialDelay(autoRetryInitialDelayValue);
            return this;
        }

        /**
         * @param autoRetryMaxDelayValue maximum auto retry delay value
         * @return builder object to create a fluent interface
         */
        public Builder autoRetryMaxDelay(int autoRetryMaxDelayValue) {
            options.setAutoRetryMaxDelay(autoRetryMaxDelayValue);
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
         * @param isRelayFlag relay flag value
         * @return builder object to create a fluent interface
         */
        public Builder relay(Boolean isRelayFlag) {
            options.setRelay(isRelayFlag);
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
         * @return build object
         */
        public RegularConnectionOptions build() {
            return options;
        }
    }

    private void setAutoRetryDelayMultiplier(float autoRetryDelayMultiplier) {
        this.autoRetryDelayMultiplier = autoRetryDelayMultiplier;
    }

    private void setAutoRetryInitialDelay(int autoRetryInitialDelay) {
        this.autoRetryInitialDelay = autoRetryInitialDelay;
    }

    private void setAutoRetryMaxDelay(int autoRetryMaxDelay) {
        this.autoRetryMaxDelay = autoRetryMaxDelay;
    }

    private void setAutoStart(boolean autoStartFlag) {
        commonOptions.setAutoStart(autoStartFlag);
    }

    private void setConnectionName(String connectionName) {
        commonOptions.setConnectionName(connectionName);
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

    /**
     * @return builder object used to build connection options
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getAutoRetryInitialDelay() {
        return autoRetryInitialDelay;
    }

    public int getAutoRetryMaxDelay() {
        return autoRetryMaxDelay;
    }

    public float getAutoRetryDelayMultiplier() {
        return autoRetryDelayMultiplier;
    }
    
    public Optional<String> getConnectionName() {
        return commonOptions.getConnectionName();
    }
    
    public String getHost() {
        return commonOptions.getHost();
    }

    public Optional<Integer> getPort() {
        return commonOptions.getPort();
    }

    public int getServerNumber() {
        return commonOptions.getServerNumber();
    }

    public boolean getAutoStartFlag() {
        return commonOptions.getAutoStart();
    }

    public boolean isRelay() {
        return isRelay;
    }

    public void setRelay(boolean isRelayFlag) {
        this.isRelay = isRelayFlag;
    }

}
