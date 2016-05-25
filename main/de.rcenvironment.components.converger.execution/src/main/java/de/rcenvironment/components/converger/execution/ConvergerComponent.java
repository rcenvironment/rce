/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import de.rcenvironment.components.converger.common.ConvergerComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Component to get data to convergence.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 */
public class ConvergerComponent extends AbstractNestedLoopComponent {

    private static final int NO_MAX_CONVERGENCE_CHECKS = -1;

    private Map<String, CircularFifoQueue<Double>> valueTuples;

    private double epsA;

    private double epsR;

    private int valueTuplesToConsider;

    private int valueTuplesProcessed = 0;

    private int maxConvergenceChecks = NO_MAX_CONVERGENCE_CHECKS;

    private int convergenceChecks = 0;

    private NotConvergedBehavior notConvergedBehavior;

    /**
     * @author Sascha Zur on behalf of Caslav Ilic
     */
    private enum NotConvergedBehavior {
        Ignore,
        Fail,
        NotAValue;
    }

    private Boolean[] isConverged;

    private Map<String, Boolean[]> isSingleConverged;

    private NotConvergedBehavior getNotConvergedBehavior() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ConvergerComponentConstants.NOT_CONVERGED_FAIL))) {
            return NotConvergedBehavior.Fail;
        } else if (Boolean.valueOf(componentContext.getConfigurationValue(ConvergerComponentConstants.NOT_CONVERGED_NOT_A_VALUE))) {
            return NotConvergedBehavior.NotAValue;
        } else {
            return NotConvergedBehavior.Ignore;
        }
    }

    @Override
    public void startNestedComponentSpecific() throws ComponentException {

        epsA = Double.parseDouble(componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_EPS_A));
        epsR = Double.parseDouble(componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_EPS_R));
        String iterationsToConsiderAsString =
            componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_ITERATIONS_TO_CONSIDER);
        valueTuplesToConsider = Integer.parseInt(iterationsToConsiderAsString) + 1; // values of
                                                                                    // last + tuples
                                                                                    // of current
                                                                                    // iteration(s)

        String maxConvergenceChecksAsString = componentContext.getConfigurationValue(ConvergerComponentConstants.KEY_MAX_CONV_CHECKS);
        if (maxConvergenceChecksAsString != null && !maxConvergenceChecksAsString.isEmpty()) {
            maxConvergenceChecks = Integer.parseInt(maxConvergenceChecksAsString);
        }

        initializeIterationValues();

        isConverged = new Boolean[2];
        isConverged[0] = false;
        isConverged[1] = false;

        isSingleConverged = new HashMap<>();
        for (String inputName : componentContext.getInputs()) {
            Boolean[] isInputConverged = new Boolean[2];
            isInputConverged[0] = false;
            isInputConverged[1] = false;
            isSingleConverged.put(inputName, isInputConverged);
        }

        notConvergedBehavior = getNotConvergedBehavior();

        if (!hasStartValueInputs()) {
            sendValuesNestedComponentSpecific();
        }
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return !hasStartValueInputs();
    }

    private boolean hasStartValueInputs() {
        for (String input : componentContext.getInputs()) {
            if (input.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    private void initializeIterationValues() {
        valueTuples = new HashMap<String, CircularFifoQueue<Double>>();
        for (String inputName : componentContext.getInputs()) {
            if (componentContext.isDynamicInput(inputName)
                && componentContext.getDynamicInputIdentifier(inputName).equals(ConvergerComponentConstants.ENDPOINT_ID_TO_CONVERGE)
                && !inputName.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                valueTuples.put(inputName, new CircularFifoQueue<Double>(valueTuplesToConsider));
            }
            if (!inputName.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                if (Boolean.parseBoolean(componentContext.getInputMetaDataValue(inputName,
                    ConvergerComponentConstants.META_HAS_STARTVALUE))) {
                    valueTuples.get(inputName).add(Double.parseDouble(componentContext.getInputMetaDataValue(
                        inputName, ConvergerComponentConstants.META_STARTVALUE)));
                }
            }
        }
    }

    private void addValuesToLastIterationsValues(boolean startValues) {
        for (String inputName : valueTuples.keySet()) {
            if (startValues && componentContext.getInputs().contains(inputName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                valueTuples.get(inputName).add(
                    ((FloatTD) componentContext.readInput(inputName + LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)).getFloatValue());
            } else if (!startValues) {
                if (componentContext.getInputDataType(inputName) == DataType.Float) {
                    valueTuples.get(inputName).add(((FloatTD) componentContext.readInput(inputName)).getFloatValue());
                } else if (componentContext.getInputDataType(inputName) == DataType.Integer) {
                    valueTuples.get(inputName).add((double) ((IntegerTD) componentContext.readInput(inputName)).getIntValue());
                }
            }
        }
    }

    private boolean areMaxConvergenceChecksReached() {
        return maxConvergenceChecks != NO_MAX_CONVERGENCE_CHECKS && convergenceChecks >= maxConvergenceChecks;
    }

    private Map<String, Boolean[]> isSingleConverged() {

        boolean convergenceCheckSkipped = false;

        Map<String, Boolean[]> isInputConverged = new HashMap<>();
        for (String inputName : valueTuples.keySet()) {
            CircularFifoQueue<Double> values = valueTuples.get(inputName);
            int valueCount = values.size();
            int[] range = getValueTupleRange();
            boolean isCurrentConvergedAbs;
            boolean isCurrentConvergedRel;
            if (valueCount >= valueTuplesToConsider) {
                double maxValue = Collections.max(values);
                double minValue = Collections.min(values);
                isCurrentConvergedAbs = (Math.abs(maxValue - minValue) <= epsA);
                isCurrentConvergedRel = (Math.abs((maxValue - minValue) / maxValue) <= epsR);
                componentLog.componentInfo(StringUtils.format("%s [%d->%d] -> min: %s; max: %s; conv abs: %s; "
                    + "conv rel: %s; #conv check: %d", inputName, range[0], range[1],
                    minValue, maxValue, isCurrentConvergedAbs, isCurrentConvergedRel, convergenceChecks + 1));
            } else {
                isCurrentConvergedAbs = false;
                isCurrentConvergedRel = false;
                convergenceCheckSkipped = true;
                componentLog.componentInfo(
                    StringUtils.format("%s [%d->%d] -> skipped convergence check - not enough values yet (current: %s, required: %s)",
                        inputName, range[0], range[1], valueCount, valueTuplesToConsider));
            }
            Boolean[] isThisInputConverged = new Boolean[2];
            isThisInputConverged[0] = isCurrentConvergedAbs;
            isThisInputConverged[1] = isCurrentConvergedRel;
            isInputConverged.put(inputName, isThisInputConverged);
        }
        if (!convergenceCheckSkipped) {
            convergenceChecks++;
        }
        return isInputConverged;
    }

    private Boolean[] isConverged(Map<String, Boolean[]> isInputConverged) {

        boolean isConvergedAbs = true;
        boolean isConvergedRel = true;
        for (String inputName : valueTuples.keySet()) {
            Boolean[] isThisInputConverged = isInputConverged.get(inputName);
            if (!isThisInputConverged[0]) {
                isConvergedAbs = false;
            }
            if (!isThisInputConverged[1]) {
                isConvergedRel = false;
            }
        }
        return new Boolean[] { isConvergedAbs, isConvergedRel };
    }

    private int[] getValueTupleRange() {
        int rangeStart = valueTuplesProcessed - valueTuplesToConsider + 1;
        if (rangeStart < 0) {
            rangeStart = 0;
        }
        return new int[] { rangeStart, valueTuplesProcessed };
    }

    @Override
    protected void sendValuesNestedComponentSpecific() {
        for (String inputName : valueTuples.keySet()) {
            if (!valueTuples.get(inputName).isEmpty()) {
                if (componentContext.getInputDataType(inputName) == DataType.Float) {
                    writeOutput(inputName, typedDatumFactory.createFloat(valueTuples.get(inputName)
                        .get(valueTuples.get(inputName).size() - 1)));
                } else if (componentContext.getInputDataType(inputName) == DataType.Integer) {
                    double value = valueTuples.get(inputName)
                        .get(valueTuples.get(inputName).size() - 1);
                    writeOutput(inputName, typedDatumFactory.createInteger((long) value));
                }
                writeOutput(inputName + ConvergerComponentConstants.IS_CONVERGED_OUTPUT_SUFFIX, typedDatumFactory.createBoolean(
                    isSingleConverged.get(inputName)[0] || isSingleConverged.get(inputName)[1]));
            }
        }
    }

    private void forwardFinalValues() {
        Set<String> inputs = componentContext.getInputsWithDatum();
        for (String input : inputs) {
            if (componentContext.getDynamicInputIdentifier(input).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                writeOutput(input + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                    componentContext.readInput(input));
            }
        }
    }

    private void sendConvergedValues() {
        if (notConvergedBehavior == NotConvergedBehavior.NotAValue && areMaxConvergenceChecksReached()) {
            TypedDatumFactory factory = componentContext.getService(TypedDatumService.class).getFactory();
            componentContext.writeOutput(ConvergerComponentConstants.CONVERGED, factory.createNotAValue());
            componentContext.writeOutput(ConvergerComponentConstants.CONVERGED_ABSOLUTE, factory.createNotAValue());
            componentContext.writeOutput(ConvergerComponentConstants.CONVERGED_RELATIVE, factory.createNotAValue());
        } else {
            writeOutput(ConvergerComponentConstants.CONVERGED, typedDatumFactory.createBoolean(
                isConverged[0] | isConverged[1]));
            writeOutput(ConvergerComponentConstants.CONVERGED_ABSOLUTE, typedDatumFactory.createBoolean(isConverged[0]));
            writeOutput(ConvergerComponentConstants.CONVERGED_RELATIVE, typedDatumFactory.createBoolean(isConverged[1]));
        }
    }

    @Override
    protected void resetNestedComponentSpecific() {
        convergenceChecks = 0;
        for (CircularFifoQueue<Double> queues : valueTuples.values()) {
            queues.clear();
        }
        isConverged[0] = false;
        isConverged[1] = false;
        valueTuplesProcessed = 0;
    }

    @Override
    protected void finishLoopNestedComponentSpecific() {
        componentLog.componentInfo("Finished: converged abs: " + isConverged[0]
            + "; converged rel: " + isConverged[1] + "; reached max. conv checks: "
            + areMaxConvergenceChecksReached());
    }

    @Override
    protected boolean isDoneNestedComponentSpecific() {
        return isConverged[0] || isConverged[1] || areMaxConvergenceChecksReached();
    }

    @Override
    protected void processInputsNestedComponentSpecific() {
        addValuesToLastIterationsValues(
            componentContext.getInputsWithDatum().iterator().next().endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX));
        isSingleConverged = isSingleConverged();
        isConverged = isConverged(isSingleConverged);
        valueTuplesProcessed++;
    }

    @Override
    protected void sendFinalValues() throws ComponentException {
        boolean maxIterationsReached = areMaxConvergenceChecksReached();
        TypedDatumFactory factory = componentContext.getService(TypedDatumService.class).getFactory();
        if (notConvergedBehavior == NotConvergedBehavior.Fail && maxIterationsReached) {
            throw new ComponentException("Maximum number of checks reached without convergence.");
        } else if (notConvergedBehavior == NotConvergedBehavior.NotAValue && maxIterationsReached) {
            componentContext.getLog().componentError(StringUtils.format("Maximum number of checks (%d) reached without convergence",
                maxConvergenceChecks));
        }
        for (String key : valueTuples.keySet()) {
            int valueCount = valueTuples.get(key).size();
            if (valueCount > 0) {
                if (notConvergedBehavior == NotConvergedBehavior.NotAValue && maxIterationsReached) {
                    componentContext.writeOutput(key + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                        factory.createNotAValue());
                } else {
                    if (componentContext.getOutputDataType(key).equals(DataType.Float)) {
                        writeOutput(key + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                            typedDatumFactory.createFloat(valueTuples.get(key).get(valueCount - 1)));
                    } else if (componentContext.getOutputDataType(key).equals(DataType.Integer)) {
                        long value = (long) ((double) valueTuples.get(key).get(valueCount - 1));
                        writeOutput(key + ConvergerComponentConstants.CONVERGED_OUTPUT_SUFFIX,
                            typedDatumFactory.createInteger(value));
                    }
                }
            }
        }
        forwardFinalValues();
        sendConvergedValues();
    }

}
