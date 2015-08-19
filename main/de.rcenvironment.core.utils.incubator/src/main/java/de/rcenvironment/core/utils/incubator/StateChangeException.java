/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Represents an error that prevented a transition in a state machine to happen.
 * 
 * @author Robert Mischke
 */
public class StateChangeException extends Exception {

    private static final long serialVersionUID = 6118361184020269248L;

    private final Enum<?> oldState;

    private final Enum<?> targetState;

    public StateChangeException(StateChangeException cause, Enum<?> oldState, Enum<?> targetState) {
        // re-wrap; do not add an extra layer to the stacktrace
        super(cause.getMessage(), cause.getCause());
        this.oldState = oldState;
        this.targetState = targetState;
    }

    public StateChangeException(String message, Throwable cause) {
        super(message, cause);
        oldState = null;
        targetState = null;
    }

    public StateChangeException(String message) {
        super(message);
        oldState = null;
        targetState = null;
    }

    public Enum<?> getOldState() {
        return oldState;
    }

    public Enum<?> getTargetState() {
        return targetState;
    }

    @Override
    public String toString() {
        if (oldState == null && targetState == null) {
            return getMessage();
        } else {
            return String.format("%s (old state: %s, target state: %s)", getMessage(), oldState, targetState);
        }
    }

}
