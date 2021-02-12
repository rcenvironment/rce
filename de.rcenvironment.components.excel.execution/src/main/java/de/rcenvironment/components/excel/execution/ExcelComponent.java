/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.execution;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.excel.common.ChannelValue;
import de.rcenvironment.components.excel.common.ExcelAddress;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelComponentHistoryDataItem;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.model.api.LocalExecutionOnly;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;


/**
 * Excel implementation of {@link Component}.
 * 
 * @author Markus Kunde
 * @author Patrick Schaefer
 */
@LocalExecutionOnly
public class ExcelComponent extends ExcelRCEComponent {
    
    private static final String EXCEPTION_MSG_CANNOT_DETERMINE_VALUE = "No value at Excel cell(s), cannot determine value(s)";

    private static final String EXCEPTION_MSG_WRONGTYPE_2 = " while value is of type ";

    private static final String EXCEPTION_MSG_WRONGTYPE_1 = "Output type is ";

    /** OLE cannot handle filename longer than 20 characters. */
    private static final int MAXIMUM_FILENAME_OLE_ACCEPTS = 20;

    private static final Log LOG = LogFactory.getLog(ExcelComponent.class);
    
    private long iteration = 0;
    
    private ExcelComponentHistoryDataItem historyDataItem;
    
    private TempFileService tempFileUtils = TempFileServiceAccess.getInstance();
    
    @Override
    public void start() throws ComponentException {
        super.start();
        initializeNewHistoryDataItem();
    }
    @Override
    protected void executingOneStep() throws ComponentException {
        
        initializeNewHistoryDataItem();
        
        File excelWorkingFile = null;
        
        try {
            iteration++;
            
            /* Copy origin Excel file to temp Excel file. */
            
            File originExcelFile = null;
            try {
                originExcelFile = ExcelUtils.getAbsoluteFile(componentContext.getConfigurationValue(ExcelComponentConstants.XL_FILENAME));

                if (originExcelFile == null) {
                    throw new ComponentException("No Excel file given");
                }
                excelWorkingFile = tempFileUtils.createTempFileWithFixedFilename(originExcelFile.getName()); 
                FileUtils.copyFile(originExcelFile, excelWorkingFile, true);
                                
                /* Cutting filename to maximum size. */
                String fileName = excelWorkingFile.getName();
                if (fileName.length() > MAXIMUM_FILENAME_OLE_ACCEPTS) {
                    String newFileName = fileName.substring(fileName.length() - MAXIMUM_FILENAME_OLE_ACCEPTS, fileName.length());
                    File dest = new File(excelWorkingFile.getParent() + File.separator + newFileName);
                    boolean renamed = excelWorkingFile.renameTo(dest);
                    if (renamed) {
                        excelWorkingFile = dest;
                    } else {
                        componentLog.componentError("Failed to shorten file name. It's possible that VBA Codes did not execute."
                            + " Component will continue to try.");
                    }
                } 
            } catch (IOException e) {
                throw new ComponentException("Failed to copy origin Excel file to temporary directory"
                    + " (required for the Excel component to work)", e);
            }
               
            
            /* Run macro start */
            excelService.runMacro(excelWorkingFile, componentContext.getConfigurationValue(ExcelComponentConstants.PRE_MACRO));
            
            /* Processing inputs */
            if (!componentContext.getInputs().isEmpty()) {
                processingInputChannels(excelWorkingFile);
            }
            
            
            /* Run macro run */
            excelService.runMacro(excelWorkingFile, componentContext.getConfigurationValue(
                ExcelComponentConstants.RUN_MACRO));
            
            
            /* Processing outputs */
            processingOutputChannels(excelWorkingFile);
            
            /* Run macro close */
            excelService.runMacro(excelWorkingFile, componentContext.getConfigurationValue(
                ExcelComponentConstants.POST_MACRO));

        } finally {
            
            storeHistoryDataItem();
            
            if (excelWorkingFile != null) {
                try {
                    tempFileUtils.disposeManagedTempDirOrFile(excelWorkingFile);
                } catch (IOException e) {
                    LOG.error("Failed to delete temporary Excel file", e);
                }
            }
        }
    }
    
    /**
     * Processes all output channels in one processing step.
     * 
     * @param excelFile the Excel file where input values should be written to
     * @throws ComponentException thrown if processing output channel has gone wrong
     */
    private void processingOutputChannels(final File excelFile) throws ComponentException {
        for (String outputName: componentContext.getOutputs()) {
            final boolean expand = Boolean.valueOf(componentContext.getOutputMetaDataValue(outputName,
                ExcelComponentConstants.METADATA_EXPANDING));
            final String address = componentContext.getOutputMetaDataValue(outputName, ExcelComponentConstants.METADATA_ADDRESS);
            final ExcelAddress addr = new ExcelAddress(excelFile, address);
            SmallTableTD value = excelService.getValueOfCells(excelFile, addr);
            final TypedDatum td;
            
            switch(componentContext.getOutputDataType(outputName)) {
            case ShortText:
                td = value.getTypedDatumOfCell(0, 0);
                if (td != null && td instanceof ShortTextTD) {
                    ShortTextTD text = (ShortTextTD) td;
                    componentContext.writeOutput(outputName, text);
                } else if (td != null) {
                    componentLog.componentInfo(StringUtils.format("Trying to convert '%s' (value of cell in Excel) to"
                        + " %s (data type of output '%s')...", td, DataType.ShortText.getDisplayName(), outputName));
                    ShortTextTD text;
                    try {
                        text = typedDatumConverter.castOrConvertUnsafe(td, ShortTextTD.class);
                        componentLog.componentInfo(StringUtils.format("Sucessfully converted '%s' to ShortText", td));
                        componentContext.writeOutput(outputName, text);
                    } catch (DataTypeException e) {
                        throw new ComponentException(EXCEPTION_MSG_WRONGTYPE_1 + DataType.ShortText.getDisplayName()
                            + EXCEPTION_MSG_WRONGTYPE_2 + td.getDataType().getDisplayName(), e);
                    }
                } else {
                    throw new ComponentException(EXCEPTION_MSG_CANNOT_DETERMINE_VALUE);
                }
                break;
            case Float:
                td = value.getTypedDatumOfCell(0, 0);
                if (td != null && td instanceof FloatTD) {
                    FloatTD number = (FloatTD) td;
                    componentContext.writeOutput(outputName, number);
                } else if (td != null) {
                    componentLog.componentInfo(StringUtils.format("Trying to convert '%s' (value of cell in Excel) to"
                        + " %s (data type of output '%s')...", td, DataType.Float.getDisplayName(), outputName));
                    FloatTD number;
                    try {
                        number = typedDatumConverter.castOrConvert(td, FloatTD.class);
                        componentLog.componentInfo(StringUtils.format("Sucessfully converted '%s' to Float", td));
                        componentContext.writeOutput(outputName, number);
                    } catch (DataTypeException e) {
                        throw new ComponentException(EXCEPTION_MSG_WRONGTYPE_1 + DataType.Float.getDisplayName()
                            + EXCEPTION_MSG_WRONGTYPE_2 + td.getDataType().getDisplayName(), e);
                    }
                } else {
                    throw new ComponentException(EXCEPTION_MSG_CANNOT_DETERMINE_VALUE);
                }
                break;
            case Integer:
                td = value.getTypedDatumOfCell(0, 0);
                if (td != null && td instanceof IntegerTD) {
                    IntegerTD number = (IntegerTD) td;
                    componentContext.writeOutput(outputName, number);
                } else if (td != null) {
                    componentLog.componentInfo(StringUtils.format("Trying to convert '%s' (value of cell in Excel) to %s "
                        + "(data type of output '%s')...", td, DataType.Integer.getDisplayName(), outputName));
                    IntegerTD number;
                    try {
                        number = typedDatumConverter.castOrConvert(td, IntegerTD.class);
                        componentLog.componentInfo(StringUtils.format("Sucessfully converted '%s' to Integer", td));
                        componentContext.writeOutput(outputName, number);
                    } catch (DataTypeException e) {
                        throw new ComponentException(EXCEPTION_MSG_WRONGTYPE_1 + DataType.Integer.getDisplayName()
                            + EXCEPTION_MSG_WRONGTYPE_2 + td.getDataType().getDisplayName(), e);
                    }
                } else {
                    throw new ComponentException(EXCEPTION_MSG_CANNOT_DETERMINE_VALUE);
                }
                break;
            case Boolean:
                td = value.getTypedDatumOfCell(0, 0);
                if (td != null && td instanceof BooleanTD) {
                    BooleanTD b = (BooleanTD) td;
                    componentContext.writeOutput(outputName, b);
                } else if (td != null) {
                    componentLog.componentInfo(StringUtils.format("Trying to convert '%s' (value of cell in Excel) to %s "
                        + "(data type of output '%s')...", td, DataType.Boolean.getDisplayName(), outputName));
                    BooleanTD b;
                    try {
                        b = typedDatumConverter.castOrConvert(td, BooleanTD.class);
                        componentLog.componentInfo(StringUtils.format("Sucessfully converted '%s' to Integer", td));
                        componentContext.writeOutput(outputName, b);
                    } catch (DataTypeException e) {
                        throw new ComponentException(EXCEPTION_MSG_WRONGTYPE_1 + DataType.Boolean.getDisplayName()
                            + EXCEPTION_MSG_WRONGTYPE_2 + td.getDataType().getDisplayName(), e);
                    }
                } else {
                    throw new ComponentException(EXCEPTION_MSG_CANNOT_DETERMINE_VALUE);
                }
                break;
            case SmallTable:
                
                boolean pruning = Boolean.valueOf(componentContext.getOutputMetaDataValue(outputName,
                    ExcelComponentConstants.METADATA_PRUNING));
                if (pruning) {
                    int latestRowIndex = ExcelUtils.getRowIndexLastCellFilled(value.toArray());
                    int latestColumnIndex = value.getColumnCount();
                    
                    value = value.getSubTable(0, 0, latestRowIndex + 1, latestColumnIndex);
                }
                
                if (value != null) {
                    componentContext.writeOutput(outputName, value);
                } else {
                    throw new ComponentException(EXCEPTION_MSG_CANNOT_DETERMINE_VALUE);
                }
                break;
            default:
                throw new ComponentException("Output type not supported: " + componentContext
                    .getOutputDataType(outputName).getDisplayName());
            } 
            
            //Fill runtime GUI data
            File originExcelFile = ExcelUtils.getAbsoluteFile(componentContext.getConfigurationValue(ExcelComponentConstants.XL_FILENAME));
            ChannelValue dataval = new ChannelValue(originExcelFile, addr, outputName, false, expand, iteration);
            dataval.setValues(value);
            dataval.setPreMacro(componentContext.getConfigurationValue(ExcelComponentConstants.PRE_MACRO));
            dataval.setRunMacro(componentContext.getConfigurationValue(ExcelComponentConstants.RUN_MACRO));
            dataval.setPostMacro(componentContext.getConfigurationValue(ExcelComponentConstants.POST_MACRO));
            notificationService.send(componentContext.getExecutionIdentifier() + ExcelComponentConstants.NOTIFICATION_SUFFIX, dataval);
        }
    }
    
    
    /**
     * Processes all input channels in one processing step.
     * 
     * @param excelFile the Excel file where input values should be written to
     * @param newInput {@link #runStep(Input, Map) newInput}
     * @param inputValues {@link #runStep(Input, Map) inputValues}
     * @throws ComponentException thrown if processing output channel has gone wrong
     */
    private void processingInputChannels(final File excelFile)
        throws ComponentException {
        
        for (final String inputName : componentContext.getInputsWithDatum()) {
            TypedDatum input = componentContext.readInput(inputName);
            
            String address = componentContext.getInputMetaDataValue(inputName, ExcelComponentConstants.METADATA_ADDRESS);
            
            ExcelAddress addr = new ExcelAddress(excelFile, address);
            
            final boolean expand = Boolean.valueOf(componentContext.getInputMetaDataValue(inputName,
                ExcelComponentConstants.METADATA_EXPANDING));
            
            SmallTableTD value = null;
            
            switch(componentContext.getInputDataType(inputName)) {
            case ShortText:
                value = typedDatumFactory.createSmallTable(1, 1);
                value.setTypedDatumForCell(input, 0, 0);
                excelService.setValues(excelFile, addr, value);
                break;
            case Float:
                value = typedDatumFactory.createSmallTable(1, 1);
                value.setTypedDatumForCell(input, 0, 0);
                excelService.setValues(excelFile, addr, value);
                break;
            case Integer:
                value = typedDatumFactory.createSmallTable(1, 1);
                value.setTypedDatumForCell(input, 0, 0);
                excelService.setValues(excelFile, addr, value);
                break;
            case Boolean:
                value = typedDatumFactory.createSmallTable(1, 1);
                value.setTypedDatumForCell(input, 0, 0);
                excelService.setValues(excelFile, addr, value);
                break;
            case SmallTable:
                value = (SmallTableTD) input;
                
                if (expand) {
                    addr = ExcelAddress.getExcelAddressForTableRange(excelFile, addr, value.getRowCount(), value.getColumnCount());
                }                
                excelService.setValues(excelFile, addr, value);
                break;
            default:
                break;
            }
            
            //Fill data for runtime GUI and send it
            final File originExcelFile = ExcelUtils.getAbsoluteFile(componentContext
                .getConfigurationValue(ExcelComponentConstants.XL_FILENAME));
            final ChannelValue dataval = new ChannelValue(originExcelFile, addr, inputName, true, expand, iteration);
            dataval.setValues(value);
            dataval.setPreMacro(componentContext.getConfigurationValue(ExcelComponentConstants.PRE_MACRO));
            dataval.setRunMacro(componentContext.getConfigurationValue(ExcelComponentConstants.RUN_MACRO));
            dataval.setPostMacro(componentContext.getConfigurationValue(ExcelComponentConstants.POST_MACRO));
            notificationService.send(componentContext.getExecutionIdentifier() + ExcelComponentConstants.NOTIFICATION_SUFFIX, dataval);
        }
    }
    
    private void initializeNewHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            historyDataItem = new ExcelComponentHistoryDataItem();
            historyDataItem.setExcelFilePath(ExcelUtils.getAbsoluteFile(componentContext.getConfigurationValue(
                    ExcelComponentConstants.XL_FILENAME)).getAbsolutePath());
        }
    }
    
    private void storeHistoryDataItem() {
        if (Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            componentContext.writeFinalHistoryDataItem(historyDataItem);
        }
    }
    
}
