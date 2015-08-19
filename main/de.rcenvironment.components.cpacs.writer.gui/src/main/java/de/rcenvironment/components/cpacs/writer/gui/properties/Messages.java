/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Markus Kunde
 */
public class Messages extends NLS {

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
    public static String overwrite;

    /** Validation message. */
    public static String localFolderNotConfigured;

    /** Validation message. */
    public static String localFolderNotConfiguredLong;

    /** Validation message. */
    public static String localFolderPathNotAbsolute;

    /** Validation message. */
    public static String localFolderPathNotAbsoluteLong;

    /** Constant. */
    public static String localFolderTitle;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
