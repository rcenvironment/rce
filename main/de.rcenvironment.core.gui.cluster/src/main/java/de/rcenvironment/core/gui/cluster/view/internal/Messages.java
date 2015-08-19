/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.view.internal;

import org.eclipse.osgi.util.NLS;

/**
 * I18N.
 *
 * @author Doreen Seider
 */
public class Messages extends NLS {
    
    /** Constant. */
    public static String columnJobId;

    /** Constant. */
    public static String columnJobName;

    /** Constant. */
    public static String columnUser;

    /** Constant. */
    public static String columnQueue;

    /** Constant. */
    public static String columnRemainingTime;

    /** Constant. */
    public static String columnStartTime;
    
    /** Constant. */
    public static String columnQueueTime;
    
    /** Constant. */
    public static String columnJobState;
    
    /** Constant. */
    public static String columnWorkflowInformation;

    /** Constant. */
    public static String connectToolTip;

    /** Constant. */
    public static String disconnectToolTip;
    
    /** Constant. */
    public static String refreshToolTip;
    
    /** Constant. */
    public static String informationToolTip;
    
    /** Constant. */
    public static String completedFilter;

    /** Constant. */
    public static String queuedFilter;
    
    /** Constant. */
    public static String runningFilter;

    /** Constant. */
    public static String othersFilter;
    
    /** Constant. */
    public static String searchFilter;
    
    /** Constant. */
    public static String notConnectedSelection;
    
    /** Constant. */
    public static String updateJobTitle;

    /** Constant. */
    public static String updateJobMessage;
    
    /** Constant. */
    public static String informationDialogTitle;

    /** Constant. */
    public static String informationDialogMessage;
    
    /** Constant. */
    public static String ok;
    
    /** Constant. */
    public static String cancelJob;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
