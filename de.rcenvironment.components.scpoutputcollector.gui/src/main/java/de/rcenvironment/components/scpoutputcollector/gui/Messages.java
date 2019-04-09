/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.scpoutputcollector.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Contains the messages of ScpOutputCollector GUI.
 * 
 * @author Brigitte Boden
 * 
 */
public class Messages extends NLS {

    /** title of inputsTab. */
    public static String inputs;

    private static final String BUNDLE_NAME = Messages.class.getPackage()
        .getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

}
