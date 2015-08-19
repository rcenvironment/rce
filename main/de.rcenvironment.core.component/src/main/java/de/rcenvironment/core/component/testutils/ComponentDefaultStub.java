/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
        }
        
        @Override
        public void start() throws ComponentException {
        }

        @Override
        public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        }

        @Override
        public void processInputs() throws ComponentException {
        }

        @Override
        public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
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
        public void reset() {
        }

        @Override
        public boolean treatStartAsComponentRun() {
            return false;
        }


    }
}
