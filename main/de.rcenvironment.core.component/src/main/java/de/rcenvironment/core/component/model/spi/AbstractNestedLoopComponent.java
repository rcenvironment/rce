/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.spi;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.InternalTD;

/**
 * The {@link AbstractNestedLoopComponent} is an abstract class that must be used for components
 * that should be able to run in a nested loop. It defines the lifecycle for a nested component for
 * being used as inner or as oter loop component. If the scheduling from the
 * {@link DefaultComponent} should be used, the "callDefaultScheduling" method can be used.
 * 
 * Note that this class redefines the life cycle of the {@link DefaultComponent}. The standard life
 * cycle methods like runStep are overwritten and made final, because they must not be overwritten
 * by the sub class. To implement the component's individual logic, there are custom abstract
 * methods of all lifecycle methods.
 * 
 * If this class is used for a component, there are some more things that must be used:
 * <ul>
 * <li>
 * In the component's GUI, the {@link NestedLoopSection} must be added</li>
 * <li>
 * The component must have a {@link BooleanTD} output, which is used in this class for telling inner
 * loops, if the outer loop is finished. This output's name is what the getLoopFinishedEnpointName()
 * method must return.</li>
 * <li>
 * The component must define the input "Outer loop done" as boolean, which will be created by the
 * {@link NestedLoopSection}</li>
 * </ul>
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public abstract class AbstractNestedLoopComponent extends DefaultComponent {

    protected ComponentContext componentContext;

    protected TypedDatumFactory typedDatumFactory;

    private boolean isInInnerLoop;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();

        isInInnerLoop = Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));

        startHook();
    }

    @Override
    public void processInputs() throws ComponentException {

        if (componentContext.getInputsWithDatum().contains(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
            if (((BooleanTD) componentContext.readInput(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)).getBooleanValue()) {
                outerLoopFinished();
            }
            return;
        }
        processInputsHook();
        if (isFinished()) {
            onLoopFinished();
        } else {
            sendValues();
        }
    }

    @Override
    public void reset() throws ComponentException {
        finishLoop(false);
        resetInnerLoopHook();
        start();
    }

    private void sendValues() {
        sendValuesHook();
    }

    private void outerLoopFinished() throws ComponentException {
        finishLoop(true);
        componentContext.closeAllOutputs();
    }

    private void finishLoop(boolean outerLoopFinished) throws ComponentException {
        if (outerLoopFinished) {
            sendLoopDone();
            if (!isInInnerLoop) {
                sendFinalValues();
                finishLoopHook();
            }
        } else {
            sendFinalValues();
            finishLoopHook();
        }
    }

    private void onLoopFinished() throws ComponentException {
        if (isInInnerLoop) {
            sendReset();
        } else {
            outerLoopFinished();
        }
    }

    private void sendLoopDone() {
        componentContext.writeOutput(getLoopFinishedEndpointName(), typedDatumFactory.createBoolean(true));
    }

    /**
     * Method is called after the {@link AbstractNestedLoopComponent} has initialized all it needs.
     * It replaces the original runInitial() Method.
     * 
     * @return true, if component is not finished.
     * @throws ComponentException
     */
    protected void startHook() throws ComponentException {}

    /**
     * This hook is for implementing the component logic.
     * 
     * @param newInput
     * @param inputValues
     * @return
     * @throws ComponentException
     */
    protected void processInputsHook() throws ComponentException {}

    /**
     * Component dependent part when a loop is reset if it is an inner loop.
     */
    protected abstract void resetInnerLoopHook();

    /**
     * Component dependent part when a loop is finished.
     */
    protected abstract void finishLoopHook();

    /**
     * @return name of the boolean output that is used for telling other components if the loop is
     *         finished.
     */
    protected abstract String getLoopFinishedEndpointName();

    /**
     * @return whether the components logic is finished (no more runSteps) or not.
     */
    protected abstract boolean isFinished();

    /**
     * If needed, the component should send e.g. optimized values at this point.
     * 
     * @throws ComponentException
     */
    protected abstract void sendFinalValues() throws ComponentException;

    /**
     * Send {@link InternalTD} to all inner loop outputs.
     */
    protected abstract void sendReset();

    /**
     * This method consumes a start value from an input. Receiving new values should be individual
     * for every component.
     */
    protected abstract void sendValuesHook();

}
