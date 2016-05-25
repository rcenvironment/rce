/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.excel.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Doreen Seider
 * @author Markus Kunde
 */
public class Messages extends NLS {

    /*
     * Validator messages.
     */
    
    /** Constant. */
    public static String errorNoExcelFileRelative;

    /** Constant. */
    public static String errorNoExcelFileAbsolute;
    
    /** Constant. */
    public static String errorWrongPreMacroRelative;

    /** Constant. */
    public static String errorWrongPreMacroAbsolute;
    
    /** Constant. */
    public static String errorWrongRunMacroRelative;

    /** Constant. */
    public static String errorWrongRunMacroAbsolute;
    
    /** Constant. */
    public static String errorWrongPostMacroRelative;

    /** Constant. */
    public static String errorWrongPostMacroAbsolute;
    
    /** Constant. */
    public static String errorNoOutputAsDriverRelative;

    /** Constant. */
    public static String errorNoOutputAsDriverAbsolute;
    
    /** Constant. */
    public static String errorNoMetaDataAddressRelative;
    
    /** Constant. */
    public static String errorNoMetaDataAddressAbsolute;
    
    /*
     * GUI elements.
     */
    
    /** Constant. */
    public static String macrosChoosingSectionName;
    
    /** Constant. */
    public static String macrosDiscoverButtonLabel;
    
    /** Constant. */
    public static String macrosSectionDescription;
    
    /** Constant. */
    public static String preMacro;
    
    /** Constant. */
    public static String postMacro;
    
    /** Constant. */
    public static String runMacro;
    
    /** Constant. */
    public static String autoDiscover;
    
    /** Constant. */
    public static String prune;
    
    /** Constant. */
    public static String expand;
    
    /** Constant. */
    public static String newChannel;
    
    /** Constant. */
    public static String editChannel;
    
    /** Constant. */
    public static String address;
    
    /** Constant. */
    public static String fileChoosingSectionName;
    
    /** Constant. */
    public static String fileLinkButtonLabel;
    
    /** Constant. */
    public static String fileSectionDescription;
    
    /** Constant. */
    public static String loadTitle;
    
    /** Constant. */
    public static String loadMessage;
    
    /** Constant. */
    public static String selectButton;
    
    /** Constant. */
    public static String outputPaneName;
    
    /** Constant. */
    public static String inputPaneName;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
