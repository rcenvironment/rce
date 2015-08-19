/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * Component to get data to convergence.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ConvergerComponent extends AbstractNestedLoopComponent {

    private static final int NO_MAX_ITERATIONS = -1;

    private Map<String, CircularFifoQueue<Double>> iterationsValues;

    private double epsA;

    private double epsR;

    private int iterationsToConsider;

    private int maxIterations = NO_MAX_ITERATIONS;

    private int iterations = 1;

    private Boolean[] isConverged;

    @Override
    public void startHook() throws ComponentException {

        epsA = Double.parseDouble(componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_EPS_A));
        epsR = Double.parseDouble(componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_EPS_R));
        String iterationsToConsiderAsString = componentContext.getConfigurationValue(ConvergerComponentConstants
            .KEY_ITERATIONS_TO_CONSIDER);
        iterationsToConsider = Integer.parseInt(iterationsToConsiderAsString);

        String maxIterationsAsString = componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_MAX_ITERATIONS);
        if (maxIterationsAsString != null && !maxIterationsAsString.isEmpty()) {
            maxIterations = Integer.parseInt(maxIterationsAsString);
        }

        initializeIterationValues();

        isConverged = new Boolean[2];
        isConverged[0] = false;
        isConverged[1] = false;
    }

    private void initializeIterationValues() {
        iterationsValues = new HashMap<String, CircularFifoQueue<Double>>();
        for (String inputName : componentContext.getInputs()) {
            if (componentContext.isDynamicInput(inputName)
                && componentContext.getDynamicInputIdentifier(inputName).equals(ConvergerComponentConstants.ID_VALUE_TO_CONVERGE)) {
                iterationsValues.put(inputName, new CircularFifoQueue<Double>(iterationsToConsider + 1));
            }
        }
    }

    private void addValuesToLastIterationsValues() {
        for (String key : iterationsValues.keySet()) {
            iterationsValues.get(key).add(((FloatTD) componentContext.readInput(key)).getFloatValue());
        }
    }

    private boolean areMaxIterationsReached() {
        return maxIterations != NO_MAX_ITERATIONS && iterations >= maxIterations;
    }

    private Boolean[] isConverged() {

        boolean isConvergedAbs = true;
        boolean isConvergedRel = true;
        for (String inputName : iterationsValues.keySet()) {
            CircularFifoQueue<Double> values = iterationsValues.get(inputName);
            int valueCount = values.size();
            if (valueCount > iterationsToConsider) {
                double maxValue = Collections.max(values);
                double minValue = Collections.min(values);
                if (Math.abs(maxValue - minValue) > epsA) {
                    isConvergedAbs = false;
                }
                if (Math.abs((maxValue - minValue) / maxValue) > epsR) {
                    isConvergedRel = false;
                }
                componentContext.printConsoleLine(String.format("%s -> min: %s; max: %s; converged abs: %s; converged rel: %s",
                    inputName + iterations, minValue, maxValue, isConvergedAbs, isConvergedRel), ConsoleRow.Type.STDOUT);
            } else {
                isConvergedAbs = false;
                isConvergedRel = false;
                componentContext.printConsoleLine(
                    String.format("%s -> skipped convergence check - not enough iterations yet (current: %s, required: %s)",
                        inputName + iterations, iterations, iterationsToConsider), ConsoleRow.Type.STDOUT);
            }
        }
        return new Boolean[] { isConvergedAbs, isConvergedRel };
    }

    @Override
    protected void sendValuesHook() {
        for (String inputName : iterationsValues.keySet()) {
            if (!iterationsValues.get(inputName).isEmpty()) {
                componentContext.writeOutput(inputName, typedDatumFactory.createFloat(iterationsValues.get(inputName)
                    .get(iterationsValues.get(inputName).size() - 1)));
            }
        }
        sendConvergedValues();
    }
    
    private void sendConvergedValues() {
        componentContext.writeOutput(ConvergerComponentConstants.CONVERGED, typedDatumFactory.createBoolean(
            isConverged[0] | isConverged[1]));
        componentContext.writeOutput(ConvergerComponentConstants.CONVERGED_ABSOLUTE, typedDatumFactory.createBoolean(isConverged[0]));
        componentContext.writeOutput(ConvergerComponentConstants.CONVERGED_RELATIVE, typedDatumFactory.createBoolean(isConverged[1]));
    }

    @Override
    protected void resetInnerLoopHook() {
        iterations = 1;
        for (CircularFifoQueue<Double> queues : iterationsValues.values()) {
            queues.clear();
        }
        isConverged[0] = false;
        isConverged[1] = false;
    }

    @Override
    protected void finishLoopHook() {
        componentContext.printConsoleLine("Finished: converged abs: " + isConverged[0]
            + "; converged rel: " + isConverged[1] + "; reached max. iterations: "
            + areMaxIterationsReached(), ConsoleRow.Type.STDOUT);
    }

    @Override
    protected boolean isFinished() {
        return isConverged[0] || isConverged[1] || areMaxIterationsReached();
    }

    @Override
    protected void processInputsHook() {
        addValuesToLastIterationsValues();
        isConverged = isConverged();
        iterations++;
    }

    @Override
    protected String getLoopFinishedEndpointName() {
        return ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE;
    }

    @Override
    protected void sendFinalValues() {
        for (String key : iterationsValues.keySet()) {
            int valueCount = iterationsValues.get(key).size();
            if (valueCount > 0) {
                componentContext.writeOutput(key + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                    typedDatumFactory.createFloat(iterationsValues.get(key).get(valueCount - 1)));
            }
        }
        sendConvergedValues();
    }

    @Override
    protected void sendReset() {
        for (String key : iterationsValues.keySet()) {
            componentContext.resetOutput(key);
        }
    }

}
