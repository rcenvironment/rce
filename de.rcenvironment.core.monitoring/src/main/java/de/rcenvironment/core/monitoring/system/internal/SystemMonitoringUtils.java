/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

/**
 * Utility methods.
 * 
 * @author Robert Mischke
 */
public final class SystemMonitoringUtils {

    /**
     * One hundred percent, as represented internally.
     */
    public static final double ONE_HUNDRED_PERCENT_CPU_VALUE = 1.0;

    private SystemMonitoringUtils() {}

    /**
     * @param input a percentage value
     * @return NaN if the value was NaN, 0% if <0%, 100% if >100%, otherwise the original value
     */
    public static double clampToPercentageOrNAN(double input) {
        // apply fallbacks in case the input is invalid or "unusual"
        if (Double.isNaN(input)) {
            return Double.NaN;
        } else if (input < 0.0) {
            return 0.0;
        } else if (input > ONE_HUNDRED_PERCENT_CPU_VALUE) {
            return ONE_HUNDRED_PERCENT_CPU_VALUE;
        } else {
            return input;
        }
    }

}
