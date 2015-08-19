/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.utils.ole;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.OleListener;

/**
 * Specified all the events that are available to the active-x interface Excel.Application. Allows
 * to simply add EventListener into the Excel object to control the calling java application.
 * 
 * @author Philipp Fischer
 */
public class OleXLEvents extends OleXLGeneral {

    // IDs taken from Excel TypeLib (use OLEViewer.exe from the MS resource kit to follow)
    // Apparently there is an implementation issue in the excel interface that cannot hand back
    // the standard event source and sink. Further on it is not clear how to resolve the IDs of the
    // events themselves even though it should work using the getIDsOfNames. Looks like it is a
    // question of using the right OleAutomation object to make all this work properly.

    /** Event Sink ID. */
    public static final String IID_APP_EVENTS = "{00024413-0000-0000-C000-000000000046}";

    /** Event ID. */
    public static final int NEW_WORKBOOK = 0x0000061d;
    /** Event ID. */
    public static final int SHEET_SELECTION_CHANGE = 0x00000616;
    /** Event ID. */
    public static final int SHEET_BEFORE_DOUBLE_CLICK = 0x00000617;
    /** Event ID. */
    public static final int SHEET_BEFORE_RIGHT_CLICK = 0x00000618;
    /** Event ID. */
    public static final int SHEET_ACTIVATE = 0x00000619;
    /** Event ID. */
    public static final int SHEET_DEACTIVATE = 0x0000061a;
    /** Event ID. */
    public static final int SHEET_CALCULATE = 0x0000061b;
    /** Event ID. */
    public static final int SHEET_CHANGE = 0x0000061c;
    /** Event ID. */
    public static final int WORKBOOK_OPEN = 0x0000061f;
    /** Event ID. */
    public static final int WORKBOOK_ACTIVATE = 0x00000620;
    /** Event ID. */
    public static final int WORKBOOK_DEACTIVATE = 0x00000621;
    /** Event ID. */
    public static final int WORKBOOK_BEFORE_CLOSE = 0x00000622;
    /** Event ID. */
    public static final int WORKBOOK_BEFORE_SAVE = 0x00000623;
    /** Event ID. */
    public static final int WORKBOOK_BEFORE_PRINT = 0x00000624;
    /** Event ID. */
    public static final int WORKBOOK_NEW_SHEER = 0x00000625;
    /** Event ID. */
    public static final int WORKBOOK_ADDIN_INSTALL = 0x00000626;
    /** Event ID. */
    public static final int WORKBOOK_ADDIN_UNINSTALL = 0x00000627;
    /** Event ID. */
    public static final int WINDOW_RESIZE = 0x00000612;
    /** Event ID. */
    public static final int WINDOW_ACTIVATE = 0x00000614;
    /** Event ID. */
    public static final int WINDOW_DEACTIVATE = 0x00000615;
    /** Event ID. */
    public static final int SHEET_FOLLOW_HYPERLINK = 0x0000073e;

    /**
     * General constructor.
     * 
     * @param oleContainer Automation-object corresponding to the container.
     * @param controlSite OleControlSite that corresponds to the excel object.
     */
    public OleXLEvents(OleAutomation oleGeneral, OleControlSite controlSite) {
        super(oleGeneral, controlSite);
    }

    /**
     * To attach an EventListener onto an ExcelEvent.
     * 
     * @param eventID ID of the event to attach to. EventIDs are stored as static integers into the
     *        class.
     * @param listener The listener that is supposed to be attached.
     */
    public void addEventListener(int eventID, OleListener listener) {
        controlSite.addEventListener(oleObject, IID_APP_EVENTS, eventID, listener);
    }
}
