/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Message. */
    public static String algorithm;

    /** Message. */
    public static String loadTitle;

    /** Message. */
    public static String dataTabText;

    /** Message. */
    public static String dataTabToolTipText;

    /** Message. */
    public static String chartTabText;

    /** Message. */
    public static String chartTabToolTipText;

    /** Message. */
    public static String configurationTreeDimensionsLabel;

    /** Message. */
    public static String configurationTreeMeasuresLabel;

    /** Message. */
    public static String copyToClipboardLabel;

    /** Message. */
    public static String propertyLabel;

    /** Message. */
    public static String removeTraceActionLabel;

    /** Message. */
    public static String addTraceButtonLabel;

    /** Message. */
    public static String saveData;

    /** Message. */
    public static String targetFunction;

    /** Message. */
    public static String constraints;

    /** Message. */
    public static String designVariables;

    /** Message. */
    public static String algorithmProperties;

    /** Message. */
    public static String excelExport;

    /** Message. */
    public static String newGradientText;

    /** Message. */
    public static String pythonForMethodInstalled;

    /** Message. */
    public static String noDataErrorTitle;

    /** Message. */
    public static String noDataError;

    /** Message. */
    public static String startValueInput;

    /** Message. */
    public static String optimalSolutionOutput;

    /** Message. */
    public static String dakotaOSHint;

    /** Message. */
    public static String restoreDefaultAlgorithmProperties;

    /** Message. */
    public static String noteDiscreteMethods;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
