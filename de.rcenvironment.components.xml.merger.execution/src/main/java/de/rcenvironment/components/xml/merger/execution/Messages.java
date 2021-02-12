/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.components.xml.merger.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Validation message. */
    public static String noXmlFileLoaded;

    /** Validation message. */
    public static String noXmlFileLoadedLong;

    /** Validation message. */
    public static String unknownMappingType;

    /** Validation message. */
    public static String unknownMappingTypeLong;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
