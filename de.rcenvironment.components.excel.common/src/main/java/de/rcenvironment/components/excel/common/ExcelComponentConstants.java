/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.excel.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants shared by GUI and Non-GUI implementations.
 * 
 * @author Patrick Schaefer
 * @author Markus Kunde
 */
public final class ExcelComponentConstants {

    /** Separator of columns in one line String copy process. */
    public static final String STRINGLINESEPARATOR = "; ";
    
    /** Separator for runtime table viewer and copy to clipboard functinality. */
    public static final String TABLEVALUESEPARATOR = "\t";

    /** Separator for runtime table viewer and copy to clipboard functinality. */
    public static final String TABLELINESEPARATOR = "\r\n";
    
    /** Name of the component as it is defined declaratively in OSGi component. */
    public static final String COMPONENT_NAME = "Excel";

    /** Internal identifier of the Excel component. */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "excel";
    
    /** Internal identifier of the Excel component. */
    public static final String[] COMPONENT_IDS = new String[] { COMPONENT_ID, 
        "de.rcenvironment.rce.components.excel.ExcelComponent_" + COMPONENT_NAME }
    ;
    /** Suffix used for publishing Excel notifications. */
    public static final String NOTIFICATION_SUFFIX = ":rce.component.excel";
    
    /** Property key as it is defined declaratively in OSGi component. */
    public static final String XL_FILENAME = "xlFilename";

    /** Property key as it is defined declaratively in OSGi component. */
    public static final String PRE_MACRO = "preMacro";

    /** Property key as it is defined declaratively in OSGi component. */
    public static final String RUN_MACRO = "runMacro";

    /** Property key as it is defined declaratively in OSGi component. */
    public static final String POST_MACRO = "postMacro";
    
    /** Property key as it is defined declaratively in OSGi component. */
    public static final String DRIVER = "Driver";
    
    
    /*
     * Channels.
     * 
     */
    
    /** Regex for input user defined variable names. */
    public static final String DISCOVER_INPUT_REGEX = "^(I_)[ A-Za-z0-9!\"#$%&'()*+,./:;<=>?@\\^_`{|}~-]*";
    
    /** Regex for output user defined variable names. */
    public static final String DISCOVER_OUTPUT_REGEX = "^(O_)[ A-Za-z0-9!\"#$%&'()*+,./:;<=>?@\\^_`{|}~-]*";
    
    /** Property key for address of channel. */
    public static final String METADATA_ADDRESS = "address";
    
    /** Property key for expanding of cell flag. */
    public static final String METADATA_EXPANDING = "expanding";
    
    /** Property key for pruning table at right or bottom. */
    public static final String METADATA_PRUNING = "pruning";
    
    
    
    /*
     * Excel specific.
     * 
     */    
    
    /** Divider between Table and cell address, e. g., like Table1!A1 */
    public static final String DIVIDER_TABLECELLADDRESS = "!";
    
    /** Divider between cell address, e. g., like A1:B5 */
    public static final String DIVIDER_CELLADDRESS = ":";
    
    /** Flag in Excel addresses for absolute address. */
    public static final String ABSOLUTEFLAG = "$";
    
    /** First row number in Excel. */
    public static final String DEFAULTROWBEGIN = "1";

    /** First column char in Excel. */
    public static final String DEFAULTCOLUMNBEGIN = "A";
    
    /** Default table expanding for auto discovery. */
    public static final boolean DEFAULT_TABLEEXPANDING = false;
    
    /** Default pruning for auto discovery. */
    public static final boolean DEFAULT_TABLEPRUNING = false;
    
    
    
    private ExcelComponentConstants() {}

}
