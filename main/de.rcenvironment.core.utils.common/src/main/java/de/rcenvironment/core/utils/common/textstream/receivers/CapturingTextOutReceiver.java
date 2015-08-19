/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that captures all content in a text buffer.
 * Individual lines are concatenated with "\n".
 * 
 * @author Robert Mischke
 */
public class CapturingTextOutReceiver implements TextOutputReceiver {

    private StringBuilder buffer;

    /**
     * The text to append if the stream finished normally.
     */
    private String endOfStreamMarker;

    /**
     * @param endOfStreamMarker the text to append when the stream finished normally
     */
    public CapturingTextOutReceiver(String endOfStreamMarker) {
        this.endOfStreamMarker = endOfStreamMarker;
        buffer = new StringBuilder();
    }

    @Override
    public void onStart() {
        // NOP
    }

    @Override
    public synchronized void onFinished() {
        buffer.append(endOfStreamMarker);
    }

    @Override
    public synchronized void onFatalError(Exception e) {
        buffer.append("Exception:\n");
        buffer.append(e.toString());
    }

    @Override
    public synchronized void addOutput(String line) {
        buffer.append(line);
        buffer.append('\n');
    }

    public synchronized String getBufferedOutput() {
        return buffer.toString();
    }

}
