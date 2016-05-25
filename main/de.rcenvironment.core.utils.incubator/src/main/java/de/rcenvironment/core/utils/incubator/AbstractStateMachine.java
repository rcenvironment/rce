/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;

/**
 * An abstract base class to simplify the creation of state machine implementations.
 * 
 * @param <S> the Enum defining the states of this state machine
 * @param <E> the type representing events posted to this state maching
 * @author Robert Mischke
 */
public abstract class AbstractStateMachine<S extends Enum<?>, E> {

    private S currentState;

    private AsyncOrderedExecutionQueue eventQueue;

    public AbstractStateMachine(S initialState) {
        this.currentState = initialState;
        this.eventQueue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, SharedThreadPool.getInstance());
    }

    /**
     * Asynchronously post an event to the state machine. Events are processed in order, and each event may or may not change the state
     * machine's state.
     * 
     * @param event the event to post, which the state machine's logic will process asynchronously
     */
    public void postEvent(final E event) {
        eventQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                processEventInternal(event);
            }
        });
    }

    /**
     * Triggers a state transition. Setting the new state is always a blocking operation. Whether the actions caused by the state change are
     * blocking or asynchronous is left to the subclasses' implementations.
     * 
     * @param newState the state this state machine should transition to
     * @throws StateChangeException if the state change failed; the internal state will be unchanged
     */
    private synchronized void processEventInternal(E event) {
        try {
            S newState = processEvent(currentState, event);
            if (newState == null || newState == currentState) {
                // no state change requested; done
                return;
            }
            try {
                checkProposedStateChange(currentState, newState);
            } catch (StateChangeException e) {
                // re-wrap to add old and new state information
                throw new StateChangeException(e, currentState, newState);
            }
            S oldState = currentState;
            currentState = newState;
            // internal state change handler first...
            onStateChanged(oldState, newState);
        } catch (StateChangeException e) {
            onStateChangeException(event, e);
        }
    }

    public synchronized S getState() {
        return currentState;
    }

    protected abstract S processEvent(S oldState, E event) throws StateChangeException;

    protected void checkProposedStateChange(S oldState, S newState) throws StateChangeException {}

    protected void onStateChanged(S oldState, S newState) {}

    protected abstract void onStateChangeException(E event, StateChangeException e);

}
