/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration.cpacs;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Jan Flink
 */
public class Messages extends NLS {

    /** NLS Constant. */
    public static String cpacsPageDescription;

    /** NLS Constant. */
    public static String cpacsPageTitle;

    /** NLS Constant. */
    public static String alwaysRunCheckbox;

    /** NLS Constant. */
    public static String toolSpecificInputCheckbox;

    /** NLS Constant. */
    public static String labelIncomingCpacsEndpoint;

    /** NLS Constant. */
    public static String labelInputMappingFile;

    /** NLS Constant. */
    public static String fileChooser;

    /** NLS Constant. */
    public static String labelOutputMappingFile;

    /** NLS Constant. */
    public static String labelCpacsResultFilename;

    /** NLS Constant. */
    public static String labelToolSpecificInput;

    /** NLS Constant. */
    public static String labelToolSpecificMapping;

    /** NLS Constant. */
    public static String labelToolInputFilename;

    /** NLS Constant. */
    public static String labelToolOutputFilename;

    /** NLS Constant. */
    public static String fileNotRelativeTitle;

    /** NLS Constant. */
    public static String fileNotRelativeText;

    /** NLS Constant. */
    public static String configurationValueMissing;

    /** NLS Constant. */
    public static String mappingTitle;

    /** NLS Constant. */
    public static String toolSpecMappingTitle;

    /** NLS Constant. */
    public static String executionOptionsTitle;

    /** NLS Constant. */
    public static String labelOutgoingCpacsEndpoint;

    /** NLS Constant. */
    public static String errorNoInput;

    /** NLS Constant. */
    public static String errorNoOutput;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
