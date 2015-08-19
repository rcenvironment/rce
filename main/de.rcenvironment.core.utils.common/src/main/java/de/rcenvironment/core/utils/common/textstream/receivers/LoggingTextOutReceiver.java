/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that logs all received events to an Apache
 * Commons logger, with an optional prefix set through the constructor.
 * 
 * This class is mainly intended for unit/integration test output. The log level is currently
 * hard-coded to "debug".
 * 
 * @author Robert Mischke
 * 
 */
public class LoggingTextOutReceiver implements TextOutputReceiver {

    private final Log log = LogFactory.getLog(getClass());

    private final String logPrefix;

    private TextOutputReceiver forwardTarget;

    /**
     * @param logPrefix the prefix for all log messages
     */
    public LoggingTextOutReceiver(String logPrefix) {
        this(logPrefix, null);
    }

    /**
     * @param logPrefix the prefix for all log messages
     * @param forwardTarget a {@link TextOutputReceiver} that all events are forwarded to after they
     *        have been logged; can be used to wrap this logger around the actual receiver
     */
    public LoggingTextOutReceiver(String logPrefix, TextOutputReceiver forwardTarget) {
        this.logPrefix = logPrefix;
        this.forwardTarget = forwardTarget;
    }

    @Override
    public void onStart() {
        log.debug(logPrefix + " -> Started reading");
        if (forwardTarget != null) {
            forwardTarget.onStart();
        }
    }

    @Override
    public void onFinished() {
        log.debug(logPrefix + " -> End of Stream");
        if (forwardTarget != null) {
            forwardTarget.onFinished();
        }
    }

    @Override
    public void onFatalError(Exception e) {
        log.debug(logPrefix + " -> Exception", e);
        if (forwardTarget != null) {
            forwardTarget.onFatalError(e);
        }
    }

    @Override
    public void addOutput(String line) {
        log.debug(logPrefix + ": " + line);
        if (forwardTarget != null) {
            forwardTarget.addOutput(line);
        }
    }

}
