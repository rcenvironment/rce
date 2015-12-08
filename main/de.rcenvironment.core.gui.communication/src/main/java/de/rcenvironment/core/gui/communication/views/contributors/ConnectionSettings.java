/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

/**
 * Entity class for connection settings.
 *
 * @author Goekhan Guerkan
 */
public class ConnectionSettings {

    private static final String COM = ",";

    private int autoRetryInitialDelay;

    private int autoRetryMaximumDelay;

    private double autoRetryDelayMultiplier;

    private boolean connectOnStartup = true;

    public ConnectionSettings() {

        final int autoRetryDefault = 0;
        final int autoRetryMaxi = 0;
        final double autoRetryDelayMulti = 0;

        autoRetryInitialDelay = autoRetryDefault;
        autoRetryMaximumDelay = autoRetryMaxi;
        autoRetryDelayMultiplier = autoRetryDelayMulti;
    }

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

        return "autoRetryInitialDelay= " + autoRetryInitialDelay + COM + " autoRetryMaximumDelay= " + autoRetryMaximumDelay + COM
            + " autoRetryDelayMultiplier= " + autoRetryDelayMultiplier;
    }

   /**
    * 
    * @param initialDelay settingsparameter
    * @param maximumDelay settingsparameter
    * @param multiplier settingsparameter
    * @return String for the settings Textfield.
    */
    public String createStringForsettings(int initialDelay, int maximumDelay, double multiplier) {

        return "autoRetryInitialDelay= " + initialDelay + COM + " autoRetryMaximumDelay= " + maximumDelay + COM
            + " autoRetryDelayMultiplier= " + multiplier;
    }

    public boolean isConnectOnStartup() {
        return connectOnStartup;
    }

    public void setConnectOnStartup(boolean connectOnStartup) {
        this.connectOnStartup = connectOnStartup;
    }

}
