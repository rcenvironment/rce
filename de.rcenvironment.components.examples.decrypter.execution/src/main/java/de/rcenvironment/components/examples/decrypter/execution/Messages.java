/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.examples.decrypter.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Field for Validation. */
    public static String noAlgorithmSmall;

    /** Field for Validation. */
    public static String noAlgorithmLarge;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
