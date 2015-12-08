/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.util.NoSuchElementException;
import java.util.Set;

import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Callback class used to callback {@link ComponentExecutionController}s, mainly by associated {@link ComponentContext} objects.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionControllerCallback {

    /**
     * Reads input with the given name.
     * 
     * @param inputName name of the input to read
     * @return {@link TypedDatum} currently available at the input
     * @throws NoSuchElementException if there is no {@link TypedDatum} available
     */
    TypedDatum readInput(String inputName) throws NoSuchElementException;
    
    /**
     * @return all inputs with a value. I.e. {@link #readInput(String)} will be return a {@link TypedDatum}.
     */
    Set<String> getInputsWithDatum();
    
    /**
     * Writes given {@link TypedDatum} to the output. If the output is connected to inputs, the {@link TypedDatum} will be delivered
     * to all inputs.
     * 
     * @param outputName name of the ouput to write
     * @param datumToSent {@link TypedDatum} to be sent
     */
    void writeOutput(String outputName, TypedDatum datumToSent);
    
    /**
     * Resets an output with given name.
     * 
     * @param outputName name of output
     */
    void resetOutput(String outputName);
    
    /**
     * Closes an output with given name.
     * 
     * @param outputName name of output to close.
     */
    void closeOutput(String outputName);
    
    /**
     * Closes all outputs.
     */
    void closeAllOutputs();

    /**
     * @param outputName name of output
     * @return <code>true</code> if output is closed, otherwise <code>false</code>
     */
    boolean isOutputClosed(String outputName);
    
    /**
     * Prints given line to the workflow console. As type {@link ConsoleRow.Type.STDOUT} and
     * {@link ConsoleRow.Type.STDERR} are allowed-
     * 
     * @param line line to print
     * @param consoleRowType type of the console row. Must be one of {@link ConsoleRow.Type.STDOUT} or
     *        {@link ConsoleRow.Type.STDERR} are allowed-
     */
    void printConsoleRow(String line, Type consoleRowType);
    
    /**
     * @return current execution count of the component. Count starts with 1. It is 1 within {@link Component#start(ComponentContext)} and
     *         is 1 within {@link Component#processInputs()} if {@link Component#start(ComponentContext)} returns <code>false</code> or 2
     *         otherwise.
     */
    int getExecutionCount();
    
    /**
     * Writes intermediate history data. Each new intermediate history data will overwrite a previous one.
     * 
     * @param historyDataItem {@link ComponentHistoryDataItem} to write
     */
    void writeIntermediateHistoryData(ComponentHistoryDataItem historyDataItem);
    
    /**
     * Writes final history data. It will overwrite any intermediate ones.
     * 
     * @param historyDataItem {@link ComponentHistoryDataItem} to write
     */
    void writeFinalHistoryDataItem(ComponentHistoryDataItem historyDataItem);
    
    /**
     * @return data management id of latest component execution
     */
    Long getComponentExecutionDataManagementId();
    
}
