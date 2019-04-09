/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream;

/**
 * An interface for receivers of line-based text output, for example the stdout/stderr output of
 * invoked programs.
 * 
 * @author Robert Mischke
 * 
 */
public interface TextOutputReceiver {

    /**
     * Initialization event; guaranteed to be fired before any {@link #addOutput(String)} calls.
     */
    void onStart();

    /**
     * Provides the next line of text, as received from {@link BufferedReader#readLine()}.
     * 
     * @param line the received line
     */
    void addOutput(String line);

    /**
     * Fired when the end of the stream is reached normally. This event is mutually exclusive with
     * {@link #onFatalError(Exception)}.
     */
    void onFinished();

    /**
     * Fired when an exception occurred during reading. Typical reasons are standard stream
     * {@link IOException}s, or an {@link InterruptedException} if the reading thread was
     * interrupted. This event is mutually exclusive with {@link #onFinished()}.
     * 
     * @param e the Exception that has occurred
     */
    void onFatalError(Exception e);

}
