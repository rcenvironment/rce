/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.RollingFileAppender;

/**
 * A {@link RollingFileAppender} that fetches the {@link EarlyLogCapturingAppender} instance of the same type (e.g. "warnings" vs. "debug")
 * and inserts the early log output captured by that instance at the beginning of the log file.
 *
 * @author Robert Mischke
 */
public class EarlyLogInsertingAppender extends RollingFileAppender {

    private String earlyLogFileLocation;

    private String type;

    public void setEarlyLogFileLocation(String location) {
        this.earlyLogFileLocation = location;
    }

    public void setInternalType(String value) {
        this.type = value;
    }

    @Override
    protected void writeHeader() {
        super.writeHeader();

        // fetch output from corresponding instance
        final EarlyLogCapturingAppender earlyLogCapture = EarlyLogCapturingAppender.getAndDiscardInstance(type);
        if (earlyLogCapture == null) {
            // no early log output was generated, in which case the early log appender was not instantiated at all
            return;
        }

        final String bufferedLogOutput = earlyLogCapture.getBufferedLogOutput();
        if (!bufferedLogOutput.isEmpty()) {
            // prepend to own log file
            this.qw.write(bufferedLogOutput);
            this.qw.flush();
        }

        // after successfully appending and flushing the early log content, try to delete the early log file
        try {
            Files.delete(Paths.get(earlyLogFileLocation));
        } catch (IOException e) {
            this.qw.write(
                "Failed to delete the early log capture file \"" + earlyLogFileLocation + "\": " + e.toString() + System.lineSeparator());
        }
    };
}
