/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.ole.win32.Variant;


/**
 * Static class that contains factory methods to create Excel-Objects into the SWT OleFrame.
 * 
 * @author Philipp Fischer
 * @author Markus Kunde
 */
public final class OleExcel {

    private static final String EXCEL_APPLICATION = "Excel.Application";

    private OleExcel() {}

    /**
     * Factory-Method to create an Excel-Instance into an SWT OleFrame.
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @return An object of OleXLWorkbook which is abstracting the complex Active-X interface.
     */
    public static OleXLWorkbook createExcelSheet(OleFrame oleFrame) {
        // Connect to the SWT Frame and create an Excel.Addin within.
        // The Excel.AddIn will open an excel sheet within the client zone of the SWT
        // Window. Afterwards the OleAutomation object is created which corresponds to the
        // Workbook when initiating the communication with Excel.AddIn
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, "Excel.Sheet");
        OleAutomation automationObject = new OleAutomation(controlSite);
        controlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);

        // Now we will create the OleWorkbook object that is abstracting the complex
        // Active-X interface.
        OleXLWorkbook retXLWorkbook = new OleXLWorkbook(automationObject, controlSite);

        // Hand out the FactoryMethod Result
        return retXLWorkbook;
    }

    /**
     * Factory-Method to create an Excel-Instance into an SWT OleFrame. This method loads the
     * given Excel file and thus opens Excel.
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @param xlFile Excel file to display.
     * @return An object of OleXLWorkbook which is abstracting the complex Active-X interface.
     */
    public static OleXLWorkbook createExcelSheet(OleFrame oleFrame, File xlFile) {
        // Connect to the SWT Frame and create an Excel.Addin within.
        // The Excel.AddIn will open an excel sheet within the client zone of the SWT
        // Window. Afterwards the OleAutomation object is created which corresponds to the
        // Workbook when initiating the communication with Excel.AddIn
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, xlFile);
        OleAutomation automationObject = new OleAutomation(controlSite);
        controlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);

        // Now we will create the OleWorkbook object that is abstracting the complex
        // Active-X interface.
        OleXLWorkbook retXLWorkbook = new OleXLWorkbook(automationObject, controlSite);

        // Hand out the FactoryMethod Result
        return retXLWorkbook;
    }

    /**
     * Factory-Method to create an Excel-Instance into an SWT OleFrame. This method opens Excel
     * and loads the given Excel file. It is faster as createExcelSheet().
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @param xlFile Excel file to display.
     * @return An object of OleXLApplication which is abstracting the complex Active-X interface.
     */
    public static OleXLApplication createExcelApplicationFast(OleFrame oleFrame, File xlFile) {
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, EXCEL_APPLICATION);
        controlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);
        OleAutomation automationObject = new OleAutomation(controlSite);

        Variant xlsFileVariant = new Variant(xlFile.getAbsolutePath());
        Variant[] openArgs = new Variant[] { xlsFileVariant };

        int[] excelIds = automationObject.getIDsOfNames(new String[] { "Workbooks" });
        Variant workbooksVariant = automationObject.getProperty(excelIds[0]);
        OleAutomation workbooks = workbooksVariant.getAutomation();
        int[] workbooksIds = workbooks.getIDsOfNames(new String[] { "Open" });
        int workbooksOpenId = workbooksIds[0];

        workbooks.invoke(workbooksOpenId, openArgs);

        Variant[] arguments = new Variant[1];
        arguments[0] = new Variant("false");

        int[] ids = automationObject.getIDsOfNames(new String[] { "Visible" });
        automationObject.setProperty(ids[0], arguments);

        // Now we will create the OleWorkbook object that is abstracting the complex
        // Active-X interface.
        OleXLApplication retXLApplication = new OleXLApplication(automationObject, controlSite);

        // Hand out the FactoryMethod Result
        return retXLApplication;

    }

    /**
     * Factory-Method to create an Excel-Instance with an SWT OleFrame. Excel is not started in the
     * frame and is invisible in the background.
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @return An object of OleXLApplication which is abstracting the complex Active-X interface.
     */
    public static OleXLApplication createExcelApplication(OleFrame oleFrame) {
        // Connect to the SWT Frame and create an Excel.Addin within.
        // The Excel.AddIn will open an excel sheet within the client zone of the SWT
        // Window. Afterwards the OleAutomation object is created which corresponds to the
        // Workbook when initiating the communication with Excel.AddIn
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, EXCEL_APPLICATION);
        OleAutomation automationObject = new OleAutomation(controlSite);

        // Now we will create the OleWorkbook object that is abstracting the complex
        // Active-X interface.
        OleXLApplication retXLApplication = new OleXLApplication(automationObject, controlSite);

        // Hand out the FactoryMethod Result
        return retXLApplication;
    }

    /**
     * Factory-Method to create an Excel-Instance with an SWT OleFrame. Excel is not started in the
     * frame and is invisible in the background.
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @param xlFile Excel file to load.
     * @return An object of OleXLApplication which is abstracting the complex Active-X interface.
     */
    public static OleXLApplication createExcelApplication(OleFrame oleFrame, File xlFile) {
        // Connect to the SWT Frame and create an Excel.Addin within.
        // The Excel.AddIn will open an excel sheet within the client zone of the SWT
        // Window. Afterwards the OleAutomation object is created which corresponds to the
        // Workbook when initiating the communication with Excel.AddIn
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, xlFile);
        OleAutomation automationObject = new OleAutomation(controlSite);

        // Now we will create the OleWorkbook object that is abstracting the complex
        // Active-X interface.
        OleXLApplication retXLApplication = new OleXLApplication(automationObject, controlSite);

        // Hand out the FactoryMethod Result
        return retXLApplication;
    }
    
    /**
     * Deactivates all COM-Addins.
     * Must open and close Excel.Application.
     * 
     * @param oleFrame Object of an OleFrame from the SWT framework.
     * @return String-Array of all deactivated COM-Addins (for a later activation).
     */
    public static String[] deactivateAllCOMAddins(OleFrame oleFrame) {
        List<String> arl = new ArrayList<String>();
        OleControlSite controlSite = new OleControlSite(oleFrame, SWT.NONE, EXCEL_APPLICATION);
        OleAutomation application = new OleAutomation(controlSite);
        
        
        int idAddIns = application.getIDsOfNames(new String[] {"COMAddIns"})[0];
        Variant addInsVar = application.getProperty(idAddIns);
        OleAutomation addIns = addInsVar.getAutomation();
        
        int countID = addIns.getIDsOfNames(new String[] {"Count"})[0];
        Variant countVariant = addIns.getProperty(countID);
                
        int addInsItemID = addIns.getIDsOfNames(new String[] {"Item"})[0];
        for (int i = 1; i <= countVariant.getInt(); i++) {
            Variant addInsItemVariant = addIns.invoke(addInsItemID, new Variant[] {new Variant(i)});
            OleAutomation addInsItem = addInsItemVariant.getAutomation();
            
            int addInsItemConnectID = addInsItem.getIDsOfNames(new String[] { "Connect" })[0];
            if (addInsItem.getProperty(addInsItemConnectID).getBoolean()) {
                int addInsItemNameID = addInsItem.getIDsOfNames(new String[] { "ProgId" })[0];
                Variant addInsItemNameVariant = addInsItem.getProperty(addInsItemNameID);
                String name = addInsItemNameVariant.getString();
                arl.add(name);
                
                addInsItem.setProperty(addInsItemConnectID, new Variant(false));
            }
        }
        
        // Close Excel
        int displayAlertsID = application.getIDsOfNames(new String[] { "DisplayAlerts" })[0];
        application.setProperty(displayAlertsID, new Variant(false));
        
        int quitID = application.getIDsOfNames(new String[] { "Quit" })[0];
        application.invokeNoReply(quitID);
        
        return arl.toArray(new String[0]);
    }
}
