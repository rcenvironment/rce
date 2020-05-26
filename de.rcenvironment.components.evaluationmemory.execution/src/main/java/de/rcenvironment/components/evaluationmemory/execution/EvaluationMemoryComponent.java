/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.evaluationmemory.execution;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants;
import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentConstants.OverlapBehavior;
import de.rcenvironment.components.evaluationmemory.common.EvaluationMemoryComponentHistoryDataItem;
import de.rcenvironment.components.evaluationmemory.execution.internal.EvaluationMemoryAccess;
import de.rcenvironment.components.evaluationmemory.execution.internal.EvaluationMemoryFileAccessService;
import de.rcenvironment.components.evaluationmemory.execution.internal.ToleranceHandling;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of the Evaluation Memory component.
 * 
 * @author Doreen Seider
 */
public class EvaluationMemoryComponent extends DefaultComponent {

    static final double PERCENT_TO_REAL_NUMBER_FACTOR = 0.01;

    /**
     * 'Processing input' modes.
     * 
     * @author Doreen Seider
     */
    private enum Mode {
        Check,
        Store;
    }

    private Log log = LogFactory.getLog(getClass());

    private ComponentLog componentLog;

    private EvaluationMemoryFileAccessService memoryFileAccessService;

    private ComponentDataManagementService dataManagementService;

    private ComponentContext componentContext;

    private boolean considerLoopFailures;

    private EvaluationMemoryAccess memoryAccess;

    private SortedMap<String, DataType> inputsToEvaluate = new TreeMap<>();

    private SortedMap<String, DataType> outputsEvaluationResult = new TreeMap<>();

    private SortedMap<String, TypedDatum> valuesToEvaluate;

    private String memoryFilePath;

    private File memoryFile;

    private EvaluationMemoryComponentHistoryDataItem historyData;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        super.setComponentContext(componentContext);
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
        considerLoopFailures = Boolean.valueOf(componentContext.getConfigurationValue(
            EvaluationMemoryComponentConstants.CONFIG_CONSIDER_LOOP_FAILURES));
    }

    @Override
    public void start() throws ComponentException {

        memoryFileAccessService = componentContext.getService(EvaluationMemoryFileAccessService.class);
        dataManagementService = componentContext.getService(ComponentDataManagementService.class);
        setInputsAndOutputs();

        initializeMemoryFileAccess(getMemoryFilePath());
    }

    private void setInputsAndOutputs() {
        for (String input : getInputsOfTypeToEvaluateSortedByName()) {
            inputsToEvaluate.put(input, componentContext.getInputDataType(input));
        }
        for (String output : getOutputsOfTypeEvaluationResultsSortedByName()) {
            outputsEvaluationResult.put(output, componentContext.getInputDataType(output));
        }
    }

    private String getMemoryFilePath() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(
            EvaluationMemoryComponentConstants.CONFIG_SELECT_AT_WF_START))) {
            return componentContext.getConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE_WF_START);
        } else {
            return componentContext.getConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_MEMORY_FILE);
        }
    }

    private void initializeMemoryFileAccess(String path) throws ComponentException {
        if (path == null || path.isEmpty()) {
            throw new ComponentException("No memory file given. Did you forget to configure one?");
        }
        memoryFile = new File(path);
        memoryFilePath = memoryFile.getAbsolutePath();
        try {
            memoryAccess = memoryFileAccessService.acquireAccessToMemoryFile(memoryFilePath);
            if (memoryFile.exists() && FileUtils.sizeOf(memoryFile) > 0) { // exists and is not empty
                memoryAccess.validateEvaluationMemory(inputsToEvaluate, outputsEvaluationResult);
            } else {
                memoryFile.createNewFile();
                memoryAccess.setInputsOutputsDefinition(inputsToEvaluate, outputsEvaluationResult);
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to access memory file: " + memoryFilePath, e);
        }
    }

    @Override
    public void processInputs() throws ComponentException {

        Set<String> inputsWithDatum = componentContext.getInputsWithDatum();
        SortedMap<String, TypedDatum> inputValues = getInputValuesSortedByInputsName(inputsWithDatum);

        switch (getInputProcessingMode(inputsWithDatum)) {
        case Check:
            initializeNewHistoryData();
            processInputsInCheckMode(inputValues);
            break;
        case Store:
            initializeNewHistoryData();
            processInputsInStoreMode();
            break;
        default:
            break;
        }

        try {
            addMemoryFileToHistoryData();
        } catch (IOException e) {
            String errorMessage = StringUtils.format("Failed to store memory file into the data management for '%s' (%s)",
                componentContext.getComponentName(), componentContext.getExecutionIdentifier());
            String errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(log, errorMessage, e);
            componentLog.componentError(errorMessage, e, errorId);

        }
        writeFinalHistoryData();
    }

    private void initializeNewHistoryData() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyData = new EvaluationMemoryComponentHistoryDataItem(EvaluationMemoryComponentConstants.COMPONENT_ID);
            historyData.setMemoryFilePath(memoryFilePath);
        }
    }

    private void addMemoryFileToHistoryData() throws IOException {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            String memoryFileReference = dataManagementService.createTaggedReferenceFromLocalFile(
                componentContext, memoryFile, memoryFile.getName());
            historyData.setMemoryFileReference(memoryFileReference);
        }
    }

    private void writeFinalHistoryData() {
        if (historyData != null
            && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyData);
            historyData = null;
        }
    }

    private void processInputsInCheckMode(SortedMap<String, TypedDatum> inputValues) throws ComponentException {
        if (valuesToEvaluate != null) {
            componentLog.componentWarn(StringUtils.format("Values to evaluate left: '%s' "
                + "- no result values received (usually in case of component failure in loop) -> skip values", inputValues));
            valuesToEvaluate = null;
        }

        SortedMap<String, TypedDatum> evaluationResults = null;
        try {
            final SortedMap<String, Double> tolerances = constructTolerances(inputValues.keySet());
            final ToleranceHandling toleranceHandling = constructToleranceHandling();
            evaluationResults = memoryAccess.getEvaluationResult(inputValues, outputsEvaluationResult, tolerances, toleranceHandling);
        } catch (IOException e) {
            String errorMessage = StringUtils.format("Failed to get evaluation results for values '%s' from evaluation memory '%s';"
                + " cause: %s - as it is not workflow-critical, continue with execution...", inputValues, memoryFile, e.getMessage());
            log.error(errorMessage, e);
            componentLog.componentError(errorMessage);
        }
        if (evaluationResults == null) {
            componentLog.componentInfo(StringUtils.format("Forward values '%s' - no evaluation results in memory", inputValues));
            forwardValues(inputValues);
            valuesToEvaluate = inputValues;
        } else {
            if (evaluationResultsContainValuesOfTypeNotAValue(evaluationResults) && !considerLoopFailures) {
                componentLog.componentInfo(StringUtils.format("Forward values '%s' - found evaluation results "
                    + "in memory, but they are ignored as they contain values of type not-a-value (loop failures) and component is "
                    + "configured to not consider loop failures as loop result", inputValues, evaluationResults));
                forwardValues(inputValues);
                valuesToEvaluate = inputValues;
            } else {
                componentLog.componentInfo(StringUtils.format("Found evaluation results for values '%s' "
                    + "in memory: %s -> directly feed back", inputValues, evaluationResults));
                for (Entry<String, TypedDatum> entry : evaluationResults.entrySet()) {
                    componentContext.writeOutput(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private SortedMap<String, Double> constructTolerances(Set<String> inputs) throws ComponentException {
        final SortedMap<String, Double> tolerances = new TreeMap<>();
        for (String input : inputs) {
            final String toleranceString = componentContext.getInputMetaDataValue(input, EvaluationMemoryComponentConstants.META_TOLERANCE);
            if (toleranceString == null) {
                final String errorMessage = String.format("No tolerance configuration found for input '%s'", input);
                componentLog.componentError(errorMessage);
                throw new ComponentException(errorMessage);
            } else if (toleranceString.isEmpty()) {
                tolerances.put(input, null);
            } else {
                final Double toleranceValuePercent;
                try {
                    // Since we store the percentage sign as part of the metadata for the sake of usability (since the metadata is displayed
                    // directly to the user), we have to remove this percentage sign before
                    toleranceValuePercent = Double.valueOf(toleranceString.substring(0, toleranceString.length() - 1));
                } catch (NumberFormatException e) {
                    final String errorMessage =
                        String.format("Invalid tolerance specification stored for input '%s': '%s'", input, toleranceString);
                    componentLog.componentError(errorMessage);
                    throw new ComponentException(errorMessage);
                }

                // We allow the user to give the tolerance in percent. For later calculations, however, it is simpler to have the tolerance
                // given as a real number, i.e., as 0.2 instead of 20%. Hence, we convert percent into real numbers before returning
                tolerances.put(input, toleranceValuePercent.doubleValue() * PERCENT_TO_REAL_NUMBER_FACTOR);
            }
        }

        return tolerances;
    }

    private ToleranceHandling constructToleranceHandling() throws ComponentException {
        final String overlapBehaviorString =
            componentContext.getConfigurationValue(EvaluationMemoryComponentConstants.CONFIG_KEY_TOLERANCE_OVERLAP_BEHAVIOR);

        switch (OverlapBehavior.parseConfigValue(overlapBehaviorString)) {
        case LENIENT:
            return ToleranceHandling.constructLenientHandling(this.componentLog);
        case STRICT:
            return ToleranceHandling.constructStrictHandling(this.componentLog);
        default:
            // This should never happen - in order to keep the code free of warnings, we have to check for it anyways
            throw new ComponentException("Unknown overlap behavior found in configuration");
        }
    }

    private boolean evaluationResultsContainValuesOfTypeNotAValue(SortedMap<String, TypedDatum> evaluationResults) {
        for (TypedDatum result : evaluationResults.values()) {
            if (result.getDataType().equals(DataType.NotAValue)) {
                return true;
            }
        }
        return false;
    }

    private void processInputsInStoreMode() throws ComponentException {
        Set<String> inputsWithDatum = componentContext.getInputsWithDatum();
        SortedMap<String, TypedDatum> evaluationResults = getDynamicInputValuesSortedByInputsName(inputsWithDatum);

        for (Entry<String, TypedDatum> inputEntry : evaluationResults.entrySet()) {
            if (componentContext.isDynamicInput(inputEntry.getKey())) {
                componentContext.writeOutput(inputEntry.getKey(), inputEntry.getValue());
            }
        }

        if (valuesToEvaluate == null) {
            throw new ComponentException(StringUtils.format("Failed to store evaluation results in evaluation memory file: %s"
                + " - no values (to evaluate) stored from a previous run", memoryFilePath));
        }
        SortedMap<String, TypedDatum> values = valuesToEvaluate;
        try {
            memoryAccess.addEvaluationValues(values, evaluationResults);
            componentLog.componentInfo(StringUtils.format("Stored evaluation results for values '%s' "
                + "in memory: %s", values, evaluationResults));
        } catch (IOException e) {
            String errorMessage = StringUtils.format("Failed to write evaluation values '%s' with '%s' to evaluation memory '%s';"
                + " cause: %s - as it is not workflow-critical, continue with execution...", values, evaluationResults, memoryFile,
                e.getMessage());
            log.error(errorMessage, e);
            componentLog.componentError(errorMessage);
        }

        valuesToEvaluate = null;
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeFinalHistoryData();
    }

    @Override
    public void tearDown(FinalComponentState state) {
        if (memoryAccess != null) {
            if (!memoryFileAccessService.releaseAccessToMemoryFile(memoryFilePath)) {
                log.warn("Access to memory file wasn't acquired earlier, but access to it should released anyway now: " + memoryFilePath);
            }
        } else {
            log.debug("No need to release access to memory file as it wasn't acquired before: " + memoryFilePath);
        }
    }

    private Mode getInputProcessingMode(Set<String> inputsWithDatum) throws ComponentException {
        String input = inputsWithDatum.iterator().next();
        if (componentContext.isDynamicInput(input)) {
            if (componentContext.getDynamicInputIdentifier(input).equals(EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE)) {
                return Mode.Check;
            } else if (componentContext.getDynamicInputIdentifier(input).equals(
                EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS)) {
                return Mode.Store;
            }
        }
        // should never happen
        throw new ComponentException("Unexpected set of input values");
    }

    private SortedMap<String, TypedDatum> getInputValuesSortedByInputsName(Set<String> inputsWithDatum) {
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        for (String input : inputsWithDatum) {
            inputValues.put(input, componentContext.readInput(input));
        }
        return inputValues;
    }

    private SortedSet<String> getOutputsOfTypeEvaluationResultsSortedByName() {
        SortedSet<String> outputs = new TreeSet<>();
        for (String output : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(output)
                && componentContext.getDynamicOutputIdentifier(output).equals(
                    EvaluationMemoryComponentConstants.ENDPOINT_ID_EVALUATION_RESULTS)) {
                outputs.add(output);
            }
        }
        return outputs;
    }

    private SortedSet<String> getInputsOfTypeToEvaluateSortedByName() {
        SortedSet<String> inputs = new TreeSet<>();
        for (String input : componentContext.getOutputs()) {
            if (componentContext.isDynamicInput(input)
                && componentContext.getDynamicInputIdentifier(input).equals(
                    EvaluationMemoryComponentConstants.ENDPOINT_ID_TO_EVALUATE)) {
                inputs.add(input);
            }
        }
        return inputs;
    }

    private SortedMap<String, TypedDatum> getDynamicInputValuesSortedByInputsName(Set<String> inputsWithDatum) {
        SortedMap<String, TypedDatum> inputValues = new TreeMap<>();
        for (String input : inputsWithDatum) {
            if (componentContext.isDynamicInput(input)) {
                inputValues.put(input, componentContext.readInput(input));
            }
        }
        return inputValues;
    }

    private void forwardValues(SortedMap<String, TypedDatum> inputValues) {
        for (Entry<String, TypedDatum> inputEntry : inputValues.entrySet()) {
            componentContext.writeOutput(inputEntry.getKey(), inputEntry.getValue());
        }
    }

}
