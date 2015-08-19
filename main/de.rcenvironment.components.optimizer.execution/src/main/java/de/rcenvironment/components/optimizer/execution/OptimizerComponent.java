/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.execution;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.components.optimizer.common.OptimizerComponentHistoryDataItem;
import de.rcenvironment.components.optimizer.common.OptimizerFileLoader;
import de.rcenvironment.components.optimizer.common.OptimizerPublisher;
import de.rcenvironment.components.optimizer.common.OptimizerResultService;
import de.rcenvironment.components.optimizer.common.OptimizerResultSet;
import de.rcenvironment.components.optimizer.common.ResultStructure;
import de.rcenvironment.components.optimizer.common.execution.OptimizerAlgorithmExecutor;
import de.rcenvironment.components.optimizer.execution.algorithms.registry.OptimizerAlgorithmExecutorFactoryRegistry;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Optimizer implementation of {@link Component}.
 * 
 * @author Sascha Zur
 */
@LazyDisposal
public class OptimizerComponent extends AbstractNestedLoopComponent {

    private static final double CONST_1E99 = 1E99;

    private static final String COMMA = ",";

    private static OptimizerResultService optimizerResultService;

    private static OptimizerAlgorithmExecutorFactoryRegistry optimizerAlgorithmExecutorFactoryRegistry;

    private static final Log LOGGER = LogFactory.getLog(OptimizerComponent.class);

    private static TypedDatumFactory typedDatumFactory;

    /** Flag. */
    public boolean programThreadInterrupted;

    private OptimizerPublisher resultPublisher;

    private Collection<String> output;

    private Collection<String> input;

    private Map<String, TypedDatum> outputValues;

    private Map<String, TypedDatum> runtimeViewValues = new HashMap<String, TypedDatum>();

    private OptimizerResultSet dataset = null;

    private String algorithm;

    private OptimizerAlgorithmExecutor optimizer;

    private Thread programThread;

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, MethodDescription> methodConfigurations;

    private Map<String, Double> startValues;

    private int iterationCount = 0;

    private Map<String, Double> lowerBoundsStartValues;

    private Map<String, Double> upperBoundsStartValues;

    private boolean initFailed = false;

    private boolean optimizerStarted;

    private OptimizerComponentHistoryDataItem historyDataItem;

    private void prepareExternalProgram() throws IOException, ComponentException {
        LOGGER.debug("Preparing external program.");
        Map<String, Map<String, Double>> boundMaps = new HashMap<String, Map<String, Double>>();
        boundMaps.put("lower", lowerBoundsStartValues);
        boundMaps.put("upper", upperBoundsStartValues);
        optimizer = optimizerAlgorithmExecutorFactoryRegistry.createAlgorithmProviderInstance(
            methodConfigurations.get(algorithm.split(COMMA)[0]).getOptimizerPackage(),
            algorithm, methodConfigurations, outputValues, input, componentContext, boundMaps);
        programThreadInterrupted = false;
        programThread = new Thread(optimizer);
        programThread.start();
        if (!programThread.isAlive() || optimizer.isInitFailed()) {
            throw new ComponentException("External program could not be started. ");
        }
        LOGGER.debug("External program prepared");
    }

    private void manageNewInput(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables, Map<String, Double> constraintVariablesGradients) {
        Set<String> inputValues = componentContext.getInputsWithDatum();
        for (String e : input) {
            if (inputValues.contains(e)) {
                if (componentContext.getInputDataType(e) == DataType.Vector) {
                    if (componentContext.readInput(e).getDataType() == DataType.NotAValue) {
                        for (int i = 0; i < Integer.parseInt(componentContext.getOutputMetaDataValue(
                            e.substring(e.lastIndexOf(OptimizerComponentConstants.GRADIENT_DELTA) + 1),
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                            convertValue(inputVariables, inputVariablesGradients, constraintVariables, e
                                + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                componentContext.readInput(e));
                        }
                    } else {
                        VectorTD vector = (VectorTD) componentContext.readInput(e);
                        for (int i = 0; i < vector.getRowDimension(); i++) {
                            convertValue(inputVariables, inputVariablesGradients, constraintVariables, e
                                + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                vector.getFloatTDOfElement(i));
                        }
                    }
                } else {
                    convertValue(inputVariables, inputVariablesGradients, constraintVariables, e,
                        componentContext.readInput(e));
                }
            }
        }

        fillRuntimeView(inputVariables, inputVariablesGradients, constraintVariables);
    }

    private void fillRuntimeView(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables) {
        for (String key : inputVariables.keySet()) {
            if (inputVariables.get(key).isInfinite()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(inputVariables.get(key)));
            }
        }
        for (String key : inputVariablesGradients.keySet()) {
            if (inputVariablesGradients.get(key).isInfinite()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(inputVariablesGradients.get(key)));
            }
        }
        for (String key : constraintVariables.keySet()) {
            if (constraintVariables.get(key).isInfinite()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(constraintVariables.get(key)));
            }
        }
        runtimeViewValues.put("Iteration", typedDatumFactory.createInteger(iterationCount));
        dataset = new OptimizerResultSet(runtimeViewValues, componentContext.getExecutionIdentifier());
        resultPublisher.add(dataset);
        runtimeViewValues = new HashMap<String, TypedDatum>();
    }

    private void convertValue(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables, String variableName, TypedDatum value) {
        double inputField;
        if (value.getDataType() != DataType.NotAValue) {
            inputField = ((FloatTD) value).getFloatValue();
        } else {
            inputField = Double.POSITIVE_INFINITY;
        }
        if (variableName.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
            inputVariablesGradients.put(variableName, inputField);
        } else {
            String identifier;
            if (variableName.contains(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)
                && componentContext.getDynamicInputIdentifier(
                    variableName.substring(0, variableName.indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL))) != null) {
                identifier = componentContext.getDynamicInputIdentifier(
                    variableName.substring(0, variableName.indexOf(OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL)));
            } else {
                identifier = componentContext.getDynamicInputIdentifier(variableName);
            }

            if (identifier.equals(OptimizerComponentConstants.ID_OBJECTIVE)) {
                inputVariables.put(variableName, inputField);
            } else {
                constraintVariables.put(variableName, inputField);
            }
        }
    }

    private void terminateExecutor() {
        if (optimizer != null) {
            optimizer.closeConnection();
            programThreadInterrupted = true;
            optimizer.stop();
            programThread.interrupt();
            File workDir = optimizer.getWorkingDir();
            optimizer.dispose();
            optimizer = null;
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(workDir);
            } catch (IOException e) {
                LOGGER.error("Optimizer: ", e);
            }
        }

    }

    private ResultStructure createStructure() {
        final ResultStructure structure = new ResultStructure();
        // outputs are dimensions
        for (String e : output) {
            if (componentContext.getOutputDataType(e) == DataType.Vector) {
                for (int i = 0; i < Integer.parseInt(
                    componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                    final de.rcenvironment.components.optimizer.common.Dimension dimension =
                        new de.rcenvironment.components.optimizer.common.Dimension(
                            e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i, //
                            DataType.Float.getDisplayName(), //
                            true);
                    structure.addDimension(dimension);
                }
            } else {

                final de.rcenvironment.components.optimizer.common.Dimension dimension =
                    new de.rcenvironment.components.optimizer.common.Dimension(
                        e, //
                        componentContext.getOutputDataType(e).getDisplayName(), //
                        true);
                structure.addDimension(dimension);
            }
        }
        final de.rcenvironment.components.optimizer.common.Dimension dimension =
            new de.rcenvironment.components.optimizer.common.Dimension(
                "Iteration", //
                DataType.Integer.getDisplayName(), //
                true);
        structure.addDimension(dimension);
        // inputs are measures
        for (String e : input) {
            if (componentContext.getInputDataType(e) == DataType.Vector) {
                int vecSize =
                    Integer.parseInt(componentContext
                        .getOutputMetaDataValue(e.substring(e.lastIndexOf(OptimizerComponentConstants.GRADIENT_DELTA) + 1),
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                for (int i = 0; i < vecSize; i++) {
                    final de.rcenvironment.components.optimizer.common.Measure measure =
                        new de.rcenvironment.components.optimizer.common.Measure(
                            e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i, //
                            DataType.Float.getDisplayName());
                    structure.addMeasure(measure);
                }
            } else {
                final de.rcenvironment.components.optimizer.common.Measure measure =
                    new de.rcenvironment.components.optimizer.common.Measure(
                        e, //
                        DataType.Float.getDisplayName());
                structure.addMeasure(measure);

            }

        }
        return structure;
    }

    @Override
    public boolean treatStartAsComponentRun() {
        boolean runInitial = true;
        for (String e : componentContext.getInputs()) {
            if (e.endsWith(OptimizerComponentConstants.STARTVALUE_SIGNATURE)) {
                runInitial = false;
            }
        }
        return runInitial;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void startHook() throws ComponentException {

        optimizerResultService = componentContext.getService(OptimizerResultService.class);
        optimizerAlgorithmExecutorFactoryRegistry = componentContext.getService(OptimizerAlgorithmExecutorFactoryRegistry.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        output = new HashSet<String>();
        output.addAll(componentContext.getOutputs());
        List<String> toRemove = new LinkedList<String>();

        for (String e : output) {
            if (e.endsWith(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                toRemove.add(e);
            }
            if (e.endsWith(OptimizerComponentConstants.OPTIMIZER_FINISHED_OUTPUT)) {
                toRemove.add(e);
            }
            if (e.endsWith(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME)) {
                toRemove.add(e);
            }
            if (e.endsWith(OptimizerComponentConstants.DERIVATIVES_NEEDED)) {
                toRemove.add(e);
            }
            if (e.endsWith(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
                toRemove.add(e);
            }
        }
        for (String e : toRemove) {
            output.remove(e);
        }
        input = new HashSet<String>();
        input.addAll(componentContext.getInputs());
        for (String e : input) {
            if (e.endsWith(OptimizerComponentConstants.STARTVALUE_SIGNATURE)) {
                toRemove.add(e);
            }
            if (e.endsWith(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
                toRemove.add(e);
            }
        }
        for (String e : toRemove) {
            input.remove(e);
        }
        algorithm = componentContext.getConfigurationValue(OptimizerComponentConstants.ALGORITHMS);
        String configurations = componentContext.getConfigurationValue(
            OptimizerComponentConstants.METHODCONFIGURATIONS);
        if (output.isEmpty() || input.isEmpty()) {
            throw new ComponentException("Design Variables or Target Functions not defined!");
        }
        try {
            if (configurations != null && !configurations.equals("")) {
                methodConfigurations = mapper.readValue(configurations, new HashMap<String, MethodDescription>().getClass());
                for (String key : methodConfigurations.keySet()) {
                    methodConfigurations.put(key, mapper.convertValue(methodConfigurations.get(key), MethodDescription.class));
                }
            } else {
                methodConfigurations = OptimizerFileLoader.getAllMethodDescriptions("optimizer");
            }
        } catch (JsonParseException e) {
            LOGGER.error("Could not parse method file ", e);
        } catch (JsonMappingException e) {
            LOGGER.error("Could not map method file ", e);
        } catch (IOException e) {
            LOGGER.error("Could not load method file ", e);
        }
        String[] splittedAlgorithms = algorithm.split(",");
        for (String alg : splittedAlgorithms) {
            if (!methodConfigurations.containsKey(alg)) {
                throw new ComponentException("Algorithm " + alg + " could not be loaded");
            }
        }
        resultPublisher = optimizerResultService.createPublisher(
            componentContext.getExecutionIdentifier(),
            componentContext.getInstanceName(),
            createStructure());
        LOGGER.debug("Optimizer Component prepared");
        optimizerStarted = false;
        boolean runInitial = treatStartAsComponentRun();
        if (runInitial) {
            super.processInputs();
        }
    }

    @Override
    protected void processInputsHook() throws ComponentException {
        initializeNewHistoryDataItem();
        if (!optimizerStarted) {
            firstRun();
        } else {
            if (!initFailed) {
                Map<String, Double> inputVariables = new HashMap<String, Double>();
                Map<String, Double> constraintVariables = new HashMap<String, Double>();
                Map<String, Double> inputVariablesGradients = new HashMap<String, Double>();
                Map<String, Double> constraintVariablesGradients = new HashMap<String, Double>();
                // isolate variables and save result
                manageNewInput(inputVariables, inputVariablesGradients,
                    constraintVariables, constraintVariablesGradients);

                // start new algorithm run
                if (optimizer != null && !optimizer.isStopped()) {
                    optimizer.runStep(inputVariables, inputVariablesGradients,
                        constraintVariables, constraintVariablesGradients, outputValues);

                }
            } else {
                if (optimizer != null) {
                    terminateExecutor();
                }
                throw new ComponentException("Initialization for optimizer failed");
            }
            if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
                optimizer.writeHistoryDataItem(historyDataItem);
                writeFinalHistoryDataItem();
            }
        }
        iterationCount++;

    }

    private void firstRun() throws ComponentException {
        outputValues = new HashMap<String, TypedDatum>();
        startValues = new HashMap<String, Double>();
        lowerBoundsStartValues = new HashMap<String, Double>();
        upperBoundsStartValues = new HashMap<String, Double>();
        for (String e : output) {
            String hasStartValue =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_HAS_STARTVALUE);
            String hasBoundValues =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
            String startValue = componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_STARTVALUE);
            if (startValue.equals("-")) {
                startValue = "";
            }
            if ((hasStartValue != null && Boolean.parseBoolean(hasStartValue) && !startValue.isEmpty())
                || (hasStartValue == null && !startValue.isEmpty())) {
                if (componentContext.getOutputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < Integer.parseInt(componentContext
                        .getOutputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                        startValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            Double.parseDouble(componentContext
                                .getOutputMetaDataValue(e, OptimizerComponentConstants.META_STARTVALUE)));
                    }
                } else {
                    startValues.put(e, Double.parseDouble(startValue));
                }
            } else if (!Boolean.parseBoolean(hasStartValue)) {
                outputValues.put(e, componentContext.readInput(e + OptimizerComponentConstants.STARTVALUE_SIGNATURE));
            }
            if (hasBoundValues != null && Boolean.parseBoolean(hasBoundValues)) {
                if (componentContext.getOutputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < Integer.parseInt(componentContext
                        .getOutputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                        lowerBoundsStartValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            Double.parseDouble(componentContext
                                .getOutputMetaDataValue(e, OptimizerComponentConstants.META_LOWERBOUND)));
                        upperBoundsStartValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            Double.parseDouble(componentContext
                                .getOutputMetaDataValue(e, OptimizerComponentConstants.META_UPPERBOUND)));
                    }
                } else {
                    lowerBoundsStartValues.put(e,
                        Double.parseDouble(componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_LOWERBOUND)));
                    upperBoundsStartValues.put(e,
                        Double.parseDouble(componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_UPPERBOUND)));
                }
            }
        }
        for (String e : input) {
            String hasBoundValues =
                componentContext.getInputMetaDataValue(e, OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
            if (hasBoundValues != null && Boolean.parseBoolean(hasBoundValues)) {
                if (componentContext.getInputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < Integer.parseInt(componentContext
                        .getInputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                        lowerBoundsStartValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            Double.parseDouble(componentContext.getInputMetaDataValue(e, OptimizerComponentConstants.META_LOWERBOUND)));
                        upperBoundsStartValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            Double.parseDouble(componentContext.getInputMetaDataValue(e, OptimizerComponentConstants.META_UPPERBOUND)));
                    }
                } else {
                    lowerBoundsStartValues.put(e,
                        Double.parseDouble(componentContext.getInputMetaDataValue(e, OptimizerComponentConstants.META_LOWERBOUND)));
                    upperBoundsStartValues.put(e,
                        Double.parseDouble(componentContext.getInputMetaDataValue(e, OptimizerComponentConstants.META_UPPERBOUND)));
                }
            }
        }
        for (String e : componentContext.getInputsWithDatum()) {
            if (e.contains(OptimizerComponentConstants.BOUNDS_STARTVALUE_LOWER_SIGNITURE)) {
                if (componentContext.getInputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < ((VectorTD) componentContext.readInput(e)).getRowDimension(); i++) {
                        lowerBoundsStartValues.put(e.substring(0, e.indexOf(OptimizerComponentConstants.BOUNDS_STARTVALUE_LOWER_SIGNITURE))
                            + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            ((VectorTD) componentContext.readInput(e)).getFloatTDOfElement(0).getFloatValue());
                    }
                } else {
                    lowerBoundsStartValues.put(e.substring(0, e.indexOf(OptimizerComponentConstants.BOUNDS_STARTVALUE_LOWER_SIGNITURE)),
                        ((FloatTD) componentContext.readInput(e)).getFloatValue());
                }
            }
            if (e.contains(OptimizerComponentConstants.BOUNDS_STARTVALUE_UPPER_SIGNITURE)) {
                if (componentContext.getInputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < ((VectorTD) componentContext.readInput(e)).getRowDimension(); i++) {
                        upperBoundsStartValues.put(e.substring(0, e.indexOf(OptimizerComponentConstants.BOUNDS_STARTVALUE_UPPER_SIGNITURE))
                            + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            ((VectorTD) componentContext.readInput(e)).getFloatTDOfElement(0).getFloatValue());
                    }
                } else {
                    upperBoundsStartValues.put(e.substring(0, e.indexOf(OptimizerComponentConstants.BOUNDS_STARTVALUE_UPPER_SIGNITURE)),
                        ((FloatTD) componentContext.readInput(e)).getFloatValue());
                }
            }
        }
        for (String e : startValues.keySet()) {
            outputValues.put(e, typedDatumFactory.createFloat(startValues.get(e)));
        }
        try {
            prepareExternalProgram();
        } catch (IOException e) {
            LOGGER.error("Error preparing external program", e);
        }
        if (optimizer != null && !optimizer.isInitFailed() && !(optimizer.getStartFailed().get())) {
            try {
                if (optimizer.initializationLoop()) {
                    optimizer.readOutputFileFromExternalProgram(outputValues);
                    sendValuesHook();
                } else {
                    sendFinalValues();
                    componentContext.closeAllOutputs();
                }
                optimizerStarted = true;
            } catch (IOException e1) {
                initFailed = true;
            }
        } else {
            throw new ComponentException("Could not start optimizer.");
        }
    }

    @Override
    protected void sendValuesHook() {
        if (optimizerStarted && !optimizer.isStopped()) {
            for (String e : output) {
                componentContext.writeOutput(e, outputValues.get(e));
                if (componentContext.getOutputDataType(e) == DataType.Vector) {
                    for (int i = 0; i < Integer.parseInt(componentContext.getOutputMetaDataValue(e,
                        OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                        runtimeViewValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                            ((VectorTD) outputValues.get(e)).getFloatTDOfElement(i));
                    }
                } else {
                    runtimeViewValues.put(e, outputValues.get(e));
                }
            }
            componentContext.writeOutput(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME,
                typedDatumFactory.createInteger(iterationCount));
            componentContext.writeOutput(OptimizerComponentConstants.OPTIMIZER_FINISHED_OUTPUT, typedDatumFactory.createBoolean(false));
            if (optimizer != null) {
                componentContext.writeOutput(OptimizerComponentConstants.DERIVATIVES_NEEDED,
                    typedDatumFactory.createBoolean(optimizer.getDerivativedNeeded()));
            }
        }
    }

    @Override
    protected void resetInnerLoopHook() {
        programThreadInterrupted = false;
        outputValues.clear();
        runtimeViewValues.clear();
        programThread = null;
        startValues = new HashMap<String, Double>();
        lowerBoundsStartValues = new HashMap<String, Double>();
        upperBoundsStartValues = new HashMap<String, Double>();
        iterationCount = 0;
        optimizerStarted = false;
    }

    @Override
    protected void finishLoopHook() {
        writeFinalHistoryDataItem();
    }

    @Override
    protected String getLoopFinishedEndpointName() {
        return ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE;
    }

    @Override
    protected boolean isFinished() {
        return optimizerStarted && (programThreadInterrupted || optimizer == null || optimizer.isStopped());
    }

    @Override
    protected void sendFinalValues() throws ComponentException {
        if (optimizer != null) {
            Map<String, Double> optimum = optimizer.getOptimalDesignVariables();
            if (optimum != null && !optimum.isEmpty()) {
                for (String e : output) {
                    if (componentContext.getOutputDataType(e) == DataType.Vector) {
                        VectorTD optimumVector = typedDatumFactory.createVector(Integer.parseInt(componentContext.getOutputMetaDataValue(e,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)));
                        for (int i = 0; i < Integer.parseInt(componentContext.getOutputMetaDataValue(e,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                            runtimeViewValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                ((VectorTD) outputValues.get(e)).getFloatTDOfElement(i));
                            optimumVector.setFloatTDForElement(((VectorTD) outputValues.get(e)).getFloatTDOfElement(i), i);
                        }
                        componentContext.writeOutput(e + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX, optimumVector);
                    } else {
                        componentContext.writeOutput(e + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                            typedDatumFactory.createFloat(optimum.get(e)));
                    }
                }
                componentContext.writeOutput(OptimizerComponentConstants.OPTIMIZER_FINISHED_OUTPUT, typedDatumFactory.createBoolean(true));
            } else {
                componentContext.printConsoleLine("Could not read optimal design variables", Type.STDERR);
                throw new ComponentException("Could not read optimal design variables");
            }
        }
        terminateExecutor();
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new OptimizerComponentHistoryDataItem();
        }
    }

    private void writeFinalHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    @Override
    protected void sendReset() {
        for (String outputs : output) {
            componentContext.resetOutput(outputs);
        }
    }

    @Override
    public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
        super.onProcessInputsInterrupted(executingThreadHandler);
        if (optimizer != null) {
            optimizer.dispose();
        }
        terminateExecutor();
    }

    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        super.onStartInterrupted(executingThreadHandler);
        terminateExecutor();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        terminateExecutor();
    }

}
