/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.spi;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
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
public abstract class AbstractNestedLoopComponent extends AbstractLoopComponent {

    private boolean isInInnerLoop;
    
    private boolean isFinallyDone = false;

    private boolean isReset;

    @Override
    public void startComponentSpecific() throws ComponentException {
        isInInnerLoop = Boolean.valueOf(componentContext.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));
        startNestedComponentSpecific();
    }

    @Override
    public void processInputsComponentSpecific() throws ComponentException {
        
        if (loopFailureRequested) {
            isReset = true;
            sendReset();
        } else {
            if (componentContext.getInputsWithDatum().contains(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
                if (((BooleanTD) componentContext.readInput(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)).getBooleanValue()) {
                    outerLoopIsFinished();
                }
                return;
            }
            processInputsNestedComponentSpecific();
            if (isDone()) {
                loopIsFinished();
            } else {
                sendValuesNestedComponentSpecific();
            }
        }
    }

    @Override
    public void resetComponentSpecific() throws ComponentException {
        resetNestedComponentSpecific();
        startNestedComponentSpecific();
        isReset = false;
    }
    
    @Override
    protected boolean isDone() {
        return isDoneNestedComponentSpecific() || (!isInInnerLoop() && isFinallyDone());
    }
    
    @Override
    protected boolean isFinallyDone() {
        return isFinallyDone;
    }
    
    protected boolean isInInnerLoop() {
        return isInInnerLoop;
    }

    @Override
    protected boolean isReset() {
        return isReset;
    }

    private void loopIsFinished() throws ComponentException {
        if (isInInnerLoop) {
            isReset = true;
            sendReset();
        } else {
            outerLoopIsFinished();
        }
    }
    
    private void outerLoopIsFinished() throws ComponentException {
        finishLoop(true);
    }
    
    @Override
    protected void finishLoopComponentSpecific(boolean outerLoopFinished) throws ComponentException {
        finishLoop(outerLoopFinished);
    }

    private void finishLoop(boolean outerLoopFinished) throws ComponentException {
        if (outerLoopFinished) {
            isFinallyDone = true;
            if (!isInInnerLoop) {
                sendFinalValues();
                finishLoopNestedComponentSpecific();
            }
        } else {
            sendFinalValues();
            finishLoopNestedComponentSpecific();
        }
    }

    /**
     * Method is called after the {@link AbstractNestedLoopComponent} has initialized all it needs.
     * It replaces the original runInitial() Method.
     * 
     * @return true, if component is not finished.
     * @throws ComponentException
     */
    protected void startNestedComponentSpecific() throws ComponentException {}

    /**
     * This hook is for implementing the component logic.
     * 
     * @param newInput
     * @param inputValues
     * @return
     * @throws ComponentException
     */
    protected void processInputsNestedComponentSpecific() throws ComponentException {}

    /**
     * Component dependent part when a loop is reset if it is an inner loop.
     */
    protected abstract void resetNestedComponentSpecific();

    /**
     * Component dependent part when a loop is finished.
     */
    protected abstract void finishLoopNestedComponentSpecific();

    /**
     * @return whether the components logic is finished (no more runSteps) or not.
     */
    protected abstract boolean isDoneNestedComponentSpecific();
    
    /**
     * If needed, the component should send e.g. optimized values at this point.
     * 
     * @throws ComponentException
     */
    protected abstract void sendFinalValues() throws ComponentException;

    /**
     * Send {@link InternalTD} to all self loop outputs.
     */
    protected void sendReset() {
        for (String output : componentContext.getOutputs()) {
            if (LoopEndpointType.fromString(componentContext.getOutputMetaDataValue(
                output, LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE)) == LoopEndpointType.SelfLoopEndpoint) {
                componentContext.resetOutput(output);
            }
        }
    }

    /**
     * This method consumes a start value from an input. Receiving new values should be individual
     * for every component.
     */
    protected abstract void sendValuesNestedComponentSpecific();

}
