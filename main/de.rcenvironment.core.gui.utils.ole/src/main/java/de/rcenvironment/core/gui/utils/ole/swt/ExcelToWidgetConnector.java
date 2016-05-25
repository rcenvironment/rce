/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole.swt;

import org.eclipse.swt.ole.win32.Variant;

/**
 * Enables the interchange of data between a SWT widget and applications like Excel via genere OLE
 * mechanism.
 * 
 * @author Philipp Fischer
 */
public abstract class ExcelToWidgetConnector {

    /**
     * @param data the data to set.
     */
    public void setDataToWidget(Variant data) {}

    /**
     * @return the data from the widget.
     */
    public Variant getDataFromWidget() {
        return (new Variant());
    }
}
