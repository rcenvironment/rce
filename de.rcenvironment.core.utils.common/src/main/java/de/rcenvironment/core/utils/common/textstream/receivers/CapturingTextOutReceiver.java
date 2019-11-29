/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that captures all content in a text buffer. Individual lines are terminated with
 * "\n" by default; this can be customized.
 * 
 * @author Robert Mischke
 */
public class CapturingTextOutReceiver implements TextOutputReceiver {

    private static final int INITIAL_BUFFER_SIZE = 1024; // arbitrary

    private StringBuilder buffer;

    /**
     * The text to append if the stream finished normally.
     */
    private String endOfStreamSuffix;

    private String lineTerminator;

    /**
     * Default constructor: sets up a receiver with no prefix or suffix, and "\n" as the line separator.
     */
    public CapturingTextOutReceiver() {
        this(null, "\n", null);
    }

    /**
     * Constructor that allows setting custom values for prefix, line terminator, and suffix.
     * 
     * @param prefix the string to append before the received text lines; may be null to disable
     * @param lineTerminator the string to terminate each line (including the last) with
     * @param endOfStreamSuffix the string to append if (and only if) the end of the stream was reached normally; may be null to disable
     */
    public CapturingTextOutReceiver(String prefix, String lineTerminator, String endOfStreamSuffix) {
        this.lineTerminator = lineTerminator;
        this.endOfStreamSuffix = endOfStreamSuffix;

        buffer = new StringBuilder(INITIAL_BUFFER_SIZE);
        if (prefix != null) {
            buffer.append(prefix);
        }
    }

    @Override
    public void onStart() {
        // NOP
    }

    @Override
    public synchronized void onFinished() {
        if (endOfStreamSuffix != null) {
            buffer.append(endOfStreamSuffix);
        }
    }

    @Override
    public synchronized void onFatalError(Exception e) {
        buffer.append("Exception:");
        buffer.append(lineTerminator);
        buffer.append(e.toString());
    }

    @Override
    public synchronized void addOutput(String line) {
        buffer.append(line);
        buffer.append(lineTerminator);
    }

    public synchronized String getBufferedOutput() {
        return buffer.toString();
    }

}
