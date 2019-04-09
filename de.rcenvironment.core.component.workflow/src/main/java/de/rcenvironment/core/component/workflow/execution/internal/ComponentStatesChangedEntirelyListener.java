/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Set;

import de.rcenvironment.core.component.execution.api.ComponentState;

/**
 * Callback interface. Called on certain component lifecycle changes.
 * 
 * @author Doreen Seider
 * 
 * Note: Communication between a workflow controller and its components is done with RPCs. E.g., if a workflow controller wants all
 * of its components to start, it calls their start method and waits for certain callbacks which announce certain states. It
 * continues not before all of the states are reached.
 * This class is used to get notified if the components reached certain states, e.g. all of them are finished.
 * 
 * In a future implementation I would consider to move from the RPC-based approach to a messaging-based approach for the
 * workflow-component communication. I would expect to get cleaner code without so many waiting implemented in the workflow engine
 * code which is error-prone. --seid_do
 */
public interface ComponentStatesChangedEntirelyListener {

    /**
     * Called if all relevant components are in {@link ComponentState#PREPARED}.
     */
    void onComponentStatesChangedCompletelyToPrepared();

    /**
     * Called if all relevant components are in {@link ComponentState#PAUSED}.
     */
    void onComponentStatesChangedCompletelyToPaused();
    
    /**
     * Called if all relevant components running are in any final {@link ComponentState}.
     */
    void onComponentStatesChangedCompletelyToResumed();
    
    /**
     * Called if all relevant components are in {@link ComponentState#FINISHED} or in {@link ComponentState#FINISHED_WITHOUT_EXECUTION}.
     */
    void onComponentStatesChangedCompletelyToFinished();

    /**
     * Called if all relevant components are in {@link ComponentState#DISPOSED}.
     */
    void onComponentStatesChangedCompletelyToDisposed();
    
    /**
     * Called if all relevant components are in any final {@link ComponentState}.
     */
    void onComponentStatesChangedCompletelyToAnyFinalState();
    
    /**
     * Called if last console row was received from all components.
     */
    void onLastConsoleRowsReceived();
    
    /**
     * Called if component(s) is/are lost.
     * 
     * @param componentsLost execution identifier of lost components
     */
    void onComponentsLost(Set<String> componentsLost);

}
