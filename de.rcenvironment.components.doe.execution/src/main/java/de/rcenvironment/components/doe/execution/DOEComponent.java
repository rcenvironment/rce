/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.components.doe.common.DOEAlgorithms;
import de.rcenvironment.components.doe.common.DOEComponentHistoryDataItem;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.components.doe.common.DOEUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.model.spi.AbstractNestedLoopComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Component for doing a design of experiments.
 * 
 * @author Sascha Zur
 * @author Doreen Seider (logging)
 * @author Jascha Riedel (#14117)
 * @author Kathrin Schaffert (#17665)
 */
public class DOEComponent extends AbstractNestedLoopComponent {

    private static final int MINUS_ONE = -1;

    private static final String PLACEHOLDER_STRING = "%s: %s";

    private static final String WROTE_VALUE_TO_OUTPUT_TEXT = "Wrote to output '%s': %s";

    private static final Log LOGGER = LogFactory.getLog(DOEComponent.class);

    private Double[][] valuesTable;

    private final Map<Integer, Map<String, Double>> resultData = new HashMap<>();

    private int runNumber = 0;

    private int endSample = MINUS_ONE;

    private DOEComponentHistoryDataItem historyDataItem;

    private FileReferenceTD tableFileReference;

    private Double[][] codedValues;

    private List<String> outputs;

    private boolean isDone;

    private File tableFile;

    private volatile boolean canceled = false;

    private String method = "";

    @Override
    public void startNestedComponentSpecific() throws ComponentException {
        outputs = new LinkedList<>(componentContext.getOutputs());
        removeOutputsNotConsidered();
        Collections.sort(outputs);
        method = componentContext.getConfigurationValue(DOEConstants.KEY_METHOD);
        int runNumberCount = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_RUN_NUMBER));
        int seedNumber = 0;
        if (componentContext.getConfigurationValue(DOEConstants.KEY_SEED_NUMBER) != null) {
            seedNumber = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_SEED_NUMBER));
        }
        valuesTable = new Double[0][0];
        if (!(method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            || method.equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO)
            || method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT))
            && outputs.size() < 2) {
            throw new ComponentException("Number of outputs for chosen method too few - must be >=2, but is " + outputs.size());
        }
        switch (method) {
        case DOEConstants.DOE_ALGORITHM_FULLFACT:
            if (runNumberCount >= 2) {
                valuesTable = DOEAlgorithms.populateTableFullFactorial(outputs.size(), runNumberCount);
                if (valuesTable.length == 0) {
                    throw new ComponentException("The chosen configuration produced too many samples");
                }
            } else {
                throw new ComponentException("Level number for full factorial design too low - must be >=2, but is " + runNumberCount);
            }
            break;
        case DOEConstants.DOE_ALGORITHM_LHC:
            valuesTable = DOEAlgorithms.populateTableLatinHypercube(outputs.size(), runNumberCount, seedNumber);
            break;
        case DOEConstants.DOE_ALGORITHM_MONTE_CARLO:
            valuesTable = DOEAlgorithms.populateTableMonteCarlo(outputs.size(), runNumberCount, seedNumber);
            break;
        case DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE:
            readCustomTable();
            break;
        case DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT:
            valuesTable = new Double[0][0];
            break;
        default:
            break;
        }
        if (this.endSample < 0) {
            this.endSample = valuesTable.length;
        }
        if (!DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT.equals(method)) {
            codeOutputsAndWriteToFile();
        }

        if (treatStartAsComponentRun()) {
            processInputsNestedComponentSpecific();
        }
    }

    private void readCustomTable() throws ComponentException {
        try {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
            if (componentContext.getConfigurationValue(DOEConstants.KEY_TABLE) != null
                && !componentContext.getConfigurationValue(DOEConstants.KEY_TABLE).isEmpty()) {
                this.valuesTable = mapper.readValue(componentContext.getConfigurationValue(DOEConstants.KEY_TABLE), Double[][].class);
                if (valuesTable == null) {
                    throw new ComponentException("No table given");
                }
            } else {
                throw new ComponentException("No table given");
            }
            if (componentContext.getConfigurationValue(DOEConstants.KEY_START_SAMPLE) != null
                && !componentContext.getConfigurationValue(DOEConstants.KEY_START_SAMPLE).isEmpty()) {
                this.runNumber = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_START_SAMPLE));
            }
            if (componentContext.getConfigurationValue(DOEConstants.KEY_END_SAMPLE) != null
                && !componentContext.getConfigurationValue(DOEConstants.KEY_END_SAMPLE).isEmpty()) {
                this.endSample = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_END_SAMPLE));
            }
            if (this.runNumber < 0) {
                componentLog.componentInfo("Start sample value < 0 -> set it to 0");
                this.runNumber = 0;
            }
            if (this.runNumber >= valuesTable.length) {
                throw new ComponentException(StringUtils.format("Start sample value (%s) is greater than the number of samples (%s)",
                    this.runNumber,
                    valuesTable.length));
            }
            if (this.runNumber > this.endSample) {
                throw new ComponentException(StringUtils.format("Start sample value (%s) is greater than end sample value (%s)",
                    this.runNumber,
                    this.endSample));
            }
            if (valuesTable.length > 0 && valuesTable[0].length < outputs.size()) {
                throw new ComponentException(StringUtils.format(
                    "Number of values per sample (%s) is lower than the number of outputs (%s)",
                    valuesTable[0].length,
                    outputs.size()));
            }
            for (int i = runNumber; i <= endSample && i < valuesTable.length; i++) {
                for (int j = 0; j < valuesTable[i].length; j++) {
                    if (valuesTable[i][j] == null) {
                        throw new ComponentException("Values in table are incomplete");
                    }
                }
            }
        } catch (IOException e) {
            throw new ComponentException("Failed to read given table", e);
        }
    }

    private void codeOutputsAndWriteToFile() {
        try {
            int i = 0;
            codedValues = new Double[valuesTable.length][valuesTable[0].length];
            for (String output : outputs) {
                Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
                Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
                for (int run = 0; run < valuesTable.length && run <= endSample; run++) {
                    if (valuesTable[run][i] != null) {
                        codedValues[run][i] = DOEAlgorithms.convertValue(low, up, valuesTable[run][i]);
                    }
                }
                i++;
            }
            if (!outputs.isEmpty()) {
                tableFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("DOETable*.csv");
                if (!(method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                    || method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT))) {
                    DOEUtils.writeTableToCSVFile(codedValues, tableFile.getAbsolutePath(), outputs);
                } else {
                    DOEUtils.writeTableToCSVFile(valuesTable, tableFile.getAbsolutePath(), outputs);
                }
            }
        } catch (IOException e) {
            String errorMessage = "Failed to write DOE table file";
            componentLog.componentError(StringUtils.format(PLACEHOLDER_STRING, errorMessage, e.getMessage()));
            LOGGER.error(errorMessage, e);
        }
    }

    private void removeOutputsNotConsidered() {
        outputs.remove(LoopComponentConstants.ENDPOINT_NAME_LOOP_DONE);
        outputs.remove(DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES);
        Iterator<String> outputsIterator = outputs.iterator();
        while (outputsIterator.hasNext()) {
            String outputName = outputsIterator.next();
            if (componentContext.isDynamicOutput(outputName)
                && componentContext.getDynamicOutputIdentifier(outputName).equals(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD)) {
                outputsIterator.remove();
            }
        }
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return !hasForwardingStartInputs()
            && componentContext.getDynamicInputsWithIdentifier(DOEConstants.CUSTOM_TABLE_ENDPOINT_ID).isEmpty();
    }

    @Override
    public void processInputsNestedComponentSpecific() throws ComponentException {
        if (componentContext.getInputsWithDatum().contains(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME)) {
            readCustomTableFromInput();
            codeOutputsAndWriteToFile();
        }
        initializeNewHistoryDataItem();
        if (historyDataItem != null && tableFileReference != null) {
            historyDataItem.setTableFileReference(tableFileReference.getFileReference());
        } else if (historyDataItem != null) {
            createTableFileReference();
        }
        processInput();

        if (runNumber == 0) {
            componentContext.writeOutput(DOEConstants.OUTPUT_NAME_NUMBER_OF_SAMPLES, typedDatumFactory.createInteger(valuesTable.length));
        }
        writeNewOutput();
        writeResultFile();

    }

    private void readCustomTableFromInput() throws ComponentException {
        MatrixTD customTable = (MatrixTD) componentContext.readInput(DOEConstants.CUSTOM_TABLE_ENDPOINT_NAME);
        int rowDimension = customTable.getRowDimension();
        int columnDimension = customTable.getColumnDimension();
        if (!(rowDimension > 0 && columnDimension > 0)) {
            throw new ComponentException(StringUtils.format("Dimension of table must be > 0 but is %sx%s", rowDimension, columnDimension));
        }
        if (columnDimension != outputs.size()) {
            throw new ComponentException(StringUtils.format("Column dimension (%s) of table does not match number of outputs (%s)",
                columnDimension, outputs.size()));
        }
        valuesTable = new Double[rowDimension][columnDimension];
        for (int i = 0; i < rowDimension; i++) {
            for (int j = 0; j < columnDimension; j++) {
                valuesTable[i][j] = customTable.getFloatTDOfElement(i, j).getFloatValue();
            }
        }
    }

    private void createTableFileReference() {
        if (historyDataItem != null && tableFile != null) {
            try {
                tableFileReference =
                    componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(componentContext,
                        tableFile, "DOETable.csv");
                historyDataItem.setTableFileReference(tableFileReference.getFileReference());
            } catch (IOException e) {
                String errorMessage = "Failed to create DOE table file";
                componentLog.componentError(StringUtils.format(PLACEHOLDER_STRING, errorMessage, e.getMessage()));
                LOGGER.error(errorMessage, e);
            } finally {
                if (tableFile != null && tableFile.exists()) {
                    try {
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tableFile);
                    } catch (IOException e) {
                        LOGGER.error("Failed to dispose temporary file: " + tableFile.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    @Override
    public void completeStartOrProcessInputsAfterFailure() throws ComponentException {
        writeResultFile();
        writeFinalHistoryDataItem();
    }

    @Override
    public void onStartInterrupted(ThreadHandler executingThreadHandler) {
        canceled = true;
    }

    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        if (tableFile != null && tableFile.exists()) {
            try {
                TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tableFile);
            } catch (IOException e) {
                LOGGER.error("Could not dispose temp file: ", e);
            }
        }
    }

    private void writeResultFile() {
        if (!componentContext.getDynamicInputsWithIdentifier(DOEConstants.INPUT_ID_NAME).isEmpty() && historyDataItem != null) {
            File resultFile = null;
            try {
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("DOEResult*.csv");
                if (method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                    || method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
                    int startSample = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_START_SAMPLE));
                    DOEUtils.writeResultToCSVFile(valuesTable, resultData, resultFile.getAbsolutePath(), runNumber, outputs, startSample);
                } else {
                    DOEUtils.writeResultToCSVFile(codedValues, resultData, resultFile.getAbsolutePath(), runNumber, outputs);
                }
                FileReferenceTD resultFileReference =
                    componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(componentContext,
                        resultFile, "Result.csv");
                historyDataItem.setResultFileReference(resultFileReference.getFileReference());
            } catch (IOException e) {
                String errorMessage = "Failed to store history data";
                componentLog.componentError(StringUtils.format(PLACEHOLDER_STRING, errorMessage, e.getMessage()));
                LOGGER.error(errorMessage, e);
            } finally {
                if (resultFile != null && resultFile.exists()) {
                    try {
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(resultFile);
                    } catch (IOException e) {
                        LOGGER.error("Failed to dispose temporary file: " + resultFile.getAbsolutePath(), e);
                    }
                }
            }

        }
    }

    private void writeNewOutput() {
        if (valuesTable != null) {
            if (componentContext.getDynamicInputsWithIdentifier(DOEConstants.INPUT_ID_NAME).isEmpty()
                && componentContext.getDynamicInputsWithIdentifier(LoopComponentConstants.ENDPOINT_ID_TO_FORWARD).isEmpty()
                && !hasForwardingStartInputs()) {
                writeAllOutputs();
            } else if (runNumber < valuesTable.length) {
                writeNextOutput();
            } else {
                setLoopDone();
            }
        }
    }

    private void setLoopDone() {
        setLoopDone(true);
    }

    private void setLoopDone(boolean done) {
        isDone = done;
    }

    private void writeNextOutput() {
        if (method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            && runNumber > endSample) {
            setLoopDone();
            return;
        }
        int i = 0;
        for (String output : outputs) {
            Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
            Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
            double value = valuesTable[runNumber][i++];
            if (!method
                .equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE) && !method
                    .equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
                value = DOEAlgorithms.convertValue(low, up, value);
            }
            writeOutput(output,
                componentContext.getService(TypedDatumService.class).getFactory().createFloat(value));
            componentLog.componentInfo(StringUtils.format(WROTE_VALUE_TO_OUTPUT_TEXT, output, value));

        }
        runNumber++;
        setLoopDone(false);
    }

    private void writeAllOutputs() {
        while (runNumber < valuesTable.length) {
            if (method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE) && runNumber > endSample) {
                break;
            }
            int i = 0;
            for (String output : outputs) {
                double value = valuesTable[runNumber][i++];
                if (!method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                    && !method.equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE_INPUT)) {
                    Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
                    Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
                    value = DOEAlgorithms.convertValue(low, up, value);
                }
                writeOutput(output,
                    componentContext.getService(TypedDatumService.class).getFactory().createFloat(value));
                componentLog.componentInfo(StringUtils.format(WROTE_VALUE_TO_OUTPUT_TEXT, output, value));
            }
            runNumber++;
            if (canceled) {
                break;
            }
        }
        setLoopDone();
    }

    private void processInput() {
        if (!componentContext.getInputsWithDatum().isEmpty()) {
            Map<String, Double> runInput = new HashMap<>();
            for (String inputName : componentContext.getInputsWithDatum()) {
                if (componentContext.getDynamicInputIdentifier(inputName).equals(DOEConstants.INPUT_ID_NAME)) {
                    if (componentContext.readInput(inputName).getDataType() != DataType.NotAValue) {
                        runInput.put(inputName, ((FloatTD) componentContext.readInput(inputName)).getFloatValue());
                    } else {
                        runInput.put(inputName, Double.NaN);
                    }
                }
            }
            resultData.put(runNumber - 1, runInput);
        }
    }

    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new DOEComponentHistoryDataItem();
        }
    }

    private void writeFinalHistoryDataItem() {
        if (historyDataItem != null && !outputs.isEmpty()
            && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

    @Override
    protected boolean isDoneNestedComponentSpecific() {
        return isDone;
    }

    @Override
    protected void resetNestedComponentSpecific() {
        runNumber = 0;
        isDone = false;
    }

    @Override
    protected void finishLoopNestedComponentSpecific() {
        // Not needed in DOE
    }

    @Override
    protected void sendFinalValues() throws ComponentException {
        writeFinalHistoryDataItem();
    }

    @Override
    protected void sendValuesNestedComponentSpecific() {
        writeFinalHistoryDataItem();
    }

}
