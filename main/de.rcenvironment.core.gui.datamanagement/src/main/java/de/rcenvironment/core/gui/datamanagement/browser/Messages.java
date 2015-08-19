/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.datamanagement.browser;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String waitSignalNodeLabel;

    /** Constant. */
    public static String deleteNodeActionContextMenuLabel;

    /** Constant. */
    public static String deleteFilesActionContextMenuLabel;

    /** Constant. */
    public static String saveNodeActionContextMenuLabel;

    /** Constant. */
    public static String refreshNodeActionContextMenuLabel;

    /** Constant. */
    public static String refreshAllNodesActionContextMenuLabel;

    /** Constant. */
    public static String collapseAllNodesActionContextMenuLabel;

    /** Constant. */
    public static String dataManagementBrowser;

    /** Constant. */
    public static String fetchingData;

    /** Constant. */
    public static String fetchingWorkflows;

    /** Constant. */
    public static String sorting;

    /** Constant. */
    public static String sortUp;

    /** Constant. */
    public static String sortDown;

    /** Constant. */
    public static String sortTimeDesc;

    /** Constant. */
    public static String compareMsg;

    /** Constant. */
    public static String sortTime;

    /** Constant. */
    public static String dialogMessageDelete;

    /** Constant. */
    public static String jobTitleDelete;

    /** Constant. */
    public static String jobTitleDeleteFiles;

    /** Constant. */
    public static String dialogTitleDelete;

    /** Constant. */
    public static String dialogTitleDeleteFiles;

    /** Constant. */
    public static String historyNodeWarningMessage;

    /** Constant. */
    public static String historyNodeWarningTitle;

    /** Constant. */
    public static String autoRefreshActionContextMenuLabel;

    /** Constant. */
    public static String exportErrorText;

    /** Constant. */
    public static String exportSuccessText;

    /** Constant. */
    public static String shortcutDelete;

    /** Constant. */
    public static String shortcutRefreshAll;

    /** Constant. */
    public static String shortcutRefreshSelected;

    /** Constant. */
    public static String dialogMessageDeleteWithNotDeletableNodes;

    /** Constant. */
    public static String toolTipNoHistoryData;

    /** Constant. */
    public static String dialogMessageDeleteFilesWithNotDeletableNodes;

    /** Constant. */
    public static String dialogMessageDeleteFiles;

    /** Constant. */
    public static String runInformationControllerNode;

    /** Constant. */
    public static String runInformationStarttime;

    /** Constant. */
    public static String runInformationEndtime;

    /** Constant. */
    public static String runInformationFinalState;

    /** Constant. */
    public static String runInformationAdditionalInformation;

    /** Constant. */
    public static String runInformationFilesDeleted;

    /** Constant. */
    public static String runInformationTitle;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
