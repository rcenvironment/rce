/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.parametricstudy.execution;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.components.parametricstudy.common.Dimension;
import de.rcenvironment.components.parametricstudy.common.Measure;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyComponentConstants;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyService;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Parametric Study component implementation.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Doreen Seider
 */
@LazyDisposal
public class ParametricStudyComponent extends AbstractNestedLoopComponent {

    private static final int MINUS_ONE = -1;

    private static ParametricStudyService parametricStudyService;

    private StudyPublisher study;

    private double from;

    private double to;

    private double stepSize;

    private double designVariable;

    private long steps;

    private boolean fitStepSizeToBounds = true;

    private int currentStep = 1;
    
    private int stepCount = 1;
    
    private boolean isDone;

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
        designVariable = from + (to - from) * (step - 1.0) / (steps - 1.0);
        return designVariable;
    }
    
    private double getLastDesignVariableFittingStepSizeToBounds() {
        return designVariable;
    }
    
    private double calculateInitialDesignVariableNotFittingStepSizeToBounds() {
        return from;
    }
    
    private double calculateDesignVariableNotFittingStepSizeToBounds() {
        return from + currentStep * stepSize;
    }
    
    private double getLastDesignVariableNotFittingStepSizeToBounds() {
        return from + (currentStep - 1) * stepSize;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return !hasForwardingStartInputs();
    }

    @Override
    public void startNestedComponentSpecific() throws ComponentException {

        parametricStudyService = componentContext.getService(ParametricStudyService.class);
        
        from = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            ParametricStudyComponentConstants.OUTPUT_METATDATA_FROMVALUE));
        to = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            ParametricStudyComponentConstants.OUTPUT_METATDATA_TOVALUE));
        
        stepSize = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            ParametricStudyComponentConstants.OUTPUT_METATDATA_STEPSIZE));
        
        if (stepSize <= 0) {
            throw new ComponentException(StringUtils.format("Invalid step size: %d; must be >= 0", stepSize));
        }
        
        if (from > to) {
            stepSize = stepSize * MINUS_ONE;
        }
        
        study = parametricStudyService.createPublisher(
            componentContext.getExecutionIdentifier(),
            componentContext.getInstanceName(),
            createStructure(componentContext));

        steps = ((Double) Math.floor((to - from) / stepSize)).longValue() + 1; // including first AND last

        componentLog.componentInfo(StringUtils.format("Sampling from %s to %s with step size %s -> %d value(s)",
            from, to, stepSize, steps));

        if (componentContext.getInputs().isEmpty()) {
            componentLog.componentInfo("No input(s) defined -> writing all value at once");
        }

        if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS) != null) {
            fitStepSizeToBounds =
                Boolean.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
                    ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS));
        }
        if (!hasForwardingStartInputs()) {
            sendDesignVariableToOutput(calculateInitialDesignVariable());
            study.add(new StudyDataset(new HashMap<String, Serializable>()));
            if (componentContext.getInputs().isEmpty()) {
                if (fitStepSizeToBounds) {
                    for (int step = 2; step <= steps; step++) {
                        sendDesignVariableToOutput(calculateDesignVariableFittingStepSizeToBounds(step));
                    }
                } else {
                    while (calculateDesignVariableNotFittingStepSizeToBounds() <= to) {
                        sendDesignVariableToOutput(calculateDesignVariableNotFittingStepSizeToBounds());
                        currentStep++;
                    }
                }
                setComponentDone(true);
            } else {
                setComponentDone(false);
            }
        }
    }
    
    private void sendDesignVariableToOutput(double value) {
        writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME_DV,
            typedDatumFactory.createFloat(value));
        componentLog.componentInfo(StringUtils.format("Wrote to output '%s': %s",
            ParametricStudyComponentConstants.OUTPUT_NAME_DV, value));
        stepCount++;
    }

    @Override
    public void processInputsNestedComponentSpecific() throws ComponentException {
        if (componentContext.isOutputClosed(ParametricStudyComponentConstants.OUTPUT_NAME_DV)) {
            throw new ComponentException(StringUtils.format("Too many values received. "
                + "Expect exactly one value per input per design variable sent. "
                + "%s design variables(s) sent and %s value(s) received", steps, steps + 1));
        }
        // send input parameters to study service for monitoring purposes
        final Map<String, Serializable> values = new HashMap<String, Serializable>();
        // input parameters are response to previous iteration
        if (fitStepSizeToBounds) {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME_DV, getLastDesignVariableFittingStepSizeToBounds());
        } else {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME_DV, getLastDesignVariableNotFittingStepSizeToBounds());
        }
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)
                && !componentContext.getDynamicInputIdentifier(inputName).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                TypedDatum input = componentContext.readInput(inputName);
                if (input.getDataType().equals(DataType.NotAValue)) {
                    values.put(inputName, Double.NaN);
                } else {
                    values.put(inputName, ((FloatTD) input).getFloatValue());
                }
            }
        }
        study.add(new StudyDataset(values));
        
        setComponentDone(allDesignVariablesSent());
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
        currentStep = 1;
        stepCount = 1;
        setComponentDone(false);
    }

    @Override
    protected void finishLoopNestedComponentSpecific() {}

    @Override
    protected void sendFinalValues() throws ComponentException {}

    @Override
    protected void sendValuesNestedComponentSpecific() {
        if (fitStepSizeToBounds) {
            sendDesignVariableToOutput(calculateDesignVariableFittingStepSizeToBounds(stepCount));
        } else {
            sendDesignVariableToOutput(calculateDesignVariableNotFittingStepSizeToBounds());
            currentStep++;
        }
    }
    
    private boolean allDesignVariablesSent() {
        if (fitStepSizeToBounds) {
            if (stepCount <= steps) {
                return false;
            }
        } else {
            if (calculateDesignVariableNotFittingStepSizeToBounds() <= to) {
                return false;
            }
        }
        return true;
    }
    
    private void setComponentDone(boolean done) {
        isDone = done;
    }

}
