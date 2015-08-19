/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamanagement.backend.metadata.derby.internal;

import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BIG_VALUE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BINARY_REFERENCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.BINARY_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_INSTANCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_INSTANCE_NAME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPONENT_RUN_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COMPRESSION;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.CONTROLLER_NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.COUNTER;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATAMANAGEMENT_NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATA_REFERENCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.DATA_REFERENCE_KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.ENDPOINT_INSTANCE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.ENDTIME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.FINAL_STATE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.HISTORY_DATA_ITEM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.KEY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.NAME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.NODE_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REFERENCES_DELETED;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTINSTANCE_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_COMPONENTRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_DATAREFERENCE_BINARYREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REL_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.REVISION;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.STARTTIME;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_BINARY_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_COMPONENT_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_DATA_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_INSTANCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TIMELINE_INTERVAL;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_TYPED_DATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_WORKFLOW_RUN_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TIMELINE_DATA_ITEM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TIMELINE_INTERVAL_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TO_BE_DELETED;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TYPE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TYPED_DATUM_ID;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VALUE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_RUNS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_TIMELINE_INTERVALS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_COMPONENTRUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_TYPEDDATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.WORKFLOW_RUN_ID;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;

/**
 * Static strings representing sql statements for setting up database tables and views.
 * 
 * @author Jan Flink
 */
public abstract class DerbyDatabaseSetupSqlStatements {

    private static final String CLOB = " CLOB ";

    private static final String UNIQUE = "UNIQUE";

    private static final String STRING_PLACEHOLDER = "%s";

    private static final String WHERE = " WHERE ";

    private static final String EQUAL = " = ";

    private static final String APO = "'";

    private static final String COMMA = " , ";

    private static final String SELECT = " SELECT ";

    private static final String FROM = " FROM ";

    private static final String CREATE_TABLE = " CREATE TABLE ";

    private static final String PRIMARY_KEY = " PRIMARY KEY ";

    private static final String FOREIGN_KEY = " FOREIGN KEY ";

    private static final String REFERENCES = " REFERENCES ";

    private static final String INTEGER = " INTEGER ";

    private static final String BIGINT = " BIGINT ";

    private static final String CHAR_5 = " CHAR(5) ";

    private static final String CHAR_36 = " CHAR(36) ";

    private static final String VARCHAR_255 = " VARCHAR(255) ";

    private static final String AS_IDENTITY = " GENERATED ALWAYS AS IDENTITY ";

    private static final String TIMESTAMP = " TIMESTAMP ";

    private static final String VARCHAR = " VARCHAR ";

    private static final String NOT_NULL = " NOT NULL ";

    private static final String LONG_VARCHAR = " LONG" + VARCHAR;

    private static final String DOT = ".";

    private static final String INNER_JOIN = " INNER JOIN ";

    private static final String ON = " ON ";

    private static final String CREATE_VIEW = " CREATE VIEW ";

    private static final String AS = " AS ";

    private static final String LEFT_OUTER_JOIN = " LEFT OUTER JOIN ";

    private static final String BOOLEAN = " BOOLEAN ";

    protected static String getSqlRelationWorkflowRunDataReference() {
        String sql = CREATE_TABLE + REL_WORKFLOWRUN_DATAREFERENCE + "("
            + WORKFLOW_RUN_ID + BIGINT + NOT_NULL + COMMA
            + DATA_REFERENCE_ID + BIGINT + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + WORKFLOW_RUN_ID + COMMA + DATA_REFERENCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + WORKFLOW_RUN_ID + ")" + REFERENCES
            + TABLE_WORKFLOW_RUN + "(" + WORKFLOW_RUN_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + DATA_REFERENCE_ID + ")" + REFERENCES
            + TABLE_DATA_REFERENCE + "(" + DATA_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlRelationComponentInstanceDataReference() {
        String sql = CREATE_TABLE + REL_COMPONENTINSTANCE_DATAREFERENCE + "("
            + COMPONENT_INSTANCE_ID + BIGINT + NOT_NULL + COMMA
            + DATA_REFERENCE_ID + BIGINT + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_INSTANCE_ID + COMMA + DATA_REFERENCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_INSTANCE_ID + ")" + REFERENCES
            + TABLE_COMPONENT_INSTANCE + "(" + COMPONENT_INSTANCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + DATA_REFERENCE_ID + ")" + REFERENCES
            + TABLE_DATA_REFERENCE + "(" + DATA_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlRelationDataReferenceBinaryReference() {
        String sql = CREATE_TABLE + REL_DATAREFERENCE_BINARYREFERENCE + "("
            + BINARY_REFERENCE_ID + BIGINT + NOT_NULL + COMMA
            + DATA_REFERENCE_ID + BIGINT + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + BINARY_REFERENCE_ID + COMMA + DATA_REFERENCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + BINARY_REFERENCE_ID + ")" + REFERENCES
            + TABLE_BINARY_REFERENCE + "(" + BINARY_REFERENCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + DATA_REFERENCE_ID + ")" + REFERENCES
            + TABLE_DATA_REFERENCE + "(" + DATA_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlRelationComponentRunDataReference() {
        String sql = CREATE_TABLE + REL_COMPONENTRUN_DATAREFERENCE + "("
            + COMPONENT_RUN_ID + BIGINT + NOT_NULL + COMMA
            + DATA_REFERENCE_ID + BIGINT + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_RUN_ID + COMMA + DATA_REFERENCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_RUN_ID + ")" + REFERENCES
            + TABLE_COMPONENT_RUN + "(" + COMPONENT_RUN_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + DATA_REFERENCE_ID + ")" + REFERENCES
            + TABLE_DATA_REFERENCE + "(" + DATA_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableBinaryReference() {
        String sql = CREATE_TABLE + TABLE_BINARY_REFERENCE + "("
            + BINARY_REFERENCE_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + BINARY_REFERENCE_KEY + CHAR_36 + NOT_NULL + COMMA
            + COMPRESSION + VARCHAR_255 + NOT_NULL + COMMA + REVISION + CHAR_5 + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + BINARY_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableDataReference() {
        String sql = CREATE_TABLE + TABLE_DATA_REFERENCE + "("
            + DATA_REFERENCE_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + DATA_REFERENCE_KEY + CHAR_36 + UNIQUE + COMMA + NODE_ID + CHAR_36 + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + DATA_REFERENCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableEndpointData() {
        String sql = CREATE_TABLE + TABLE_ENDPOINT_DATA + "("
            + COMPONENT_RUN_ID + BIGINT + NOT_NULL + COMMA
            + ENDPOINT_INSTANCE_ID + BIGINT + NOT_NULL + COMMA + TYPED_DATUM_ID + BIGINT + NOT_NULL + COMMA
            + COUNTER + INTEGER + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_RUN_ID + COMMA + TYPED_DATUM_ID + COMMA + ENDPOINT_INSTANCE_ID + COMMA + COUNTER + ")"
            + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_RUN_ID + ")" + REFERENCES
            + TABLE_COMPONENT_RUN + "(" + COMPONENT_RUN_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + ENDPOINT_INSTANCE_ID + ")" + REFERENCES
            + TABLE_ENDPOINT_INSTANCE + "(" + ENDPOINT_INSTANCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + TYPED_DATUM_ID + ")" + REFERENCES
            + TABLE_TYPED_DATUM + "(" + TYPED_DATUM_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableEndpointInstance() {
        String sql = CREATE_TABLE + TABLE_ENDPOINT_INSTANCE + "("
            + ENDPOINT_INSTANCE_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + COMPONENT_INSTANCE_ID + BIGINT + NOT_NULL + COMMA
            + NAME + VARCHAR_255 + NOT_NULL + COMMA + TYPE + VARCHAR_255 + NOT_NULL + COMMA
            + PRIMARY_KEY + "(" + ENDPOINT_INSTANCE_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_INSTANCE_ID + ")" + REFERENCES
            + TABLE_COMPONENT_INSTANCE + "(" + COMPONENT_INSTANCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableTypedDatum() {
        String sql = CREATE_TABLE + TABLE_TYPED_DATUM + "("
            + TYPED_DATUM_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + TYPE + VARCHAR_255 + NOT_NULL + COMMA + VALUE + LONG_VARCHAR + COMMA
            + BIG_VALUE + CLOB + COMMA
            + PRIMARY_KEY + "(" + TYPED_DATUM_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableComponentRunProperties() {
        String sql = CREATE_TABLE + TABLE_COMPONENT_RUN_PROPERTIES + "("
            + COMPONENT_RUN_ID + BIGINT + NOT_NULL + COMMA
            + KEY + VARCHAR_255 + NOT_NULL + COMMA + VALUE + LONG_VARCHAR + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_RUN_ID + COMMA + KEY + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_RUN_ID + ")" + REFERENCES
            + TABLE_COMPONENT_RUN + "(" + COMPONENT_RUN_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableComponentRun() {
        String sql = CREATE_TABLE + TABLE_COMPONENT_RUN + "("
            + COMPONENT_RUN_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + COMPONENT_INSTANCE_ID + BIGINT + NOT_NULL + COMMA + NODE_ID + CHAR_36 + NOT_NULL + COMMA
            + COUNTER + INTEGER + NOT_NULL + COMMA + HISTORY_DATA_ITEM + LONG_VARCHAR + COMMA
            + REFERENCES_DELETED + BOOLEAN + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_RUN_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_INSTANCE_ID + ")" + REFERENCES
            + TABLE_COMPONENT_INSTANCE + "(" + COMPONENT_INSTANCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableComponentInstanceProperties() {
        String sql = CREATE_TABLE + TABLE_COMPONENT_INSTANCE_PROPERTIES + "("
            + COMPONENT_INSTANCE_ID + BIGINT + NOT_NULL + COMMA + KEY + VARCHAR_255 + NOT_NULL + COMMA
            + VALUE + LONG_VARCHAR + COMMA
            + PRIMARY_KEY + "(" + COMPONENT_INSTANCE_ID + COMMA + KEY + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_INSTANCE_ID + ")" + REFERENCES
            + TABLE_COMPONENT_INSTANCE + "(" + COMPONENT_INSTANCE_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableComponentInstance() {
        String sql =
            CREATE_TABLE + TABLE_COMPONENT_INSTANCE + "("
                + COMPONENT_INSTANCE_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
                + WORKFLOW_RUN_ID + BIGINT + NOT_NULL + COMMA + COMPONENT_ID + VARCHAR_255 + NOT_NULL + COMMA
                + COMPONENT_INSTANCE_NAME + VARCHAR_255 + NOT_NULL + COMMA + FINAL_STATE + VARCHAR_255 + COMMA + PRIMARY_KEY + "("
                + COMPONENT_INSTANCE_ID + ")" + COMMA
                + FOREIGN_KEY + "(" + WORKFLOW_RUN_ID + ")" + REFERENCES
                + TABLE_WORKFLOW_RUN + "(" + WORKFLOW_RUN_ID + ")"
                + ")";
        return sql;
    }

    protected static String getSqlTableTimelineInterval() {
        String sql = CREATE_TABLE + TABLE_TIMELINE_INTERVAL + "("
            + TIMELINE_INTERVAL_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + WORKFLOW_RUN_ID + BIGINT + NOT_NULL + COMMA + TYPE + VARCHAR_255 + NOT_NULL + COMMA
            + STARTTIME + TIMESTAMP + NOT_NULL + COMMA + ENDTIME + TIMESTAMP + COMMA
            + COMPONENT_RUN_ID + BIGINT + COMMA
            + PRIMARY_KEY + "(" + TIMELINE_INTERVAL_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + WORKFLOW_RUN_ID + ")" + REFERENCES
            + TABLE_WORKFLOW_RUN + "(" + WORKFLOW_RUN_ID + ")" + COMMA
            + FOREIGN_KEY + "(" + COMPONENT_RUN_ID + ")" + REFERENCES
            + TABLE_COMPONENT_RUN + "(" + COMPONENT_RUN_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableWorkflowRunProperties() {
        String sql = CREATE_TABLE + TABLE_WORKFLOW_RUN_PROPERTIES + "("
            + WORKFLOW_RUN_ID + BIGINT + NOT_NULL + COMMA + KEY + VARCHAR_255 + NOT_NULL + COMMA
            + VALUE + LONG_VARCHAR + COMMA
            + PRIMARY_KEY + "(" + WORKFLOW_RUN_ID + COMMA + KEY + ")" + COMMA
            + FOREIGN_KEY + "(" + WORKFLOW_RUN_ID + ")" + REFERENCES
            + TABLE_WORKFLOW_RUN + "(" + WORKFLOW_RUN_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlTableWorkflowRun() {
        String sql = CREATE_TABLE + TABLE_WORKFLOW_RUN + "("
            + WORKFLOW_RUN_ID + BIGINT + NOT_NULL + AS_IDENTITY + COMMA
            + NAME + VARCHAR_255 + NOT_NULL + COMMA + CONTROLLER_NODE_ID + CHAR_36 + NOT_NULL + COMMA
            + DATAMANAGEMENT_NODE_ID + CHAR_36 + NOT_NULL + COMMA + FINAL_STATE + VARCHAR_255 + COMMA + TIMELINE_DATA_ITEM
            + LONG_VARCHAR + COMMA
            + TO_BE_DELETED + INTEGER + COMMA
            + PRIMARY_KEY + "(" + WORKFLOW_RUN_ID + ")"
            + ")";
        return sql;
    }

    protected static String getSqlViewEndpointData() {
        String sql = CREATE_VIEW + VIEW_ENDPOINT_DATA + AS
            + SELECT + TABLE_ENDPOINT_DATA + DOT + COMPONENT_RUN_ID + COMMA
            + TABLE_ENDPOINT_INSTANCE + DOT + NAME + COMMA
            + TABLE_ENDPOINT_INSTANCE + DOT + TYPE + AS + "ENDPOINT_TYPE" + COMMA
            + TABLE_ENDPOINT_DATA + DOT + COUNTER + COMMA
            + TABLE_TYPED_DATUM + DOT + TYPE + AS + "DATUM_TYPE" + COMMA
            + TABLE_TYPED_DATUM + DOT + VALUE + COMMA
            + TABLE_TYPED_DATUM + DOT + BIG_VALUE
            + FROM + TABLE_ENDPOINT_DATA
            + INNER_JOIN + TABLE_ENDPOINT_INSTANCE + ON + TABLE_ENDPOINT_INSTANCE + DOT + ENDPOINT_INSTANCE_ID + EQUAL
            + TABLE_ENDPOINT_DATA + DOT + ENDPOINT_INSTANCE_ID
            + INNER_JOIN + TABLE_TYPED_DATUM + ON + TABLE_TYPED_DATUM + DOT + TYPED_DATUM_ID + EQUAL + TABLE_ENDPOINT_DATA
            + DOT + TYPED_DATUM_ID;
        return sql;
    }

    protected static String getSqlViewWorkflowRunComponentRun() {
        String sql =
            CREATE_VIEW + VIEW_WORKFLOWRUN_COMPONENTRUN + AS
                + SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA + TABLE_COMPONENT_RUN + DOT + COMPONENT_RUN_ID + FROM
                + TABLE_WORKFLOW_RUN + INNER_JOIN
                + TABLE_COMPONENT_INSTANCE + ON + TABLE_COMPONENT_INSTANCE + DOT + WORKFLOW_RUN_ID + EQUAL
                + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + INNER_JOIN
                + TABLE_COMPONENT_RUN + ON + TABLE_COMPONENT_RUN + DOT + COMPONENT_INSTANCE_ID + EQUAL
                + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID;
        return sql;
    }

    protected static String getSqlViewComponentRuns() {
        String sql =
            String.format(
                CREATE_VIEW + VIEW_COMPONENT_RUNS + AS
                    + SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA
                    + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_ID + COMMA
                    + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_NAME + COMMA
                    + TABLE_COMPONENT_INSTANCE + DOT + FINAL_STATE + COMMA
                    + TABLE_COMPONENT_RUN + DOT + COMPONENT_RUN_ID + COMMA
                    + TABLE_COMPONENT_RUN + DOT + NODE_ID + COMMA
                    + TABLE_COMPONENT_RUN + DOT + COUNTER + COMMA
                    + TABLE_COMPONENT_RUN + DOT + REFERENCES_DELETED + COMMA
                    + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA
                    + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA
                    + TABLE_COMPONENT_RUN + DOT + HISTORY_DATA_ITEM
                    + FROM + TABLE_WORKFLOW_RUN
                    + INNER_JOIN + TABLE_COMPONENT_INSTANCE
                    + ON + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_COMPONENT_INSTANCE + DOT + WORKFLOW_RUN_ID
                    + INNER_JOIN + TABLE_COMPONENT_RUN
                    + ON + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID + EQUAL + TABLE_COMPONENT_RUN + DOT
                    + COMPONENT_INSTANCE_ID
                    + LEFT_OUTER_JOIN + TABLE_TIMELINE_INTERVAL
                    + ON + TABLE_TIMELINE_INTERVAL + DOT + COMPONENT_RUN_ID + EQUAL + TABLE_COMPONENT_RUN + DOT
                    + COMPONENT_RUN_ID
                    + WHERE + TABLE_TIMELINE_INTERVAL + DOT + TYPE + EQUAL + APO + STRING_PLACEHOLDER + APO,
                TimelineIntervalType.COMPONENT_RUN.toString());
        return sql;
    }

    protected static String getSqlViewComponentTimelineIntervals() {
        String sql =
            CREATE_VIEW + VIEW_COMPONENT_TIMELINE_INTERVALS + AS
                + SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + TYPE + COMMA
                + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_ID + COMMA
                + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_NAME
                + FROM + TABLE_WORKFLOW_RUN
                + INNER_JOIN + TABLE_TIMELINE_INTERVAL
                + ON + TABLE_TIMELINE_INTERVAL + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + LEFT_OUTER_JOIN + TABLE_COMPONENT_RUN
                + ON + TABLE_COMPONENT_RUN + DOT + COMPONENT_RUN_ID + EQUAL + TABLE_TIMELINE_INTERVAL + DOT + COMPONENT_RUN_ID
                + INNER_JOIN + TABLE_COMPONENT_INSTANCE
                + ON + TABLE_COMPONENT_RUN + DOT + COMPONENT_INSTANCE_ID + EQUAL + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID;
        return sql;
    }

    protected static String getSqlViewWorkflowRunDataReference() {
        String sql =
            CREATE_VIEW + VIEW_WORKFLOWRUN_DATAREFERENCE + AS
                + SELECT + WORKFLOW_RUN_ID + COMMA + DATA_REFERENCE_ID
                + FROM + VIEW_WORKFLOWRUN_COMPONENTRUN + COMMA + REL_COMPONENTRUN_DATAREFERENCE
                + WHERE + VIEW_WORKFLOWRUN_COMPONENTRUN + DOT + COMPONENT_RUN_ID + EQUAL + REL_COMPONENTRUN_DATAREFERENCE + DOT
                + COMPONENT_RUN_ID;
        return sql;
    }

    protected static String getSqlViewWorkflowRunTypedDatum() {
        String sql = CREATE_VIEW + VIEW_WORKFLOWRUN_TYPEDDATUM + AS
            + SELECT + WORKFLOW_RUN_ID + COMMA + TYPED_DATUM_ID
            + FROM + VIEW_WORKFLOWRUN_COMPONENTRUN + COMMA + TABLE_ENDPOINT_DATA
            + WHERE + VIEW_WORKFLOWRUN_COMPONENTRUN + DOT + COMPONENT_RUN_ID + EQUAL + TABLE_ENDPOINT_DATA + DOT
            + COMPONENT_RUN_ID;
        return sql;
    }
}
