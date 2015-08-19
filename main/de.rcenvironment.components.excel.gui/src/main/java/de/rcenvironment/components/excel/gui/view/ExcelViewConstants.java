/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.excel.gui.view;


/**
 * Constants for ExcelView.
 *
 * @author Markus Kunde
 */
public final class ExcelViewConstants {

    /**
     * Time constant how often thread should look for new channel values.
     */
    public static final int UITHREAD_SLEEPER = 200; 

    /**
     * Base path to icons.
     */
    public static final String ICONBASEPATH = "resources/";
    
    /**
     * Filename of excel icon. Size 16.
     */
    public static final String ICONNAME_EXCEL_16 = "excel_16.png";
    
    /**
     * Filename of excel icon. Size 64.
     */
    public static final String ICONNAME_EXCEL_64 = "excel_64.png";
    
    /**
     * Filename of copy to clipboard icon.
     */
    public static final String ICONNAME_CLIPBOARD = "copy_edit_co.png";
    
    /**
     * Width of normal column.
     */
    public static final int NORMALCOLUMNWIDTH = 150;

    /**
     * Private constructor.
     */
    private ExcelViewConstants() {}
}
