/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import java.io.File;

import de.rcenvironment.core.datamodel.types.api.SmallTableTD;


/**
 * Excel service interface for interaction with MS Excel.
 * Implementations should be stateless (e. g., "static oriented") regarding usage in stateless services.
 *
 * @author Markus Kunde
 */
public interface ExcelService {

    /**
     * Test if file is a valid Excel file.
     * 
     * @param xlFile Excel file
     * @return true if valid Excel file
     */
    boolean isValidExcelFile(final File xlFile);

    
    
    /**
     * @see #setValues(File, File, ExcelAddress, ITable)
     * 
     * @param xlFile xlFile
     * @param addr addr
     * @param values values
     * @throws ExcelException ExcelException
     */
    void setValues(final File xlFile, final ExcelAddress addr, final SmallTableTD values) throws ExcelException;
    
    
   
    /**
     * Writes values into Excel file and saves it.
     * The smaller range (address or values range) will be taken, rest will be cut off.
     * 
     * @param xlFile Excel file
     * @param newFile the Excel filename where to save new Excel file
     * @param addr Address where data should be inserted into
     * @param values Data which should be inserted
     * @throws ExcelException thrown if exception occurs during runtime
     */
    void setValues(final File xlFile, final File newFile, final ExcelAddress addr, final SmallTableTD values) throws ExcelException;
    
    
    
    /**
     * Reads values from Excel file and gives it back.
     * 
     * @param xlFile Excel file
     * @param addr Address where data should be inserted into
     * @return Data Store of data
     * @throws ExcelException thrown if exception occurs during runtime
     */
    SmallTableTD getValueOfCells(final File xlFile, final ExcelAddress addr) throws ExcelException;
    
    
    
    /**
     * Discovers all user defined cell names in Excel file.
     * 
     * @param xlFile Excel file
     * @return List of Excel Addresses
     * @throws ExcelException thrown if exception occurs during runtime
     */
    ExcelAddress[] getUserDefinedCellNames(final File xlFile) throws ExcelException;
    
    
    
    /**
     * Returns a list of all discovered macros in Excel file.
     * 
     * @param xlFile Excel file
     * @return list of discovered macros
     * @throws ExcelException thrown if exception occurs during runtime
     */
    String[] getMacros(final File xlFile) throws ExcelException;
    
    
    
    /**
     * Runs a (VBA-)macro in Excel file.
     * 
     * @param xlFile Excel file
     * @param macroname run this macroname in Excel file
     * @throws ExcelException thrown if exception occurs during runtime
     */
    void runMacro(final File xlFile, final String macroname) throws ExcelException;
    
    
    
    /**
     * Recalculates all formulas in Excel file.
     * 
     * @param xlFile Excel file
     * @throws ExcelException thrown if exception occurs during runtime
     */
    void recalculateFormulas(final File xlFile) throws ExcelException;
    
}
