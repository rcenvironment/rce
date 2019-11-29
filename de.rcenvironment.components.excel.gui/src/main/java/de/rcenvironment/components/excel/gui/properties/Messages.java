/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
