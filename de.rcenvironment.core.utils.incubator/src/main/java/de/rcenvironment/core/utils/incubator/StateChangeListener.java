/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Listener for state transitions in a state machine. Each listener can veto the state change by
 * throwing a {@link StateChangeException}. In this case, the state change will not be performed,
 * but other listeners may already have been executed.
 * 
 * @param <T> the enum type representing the state machine states
 * 
 * @author Robert Mischke
 */
public interface StateChangeListener<T extends Enum<?>> {

    /**
     * Reports a state transition attempt.
     * 
     * @param oldState the current/old state of the state machine
     * @param newState the new intended state, which will be set if no listener throws an exception
     * @throws StateChangeException an exception that signals an error that should prevent the state
     *         transition from happening
     */
    void onStateChange(T oldState, T newState) throws StateChangeException;
}
