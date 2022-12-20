/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

/**
 * Entity class for connection settings.
 *
 * @author Goekhan Guerkan
 * @author Kathrin Schaffert
 */
public class ConnectionSettings {

    private static final String COM = ",";

    private static final String EQUALS = " = ";

    static final long INITIAL_DELAY_DEFAULT_VAL = 5;

    static final long MAX_DELAY_DEFAULT_VAL = 300;

    static final double DELAY_MULTIPLIER_DEFAULT_VAL = 1.5;

    private long autoRetryInitialDelay = INITIAL_DELAY_DEFAULT_VAL;

    private long autoRetryMaximumDelay = MAX_DELAY_DEFAULT_VAL;

    private double autoRetryDelayMultiplier = DELAY_MULTIPLIER_DEFAULT_VAL;

    private boolean connectOnStartup = true;

    private boolean useDefaultSettings = true;
    
    private boolean autoRetry = true;

    public long getAutoRetryInitialDelay() {
        return autoRetryInitialDelay;
    }

    public void setAutoRetryInitialDelay(long autoRetryInitialDelay) {
        this.autoRetryInitialDelay = autoRetryInitialDelay;
    }

    public long getAutoRetryMaximumDelay() {
        return autoRetryMaximumDelay;
    }

    public void setAutoRetryMaximumDelay(long autoRetryMaximumDelay) {
        this.autoRetryMaximumDelay = autoRetryMaximumDelay;
    }

    public double getAutoRetryDelayMultiplier() {
        return autoRetryDelayMultiplier;
    }

    public void setAutoRetryDelayMultiplier(double autoRetryDelayMultiplier) {
        this.autoRetryDelayMultiplier = autoRetryDelayMultiplier;
    }

    public String getSettingsString() {

        return AbstractNetworkConnectionDialog.AUTO_RETRY_INITIAL_DELAY_STR + EQUALS + autoRetryInitialDelay + COM
            + AbstractNetworkConnectionDialog.AUTO_RETRY_MAXI_DELAY_STR + EQUALS + autoRetryMaximumDelay + COM
            + AbstractNetworkConnectionDialog.AUTO_RETRY_DELAY_MULTIPL + EQUALS + autoRetryDelayMultiplier;
    }

    public boolean isConnectOnStartup() {
        return connectOnStartup;
    }

    public void setConnectOnStartup(boolean connectOnStartup) {
        this.connectOnStartup = connectOnStartup;
    }
    
    public boolean isAutoRetry() {
        return autoRetry;
    }

    
    public void setAutoRetry(boolean autoRetry) {
        this.autoRetry = autoRetry;
    }

    boolean isUseDefaultSettings() {
        return useDefaultSettings;
    }

    void setUseDefaultSettings(boolean useDefaultSettings) {
        this.useDefaultSettings = useDefaultSettings;
    }

    public void setDefaultValues() {
        autoRetryInitialDelay = INITIAL_DELAY_DEFAULT_VAL;
        autoRetryMaximumDelay = MAX_DELAY_DEFAULT_VAL;
        autoRetryDelayMultiplier = DELAY_MULTIPLIER_DEFAULT_VAL;
    }

}
