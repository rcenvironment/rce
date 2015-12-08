/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

/**
 * Constants for names in meta data database.
 * 
 * @author Jan Flink
 */

public final class MetaDataConstants {

    /**
     * Name of table holding database version information.
     */
    public static final String TABLE_DB_VERSION_INFO = "DB_VERSION_INFO";

    /**
     * Name of table holding database version information.
     */
    public static final String DB_VERSION = "DB_VERSION";

    /**
     * Name of table holding {@link WorkflowRunDescription}s.
     */
    public static final String TABLE_WORKFLOW_RUN = "WORKFLOW_RUN";

    /**
     * Name of column for holding workflow run IDs.
     */
    public static final String WORKFLOW_RUN_ID = "WORKFLOW_RUN_ID";

    /**
     * Name of column for holding names.
     */
    public static final String NAME = "NAME";

    /**
     * Name of column for holding controller node IDs.
     */
    public static final String CONTROLLER_NODE_ID = "CONTROLLER_NODE_ID";

    /**
     * Name of column for holding data management node IDs.
     */
    public static final String DATAMANAGEMENT_NODE_ID = "DATAMANAGEMENT_NODE_ID";

    /**
     * Name of column for holding start times.
     */
    public static final String STARTTIME = "STARTTIME";

    /**
     * Name of column for holding end times.
     */
    public static final String ENDTIME = "ENDTIME";

    /**
     * Name of column for holding final state.
     */
    public static final String FINAL_STATE = "FINAL_STATE";
    
    /**
     * Name of column for holding workflow run IDs.
     */
    public static final String WORKFLOW_FILE_REFERENCE = "WORKFLOW_FILE_REFERENCE";

    /**
     * Name of column for holding timline data items.
     */
    public static final String TIMELINE_DATA_ITEM = "TIMELINE_DATA_ITEM";

    /**
     * Name of table holding key/value properties on workflow runs.
     */
    public static final String TABLE_WORKFLOW_RUN_PROPERTIES = "WORKFLOW_RUN_PROPERTIES";

    /**
     * Name of column for holding keys.
     */
    public static final String KEY = "KEY_";

    /**
     * Name of column for holding values.
     */
    public static final String VALUE = "VALUE";

    /**
     * Name of table holding component instances.
     */
    public static final String TABLE_COMPONENT_INSTANCE = "COMPONENT_INSTANCE";

    /**
     * Name of column for holding component instance IDs.
     */
    public static final String COMPONENT_INSTANCE_ID = "COMPONENT_INSTANCE_ID";

    /**
     * Name of table holding component instance properties.
     */
    public static final String TABLE_COMPONENT_INSTANCE_PROPERTIES = "COMPONENT_INSTANCE_PROPERTIES";

    /**
     * Name of table holding {@link ComponentRun}s.
     */
    public static final String TABLE_COMPONENT_RUN = "COMPONENT_RUN";

    /**
     * Name of column for holding component run IDs.
     */
    public static final String COMPONENT_RUN_ID = "COMPONENT_RUN_ID";

    /**
     * Name of column for holding flag for deleted references.
     */
    public static final String REFERENCES_DELETED = "REFERENCES_DELETED";

    /**
     * Name of column for holding node IDs.
     */
    public static final String NODE_ID = "NODE_ID";

    /**
     * Name of column component IDs.
     */
    public static final String COMPONENT_ID = "COMPONENT_ID";

    /**
     * Name of column component names.
     */
    public static final String COMPONENT_INSTANCE_NAME = "COMPONENT_INSTANCE_NAME";

    /**
     * Name of column component classnames.
     */
    public static final String COMPONENT_CLASS_NAME = "COMPONENT_CLASS_NAME";

    /**
     * Name of column count values.
     */
    public static final String COUNTER = "COUNTER";

    /**
     * Name of column history data items.
     */
    public static final String HISTORY_DATA_ITEM = "HISTORY_DATA_ITEM";

    /**
     * Name of table holding component run properties.
     */
    public static final String TABLE_COMPONENT_RUN_PROPERTIES = "COMPONENT_RUN_PROPERTIES";
    
    /**
     * Name of table holding endpoint instance properties.
     */
    public static final String TABLE_ENDPOINT_INSTANCE_PROPERTIES = "ENDPOINT_INSTANCE_PROPERTIES";

    /**
     * Name of table holding endpoint information.
     */
    public static final String TABLE_ENDPOINT_INSTANCE = "ENDPOINT_INSTANCE";

    /**
     * Name of column for holding endpoint IDs.
     */
    public static final String ENDPOINT_INSTANCE_ID = "ENDPOINT_INSTANCE_ID";

    /**
     * Name of table holding endpoint data information.
     */
    public static final String TABLE_ENDPOINT_DATA = "ENDPOINT_DATA";

    /**
     * Name of column for holding types.
     */
    public static final String TYPE = "TYPE";

    /**
     * Name of table holding typed datums.
     */
    public static final String TABLE_TYPED_DATUM = "TYPED_DATUM";

    /**
     * Name of column for holding typed datum IDs.
     */
    public static final String TYPED_DATUM_ID = "TYPED_DATUM_ID";

    /**
     * Name of column for holding big values as CLOB.
     */
    public static final String BIG_VALUE = "BIG_VALUE";

    /**
     * Name of table holding timeline interval information.
     */
    public static final String TABLE_TIMELINE_INTERVAL = "TIMELINE_INTERVAL";

    /**
     * Name of column timeline interval IDs.
     */
    public static final String TIMELINE_INTERVAL_ID = "TIMELINE_INTERVAL_ID";

    /**
     * Name of table holding {@link DataReference}s.
     */
    public static final String TABLE_DATA_REFERENCE = "DATA_REFERENCE";

    /**
     * Name of column in {@link DataReference} table.
     */
    public static final String DATA_REFERENCE_ID = "DATA_REFERENCE_ID";

    /**
     * Name of column in {@link DataReference} table.
     */
    public static final String DATA_REFERENCE_KEY = "DATA_REFERENCE_KEY";

    /**
     * Name of table holding binary references.
     */
    public static final String TABLE_BINARY_REFERENCE = "BINARY_REFERENCE";

    /**
     * Name of column for holding binary reference IDs.
     */
    public static final String BINARY_REFERENCE_ID = "BINARY_REFERENCE_ID";

    /**
     * Name of column for holding compression types.
     */
    public static final String COMPRESSION = "COMPRESSION";

    /**
     * Name of column for holding revision numbers.
     */
    public static final String REVISION = "REVISION";

    /**
     * Name of column for holding binary ID strings.
     */
    public static final String BINARY_REFERENCE_KEY = "BINARY_REFERENCE_KEY";

    /**
     * Name of column holding information of typed to be deleted.
     */
    public static final String TO_BE_DELETED = "TO_BE_DELETED";

    /**
     * Name of table holding n:n relations between component runs and datareferences.
     */
    public static final String REL_COMPONENTRUN_DATAREFERENCE = "REL_CR_DR";

    /**
     * Name of table holding n:n relations between component instances and datareferences.
     */
    public static final String REL_COMPONENTINSTANCE_DATAREFERENCE = "REL_CI_DR";

    /**
     * Name of table holding n:n relations between workflow runs and datareferences.
     */
    public static final String REL_WORKFLOWRUN_DATAREFERENCE = "REL_WFR_DR";

    /**
     * Name of table holding n:n relations between datareferences and binary referecnes.
     */
    public static final String REL_DATAREFERENCE_BINARYREFERENCE = "REL_DR_BR";

    /**
     * Name of the view holding joined component instances and component runs.
     */
    public static final String VIEW_COMPONENT_RUNS = "V_COMPONENT_RUNS";

    /**
     * Name of the view holding joined endpoint instances with endpoint data and typed datum.
     */
    public static final String VIEW_ENDPOINT_DATA = "V_ENDPOINT_DATA";
    
    /**
     * Name of the view holding endpoint instances.
     */
    public static final String VIEW_ENDPOINT_INSTANCE_PROPERTIES = "V_ENDPOINT_INSTANCE_PROPERTIES";

    /**
     * Name of the view holding joined component runs with timeline intervals.
     */
    public static final String VIEW_COMPONENT_TIMELINE_INTERVALS = "V_COMPONENT_TIMELINE_INTERVALS";

    /**
     * Name of the view holding component run ids with workflow run ids.
     */
    public static final String VIEW_WORKFLOWRUN_COMPONENTRUN = "V_WORKFLOW_COMPONENTRUN";

    /**
     * Name of the view holding joined workflow run ids with data reference ids.
     */
    public static final String VIEW_WORKFLOWRUN_DATAREFERENCE = "V_WORKFLOWRUN_DATAREFERENCE";

    /**
     * Name of the view holding joined data references with binary references.
     */
    public static final String VIEW_WORKFLOWRUN_TYPEDDATUM = "V_WORKFLOWRUN_TYPEDDATUM";

    /**
     * Name of the key handling deletion states properties tables.
     */
    public static final String KEY_FILES_DELETED = "FILES_DELETED";

    /**
     * Value for deletion state of manuelly deleted files.
     */
    public static final String VALUE_FILES_DELETED_MANUALLY = "DELETED_MANUALLY";

    private MetaDataConstants() {

    }
}
