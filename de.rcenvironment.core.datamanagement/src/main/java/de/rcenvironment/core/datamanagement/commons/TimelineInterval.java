/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.TimelineIntervalType;

/**
 * Identifier for TimelineInterval.
 * 
 * @author Jan Flink
 */
public class TimelineInterval implements Serializable, Comparable<TimelineInterval> {

    private static final long serialVersionUID = -1791262306434857942L;

    protected final Long starttime;

    protected final Long endtime;

    protected final TimelineIntervalType type;

    public TimelineInterval(TimelineIntervalType type, Long starttime, Long endtime) {
        this.type = type;
        this.starttime = starttime;
        this.endtime = endtime;
    }

    public Long getStartTime() {
        return starttime;
    }

    public Long getEndTime() {
        return endtime;
    }

    public TimelineIntervalType getType() {
        return type;
    }

    @Override
    public int compareTo(TimelineInterval timelineInterval) {
        return getStartTime().compareTo(timelineInterval.getStartTime());
    }
}
