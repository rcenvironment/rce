/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.gui.view;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String dataTabText;

    /** Constant. */
    public static String dataTabToolTipText;

    /** Constant. */
    public static String chartTabText;

    /** Constant. */
    public static String chartTabToolTipText;

    /** Constant. */
    public static String configurationTreeDimensionsLabel;

    /** Constant. */
    public static String configurationTreeMeasuresLabel;

    /** Constant. */
    public static String copyToClipboardLabel;

    /** Constant. */
    public static String propertyLabel;

    /** Constant. */
    public static String valueLabel;

    /** Constant. */
    public static String trueLabel;

    /** Constant. */
    public static String falseLabel;

    /** Constant. */
    public static String removeTraceActionLabel;

    /** Constant. */
    public static String addTraceButtonLabel;

    /** Constant. */
    public static String addParameter;

    /** Constant. */
    public static String excelExport;

    /** Constant. */
    public static String noDataErrorTitle;

    /** Constant. */
    public static String noDataError;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
