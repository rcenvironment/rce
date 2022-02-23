/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.excel.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
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

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
