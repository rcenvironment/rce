/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api.model;

import java.io.Serializable;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Immutable holder of system load data.
 *
 * @author Robert Mischke
 */
public final class SystemLoadInformation implements Serializable {

    private static final long serialVersionUID = 2948329066192197697L;

    private final AverageOfDoubles cpuLoadAvg;

    private final double cpuLoad;

    private final long availableRam;

    public SystemLoadInformation(AverageOfDoubles cpuLoadAvg, double cpuLoad, long availableRam) {
        this.cpuLoadAvg = cpuLoadAvg;
        this.cpuLoad = cpuLoad;
        this.availableRam = availableRam;
    }

    public AverageOfDoubles getCpuLoadAvg() {
        return cpuLoadAvg;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public long getAvailableRam() {
        return availableRam;
    }

    @Override
    public String toString() {
        return StringUtils.format("CPU load average: %f (n=%d), Available RAM: %s kiB", cpuLoadAvg.getAverage(), cpuLoadAvg.getNumSamples(),
            availableRam);
    }

}
