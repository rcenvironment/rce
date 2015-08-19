/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.doe.execution;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.components.doe.common.DOEAlgorithms;
import de.rcenvironment.components.doe.common.DOEComponentHistoryDataItem;
import de.rcenvironment.components.doe.common.DOEConstants;
import de.rcenvironment.components.doe.common.DOEUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Component for doing a design of experiments.
 * 
 * @author Sascha Zur
 */
public class DOEComponent extends DefaultComponent {

    protected static final String TABLE_IS_NULL_OR_EMPTY = "Table is null or empty";

    protected static final String TOO_FEW_OUTPUTS_EXCEPTION = "Number of outputs for chosen method too few. Must be >=2";

    protected static final String LEVEL_TOO_LOW_EXCEPTION = "Level number for full factorial design too low (must be >=2).";

    protected static final String INPUT_INVALID_EXCEPTION = "Input %s has an invalid value.";

    protected static final String NUMBER_OF_VALUES_PER_SAMPLE_LOWER_THAN_THE_NUMBER_OF_OUTPUTS =
        "Number of values per sample (%s) is lower than the number of outputs (%s).";

    protected static final String START_SAMPLE_VALUE_HIGHER_THAN_END_SAMPLE_VALUE =
        "Start sample value (%s) is higher than end sample value (%s)";

    protected static final String START_SAMPLE_VALUE_HIGHER_THAN_THE_NUMBER_OF_SAMPLES =
        "Start sample value (%s) is higher than the number of samples (%s)";

    private static final String START_SAMPLE_VALUE_0_SETTING_IT_TO_0_TEXT = "Start sample value < 0. Setting it to 0.";

    private static final String WROTE_VALUE_TO_OUTPUT_TEXT = "Wrote to output '%s': %s";

    private static final String TABLE_INVALID_EXCEPTION = "Given table could not be read.";

    private static final String INPUTS_INVALID_EXCEPTION = "Inputs %s have invalid inputs.";

    private static final String INPUT_RERUNNING_TEXT = "Input %s has an invalid value. Rerunning sample #%s";

    private static final String INPUTS_RERUNNING_TEXT = "Inputs %s have invalid values. Rerunning sample #%s";

    private static final Log LOGGER = LogFactory.getLog(DOEComponent.class);

    private ComponentContext componentContext;

    private Double[][] valuesTable;

    private final Map<Integer, Map<String, Double>> resultData = new HashMap<>();

    private int runNumber = 0;

    private int endSample = 0;

    private DOEComponentHistoryDataItem historyDataItem;

    private FileReferenceTD tableFileReference;

    private Double[][] codedValues;

    private List<String> outputs;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @Override
    public void start() throws ComponentException {
        super.start();
        outputs = new LinkedList<String>(componentContext.getOutputs());
        outputs.remove(DOEConstants.OUTPUT_FINISHED_NAME);
        Collections.sort(outputs);
        int runNumberCount = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_RUN_NUMBER));
        int seedNumber = 0;
        if (componentContext.getConfigurationValue(DOEConstants.KEY_SEED_NUMBER) != null) {
            seedNumber = Integer.parseInt(componentContext.getConfigurationValue(DOEConstants.KEY_SEED_NUMBER));
        }
        valuesTable = new Double[0][0];
        if (!(componentContext.getConfigurationValue(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            || componentContext.getConfigurationValue(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_MONTE_CARLO))
            && outputs.size() < 2) {
            throw new ComponentException(TOO_FEW_OUTPUTS_EXCEPTION);
        }
        switch (componentContext.getConfigurationValue(DOEConstants.KEY_METHOD)) {
        case DOEConstants.DOE_ALGORITHM_FULLFACT:
            if (runNumberCount >= 2) {
                valuesTable = DOEAlgorithms.populateTableFullFactorial(outputs.size(), runNumberCount);
                if (valuesTable.length == 0) {
                    throw new ComponentException("The chosen configuration produced too many samples.");
                }
            } else {
                throw new ComponentException(LEVEL_TOO_LOW_EXCEPTION);
            }
            break;
        case DOEConstants.DOE_ALGORITHM_LHC:
            valuesTable = DOEAlgorithms.populateTableLatinHypercube(outputs.size(), runNumberCount, seedNumber);
            break;
        case DOEConstants.DOE_ALGORITHM_MONTE_CARLO:
            valuesTable = DOEAlgorithms.populateTableMonteCarlo(outputs.size(), runNumberCount, seedNumber);
            break;
        case DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE:
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (componentContext.getConfigurationValue(DOEConstants.KEY_TABLE) != null
                    && !componentContext.getConfigurationValue(DOEConstants.KEY_TABLE).isEmpty()) {
                    this.valuesTable = mapper.readValue(componentContext.getConfigurationValue(DOEConstants.KEY_TABLE), Double[][].class);
                } else {
                    throw new ComponentException(TABLE_IS_NULL_OR_EMPTY);
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
                    componentContext.printConsoleLine(START_SAMPLE_VALUE_0_SETTING_IT_TO_0_TEXT, ConsoleRow.Type.STDOUT);
                    this.runNumber = 0;
                }
                if (this.runNumber >= valuesTable.length) {
                    throw new ComponentException(StringUtils.format(START_SAMPLE_VALUE_HIGHER_THAN_THE_NUMBER_OF_SAMPLES,
                        this.runNumber,
                        valuesTable.length));
                }
                if (this.runNumber > this.endSample) {
                    throw new ComponentException(StringUtils.format(START_SAMPLE_VALUE_HIGHER_THAN_END_SAMPLE_VALUE,
                        this.runNumber,
                        this.endSample));
                }
                if (valuesTable.length > 0 && valuesTable[0].length < outputs.size()) {
                    throw new ComponentException(StringUtils.format(
                        NUMBER_OF_VALUES_PER_SAMPLE_LOWER_THAN_THE_NUMBER_OF_OUTPUTS,
                        valuesTable[0].length,
                        outputs.size()));
                }
                for (int i = 0; i < endSample && i < valuesTable.length; i++) {
                    for (int j = 0; j < valuesTable[i].length; j++) {
                        if (valuesTable[i][j] == null) {
                            throw new ComponentException("Table has not defined values.");
                        }
                    }
                }
            } catch (IOException e) {
                throw new ComponentException(TABLE_INVALID_EXCEPTION);
            }
            break;
        default:
            break;
        }
        File tableFile = null;
        try {
            int i = 0;
            codedValues = new Double[valuesTable.length][valuesTable[0].length];
            for (String output : outputs) {
                Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
                Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
                for (int run = 0; run < valuesTable.length; run++) {
                    codedValues[run][i] = DOEAlgorithms.convertValue(low, up, valuesTable[run][i]);
                }
                i++;
            }
            if (outputs.size() > 0){
                tableFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("DOETable*.csv");
                DOEUtils.writeTableToCSVFile(codedValues, tableFile.getAbsolutePath(), outputs);
                tableFileReference =
                    componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(componentContext,
                        tableFile, "DOETable.csv");
            }
        } catch (IOException e) {
            LOGGER.error("Could not create DOE table file", e);
        } finally {
            if (tableFile != null && tableFile.exists()){
                try {
                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(tableFile);
                } catch (IOException e) {
                    LOGGER.error("Temp file could not be disposed", e);
                }
            }
        }
        processInputs();
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getOutputs().size() > 1;
    }

    @Override
    public void processInputs() throws ComponentException {
        initializeNewHistoryDataItem();
        if (historyDataItem != null && tableFileReference != null) {
            historyDataItem.setTableFileReference(tableFileReference.getFileReference());
        }
        processInput();
        writeResultFile();
        writeNewOutput();
        writeFinalHistoryDataItem();
    }
    @Override
    public void tearDown(FinalComponentState state) {
        super.tearDown(state);
        if (state == FinalComponentState.FAILED){
            writeResultFile();
            writeFinalHistoryDataItem();
        }
    }
    private void writeResultFile() {
        if (!componentContext.getInputsWithDatum().isEmpty() && historyDataItem != null) {
            File resultFile = null;
            try {
                resultFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("DOEResult*.csv");
                DOEUtils.writeResultToCSVFile(codedValues, resultData, resultFile.getAbsolutePath(), runNumber, outputs);
                FileReferenceTD resultFileReference =
                    componentContext.getService(ComponentDataManagementService.class).createFileReferenceTDFromLocalFile(componentContext,
                        resultFile, "Result.csv");
                historyDataItem.setResultFileReference(resultFileReference.getFileReference());
            } catch (IOException e) {
                LOGGER.error(e);
            } finally {
                if (resultFile != null && resultFile.exists()){
                    try {
                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(resultFile);
                    } catch (IOException e) {
                        LOGGER.error("Temp file could not be disposed: ", e);
                    }
                }
            }
            
        }
    }

    private void writeNewOutput() {
        if (valuesTable != null) {
            if (componentContext.getInputs().isEmpty()) {
                writeAllOutputs();
            } else if (runNumber < valuesTable.length) {
                writeNextOutput();
            } else {
                sendFinalizeOutput();
                componentContext.closeAllOutputs();
            }
        }
    }

    private void sendFinalizeOutput() {
        if (outputs.size() > 0){
            componentContext.writeOutput(DOEConstants.OUTPUT_FINISHED_NAME,
                componentContext.getService(TypedDatumService.class).getFactory().createBoolean(true));
        }
    }

    private void writeNextOutput() {
        if (componentContext.getConfigurationValue(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
            && runNumber > endSample) {
            sendFinalizeOutput();
            componentContext.closeAllOutputs();
            return;
        }
        int i = 0;
        for (String output : outputs) {
            Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
            Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
            double value = valuesTable[runNumber][i++];
            if (!componentContext.getConfigurationValue(DOEConstants.KEY_METHOD)
                .equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
                value = DOEAlgorithms.convertValue(low, up, value);
            }
            componentContext.writeOutput(output,
                componentContext.getService(TypedDatumService.class).getFactory().createFloat(value));
            componentContext.printConsoleLine(StringUtils.format(WROTE_VALUE_TO_OUTPUT_TEXT, output, value), ConsoleRow.Type.STDOUT);

        }
        runNumber++;
    }

    private void writeAllOutputs() {
        while (runNumber < valuesTable.length) {
            if (componentContext.getConfigurationValue(DOEConstants.KEY_METHOD).equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)
                && runNumber > endSample) {
                break;
            }
            int i = 0;
            for (String output : outputs) {
                Double low = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_LOWER));
                Double up = Double.valueOf(componentContext.getOutputMetaDataValue(output, DOEConstants.META_KEY_UPPER));
                double value = valuesTable[runNumber][i++];
                if (!componentContext.getConfigurationValue(DOEConstants.KEY_METHOD)
                    .equals(DOEConstants.DOE_ALGORITHM_CUSTOM_TABLE)) {
                    value = DOEAlgorithms.convertValue(low, up, value);
                }
                componentContext.writeOutput(output,
                    componentContext.getService(TypedDatumService.class).getFactory().createFloat(value));
                componentContext.printConsoleLine(StringUtils.format(WROTE_VALUE_TO_OUTPUT_TEXT, output, value),
                    ConsoleRow.Type.STDOUT);
            }
            runNumber++;
        }
        sendFinalizeOutput();
    }

    private void processInput() throws ComponentException {
        List<String> invalidInputs = new LinkedList<String>();
        for (String input : componentContext.getInputsWithDatum()) {
            if (componentContext.readInput(input).getDataType() == DataType.NotAValue) {
                invalidInputs.add(input);
            }
        }
        if (invalidInputs.size() > 0) {
            String behaviour = componentContext.getConfigurationValue(DOEConstants.KEY_FAILED_RUN_BEHAVIOUR);
            if (behaviour.equals(DOEConstants.KEY_BEHAVIOUR_ABORT)) {
                if (invalidInputs.size() == 1) {
                    throw new ComponentException(StringUtils.format(INPUT_INVALID_EXCEPTION, invalidInputs.toString()));
                } else {
                    throw new ComponentException(StringUtils.format(INPUTS_INVALID_EXCEPTION, invalidInputs.toString()));
                }
            } else if (behaviour.equals(DOEConstants.KEY_BEHAVIOUR_RERUN)) {
                if (invalidInputs.size() == 1) {
                    componentContext.printConsoleLine(
                        StringUtils.format(INPUT_RERUNNING_TEXT, invalidInputs.toString(), --runNumber),
                        Type.STDOUT);

                } else {
                    componentContext.printConsoleLine(
                        StringUtils.format(INPUTS_RERUNNING_TEXT, invalidInputs.toString(), --runNumber),
                        Type.STDOUT);
                }
            }

        }
        if (!componentContext.getInputsWithDatum().isEmpty()) {
            Map<String, Double> runInput = new HashMap<>();
            for (String inputName : componentContext.getInputsWithDatum()) {
                if (componentContext.readInput(inputName).getDataType() != DataType.NotAValue) {
                    runInput.put(inputName, ((FloatTD) componentContext.readInput(inputName)).getFloatValue());
                } else {
                    runInput.put(inputName, Double.NaN);
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
        if (historyDataItem != null && outputs.size() > 0  
            && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }

}
