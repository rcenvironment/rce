/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.spi;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ThreadHandler;

/**
 * Default implementation of {@link Component}. Intended to be extended by components to avoid changes on component implementation side if
 * {@link Component} interface is extended.
 * 
 * @author Doreen Seider
 */
public class DefaultComponent implements Component {

    @Override
    public boolean treatStartAsComponentRun() {
        return false;
    }

    @Override
    public void start() throws ComponentException {
    }
    
    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {}

    @Override
    public void processInputs() throws ComponentException {
    }

    @Override
    public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {}

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {}
    
    @Override
    public void reset() throws ComponentException {
    }
    
    @Override
    public void tearDown(FinalComponentState state) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void onIntermediateHistoryDataUpdateTimer() {
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
    }

    @Override
    public void handleVerificationToken(String verificationToken) throws ComponentException {
    }

    @Override
    public void completeStartOrProcessInputsAfterVerificationDone() throws ComponentException {
    }

}
