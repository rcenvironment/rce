/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.registration.api.Registerable;

/**
 * Interface for workflow components.
 * 
 * @author Doreen Seider
 * 
 * Note: I think this interface is well settled in general except some methods that were added over time which I am not happy with:
 * 
 * - {@link #treatStartAsComponentRun()}: It passes the concept of component runs to workflow components which should only be aware
 * of starting and processing inputs.
 * 
 * - {@link #handleVerificationToken(String)} and {@link #completeStartOrProcessInputsAfterVerificationDone()}: Manual output
 * verification can only be configured for integrated tool. So, I don't like it to be present in this general {@link Component}
 * interface. Technical reasons (workflow engine needed to interact with the tool integrator component that is just a
 * {@link Component} implementation) made it unavoidable at the time of the implementation.
 * 
 * --seid_do
 */
public interface Component extends Registerable {

    /**
     * The final states a component can have. Used to pass into {@link Component#tearDown(FinalComponentState)} method.
     * 
     * @author Doreen Seider
     */
    enum FinalComponentState {

        FINISHED,
        CANCELLED,
        FAILED,
        RESULTS_REJECTED,
    }

    /**
     * Injects the {@link ComponentContext}.
     * 
     * @param componentContext interface to the workflow engine. Used it to read component configuration, read inputs, write outputs, etc.
     */
    void setComponentContext(ComponentContext componentContext);

    /**
     * Called at workflow start before {@link Component#start(ComponentContext)}. Used to indicate if
     * {@link Component#start(ComponentContext)} must be handled as a component execution.
     * 
     * @return <code>true</code> if {@link Component#start(ComponentContext)} will perform execution, otherwise <code>false</code>
     */
    boolean treatStartAsComponentRun();

    /**
     * Called at workflow start. This method is called on all components of a workflow in parallel. Initialization stuff goes here as well
     * as component execution if it doesn't require any input values.
     * 
     * @throws ComponentException on component error
     */
    void start() throws ComponentException;

    /**
     * Called if the workflow is cancelled and the component is currently or was recently in {@link #start(ComponentContext)}. It might be
     * called in parallel {@link #start(ComponentContext)}. So, it need to be implemented thread-safe.
     * 
     * @param executingThreadHandler allows to interrupt the thread of {@link #start(ComponentContext)}
     */
    void onStartInterrupted(ThreadHandler executingThreadHandler);

    /**
     * Called if all required input values are available.
     * 
     * @throws ComponentException on component error
     */
    void processInputs() throws ComponentException;

    /**
     * Called if the workflow is cancelled and the component is currently or was recently in {@link #processInputs()}. It might be called in
     * parallel {@link #processInputs()}. So, it need to be implemented thread-safe.
     * 
     * @param executingThreadHandler allows to interrupt the thread of {@link #processInputs()}
     */
    void onProcessInputsInterrupted(ThreadHandler executingThreadHandler);

    /**
     * Called if component is part of an nested loop and nested loop has been finished.
     * 
     * @throws ComponentException on component error
     */
    void reset() throws ComponentException;

    /**
     * Called if exception in {@link #start()} or {@link #processInputs()} was thrown. It is called immediately afterwards.
     * 
     * @throws ComponentException on component error
     */
    void completeStartOrProcessInputsAfterFailure() throws ComponentException;

    /**
     * Called if the component reached any of the final states. Valid ones are defined in {@link FinalComponentState}.
     * 
     * @param state final state reached
     */
    void tearDown(FinalComponentState state);

    /**
     * Called if the workflow is disposed.
     */
    void dispose();

    /**
     * Is called periodically. Here, the component's history data can be updated. Useful for long running {@link #start(ComponentContext)}
     * or {@link #processInputs()}.
     */
    void onIntermediateHistoryDataUpdateTimer();
    
    /**
     * Injects the token generated that is used by a user to verify the latest {@link Component#start()} (if treated as run) or
     * {@link Component#processInputs()}.
     * 
     * @param verificationToken token used for verification
     * @throws ComponentException on component error
     */
    void handleVerificationToken(String verificationToken) throws ComponentException;
    
    /**
     * Called after {@link #start()} (if treated as run) or {@link #processInputs()} finished and verification is done (verification is done
     * by the user).
     * 
     * @throws ComponentException on component error
     */
    void completeStartOrProcessInputsAfterVerificationDone() throws ComponentException;

}
