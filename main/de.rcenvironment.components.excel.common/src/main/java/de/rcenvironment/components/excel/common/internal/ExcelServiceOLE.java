/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.common.internal;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.activeX.ActiveXDispatchEvents;
import com.jacob.activeX.ActiveXInvocationProxy;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelException;
import de.rcenvironment.components.excel.common.ExcelServiceGUIEvents;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;

/**
 * Excel file representation with access to its data. Methods which ends with OLE are using OLE-interface. These methods may only used
 * within MS Windows and MS Office installation.
 * 
 * Note: These methods are unstable at the moment. This is because Eclipse 3.6 SWT-OLE interface in combination with Windows 7 64bit and MS
 * Office 2010 is unstable.
 * 
 * @author Markus Kunde
 */
public class ExcelServiceOLE extends ExcelServicePOI implements ExcelServiceGUIEvents {

    /*
     * OLE Keys.
     */
    private static final String OLE_DISPLAY_ALERTS = "DisplayAlerts";

    private static final String OLE_ACTIVE_WORKBOOK = "ActiveWorkbook";

    private static final String OLE_ACTIVE_SHEET = "ActiveSheet";

    private static final String OLE_OPEN = "Open";

    private static final String OLE_WORKBOOKS = "Workbooks";

    private static final String OLE_VISIBLE = "Visible";

    private static final String EXCELAPPLICATION_PROGRAMID = "Excel.Application";

    private static final String EXCELAPPLICATION_EXE = "\\EXCEL.EXE";

    private String typeLibLocation = "C:\\Program Files (x86)\\Microsoft Office\\OFFICE14" + EXCELAPPLICATION_EXE; // Only default

    /**
     * Default constructor.
     * 
     */
    public ExcelServiceOLE() {}

    /**
     * Constructor to get typedDatumFactory not from RCE-service into ExcelService class.
     * 
     * @param typedDatumFactory the typed datum factory
     */
    public ExcelServiceOLE(TypedDatumFactory typedDatumFactory) {
        super(typedDatumFactory);
    }

    @Override
    public String[] getMacros(File xlFile) throws ExcelException {
        Set<String> macros = new HashSet<String>();

        ComThread.InitSTA();
        ActiveXComponent objExcel = null;

        try {
            objExcel = new ActiveXComponent(EXCELAPPLICATION_PROGRAMID);
            objExcel.setProperty(OLE_VISIBLE, new Variant(false));

            // Open Excel file, get the workbooks object required for access:
            Dispatch workbooks = objExcel.getProperty(OLE_WORKBOOKS).toDispatch();
            workbooks = Dispatch.call(workbooks, OLE_OPEN, xlFile.getAbsolutePath()).toDispatch();

            Dispatch activeWorkbook = objExcel.getProperty(OLE_ACTIVE_WORKBOOK).toDispatch();

            // VB specific
            Variant varVbproject = Dispatch.get(activeWorkbook, "VBProject");
            if (varVbproject != null) { // security rights are set properly
                Dispatch vbproject = varVbproject.toDispatch();
                Dispatch vbcomponents = Dispatch.get(vbproject, "VBComponents").toDispatch();
                Variant varCount = Dispatch.get(vbcomponents, "Count");
                for (int i = 1; i <= varCount.getInt(); i++) {
                    Dispatch vbComponent = Dispatch.call(vbcomponents, "Item", new Variant(i)).toDispatch();

                    String name = Dispatch.get(vbComponent, "Name").getString();

                    Dispatch codeModule =
                        Dispatch.get(vbComponent, "CodeModule").getDispatch();

                    int noOfLines =
                        Dispatch.get(codeModule, "CountOfLines").getInt();

                    for (int j = 1; j <= noOfLines; j++) {
                        String line = Dispatch.invoke(codeModule, "ProcOfLine", Dispatch.Get, new Object[] { j, 0 }, new int[1]).toString();
                        macros.add(name + "." + line);
                    }
                }
            }
        } catch (RuntimeException e) {

            String[] noMacroFoundArray = new String[1];
            noMacroFoundArray[0] = "Failed to access macros - see log for details.";
            LOGGER.warn("Failed to load macros. "
                + "Possibly your security setting prevent you from accessing macros with your Excel installation. "
                + "Excel response: " + e.getMessage());
            return noMacroFoundArray;
        } finally {
            quitExcel(objExcel, false);
            ComThread.Release();
        }
        return macros.toArray(new String[0]);
    }

    @Override
    public void runMacro(File xlFile, String macroname) throws ExcelException {
        if (macroname != null && !macroname.isEmpty()) {
            ComThread.InitSTA();
            ActiveXComponent objExcel = null;
            try {
                objExcel = new ActiveXComponent(EXCELAPPLICATION_PROGRAMID);
                objExcel.setProperty(OLE_VISIBLE, new Variant(false));

                // Open Excel file, get the workbooks object required for access:
                Dispatch workbooks = objExcel.getProperty(OLE_WORKBOOKS).toDispatch();
                workbooks = Dispatch.call(workbooks, OLE_OPEN, xlFile.getAbsolutePath()).toDispatch();

                Dispatch activeWorkbook = objExcel.getProperty(OLE_ACTIVE_WORKBOOK).toDispatch();

                // run macro specific
                Dispatch.call(objExcel, "run", macroname);

                for (int i = 0; i < BLOCKING_ITERATIONMAX; i++) {
                    try {
                        objExcel.setProperty(OLE_DISPLAY_ALERTS, false);
                        objExcel.setProperty("CalculateBeforeSave", false);
                        Dispatch.call(activeWorkbook, "Save");
                        break;
                    } catch (RuntimeException e) {
                        if (i == (BLOCKING_ITERATIONMAX - 1)) {
                            // Last iteration was not successful
                            LOGGER.error("Cannot save file with result data after running macro: " + macroname + ".");
                            throw e;
                        }
                    }
                    try {
                        Thread.sleep(BLOCKING_SLEEP);
                    } catch (InterruptedException e) {
                        LOGGER.error(e);
                    }
                }

            } catch (RuntimeException e) {
                throw new ExcelException("Cannot run macro with OLE interface.", e);
            } finally {
                quitExcel(objExcel, false);
                ComThread.Release();
            }
        }
    }

    @Override
    public void recalculateFormulas(File xlFile) throws ExcelException {
        ComThread.InitSTA();
        ActiveXComponent objExcel = null;

        try {
            objExcel = new ActiveXComponent(EXCELAPPLICATION_PROGRAMID);
            objExcel.setProperty(OLE_VISIBLE, new Variant(false));

            // Open Excel file, get the workbooks object required for access:
            Dispatch workbooks = objExcel.getProperty(OLE_WORKBOOKS).toDispatch();
            workbooks = Dispatch.call(workbooks, OLE_OPEN, xlFile.getAbsolutePath()).toDispatch();

            Dispatch activeWorkbook = objExcel.getProperty(OLE_ACTIVE_WORKBOOK).toDispatch();

            // recalculate formulas specific
            Dispatch.call(objExcel, "CalculateFullRebuild");
            objExcel.setProperty(OLE_DISPLAY_ALERTS, false);
            objExcel.setProperty("CalculateBeforeSave", false);
            Dispatch.call(activeWorkbook, "Save");

        } catch (RuntimeException e) {
            throw new ExcelException("Cannot recalculate formulas with OLE interface.", e);
        } finally {
            quitExcel(objExcel, false);
            ComThread.Release();
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.components.excel.common.ExcelServiceGUIEvents #openExcelApplicationRegisterListener(java.io.File,
     *      com.jacob.activeX.ActiveXInvocationProxy)
     */
    @Override
    public ActiveXComponent openExcelApplicationRegisterListener(final File xlFile,
        final String address, final ActiveXInvocationProxy listener) {
        ActiveXComponent objExcel = null;

        try {
            objExcel = new ActiveXComponent(EXCELAPPLICATION_PROGRAMID);
            typeLibLocation = objExcel.getProperty("Path").getString() + "\\EXCEL.EXE";
            objExcel.setProperty(OLE_VISIBLE, new Variant(true));

            // Open Excel file, get the workbooks object required for access:
            Dispatch workbooks = objExcel.getProperty(OLE_WORKBOOKS).toDispatch();
            Dispatch.call(workbooks, OLE_OPEN, xlFile.getAbsolutePath()).toDispatch();

            if (address != null && !address.isEmpty() && address.split(ExcelComponentConstants.DIVIDER_TABLECELLADDRESS).length == 2) {
                // Preselect Excel Address range if possible
                String[] rawAddress = address.split(ExcelComponentConstants.DIVIDER_TABLECELLADDRESS);
                Dispatch sheet = Dispatch.call(objExcel, "Sheets", new Variant(rawAddress[0])).toDispatch();
                Dispatch.call(sheet, "Activate");
                Dispatch range = Dispatch.call(sheet, "Range", new Variant(rawAddress[1])).toDispatch();
                Dispatch.call(range, "Select");
            } else {
                Dispatch sheet = objExcel.getProperty(OLE_ACTIVE_SHEET).toDispatch();
                Dispatch range = Dispatch.call(sheet, "Range",
                    new Variant(ExcelComponentConstants.DEFAULTCOLUMNBEGIN + ExcelComponentConstants.DEFAULTROWBEGIN)).toDispatch();
                Dispatch.call(range, "Select");
            }

            // Register listener
            if (listener != null) {
                new ActiveXDispatchEvents(objExcel, listener, EXCELAPPLICATION_PROGRAMID, typeLibLocation);
            }

            return objExcel;
        } catch (RuntimeException e) {
            quitExcel(objExcel, false);
            throw new ExcelException("Cannot open Excel Application with GUI and event listener with OLE interface.", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see de.rcenvironment.components.excel.common.ExcelServiceGUIEvents#quitExcel(com.jacob.activeX.ActiveXComponent)
     */
    @Override
    public void quitExcel(final ActiveXComponent axc, boolean displayAlerts) {
        if (axc != null) {
            axc.setProperty(OLE_DISPLAY_ALERTS, displayAlerts);
            axc.invoke("Quit", new Variant[] {});
        }
    }
}
