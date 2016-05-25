/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.common;

import java.io.File;

import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.rce.components.excel.commons.ExcelAddress;


/**
 * Class providing convenient access to the Excel Service. It serves as an
 * abstraction.
 *
 * @author Markus Kunde
 */
public class SimpleExcelService implements ExcelService {

    private ExcelService excelService;

    public SimpleExcelService() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        excelService = serviceRegistryAccess.getService(ExcelService.class);
    }
    
    @Override
    public boolean isValidExcelFile(File xlFile) {
        return excelService.isValidExcelFile(xlFile);
    }

    @Override
    public void setValues(File xlFile, ExcelAddress addr, SmallTableTD values) throws ExcelException {
        excelService.setValues(xlFile, addr, values);
    }

    @Override
    public void setValues(File xlFile, File newFile, ExcelAddress addr, SmallTableTD values) throws ExcelException {
        excelService.setValues(xlFile, newFile, addr, values);
    }

    @Override
    public SmallTableTD getValueOfCells(File xlFile, ExcelAddress addr) throws ExcelException {
        return excelService.getValueOfCells(xlFile, addr);
    }

    @Override
    public ExcelAddress[] getUserDefinedCellNames(File xlFile) throws ExcelException {
        return excelService.getUserDefinedCellNames(xlFile);
    }

    @Override
    public String[] getMacros(File xlFile) throws ExcelException {
        return excelService.getMacros(xlFile);
    }

    @Override
    public void runMacro(File xlFile, String macroname) throws ExcelException {
        excelService.runMacro(xlFile, macroname);
        
    }

    @Override
    public void recalculateFormulas(File xlFile) throws ExcelException {
        excelService.recalculateFormulas(xlFile);
    }
}
