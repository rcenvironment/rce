/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole.swt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.core.gui.utils.ole.OleXLApplication;

/**
 * Enables the interchange of data between Excel and SWT.
 *
 * @author Philipp Fischer
 */
public class ExcelToSWTConnector {

    private OleXLApplication xlApplication;

    private Map<Widget, ExcelToSWTConnection> mapConnections;

    public ExcelToSWTConnector(OleXLApplication xlApplication) {
        this.xlApplication = xlApplication;
        mapConnections = new HashMap<Widget, ExcelToSWTConnection>();
    }

    /**
     * Registers a widget in order to be able to read cells.
     * 
     * @param widget Widget to register.
     * @param fullCellAddress Cell address to read.
     * @param connector Connector establish the connection to Excel.
     */
    public void registerWidgetToReadFromCell(Widget widget, String fullCellAddress, ExcelToWidgetConnector connector) {
        mapConnections.put(widget, new ExcelToSWTConnection(fullCellAddress, false, connector, xlApplication));
    }

    /**
     * Registers a widget in order to be able to write cells.
     * 
     * @param widget Widget to register.
     * @param fullCellAddress Cell address to read.
     * @param connector Connector establish the connection to Excel.
     */
    public void registerWidgetToWriteToCell(Widget widget, String fullCellAddress, ExcelToWidgetConnector connector) {
        mapConnections.put(widget, new ExcelToSWTConnection(fullCellAddress, true, connector, xlApplication));
    }

    /**
     * Executes the data interchange for the given widget.
     * 
     * @param widget Widget to execute.
     */
    public void execute(Widget widget) {
        mapConnections.get(widget).execute();
    }

    /**
     * Executes the data interchange for all registered widgets.
     */
    public void executeAll() {
        Iterator<Entry<Widget, ExcelToSWTConnection>> mapIterator = mapConnections.entrySet().iterator();

        while (mapIterator.hasNext()) {
            Map.Entry<Widget, ExcelToSWTConnection> mapEntry = (Map.Entry<Widget, ExcelToSWTConnection>) mapIterator.next();
            mapEntry.getValue().execute();
        }
    }

    /**
     * Unregisters a widget so that it won't be included in future executions anymore.
     * 
     * @param widget Widget to unregister.
     */
    public void unregisterWidget(Widget widget) {
        mapConnections.remove(widget);
    }

    /**
     * Unregisters all widgets so that they won't be included in future executions anymore.
     */
    public void unregsiterAllWidgets() {
        mapConnections.clear();
    }
}
