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
import de.rcenvironment.core.component.execution.api.Component.FinalComponentState;
import de.rcenvironment.core.component.execution.api.ComponentContext;

/**
 * Wraps {@link Component} instances for integration testing to ensure proper {@link ComponentContext} behavior. For example, in an actual
 * workflow context, all inputs are discarded after {@link Component#processInputs()}. Likewise, the execution counter in the
 * {@link ComponentContext} must be incremented on each component invocation. By using this wrapper, test authors can focus on the actual
 * test semantics.
 * <p>
 * IMPORTANT: When using this wrapper, no methods of the wrapped {@link Component} instance should be called directly. This includes
 * {@link Component#setComponentContext(ComponentContext)} - the context is set automatically by the wrapper. Usually, you can just pass the
 * {@link Component} instance to test to the wrapper without keeping a reference to it (see code below).
 * <p>
 * Typical initialization code:
 * 
 * <code><pre>context = new ComponentContextMock(); // can also be a subclass
component = new ComponentTestWrapper(new XYComponent(), context);</pre></code>
 * 
 * @author Robert Mischke
 */
public class ComponentTestWrapper {

    private final Component component;

    private final ComponentContextMock context;

    private boolean started;

    public ComponentTestWrapper(Component component, ComponentContextMock context) {
        this.component = component;
        this.context = context;
        component.setComponentContext(context);
    }

    /**
     * Calls {@link Component#start()} on the component with added checks and context calls.
     * 
     * @throws ComponentException as thrown by {@link Component#start()}
     */
    public synchronized void start() throws ComponentException {
        if (started) {
            throw new IllegalStateException("start() was called more than once");
        }
        this.started = true;
        if (component.treatStartAsComponentRun()) {
            context.incrementExecutionCount();
        }
        component.start();
    }

    /**
     * Calls {@link Component#processInputs()} on the component with added checks and context calls.
     * 
     * @throws ComponentException as thrown by {@link Component#processInputs()}
     */
    public synchronized void processInputs() throws ComponentException {
        if (!started) {
            throw new IllegalStateException("processInputs() called before start()");
        }
        // consistency check to catch test errors
        if (context.getInputsWithDatum().isEmpty()) {
            throw new IllegalStateException("processInputs() was called without input");
        }
        context.incrementExecutionCount();
        context.resetOutputData();
        context.resetOutputClosings();
        component.processInputs();
        context.resetInputData();
    }

    /**
     * Calls {@link Component#dispose()} on the component.
     */
    public synchronized void dispose() {
        if (!started) {
            throw new IllegalStateException("dispose() called before start()");
        }
        component.dispose();
    }

    /**
     * Calls {@link Component#tearDown(FinalComponentState)} on the component.
     * 
     * @param state the component's final state
     */
    public synchronized void tearDown(FinalComponentState state) {
        if (!started) {
            throw new IllegalStateException("tearDown() called before start()");
        }
        component.tearDown(state);
    }

    /**
     * Convenience method combining {@link #tearDown(FinalComponentState)} and {@link #dispose()}.
     * 
     * @param state the component's final state, as in {@link Component#tearDown(FinalComponentState)}
     */
    public synchronized void tearDownAndDispose(FinalComponentState state) {
        tearDown(state);
        dispose();
    }

}
