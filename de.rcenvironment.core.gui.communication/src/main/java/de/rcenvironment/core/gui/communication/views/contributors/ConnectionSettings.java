/*
 * Copyright 2006-2021 DLR, Germany
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
 * @author Kathrin Schaffert (refactoring)
 */
public class ConnectionSettings {

    private static final String COM = ",";

    private static final String EQUALS = " = ";

    private static final int INITIAL_DELAY_DEFAULT_VAL = 5;

    private static final int MAX_DELAY_DEFAULT_VAL = 300;

    private static final double DELAY_MULTIPLIER_DEFAULT_VAL = 1.5;

    private int autoRetryInitialDelay = INITIAL_DELAY_DEFAULT_VAL;

    private int autoRetryMaximumDelay = MAX_DELAY_DEFAULT_VAL;

    private double autoRetryDelayMultiplier = DELAY_MULTIPLIER_DEFAULT_VAL;

    private boolean connectOnStartup = true;

    private boolean useDefaultSettings = true;

    public int getAutoRetryInitialDelay() {
        return autoRetryInitialDelay;
    }

    public void setAutoRetryInitialDelay(int autoRetryInitialDelay) {
        this.autoRetryInitialDelay = autoRetryInitialDelay;
    }

    public int getAutoRetryMaximumDelay() {
        return autoRetryMaximumDelay;
    }

    public void setAutoRetryMaximumDelay(int autoRetryMaximumDelay) {
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
