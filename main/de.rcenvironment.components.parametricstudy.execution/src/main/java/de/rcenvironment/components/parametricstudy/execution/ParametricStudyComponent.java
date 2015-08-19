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

import org.apache.commons.logging.LogFactory;

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
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;

/**
 * Parametric Study component implementation.
 * 
 * @author Markus Kunde
 * @author Arne Bachmann
 * @author Doreen Seider
 */
@LazyDisposal
public class ParametricStudyComponent extends DefaultComponent {

    private static ParametricStudyService parametricStudyService;

    private static TypedDatumFactory typedDatumFactory;

    private StudyPublisher study;

    private double from;

    private double to;

    private double stepSize;

    private double designVariable;

    private long steps;

    private ComponentContext componentContext;

    private boolean fitStepSizeToBounds = true;

    private int currentStep;

    private static StudyStructure createStructure(final ComponentContext compExeCtx) {
        final StudyStructure structure = new StudyStructure();
        // outputs are dimensions
        for (String outputName : compExeCtx.getOutputs()) {
            final Dimension dimension = new Dimension(outputName, true);
            structure.addDimension(dimension);
        }
        // inputs are measures
        for (String inputName : compExeCtx.getInputs()) {
            final Measure measure = new Measure(inputName);
            structure.addMeasure(measure);
        }
        return structure;
    }

    private double calculateInitialDesignVariable() {
        designVariable = from;
        return designVariable;
    }

    private double geLastDesignVariable() {
        return designVariable;
    }

    private double calculateDesignVariable(int step) {
        designVariable = from + (to - from) * (step - 1.0) / (steps - 1.0);
        return designVariable;
    }

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return !componentContext.getOutputs().isEmpty();
    }

    @Override
    public void start() throws ComponentException {
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        parametricStudyService = componentContext.getService(ParametricStudyService.class);

        if (treatStartAsComponentRun()) {
            from = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME,
                ParametricStudyComponentConstants.OUTPUT_METATDATA_FROMVALUE));
            to = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME,
                ParametricStudyComponentConstants.OUTPUT_METATDATA_TOVALUE));
            stepSize = Double.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME,
                ParametricStudyComponentConstants.OUTPUT_METATDATA_STEPSIZE));

            study = parametricStudyService.createPublisher(
                componentContext.getExecutionIdentifier(),
                componentContext.getInstanceName(),
                createStructure(componentContext));

            steps = ((Double) Math.floor((to - from) / stepSize)).longValue() + 1; // including
                                                                                   // first AND last
            if (steps < 0) {
                throw new ComponentException("Required number of steps exceeds range of the 'long' data type.");
            }
            if (componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME,
                ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS) != null) {
                fitStepSizeToBounds =
                    Boolean.valueOf(componentContext.getOutputMetaDataValue(ParametricStudyComponentConstants.OUTPUT_NAME,
                        ParametricStudyComponentConstants.OUTPUT_METATDATA_FIT_STEP_SIZE_TO_BOUNDS));
            }
            if (fitStepSizeToBounds) {
                componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                    typedDatumFactory.createFloat(calculateInitialDesignVariable()));
            } else {
                componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                    typedDatumFactory.createFloat(from));
            }
            study.add(new StudyDataset(new HashMap<String, Serializable>()));
            currentStep = 1;
            if (componentContext.getInputs().isEmpty()) {
                if (fitStepSizeToBounds) {
                    for (int step = 2; step <= steps; step++) {
                        componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                            typedDatumFactory.createFloat(calculateDesignVariable(step)));
                        if (step % 10 == 0 && Thread.interrupted()) {
                            LogFactory.getLog(getClass()).debug(String.format("Component '%s' was interupted",
                                componentContext.getInstanceName()));
                            return;

                        }
                    }
                } else {
                    while (from + currentStep * stepSize <= to) {
                        componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                            typedDatumFactory.createFloat(from + currentStep * stepSize));
                        if (currentStep % 10 == 0 && Thread.interrupted()) {
                            LogFactory.getLog(getClass()).debug(String.format("Component '%s' was interupted",
                                componentContext.getInstanceName()));
                            return;
                        }
                        currentStep++;
                    }
                }
            }
        }
    }

    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        executingThreadHandler.interrupt();
    }

    @Override
    public void processInputs() throws ComponentException {
        if (componentContext.isOutputClosed(ParametricStudyComponentConstants.OUTPUT_NAME)) {
            throw new ComponentException(String.format("Too many parameters received. "
                + "Expect exactly one parameter per input per design variable sent. "
                + "%s design variable sent and %s received", steps, steps + 1));
        }
        // send input parameters to study service for monitoring purposes
        final Map<String, Serializable> values = new HashMap<String, Serializable>();
        // input parameters are response to previous iteration
        if (fitStepSizeToBounds) {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME, geLastDesignVariable());
        } else {
            values.put(ParametricStudyComponentConstants.OUTPUT_NAME, from + (currentStep - 1) * stepSize);
        }
        for (String inputName : componentContext.getInputs()) {
            TypedDatum input = componentContext.readInput(inputName);
            if (input.getDataType().equals(DataType.NotAValue)) {
                values.put(inputName, Double.NaN);
            } else {
                values.put(inputName, ((FloatTD) input).getFloatValue());
            }
        }
        study.add(new StudyDataset(values));
        if (fitStepSizeToBounds) {
            if (componentContext.getExecutionCount() <= steps) {
                componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                    typedDatumFactory.createFloat(calculateDesignVariable(componentContext.getExecutionCount())));
            } else {
                componentContext.closeOutput(ParametricStudyComponentConstants.OUTPUT_NAME);
            }
        } else {
            if (from + currentStep * stepSize <= to) {
                componentContext.writeOutput(ParametricStudyComponentConstants.OUTPUT_NAME,
                    typedDatumFactory.createFloat(from + currentStep * stepSize));
            } else {
                componentContext.closeOutput(ParametricStudyComponentConstants.OUTPUT_NAME);
            }
            currentStep++;
        }

    }

    @Override
    public void dispose() {
        if (study != null) {
            study.clearStudy();
        }
    }

}
