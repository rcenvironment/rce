/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.cpacs.writer.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Validation message. */
    public static String localFolderNotConfigured;

    /** Validation message. */
    public static String localFolderNotConfiguredLong;

    /** Validation message. */
    public static String localFolderPathNotAbsolute;

    /** Validation message. */
    public static String localFolderPathNotAbsoluteLong;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
