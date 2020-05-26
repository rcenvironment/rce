/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api.model;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Immutable holder of an average value of {@link Double} elements, with information about how many elements were merged into the average.
 * If that count is zero, the average should be considered undefined; by convention, the average is set to 0.0 in this case.
 *
 * @author Robert Mischke
 */
public final class AverageOfDoubles implements Serializable {

    private static final long serialVersionUID = -2224156137940227667L;

    private final int numSamples;

    private final double average;

    /**
     * Creates an instance with no data (numSamples = 0). By convention, the average value is arbitrarily set to 0.0.
     */
    public AverageOfDoubles() {
        this.numSamples = 0;
        this.average = 0.0;
    }

    public AverageOfDoubles(int numSamples, double average) {
        if (numSamples < 1) {
            throw new IllegalAccessError("Number of samples must be positive");
        }
        this.numSamples = numSamples;
        this.average = average;
    }

    @Override
    public String toString() {
        return StringUtils.format("Avg=%s, N=%d", getAverage(), getNumSamples());
    }

    public double getAverage() {
        return average;
    }

    public int getNumSamples() {
        return numSamples;
    }
}
