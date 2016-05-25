/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.examples.encrypter.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Field for NLS. */
    public static String noInput;

    /** Field for Validation. */
    public static String noAlgorithmSmall;

    /** Field for Validation. */
    public static String noAlgorithmLarge;

    /** Field for configuration section title. */
    public static String algorithmTabTitle;

    /** Field for label. */
    public static String algorithmComboLabel;

    /** Field for label. */
    public static String useDefaultPasswordLabel;

    /** Field for property section. */
    public static String outputs;

    /** Field for property section. */
    public static String inputs;

    static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
