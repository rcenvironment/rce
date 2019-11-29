/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.logging;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A special {@link RollingFileAppender} that logs all early log output normally, but also captures it in an internal buffer so it can be
 * fetched by a {@link EarlyLogInsertingAppender} later. This mechanism is necessary as the actual log file location is not yet known at
 * startup.
 *
 * @author Robert Mischke
 */
public class EarlyLogCapturingAppender extends RollingFileAppender {

    private static final Map<String, EarlyLogCapturingAppender> INSTANCES = new HashMap<>();

    private StringBuilder buffer = new StringBuilder();

    /**
     * @param type the injected log type, usually "debug" or "warnings"
     */
    public void setInternalType(String type) {
        // note: apparently, this property is only set once the first line is actually logged, so this may never be called!
        synchronized (INSTANCES) {
            INSTANCES.put(type, this);
        }
    }

    /**
     * Returns an instance by type and deletes it to release memory.
     * 
     * @param type the log type, usually "debug" or "warnings"
     * @return the instance; may be null if no early log output was written
     */
    public static EarlyLogCapturingAppender getAndDiscardInstance(String type) {
        synchronized (INSTANCES) {
            final EarlyLogCapturingAppender removedInstance = INSTANCES.remove(type); // release memory, and prevent accidental double usage
            return removedInstance;
        }
    }

    /**
     * @return the captured early log output as a single string, including newlines
     */
    public String getBufferedLogOutput() {
        synchronized (this) {
            final String output = buffer.toString();
            buffer = null; // prevent unnoticed loss of log output after this call
            return output;
        }
    }

    @Override
    protected void subAppend(LoggingEvent event) {
        super.subAppend(event);
        // note: this ignores the if(layout.ignoresThrowable()) distinction in the super.subAppend(),
        // so the output may not be exactly the same in error cases. if this turns out to be a problem,
        // that code could be ported. -- misc_ro
        final String renderedLine = layout.format(event);
        synchronized (this) {
            if (buffer != null) {
                buffer.append(renderedLine);
            } else {
                System.err.println("Received log output after the buffered early log output was fetched for transfer: " + renderedLine);
            }
        }
    }
}
