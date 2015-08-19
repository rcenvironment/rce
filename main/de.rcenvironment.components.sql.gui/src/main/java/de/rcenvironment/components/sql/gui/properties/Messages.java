/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Markus Kunde
 */
public class Messages extends NLS {

    /** Constant. */
    public static String currentJdbcProfileButMissing;

    /** Constant. */
    public static String readerSectionConnectionSettingTitle;

    /** Constant. */
    public static String readerSectionJDBCProfileLabel;

    /** Constant. */
    public static String readerSectionSQLConfigurationTitle;

    /** Constant. */
    public static String readerSectionTableNameLabel;

    /** Constant. */
    public static String readerSectionTableNameLabelMeta;

    /** Constant. */
    public static String readerSectionInitSectionTitle;

    /** Constant. */
    public static String readerSectionRunSectionTitle;

    /** Constant. */
    public static String readerSectionSQLStatementInitLabel;

    /** Constant. */
    public static String readerSectionSQLStatementInitDoLabel;

    /** Constant. */
    public static String readerSectionSQLStatementLabel;

    /** Constant. */
    public static String readerSectionGenerateButtonLabel;

    /** Constant. */
    public static String writerSectionConnectionSettingTitle;

    /** Constant. */
    public static String writerSectionJDBCProfileLabel;

    /** Constant. */
    public static String writerSectionSQLConfigurationTitle;

    /** Constant. */
    public static String writerSectionTableNameLabel;

    /** Constant. */
    public static String writerSectionInputModeConfigurationTitle;

    /** Constant. */
    public static String writerSectionModeNullItem;

    /** Constant. */
    public static String writerSectionModeLabel;

    /** Constant. */
    public static String writerSectionBlockInputConfigurationTitle;

    /** Constant. */
    public static String writerSectionAddColumnButtonLabel;

    /** Constant. */
    public static String writerSectionRemoveColumnButtonLabel;

    /** Constant. */
    public static String writerSectionColumnTableColumnLabel;

    /** Constant. */
    public static String writerSectionColumnTableNameLabel;

    /** Constant. */
    public static String writerSectionColumnTableTypeLabel;

    /** Constant. */
    public static String readerSectionJDBCProfileNullItem;

    /** Constant. */
    public static String writerSectionJDBCProfileNullItem;

    /** Constant. */
    public static String writerSectionTableCreateCheckBoxText;

    /** Constant. */
    public static String writerSectionTableDropCheckBoxText;

    /** Constant. */
    public static String variablesLabel;

    /** Constant. */
    public static String variablesInsertButtonLabel;

    /** Constant. */
    public static String variablesInputPattern;

    /** Constant. */
    public static String variablesOutputPattern;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
