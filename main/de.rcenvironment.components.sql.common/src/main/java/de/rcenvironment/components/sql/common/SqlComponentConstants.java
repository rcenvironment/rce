/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.sql.common;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants shared by GUI and Non-GUI implementations.
 * 
 * @author Markus Kunde
 */
public final class SqlComponentConstants {

    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID_READER = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "sqlreader";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS_READER = new String[] { COMPONENT_ID_READER,
        "de.rcenvironment.rce.components.sql.SQLCommandComponent_SQL Reader" };
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID_WRITER = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "sqlwriter";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS_WRITER = new String[] { COMPONENT_ID_WRITER,
        "de.rcenvironment.rce.components.sql.SQLCommandComponent_SQL Writer" };
    
    /** Identifier of the Joiner component. */
    public static final String COMPONENT_ID_COMMAND = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "sqlcommand";
    
    /** Identifiers of the Joiner component. */
    public static final String[] COMPONENT_IDS_COMMAND = new String[] { COMPONENT_ID_COMMAND,
        "de.rcenvironment.rce.components.sql.SQLCommandComponent_SQL Command" };
    
    /** Name of the reader component as it is defined declaratively in OSGi component. */
    public static final String READER_COMPONENT_NAME = "SQL Reader";
    
    /** Name of the writer component as it is defined declaratively in OSGi component. */
    public static final String WRITER_COMPONENT_NAME = "SQL Writer";

    /** Name of the command component as it is defined declaratively in OSGi component. */
    public static final String COMMAND_COMPONENT_NAME = "SQL Command";

    /** Suffix used for publishing SQL notifications. */
    public static final String NOTIFICATION_SUFFIX = ":rce.component.sql";
    
    /** Property key for deleting temp database table flag. */
    public static final String METADATA_DELETETEMPDBTBL = "deleteTempDbTable";
    
    /** Property key for type of channel. */
    public static final String METADATA_CHANNELTYPE = "channeltype";

    /** Constant. */
    public static final String METADATA_TABLE_NAME_PROPERTY = "tableName";

    /** Constant. */
    public static final String METADATA_SQL_INIT_PROPERTY = "sqlQueryInit";

    /** Constant. */
    public static final String METADATA_DO_SQL_INIT_PROPERTY = "sqlQueryInitDo";

    /** Constant. */
    public static final String METADATA_SQL_PROPERTY = "sqlquery";

    /** Property key for the jdbc profile label. */
    public static final String METADATA_JDBC_PROFILE_PROPERTY = "jdbcProfile";

    /** Property key for the sql setup commands. */
    public static final String METADATA_SQL_SETUP_PROPERTY = "sqlSetup";

    /** Property key for the sql cleanup commands. */
    public static final String METADATA_SQL_CLEANUP_PROPERTY = "sqlCleanup";

    /** Property key for the sql dispose commands. */
    public static final String METADATA_SQL_DISPOSE_PROPERTY = "sqlDispose";

    /** Property key for the input mode. */
    public static final String METADATA_INPUT_MODE = "inputMode";

    /** Property key for the input mapping. */
    public static final String METADATA_INPUT_MAPPING = "inputMapping";

    /** Property key for the create table toggle. */
    public static final String METADATA_CREATE_TABLE = "createTable";

    /** Property key for the drop table toggle. */
    public static final String METADATA_DROP_TABLE = "dropTable";

    /** Property key for the drop table timing. */
    public static final String METADATA_DROP_TABLE_ON = "dropTableOn";
    
    private SqlComponentConstants() {}

}
