/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;


import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ThreadHandler;

/**
 * Common test/mock implementations of {@link Component}. These can be used directly, or
 * can as super classes for custom mock classes.
 * 
 * Custom mock implementations of {@link Component} should use these as superclasses
 * whenever possible to avoid code duplication, and to shield the mock classes from irrelevant API
 * changes.
 * 
 * @author Doreen Seider
 */
public abstract class ComponentDefaultStub {

    /**
     * A mock implementation of {@link Component} that throws an exception on every
     * method call. Subclasses for tests should override the methods they expect to be called.
     * 
     * @author Doreen Seider
     */
    public static class Default implements Component {

        @Override
        public void setComponentContext(ComponentContext componentContext) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void start() throws ComponentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onStartInterrupted(ThreadHandler executingThreadHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void processInputs() throws ComponentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void tearDown(FinalComponentState state) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onIntermediateHistoryDataUpdateTimer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean treatStartAsComponentRun() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void handleVerificationToken(String verificationToken) {
        }

        @Override
        public void completeStartOrProcessInputsAfterVerificationDone() throws ComponentException {
        }

    }
}
