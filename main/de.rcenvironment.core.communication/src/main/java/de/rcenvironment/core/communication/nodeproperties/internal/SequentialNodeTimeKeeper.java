/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.internal;

/**
 * This class is intended to provide a per-node monotonously-increasing sequence counter. It is
 * invalidated by calling {@link #invalidateAndGet()}; as long as this method is not called, the
 * counter remains at the same value.
 * 
 * As long as the system wall time is not set back to an earlier point in time, sequence numbers
 * generated after the restart of a node should be greater than the numbers generated before the
 * restart. This is important for protocol assumptions regarding network timestamps.
 * 
 * @author Robert Mischke
 */
public class SequentialNodeTimeKeeper {

    private long currentValue;

    public SequentialNodeTimeKeeper() {
        currentValue = getSystemTime();
    }

    /**
     * @return the currenc, unmodified sequence number
     */
    public synchronized long getCurrent() {
        return currentValue;
    }

    /**
     * @return the incremented sequence number
     */
    public synchronized long invalidateAndGet() {
        long newValue = getSystemTime();
        if (newValue <= currentValue) {
            // ensure an increase even if the system time did not change since the last update
            newValue = currentValue + 1;
        }
        currentValue = newValue;
        return currentValue;
    }

    /**
     * Made overridable for unit tests.
     */
    protected long getSystemTime() {
        return System.currentTimeMillis();
    }

}
