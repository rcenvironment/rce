/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.optimizer.execution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.model.api.LazyDisposal;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Optimizer implementation of {@link Component}.
 * 
 * @author Sascha Zur
 */
@LazyDisposal
public class OptimizerComponent extends AbstractNestedLoopComponent {

    private static final String INPUT_PREFIX_CONSTANT = "f81ec917-2221-4bcd-ac17-1c1cef6e08a5_input:";

    private static final String ITERATION = "Iteration";

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

    private OptimizerResultSet dataset = null;

    private String algorithm;

    private OptimizerAlgorithmExecutor optimizer;

    private final ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

    private Map<String, MethodDescription> methodConfigurations;

    private Map<String, Double> startValues;

    private Integer iterationCount = 0;

    private Map<String, Double> lowerBoundsStartValues;

    private Map<String, Double> upperBoundsStartValues;

    private boolean initFailed = false;

    private boolean optimizerStarted;

    private OptimizerComponentHistoryDataItem historyDataItem;

    private Map<String, TypedDatum> runtimeViewValues = new HashMap<>();

    private Map<Integer, Map<String, Double>> iterationData;

    private Map<Integer, Map<String, TypedDatum>> dataForwarded = new HashMap<>();

    private Map<String, Double> stepValues;

    private void prepareExternalProgram() throws ComponentException {
        Map<String, Map<String, Double>> boundMaps = new HashMap<>();
        boundMaps.put("lower", lowerBoundsStartValues);
        boundMaps.put("upper", upperBoundsStartValues);
        optimizer = optimizerAlgorithmExecutorFactoryRegistry.createAlgorithmProviderInstance(
            methodConfigurations.get(algorithm.split(COMMA)[0]).getOptimizerPackage(),
            methodConfigurations, outputValues, input, componentContext, boundMaps, stepValues);
        programThreadInterrupted = false;

        ConcurrencyUtils.getAsyncTaskService().execute(optimizer);
        if (optimizer.isInitFailed()) {
            throw new ComponentException("Failed to prepare optimizer", optimizer.getStartFailedException());
        }
    }

    private void manageNewInput(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables, Map<String, Double> constraintVariablesGradients) {
        Set<String> inputValues = componentContext.getInputsWithDatum();
        Map<String, Double> iteration = iterationData.get(iterationCount);
        boolean gotRealInput = false;
        for (String e : input) {
            if (inputValues.contains(e)) {
                gotRealInput = true;
                if (componentContext.getInputDataType(e) == DataType.Vector) {
                    if (componentContext.readInput(e).getDataType() == DataType.NotAValue) {
                        for (int i = 0; i < Integer.parseInt(componentContext.getOutputMetaDataValue(
                            e.substring(e.lastIndexOf(OptimizerComponentConstants.GRADIENT_DELTA) + 1),
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                            convertValue(inputVariables, inputVariablesGradients, constraintVariables, e
                                + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                componentContext.readInput(e), iteration);
                        }
                    } else {
                        VectorTD vector = (VectorTD) componentContext.readInput(e);
                        for (int i = 0; i < vector.getRowDimension(); i++) {
                            convertValue(inputVariables, inputVariablesGradients, constraintVariables, e
                                + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                vector.getFloatTDOfElement(i), iteration);
                        }
                    }
                } else {
                    convertValue(inputVariables, inputVariablesGradients, constraintVariables, e,
                        componentContext.readInput(e), iteration);
                }
            }
        }
        if (gotRealInput) {
            createNewResultfile(iteration);

            fillRuntimeView(inputVariables, inputVariablesGradients, constraintVariables);
        }
    }

    private void createNewResultfile(Map<String, Double> iteration) {
        iterationData.put(iterationCount, iteration);
        if (!componentContext.getInputsWithDatum().isEmpty() && historyDataItem != null) {
            File resultFile;
            try {
                List<String> outputs = (new LinkedList<>(componentContext.getOutputs()));
                Collections.sort(outputs);
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("OptimizerResultFile*.csv");
                writeResultToCSVFile(resultFile.getAbsolutePath());
                FileReferenceTD resultFileReference =
                    componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(componentContext,
                        resultFile, "Result.csv");

                historyDataItem.setResultFileReference(resultFileReference.getFileReference());
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(resultFile);
            } catch (IOException e) {
                String errorMessage = "Failed to store result file into the data management"
                    + "; it is not available in the workflow data browser";
                String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOGGER, errorMessage, e);
                componentLog.componentError(errorMessage, e, errorId);

            }
        }
    }

    private void writeResultToCSVFile(String path) throws IOException {

        if (path != null && !iterationData.isEmpty()) {
            List<String> orderedOutputs = new LinkedList<>(output);
            List<String> orderedInputs = new LinkedList<>(input);

            List<String> insert = new LinkedList<>();
            List<String> remove = new LinkedList<>();
            for (String outputName : orderedOutputs) {
                if (componentContext.getOutputDataType(outputName) == DataType.Vector) {
                    remove.add(outputName);
                    for (int i = 0; i < Integer.parseInt(
                        componentContext.getOutputMetaDataValue(outputName, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                        insert.add(outputName + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i);
                    }
                }
            }
            for (String toRemove : remove) {
                orderedOutputs.remove(toRemove);
            }
            for (String toInsert : insert) {
                orderedOutputs.add(toInsert);
            }

            insert = new LinkedList<>();
            remove = new LinkedList<>();
            for (String inputName : orderedInputs) {
                if (componentContext.getInputDataType(inputName) == DataType.Vector) {
                    int vectorSize = 0;
                    if (inputName.contains(OptimizerComponentConstants.GRADIENT_DELTA)) {
                        String outputName =
                            inputName.substring(inputName.lastIndexOf(OptimizerComponentConstants.GRADIENT_DELTA) + 1);
                        vectorSize = Integer.parseInt(
                            componentContext.getOutputMetaDataValue(outputName, OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                    } else {
                        vectorSize = Integer.parseInt(
                            componentContext.getInputMetaDataValue(inputName, OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                    }

                    remove.add(inputName);
                    for (int i = 0; i < vectorSize; i++) {
                        insert.add(inputName + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i);
                    }
                }
            }
            for (String toRemove : remove) {
                orderedInputs.remove(toRemove);
            }
            for (String toInsert : insert) {
                orderedInputs.add(toInsert);
            }

            Collections.sort(orderedOutputs);
            Collections.sort(orderedInputs);
            try (FileWriter fw = new FileWriter(new File(path));
                CSVPrinter printer = CSVFormat.newFormat(';').withIgnoreSurroundingSpaces()
                    .withAllowMissingColumnNames().withRecordSeparator("\n").print(fw)) {
                printer.print(ITERATION);
                for (String outputName : orderedOutputs) {
                    printer.print(outputName);
                }
                for (String inputName : orderedInputs) {
                    printer.print(inputName);
                }
                printer.println();
                // Iteration start at 1.
                for (Integer i = 1; i < iterationData.keySet().size() + 1; i++) {
                    Map<String, Double> iteration = iterationData.get(i);
                    printer.print(i);
                    for (String out : orderedOutputs) {
                        printer.print(iteration.get(out));
                    }
                    for (String in : orderedInputs) {
                        printer.print(iteration.get(INPUT_PREFIX_CONSTANT + in));
                    }
                    printer.println();
                    printer.flush();
                }
            }
        }

    }

    private void fillRuntimeView(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables) {
        for (String key : inputVariables.keySet()) {
            if (inputVariables.get(key).isNaN()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(inputVariables.get(key)));
            }
        }
        for (String key : inputVariablesGradients.keySet()) {
            if (inputVariablesGradients.get(key).isNaN()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(inputVariablesGradients.get(key)));
            }
        }
        for (String key : constraintVariables.keySet()) {
            if (constraintVariables.get(key).isNaN()) {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(CONST_1E99));
            } else {
                runtimeViewValues.put(key, typedDatumFactory.createFloat(constraintVariables.get(key)));
            }
        }
        runtimeViewValues.put(ITERATION, typedDatumFactory.createInteger(iterationCount));
        dataset = new OptimizerResultSet(runtimeViewValues, componentContext.getExecutionIdentifier());
        resultPublisher.add(dataset);
        runtimeViewValues = new HashMap<>();
    }

    private void convertValue(Map<String, Double> inputVariables, Map<String, Double> inputVariablesGradients,
        Map<String, Double> constraintVariables, String variableName, TypedDatum value, Map<String, Double> iteration) {
        double inputField;
        if (value.getDataType() != DataType.NotAValue) {
            inputField = ((FloatTD) value).getFloatValue();
        } else {
            inputField = Double.NaN;
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
        iteration.put(INPUT_PREFIX_CONSTANT + variableName, inputField);
    }

    private void terminateExecutor() {
        if (optimizer != null) {
            optimizer.closeConnection();
            programThreadInterrupted = true;
            optimizer.stop();
            File workDir = optimizer.getWorkingDir();
            optimizer.dispose();
            optimizer = null;
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(workDir);
            } catch (IOException e) {
                LOGGER.error("Failed to delete temporary directory", e);
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
                ITERATION, //
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
            if (e.endsWith(OptimizerComponentConstants.STARTVALUE_SIGNATURE)
                || e.endsWith(OptimizerComponentConstants.STEP_VALUE_SIGNATURE)
                || e.endsWith(LoopComponentConstants.ENDPOINT_STARTVALUE_SUFFIX)) {
                runInitial = false;
            }
        }
        return runInitial;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void startNestedComponentSpecific() throws ComponentException {

        optimizerResultService = componentContext.getService(OptimizerResultService.class);
        optimizerAlgorithmExecutorFactoryRegistry = componentContext.getService(OptimizerAlgorithmExecutorFactoryRegistry.class);
        typedDatumFactory = componentContext.getService(TypedDatumService.class).getFactory();
        iterationData = new TreeMap<>();
        output = new HashSet<>();
        output.addAll(componentContext.getOutputs());
        List<String> toRemove = new LinkedList<>();

        for (String e : output) {
            if (e.endsWith(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                toRemove.add(e);
            }
            if (e.endsWith(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE)) {
                toRemove.add(e);
            }
            if (e.endsWith(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME)) {
                toRemove.add(e);
            }
            if (e.endsWith(OptimizerComponentConstants.DERIVATIVES_NEEDED)) {
                toRemove.add(e);
            }
            if (componentContext.isDynamicInput(e)
                && componentContext.getDynamicInputIdentifier(e).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                toRemove.add(e);
            }
        }
        for (String e : toRemove) {
            output.remove(e);
        }

        input = new HashSet<>();
        for (String inputName : componentContext.getInputs()) {
            if (componentContext.getDynamicInputIdentifier(inputName).equals(OptimizerComponentConstants.ID_OBJECTIVE)
                || componentContext.getDynamicInputIdentifier(inputName).equals(OptimizerComponentConstants.ID_CONSTRAINT)
                || componentContext.getDynamicInputIdentifier(inputName).equals(OptimizerComponentConstants.ID_GRADIENTS)) {
                input.add(inputName);
            }
        }
        algorithm = componentContext.getConfigurationValue(OptimizerComponentConstants.ALGORITHMS);
        String configurations = componentContext.getConfigurationValue(
            OptimizerComponentConstants.METHODCONFIGURATIONS);
        if (output.isEmpty() || input.isEmpty()) {
            throw new ComponentException("Design variables or target functions not configured");
        }
        try {
            if (configurations != null && !configurations.isEmpty()) {
                methodConfigurations = mapper.readValue(configurations, new HashMap<String, MethodDescription>().getClass());
                for (String key : methodConfigurations.keySet()) {
                    methodConfigurations.put(key, mapper.convertValue(methodConfigurations.get(key), MethodDescription.class));
                }
            } else {
                methodConfigurations = OptimizerFileLoader.getAllMethodDescriptions("/optimizer");
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to load or parse method file", e);
        }
        String[] splittedAlgorithms = algorithm.split(",");
        for (String alg : splittedAlgorithms) {
            if (!methodConfigurations.containsKey(alg)) {
                throw new ComponentException("Failed to load algorithm '" + alg + "'; not found");
            }
        }
        resultPublisher = optimizerResultService.createPublisher(
            componentContext.getExecutionIdentifier(),
            componentContext.getInstanceName(),
            createStructure());
        optimizerStarted = false;
        boolean runInitial = treatStartAsComponentRun();
        if (runInitial) {
            super.processInputs();
        }
    }

    @Override
    protected void processInputsNestedComponentSpecific() throws ComponentException {
        initializeNewHistoryDataItem();
        if (!optimizerStarted) {
            firstRun();
        } else {
            if (!initFailed) {
                storeDataForwarded();

                Map<String, Double> inputVariables = new HashMap<>();
                Map<String, Double> constraintVariables = new HashMap<>();
                Map<String, Double> inputVariablesGradients = new HashMap<>();
                Map<String, Double> constraintVariablesGradients = new HashMap<>();
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
                throw new ComponentException("Failed to initialize optimizer");
            }
            if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))
                && optimizer != null) {
                optimizer.writeHistoryDataItem(historyDataItem);
                writeFinalHistoryDataItem();
            }
        }

        iterationCount++;

    }

    private void storeDataForwarded() {
        Integer iteration = iterationCount;
        for (String inputName : componentContext.getInputsWithDatum()) {
            if (componentContext.isDynamicInput(inputName)
                && componentContext.getDynamicInputIdentifier(inputName).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                if (!dataForwarded.containsKey(iteration)) {
                    dataForwarded.put(iterationCount, new HashMap<String, TypedDatum>());
                }
                dataForwarded.get(iteration).put(inputName, componentContext.readInput(inputName));
            }
        }
    }

    private void firstRun() throws ComponentException {
        final String errorMessage = "Failed to start optimizer";
        outputValues = new HashMap<>();
        startValues = new HashMap<>();
        stepValues = new HashMap<>();
        lowerBoundsStartValues = new HashMap<>();
        upperBoundsStartValues = new HashMap<>();
        for (String e : output) {
            String hasStartValue =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_HAS_STARTVALUE);
            String hasStep =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_USE_STEP);
            String hasUseUnifiedStep =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_USE_UNIFIED_STEP);
            String hasBoundValues =
                componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
            String startValue = componentContext.getOutputMetaDataValue(e, OptimizerComponentConstants.META_STARTVALUE);
            if (startValue.equals("-")) {
                startValue = "";
            }
            getStartAndStepValues(e, hasStartValue, hasStep, hasUseUnifiedStep, startValue);
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
                            ((VectorTD) componentContext.readInput(e)).getFloatTDOfElement(i).getFloatValue());
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
                            ((VectorTD) componentContext.readInput(e)).getFloatTDOfElement(i).getFloatValue());
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
        prepareExternalProgram();
        if (optimizer != null && !optimizer.isInitFailed() && !(optimizer.getStartFailed().get())) {
            try {
                if (optimizer.initializationLoop()) {
                    optimizer.readOutputFileFromExternalProgram(outputValues);
                    sendValuesNestedComponentSpecific();
                } else {
                    if (!optimizer.getStartFailed().get()) {
                        sendFinalValues();
                        componentContext.closeAllOutputs();
                    } else {
                        throw new ComponentException("Could not start optimizer. Maybe binaries are missing or not compatible with system.",
                            optimizer.getStartFailedException());
                    }
                }
                optimizerStarted = true;
            } catch (IOException e) {
                componentContext.getLog().componentError("Failed to initialize optimizer: " + e.getMessage());
                LOGGER.error("Failed to initialize optimizer", e);
                initFailed = true;
            }
        } else {
            throw new ComponentException(errorMessage);
        }
    }

    private void getStartAndStepValues(String e, String hasStartValue, String hasStep, String hasUseUnifiedStep, String startValue) {
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
        if (Boolean.parseBoolean(hasStep) && Boolean.parseBoolean(hasUseUnifiedStep)) {
            if (componentContext.getOutputDataType(e) == DataType.Vector) {
                for (int i = 0; i < Integer.parseInt(componentContext
                    .getOutputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                    stepValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                        Double.parseDouble(componentContext
                            .getOutputMetaDataValue(e, OptimizerComponentConstants.META_STEP)));
                }
            } else {
                stepValues.put(e, Double.parseDouble(componentContext
                    .getOutputMetaDataValue(e, OptimizerComponentConstants.META_STEP)));
            }
        } else if (Boolean.parseBoolean(hasStep) && !Boolean.parseBoolean(hasUseUnifiedStep)) {
            if (componentContext.getOutputDataType(e) == DataType.Vector) {
                VectorTD stepVector = ((VectorTD) componentContext.readInput(e + OptimizerComponentConstants.STEP_VALUE_SIGNATURE));
                for (int i = 0; i < Integer.parseInt(componentContext
                    .getOutputMetaDataValue(e, OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                    stepValues.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                        stepVector.getFloatTDOfElement(i).getFloatValue());
                }
            } else {
                stepValues.put(e,
                    ((FloatTD) componentContext.readInput(e + OptimizerComponentConstants.STEP_VALUE_SIGNATURE)).getFloatValue());
            }
        }
    }

    @Override
    protected void sendValuesNestedComponentSpecific() {
        if (optimizerStarted && optimizer != null && !optimizer.isStopped()) {
            Map<String, Double> iteration = new HashMap<>();
            for (String e : output) {
                if (outputValues.get(e) != null) {
                    writeOutput(e, outputValues.get(e));
                    if (componentContext.getOutputDataType(e) == DataType.Vector) {
                        for (int i = 0; i < Integer.parseInt(componentContext.getOutputMetaDataValue(e,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE)); i++) {
                            runtimeViewValues.put("Output: " + e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                ((VectorTD) outputValues.get(e)).getFloatTDOfElement(i));
                            iteration.put(e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i,
                                ((VectorTD) outputValues.get(e)).getFloatTDOfElement(i).getFloatValue());
                        }
                    } else {
                        runtimeViewValues.put("Output: " + e, outputValues.get(e));
                        iteration.put(e, ((FloatTD) outputValues.get(e)).getFloatValue());
                    }
                } else {
                    LOGGER.info(StringUtils.format("Could not send out output %s because the value was null", e));
                }
            }
            writeOutput(OptimizerComponentConstants.ITERATION_COUNT_ENDPOINT_NAME,
                typedDatumFactory.createInteger(iterationCount));
            if (optimizer != null) {
                writeOutput(OptimizerComponentConstants.DERIVATIVES_NEEDED,
                    typedDatumFactory.createBoolean(optimizer.getDerivativedNeeded()));
            }
            iterationData.put(iterationCount, iteration);
        }
    }

    @Override
    protected void resetNestedComponentSpecific() {
        programThreadInterrupted = false;
        outputValues.clear();
        runtimeViewValues.clear();
        startValues = new HashMap<>();
        lowerBoundsStartValues = new HashMap<>();
        upperBoundsStartValues = new HashMap<>();
        stepValues = new HashMap<>();
        iterationCount = 0;
        optimizerStarted = false;
    }

    @Override
    protected void finishLoopNestedComponentSpecific() {
        writeFinalHistoryDataItem();
    }

    @Override
    protected boolean isDoneNestedComponentSpecific() {
        return optimizerStarted && (programThreadInterrupted || optimizer == null || optimizer.isStopped());
    }

    @Override
    protected void sendFinalValues() throws ComponentException {
        if (optimizer != null) {
            optimizer.setIterationData(iterationData);
            int optimalRunNumber = optimizer.getOptimalRunNumber();
            Map<String, Double> optimum = iterationData.get(optimalRunNumber);
            if (optimalRunNumber != Integer.MIN_VALUE && optimum != null) {
                for (String e : output) {
                    if (componentContext.getOutputDataType(e) == DataType.Vector) {
                        int vectorSize = Integer.parseInt(componentContext.getOutputMetaDataValue(e,
                            OptimizerComponentConstants.METADATA_VECTOR_SIZE));
                        VectorTD optimumVector =
                            typedDatumFactory.createVector(vectorSize);
                        for (int i = 0; i < vectorSize; i++) {
                            String vectorOutputName = e + OptimizerComponentConstants.OPTIMIZER_VECTOR_INDEX_SYMBOL + i;
                            runtimeViewValues.put(vectorOutputName, typedDatumFactory.createFloat(optimum.get(vectorOutputName)));
                            optimumVector.setFloatTDForElement(typedDatumFactory.createFloat(optimum.get(vectorOutputName)), i);
                        }
                        writeOutput(e + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX, optimumVector);
                    } else {
                        writeOutput(e + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                            typedDatumFactory.createFloat(optimum.get(e)));
                    }
                }
                sendFinalValuesForwarded(optimalRunNumber);
            } else {
                throw new ComponentException("Failed to read optimal design variables");
            }
        }
        terminateExecutor();
    }

    private void sendFinalValuesForwarded(Integer optimalRunNumber) {
        if (optimalRunNumber == null || optimalRunNumber == Integer.MIN_VALUE) {
            componentContext.getLog().componentError("Internal error: iteration of optimal design variable cannot be determined");
            // should not happen
            return;
        }
        if (!dataForwarded.isEmpty()) {
            for (String inputName : dataForwarded.get(optimalRunNumber).keySet()) {
                writeOutput(inputName + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                    dataForwarded.get(optimalRunNumber).get(inputName));
            }
        }
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
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        terminateExecutor();
    }

}
