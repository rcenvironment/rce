/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.Variant;

/**
 * Is the top level object to access excel.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public class OleXLApplication extends OleXLGeneral {

    /**
     * General constructor.
     * 
     * @param oleContainer Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLApplication(OleAutomation oleApplication, OleControlSite controlSite) {
        super(oleApplication, controlSite);
    }

    /**
     * Gets all workbooks.
     *
     * @return the workbooks.
     */
    public OleXLWorkbooks getWorkbooks() {
        // Get the ID of the application property and read it to create an automation object out of
        // this
        int idXLWorkbooks = getIDofName("Workbooks");
        OleAutomation automationObject = oleObject.getProperty(idXLWorkbooks).getAutomation();
        logOleError(oleObject.getLastError());
        
        // Now use the automation object to create the OleXLApplication object
        OleXLWorkbooks retXLWorkbooks = new OleXLWorkbooks(automationObject, controlSite);

        // hand back the created object
        return retXLWorkbooks;
    }

    /**
     * Makes the excel-object quit itself.
     */
    public void quit() {
        invokeNoReply("Quit");
    }

    /**
     * Makes the excel-object quit itself, without any question.
     */
    public void quitWithoutQuery() {
        setProperty("DisplayAlerts", new Variant(false));
        invokeNoReply("Quit");
    }

    /**
     * Tells if the excel object is visible on the display or not.
     * 
     * @return <code>true</code> if visible, else <code>false</code>.
     */
    public boolean isVisible() {
        return getProperty("Visible").getBoolean();
    }

    /**
     * Allows to make the excel object visible or invisible on the display.
     * 
     * @param visible set to true to make the excel object visible.
     */
    public void setVisible(boolean visible) {
        setProperty("Visible", new Variant(visible));
    }

    /**
     * Gives access the currently active cell.
     * 
     * @return OleXLRange object on the active cell.
     */
    public OleXLRange getActiveCell() {
        OleAutomation oleRange = getProperty("ActiveCell").getAutomation();
        return new OleXLRange(oleRange, controlSite);
    }

    /**
     * Gives acces the currently range.
     * 
     * @return OleXLRange object on the active range.
     */
    public OleXLRange getActiveRange() {
        OleAutomation oleRange = getProperty("Selection").getAutomation();
        return new OleXLRange(oleRange, controlSite);
    }

    /**
     * Gives access the currently active sheet in the excel object.
     * 
     * @return OleXLWorksheet object to the currently active sheet.
     */
    public OleXLWorksheet getActiveSheet() {
        OleAutomation oleSheet = getProperty("ActiveSheet").getAutomation();
        return new OleXLWorksheet(oleSheet, controlSite);
    }

    /**
     * Gives access the currently active workbook in the excel object.
     * 
     * @return OleXLWorkbook object to the currently active workbook.
     */
    public OleXLWorkbook getActiveWorkbook() {
        Variant variant = getProperty("ActiveWorkbook");
        if (variant.getType() != OLE.VT_EMPTY) {
            OleAutomation oleWorkbook = variant.getAutomation();
            return new OleXLWorkbook(oleWorkbook, controlSite);
        } else {
            OleXLWorkbooks workbooks = getWorkbooks();
            OleXLWorkbook workbook = workbooks.add();
            workbook.activate();
            return workbook;
        }
    }

    /**
     * Runs a macro in an Excel application.
     * 
     * @param macroName The name of the macro to start.
     */
    public void runMacro(String macroName) {
        Variant[] params = new Variant[1];
        params[0] = new Variant(macroName);
        invoke("run", params);
    }

    /**
     * Recalculate all open workbooks.
     */
    public void calculate() {
        invoke("CalculateFullRebuild");
    }
    
    /**
     * SaveAs method of application.
     * 
     * @param fileName absolute path and filename of file
     */
    public void saveAs(final String fileName) {
        setProperty("DisplayAlerts", new Variant(false));
        setProperty("CalculateBeforeSave", new Variant(false));
        getActiveWorkbook().invoke("SaveAs", new Variant[] {new Variant(fileName)});
    }
    
    /**
     * Get all macros from application (if security rights are set properly).
     * 
     * @return array of all available macro names
     */
    public String[] getMacros() {
        Set<String> macros = new HashSet<String>();
        
        Variant vbprojectVariant = getActiveWorkbook().getProperty("VBProject");
        
        if (vbprojectVariant != null) { //security rights are set properly
            OleAutomation vbproject = vbprojectVariant.getAutomation();
            int vbComponentsID = vbproject.getIDsOfNames(new String[] {"VBComponents"})[0]; 
            Variant vbComponentsVariant = vbproject.getProperty(vbComponentsID);
            OleAutomation vbComponents = vbComponentsVariant.getAutomation();
            int countID = vbComponents.getIDsOfNames(new String[] {"Count"})[0];
            Variant countVariant = vbComponents.getProperty(countID);
            
            int vbcomponentID = vbComponents.getIDsOfNames(new String[] {"Item"})[0];
            for (int i = 1; i <= countVariant.getInt(); i++) {
                Variant vbComponentVariant = vbComponents.invoke(vbcomponentID, new Variant[] {new Variant(i)});
                OleAutomation vbComponent = vbComponentVariant.getAutomation();
                
                int vbComponentNameID = vbComponent.getIDsOfNames(new String[] {"Name"})[0];
                Variant componentNameVariant = vbComponent.getProperty(vbComponentNameID);
                String name = componentNameVariant.getString();
                
                int codeModuleID = vbComponent.getIDsOfNames(new String[] {"CodeModule"})[0];
                Variant codeModuleVariant = vbComponent.getProperty(codeModuleID);
                OleAutomation codeModule = codeModuleVariant.getAutomation();
                
                //Get number of codeLines
                int countOfLinesID = codeModule.getIDsOfNames(new String[] {"CountOfLines"})[0];
                int noOfLines = codeModule.getProperty(countOfLinesID).getInt();
                
                //for each codeline
                int procOfLineID = codeModule.getIDsOfNames(new String[] {"ProcOfLine"})[0];
                for (int j = 1; j <= noOfLines; j++) {
                    Variant lineVariant = codeModule.getProperty(procOfLineID, new Variant[] {new Variant(j), new Variant(0)});
                    macros.add(name + "." + lineVariant.getString());
                }
            }
        }
        return macros.toArray(new String[0]);
    }

}
