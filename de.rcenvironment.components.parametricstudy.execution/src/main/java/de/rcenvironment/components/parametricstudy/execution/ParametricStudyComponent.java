/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.parametricstudy.execution;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import de.rcenvironment.components.parametricstudy.common.Dimension;
import de.rcenvironment.components.parametricstudy.common.Measure;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyService;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Parametric Study component implementation.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Doreen Seider
 * @author Brigitte Boden
 */
@LazyDisposal
public class ParametricStudyComponent extends AbstractNestedLoopComponent {

    private static final String TRUE = "true";

    private static final String GET_STUDYPARAMETERS_STRING = "Not-a-value at '%s' Input.";

    private static final int MINUS_ONE = -1;

    private static ParametricStudyService parametricStudyService;

    private StudyPublisher study;

    private double from;

    private double to;

    private double stepSize;

    private double designVariable;

    private long steps;

    private boolean fitStepSizeToBounds = true;

    private int stepCount = 1;

    private boolean isDone;

    private volatile boolean canceled = false;

    private static StudyStructure createStructure(final ComponentContext compExeCtx) {
        final StudyStructure structure = new StudyStructure();
        // outputs are dimensions
        for (String outputName : compExeCtx.getOutputs()) {
            if (outputName.equals(ParametricStudyComponentConstants.OUTPUT_NAME_DV)) {
                final Dimension dimension = new Dimension(outputName, true);
                structure.addDimension(dimension);
            }
        }
        // inputs are measures
        for (String inputName : compExeCtx.getInputs()) {
            final Measure measure = new Measure(inputName);
            structure.addMeasure(measure);
        }
        return structure;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return !hasForwardingStartInputs() && !hasStudyParameterInputs();
    }

    @Override
    public void startNestedComponentSpecific() throws ComponentException {

        parametricStudyService = componentContext.getService(ParametricStudyService.class);

        if (treatStartAsComponentRun()) {
            setStudyParameters();
            checkStepSize();
            initilizeStudy();
            sendDesignVariableToOutput(calculateInitialDesignVariable());
            if (!hasEvaluationResultFromLoopInput()) {
                runFullStudyAtOnce();
            }
        } else {
            if (isNestedLoop()) {
                if (!hasEvaluationResultFromLoopInput() && !hasForwardingStartInputs()) {
                    throw new ComponentException("Component is in a nested loop and"
                        + " thus needs at least one 'foward' or 'evaluation result' input to control the flow of the inner loop.");
                }
            }
            setComponentDone(false);
        }
    }

    @Override
    public void processInputsNestedComponentSpecific() throws ComponentException {
        if (stepCount == 1) { // this is only the case if any of the study parameters is set via an Input
            setStudyParameters();
            checkStepSize();
            initilizeStudy();
            if (!hasEvaluationResultFromLoopInput() && !hasForwardingStartInputs()) {
                runFullStudyAtOnce();
            }
        }

        if (componentContext.isOutputClosed(ParametricStudyComponentConstants.OUTPUT_NAME_DV)) {
            throw new ComponentException(StringUtils.format("Too many values received. "
                + "Expect exactly one value per input per design variable sent. "
                + "%s design variables(s) sent and %s value(s) received", steps, steps + 1));
        }
        // send input parameters to study service for monitoring purposes
        final Map<String, Serializable> values = new HashMap<>();
        // input parameters are response to previous iteration
        if (fitStepSizeToBounds) {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME_DV, getLastDesignVariableFittingStepSizeToBounds());
        } else {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME_DV, getLastDesignVariableNotFittingStepSizeToBounds());
        }
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)) {
                TypedDatum input = componentContext.readInput(inputName);
                switch (input.getDataType()) {
                case NotAValue:
                    values.put(inputName, Double.NaN);
                    break;
                case Integer:
                    values.put(inputName, ((IntegerTD) input).getIntValue());
                    break;
                case Float:
                    values.put(inputName, ((FloatTD) input).getFloatValue());
                    break;
                default:
                    values.put(inputName, "Not applicable");
                }
            }
        }
        study.add(new StudyDataset(values));

        setComponentDone(allDesignVariablesSent());
    }

    @Override
    protected void sendValuesNestedComponentSpecific() {
        if (fitStepSizeToBounds) {
            sendDesignVariableToOutput(calculateDesignVariableFittingStepSizeToBounds(stepCount));
        } else {
            sendDesignVariableToOutput(calculateDesignVariableNotFittingStepSizeToBounds());
        }
    }

    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        canceled = true;
    }

    private void runFullStudyAtOnce() throws ComponentException {
        // send initial variable
        if (fitStepSizeToBounds) {
            for (int step = stepCount; step <= steps; step++) {
                sendDesignVariableToOutput(calculateDesignVariableFittingStepSizeToBounds(step));
                if (canceled) {
                    break;
                }
            }
        } else {
            double nextDesignVariable = calculateDesignVariableNotFittingStepSizeToBounds();
            while (designVariableIsInBounds(nextDesignVariable)) {
                sendDesignVariableToOutput(calculateDesignVariableNotFittingStepSizeToBounds());
                nextDesignVariable = calculateDesignVariableNotFittingStepSizeToBounds();
                if (canceled) {
                    break;
                }
            }
        }
        setComponentDone(true);
    }

    @Override
    public void dispose() {
        if (study != null) {
            study.clearStudy();
        }
    }

    @Override
    protected boolean isDoneNestedComponentSpecific() {
        return isDone;
    }

    @Override
    protected void resetNestedComponentSpecific() {
        stepCount = 1;
        setComponentDone(false);
    }

    @Override
    protected void finishLoopNestedComponentSpecific() {}

    @Override
    protected void sendFinalValues() throws ComponentException {}

    private void sendDesignVariableToOutput(double value) {
        writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            typedDatumFactory.createFloat(value));
        componentLog.componentInfo(StringUtils.format("Wrote to output '%s': %s",
            ParametricStudyComponentConstants.OUTPUT_NAME_DV, value));
        stepCount++;
    }

    private double calculateInitialDesignVariable() {
        if (fitStepSizeToBounds) {
            return calculateInitialDesignVariableFittingStepSizeToBounds();
        } else {
            return calculateInitialDesignVariableNotFittingStepSizeToBounds();
        }
    }

    private double calculateInitialDesignVariableFittingStepSizeToBounds() {
        designVariable = from;
        return designVariable;
    }

    private double calculateDesignVariableFittingStepSizeToBounds(int step) {
        // cover if there is only one step to be done (division by zero)
        if (step == 1) {
            return from;
        } else {
            designVariable = from + (to - from) * (step - 1.0) / (steps - 1.0);
            return designVariable;
        }
    }

    private double getLastDesignVariableFittingStepSizeToBounds() {
        return designVariable;
    }

    private double calculateInitialDesignVariableNotFittingStepSizeToBounds() {
        return from;
    }

    private double calculateDesignVariableNotFittingStepSizeToBounds() {
        return from + (stepCount - 1) * stepSize;
    }

    private double getLastDesignVariableNotFittingStepSizeToBounds() {
        return from + (stepCount - 1) * stepSize;
    }

    private boolean allDesignVariablesSent() {
        if (fitStepSizeToBounds) {
            if (stepCount <= steps) {
                return false;
            }
        } else {
            if (designVariableIsInBounds(calculateDesignVariableNotFittingStepSizeToBounds())) {
                return false;
            }
        }
        return true;
    }

    private void setComponentDone(boolean done) {
        isDone = done;
    }

    private boolean hasStudyParameterInputs() {
        for (String inputName : componentContext.getInputs()) {
            if (componentContext.getDynamicInputIdentifier(inputName)
                .equals(ParametricStudyComponentConstants.DYNAMIC_INPUT_STUDY_PARAMETERS)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEvaluationResultFromLoopInput() {
        for (String inputName : componentContext.getInputs()) {
            if (componentContext.getDynamicInputIdentifier(inputName)
                .equals(ParametricStudyComponentConstants.DYNAMIC_INPUT_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    private void setStudyParameters() throws ComponentException {
        try {
            if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_FROM_VALUE).equals(TRUE)) {
                TypedDatum input = componentContext.readInput(ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE);
                double value;
                switch (input.getDataType()) {
                case NotAValue:
                    throw new ComponentException(StringUtils.format(GET_STUDYPARAMETERS_STRING,
                        ParametricStudyComponentConstants.INPUT_NAME_FROM_VALUE));
                case Integer:
                    value = ((IntegerTD) input).getIntValue();
                    break;
                case Float:
                    value = ((FloatTD) input).getFloatValue();
                    break;
                default:
                    value = 0;
                }
                from = value;
            } else {
                from = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                    ParametricStudyComponentConstants.OUTPUT_METATDATA_FROMVALUE));
            }
            if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_TO_VALUE).equals(TRUE)) {
                TypedDatum input = componentContext.readInput(ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE);
                double value;
                switch (input.getDataType()) {
                case NotAValue:
                    throw new ComponentException(StringUtils.format(GET_STUDYPARAMETERS_STRING,
                        ParametricStudyComponentConstants.INPUT_NAME_TO_VALUE));
                case Integer:
                    value = ((IntegerTD) input).getIntValue();
                    break;
                case Float:
                    value = ((FloatTD) input).getFloatValue();
                    break;
                default:
                    value = 0;
                }
                to = value;
            } else {
                to = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                    ParametricStudyComponentConstants.OUTPUT_METATDATA_TOVALUE));
            }
            if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                ParametricStudyComponentConstants.OUTPUT_METADATA_USE_INPUT_AS_STEPSIZE_VALUE).equals(TRUE)) {
                TypedDatum input = componentContext.readInput(ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE);
                double value;
                switch (input.getDataType()) {
                case NotAValue:
                    throw new ComponentException(StringUtils.format(GET_STUDYPARAMETERS_STRING,
                        ParametricStudyComponentConstants.INPUT_NAME_STEPSIZE_VALUE));
                case Integer:
                    value = ((IntegerTD) input).getIntValue();
                    break;
                case Float:
                    value = ((FloatTD) input).getFloatValue();
                    break;
                default:
                    value = 0;
                }
                stepSize = value;
            } else {
                stepSize = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                    ParametricStudyComponentConstants.OUTPUT_METATDATA_STEPSIZE));
            }

        } catch (NoSuchElementException e) {
            throw new ComponentException("Expected Input not connected: " + e.getMessage(), e.getCause());
        }
    }

    private void checkStepSize() throws ComponentException {
        if (stepSize <= 0) {
            throw new ComponentException(StringUtils.format("Invalid step size: %f , must be > 0", stepSize));
        }

        if (from > to) {
            stepSize = stepSize * MINUS_ONE;
        }
    }

    private void initilizeStudy() throws ComponentException {
        study = parametricStudyService.createPublisher(
            componentContext.getExecutionIdentifier(),
            componentContext.getInstanceName(),
            createStructure(componentContext));

        double stepsDouble = (Math.floor((to - from) / stepSize)) + 1; // including first AND last
        if (stepsDouble >= Long.MAX_VALUE) {
            throw new ComponentException("The number of values produced by the Component exceeds the numerical limits.");
        }
        steps = (long) stepsDouble;

        componentLog.componentInfo(StringUtils.format("Sampling from %s to %s with step size %s -> %d value(s)",
            from, to, stepSize, steps));

        if (!hasForwardingStartInputs() && !hasEvaluationResultFromLoopInput()) {
            componentLog.componentInfo("No 'forwarding' or 'evaluation result' inputs defined -> writing all values at once");
        }

        if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS) != null) {
            fitStepSizeToBounds =
                Boolean.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                    ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS));
        }
    }

    private boolean designVariableIsInBounds(double designVariableToTest) {
        if (stepSize < 0) {
            return designVariableToTest <= from && designVariableToTest >= to;
        } else {
            return designVariableToTest >= from && designVariableToTest <= to;
        }
    }

}
