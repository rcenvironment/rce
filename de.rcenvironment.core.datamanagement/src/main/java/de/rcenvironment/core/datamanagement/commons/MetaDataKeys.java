/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.commons;

/**
 * Common meta data keys.
 * 
 * @author Robert Mischke
 */
public final class MetaDataKeys {

    /**
     * Managed meta data keys. These keys are set by the data management, and should only be used for read/query operations from client
     * code.
     * 
     * @author Dirk Rossow (original {@link MetaData} class)
     * @author Robert Mischke (conversion)
     */
    public final class Managed {

        /** External keys may not start with this prefix. */
        public static final String PROTECTED_KEY_PREFIX = "de.rcenvironment.rce.datamanagement.";

        /** The user that wrote the data. */
        public static final String AUTHOR = PROTECTED_KEY_PREFIX + "author";

        /** Date the data was written. Format: "yyyy-MM-dd HH:mm:ss". */
        public static final String DATE = PROTECTED_KEY_PREFIX + "date";

        /** Size of data. */
        public static final String SIZE = PROTECTED_KEY_PREFIX + "size";

        /** The user that initial created the {@link DataReference}; revision independent. */
        public static final String CREATOR = PROTECTED_KEY_PREFIX + "creator";

        private Managed() {}
    }

    /** An associated filename; optional. */
    public static final String FILENAME = "rce.common.filename";

    /** UUID of the associated component context; recommended for future cleanup operations. */
    public static final String COMPONENT_CONTEXT_UUID = "rce.common.component_context_uuid";

    /** End-user name of the associated component context; optional. */
    public static final String COMPONENT_CONTEXT_NAME = "rce.common.component_context_name";

    /** UUID of the associated component; optional. */
    public static final String COMPONENT_UUID = "rce.common.component_uuid";

    /** End-user name of the associated component; optional. */
    public static final String COMPONENT_NAME = "rce.common.component_name";

    /** Node identifier the meta data was fetched from; optional. */
    public static final String NODE_IDENTIFIER = "rce.common.node_id";

    /** Database identifier of the component run. */
    public static final String COMPONENT_RUN_ID = "rce.common.component_run_id";
    
    /** Database identifier of the component run. */
    public static final String COMPONENT_INSTANCE_ID = "rce.common.component_instance_id";
    
    /** Database identifier of the workflow run. */
    public static final String WORKFLOW_RUN_ID = "rce.common.workflow_run_id";

    /** Final state of the workflow run. */
    public static final String WORKFLOW_FINAL_STATE = "rce.common.workflow_final_state";

    /** Flag for existence of data references. */
    public static final String WORKFLOW_FILES_DELETED = "rce.common.workflow_files_deleted";

    /** Flag for deletion marker. */
    public static final String WORKFLOW_MARKED_FOR_DELETION = "rce.common.workflow_marked_for_deletion";

    /** Key for data type. */
    public static final String DATA_TYPE = "rce.common.data_type";

    /** Key for data type conversion information. */
    public static final String DATA_TYPE_CONVERSION = "rce.common.data_type_conversion";

    private MetaDataKeys() {}
}
