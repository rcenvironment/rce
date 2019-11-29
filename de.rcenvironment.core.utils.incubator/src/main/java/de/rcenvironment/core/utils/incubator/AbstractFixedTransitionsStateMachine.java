/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * A variant of the AbstractStateMachine base class where all valid state transitions are defined by a n-by-2 matrix, where each matrix row
 * represents a valid state change. The "from" state must be in column 1 (array index 0), the "to" state in the second (array index 1).
 * 
 * @param <S> the Enum defining the states of this state machine
 * @param <E> the event type
 * 
 * @author Robert Mischke
 */
public abstract class AbstractFixedTransitionsStateMachine<S extends Enum<?>, E> extends AbstractStateMachine<S, E> {

    private final S[][] validTransitions;

    public AbstractFixedTransitionsStateMachine(S initialState, S[][] validTransitions) {
        super(initialState);
        this.validTransitions = validTransitions;
        // TODO validate array
    }

    protected boolean isStateChangeValid(S oldState, S newState) {
        // TODO optimize if necessary
        for (S[] validTransition : validTransitions) {
            if (validTransition[0] == oldState && validTransition[1] == newState) {
                return true;
            }
        }
        return false;
    }

}
