/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.KEY_FILES_DELETED;
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
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.TABLE_ENDPOINT_INSTANCE_PROPERTIES;
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
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VALUE_FILES_DELETED_MANUALLY;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_RUNS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_COMPONENT_TIMELINE_INTERVALS;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_ENDPOINT_DATA;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_ENDPOINT_INSTANCE_PROPERTIES;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_COMPONENTRUN;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_DATAREFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.VIEW_WORKFLOWRUN_TYPEDDATUM;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.WORKFLOW_FILE_REFERENCE;
import static de.rcenvironment.core.datamanagement.commons.MetaDataConstants.WORKFLOW_RUN_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.datamanagement.commons.BinaryReference;
import de.rcenvironment.core.datamanagement.commons.ComponentInstance;
import de.rcenvironment.core.datamanagement.commons.ComponentRun;
import de.rcenvironment.core.datamanagement.commons.ComponentRunInterval;
import de.rcenvironment.core.datamanagement.commons.DataReference;
import de.rcenvironment.core.datamanagement.commons.EndpointData;
import de.rcenvironment.core.datamanagement.commons.EndpointInstance;
import de.rcenvironment.core.datamanagement.commons.TimelineInterval;
import de.rcenvironment.core.datamanagement.commons.WorkflowRun;
import de.rcenvironment.core.datamanagement.commons.WorkflowRunDescription;
import de.rcenvironment.core.datamodel.api.CompressionFormat;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Static part of the Derby meta data backend implementation.
 * 
 * @author Jan Flink
 */
public class DerbyMetaDataBackendOperationsImpl {

    private static final String IS_NULL = " IS NULL ";

    private static final String SINGE_QOUTE = "'";

    private static final String NOT_EQUAL = " != ";

    private static final int NOT_MARKED_TO_BE_DELETED = 0;

    private static final int WORKFLOW_RUN_TO_BE_DELETED = 1;

    private static final int MAX_VALUE_LENGTH = 32672;

    private static final String BRACKET_STRING_PLACEHOLDER = "(%s)";

    private static final String DB_PREFIX = "APP.";

    private static final String STRING_PLACEHOLDER = "%s";

    private static final String PLACEHOLDER_FOUR_VALUES = "(?,?,?,?)";

    private static final String PLACEHOLDER_THREE_VALUES = "(?,?,?)";

    private static final String PLACEHOLDER_TWO_VALUES = "(?,?)";

    private static final String QMARK = " ? ";

    private static final String WHERE = " WHERE ";

    private static final String EQUAL = " = ";

    private static final String COMMA = " , ";

    private static final String AND = " AND ";

    private static final String SELECT = " SELECT ";

    private static final String FROM = " FROM ";

    private static final String ORDER_BY = " ORDER BY ";

    private static final String DELETE_FROM = " DELETE FROM ";

    private static final String INSERT_INTO = " INSERT INTO ";

    private static final String UPDATE = " UPDATE ";

    private static final String SET = " SET ";

    private static final String VALUES = " VALUES ";

    private static final String DESCENDING = " DESC ";

    private static final String DOT = ".";

    private static final String INNER_JOIN = " INNER JOIN ";

    private static final String ON = " ON ";

    private static final String SELECT_ALL = " SELECT * ";

    private static final String IN = " IN ";


    /**
     * Adds a dataset to the workflow run table.
     * 
     * @param workflowTitle the workflow title
     * @param workflowControllerNodeId the workflow controller node id
     * @param workflowDataManagementNodeId thw workflow data management node id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently added dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addWorkflowRun(String workflowTitle, String workflowControllerNodeId,
        String workflowDataManagementNodeId, Connection connection, Boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_WORKFLOW_RUN + "("
            + NAME + COMMA + CONTROLLER_NODE_ID + COMMA + DATAMANAGEMENT_NODE_ID + COMMA + TO_BE_DELETED
            + ")" + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, workflowTitle);
        stmt.setString(2, workflowControllerNodeId);
        stmt.setString(3, workflowDataManagementNodeId);
        stmt.setInt(4, NOT_MARKED_TO_BE_DELETED);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Updates the workflow run table with the data reference of a workflow file.
     * 
     * @param workflowRunId the id of the dataset to be updated
     * @param wfFileReference the data reference
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addWorkflowFileToWorkflowRun(Long workflowRunId, String wfFileReference, Connection connection,
        boolean isRetry) throws SQLException {
        String sql =
            UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + WORKFLOW_FILE_REFERENCE + EQUAL + QMARK + WHERE + WORKFLOW_RUN_ID
                + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, wfFileReference);
        stmt.setLong(2, workflowRunId);
        stmt.execute();
        stmt.close();
    }

    /**
     * Adds property datasets to the properties table with the given table name.
     * 
     * @param propertiesTableName the name of the property table
     * @param relatedId the id of the related dataset
     * @param properties the properties to add as key value pairs
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addProperties(String propertiesTableName, Long relatedId, Map<String, String> properties, Connection connection,
        boolean isRetry) throws SQLException {
        String idColumnName;
        switch (propertiesTableName) {
        case TABLE_COMPONENT_INSTANCE_PROPERTIES:
            idColumnName = COMPONENT_INSTANCE_ID;
            break;
        case TABLE_ENDPOINT_INSTANCE_PROPERTIES:
            idColumnName = ENDPOINT_INSTANCE_ID;
            break;
        case TABLE_COMPONENT_RUN_PROPERTIES:
            idColumnName = COMPONENT_RUN_ID;
            break;
        default:
            idColumnName = WORKFLOW_RUN_ID;
            break;
        }
        String sql = StringUtils.format(INSERT_INTO + DB_PREFIX + STRING_PLACEHOLDER + "("
            + idColumnName + COMMA + KEY + COMMA + VALUE + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES, propertiesTableName);
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (String key : properties.keySet()) {
            stmt.setLong(1, relatedId);
            stmt.setString(2, key);
            stmt.setString(3, properties.get(key));
            stmt.execute();
        }
        stmt.close();
    }

    /**
     * Adds datasets to the component instance table.
     * 
     * @param workflowRunId the id of the related workflow run dataset
     * @param componentInstances the collection of component instances to add
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the component execution ids mapped to the corresponding database ids of the component instance datasets
     * @throws SQLException thrown on database SQL errors
     */
    public Map<String, Long> addComponentInstances(Long workflowRunId, Collection<ComponentInstance> componentInstances,
        Connection connection, boolean isRetry) throws SQLException {
        Map<String, Long> result = new HashMap<String, Long>();
        String sql = INSERT_INTO + DB_PREFIX + TABLE_COMPONENT_INSTANCE + "("
            + WORKFLOW_RUN_ID + COMMA + COMPONENT_ID + COMMA + COMPONENT_INSTANCE_NAME + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs;
        for (ComponentInstance ci : componentInstances) {
            stmt.setLong(1, workflowRunId);
            stmt.setString(2, ci.getComponentID());
            stmt.setString(3, ci.getComponentInstanceName());
            stmt.execute();
            rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                result.put(ci.getComponentExecutionID(), rs.getLong(1));
                rs.close();
            }
        }
        stmt.close();
        return result;
    }

    private static Long getGeneratedKey(PreparedStatement stmt) throws SQLException {
        ResultSet rs = stmt.getGeneratedKeys();
        Long id = null;
        if (rs != null && rs.next()) {
            id = rs.getLong(1);
            rs.close();
        }
        return id;
    }

    /**
     * Adds a dataset to the timeline interval table.
     * 
     * @param workflowRunId the related workflow run id
     * @param intervalType the interval type
     * @param starttime the startime of the timeline interval
     * @param relatedComponentId the related component run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generate dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addTimelineInterval(Long workflowRunId, TimelineIntervalType intervalType, long starttime,
        Long relatedComponentId, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_TIMELINE_INTERVAL + "("
            + WORKFLOW_RUN_ID + COMMA + TYPE + COMMA + STARTTIME + COMMA + COMPONENT_RUN_ID + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, intervalType.toString());
        stmt.setTimestamp(3, new Timestamp(starttime));
        if (relatedComponentId != null) {
            stmt.setLong(4, relatedComponentId);
        } else {
            stmt.setNull(4, java.sql.Types.BIGINT);
        }
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Adds a dataset to the component run table.
     * 
     * @param componentInstanceId the related component instance id
     * @param nodeId the id the node the component runs on
     * @param count the run counter value
     * @param starttime the starttime of the component run
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addComponentRun(Long componentInstanceId, String nodeId, Integer count, Long starttime, Connection connection,
        boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_COMPONENT_RUN + "("
            + COMPONENT_INSTANCE_ID + COMMA + NODE_ID + COMMA + COUNTER + COMMA + REFERENCES_DELETED + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, componentInstanceId);
        stmt.setString(2, nodeId);
        stmt.setInt(3, count);
        stmt.setBoolean(4, false);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Adds a dataset to the endpoint datum table.
     * 
     * @param componentRunId the related component run id
     * @param typedDatumId the related typed datum id
     * @param endpointInstanceId the related endpoint instance id
     * @param count the variable count
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addEndpointDatum(Long componentRunId, Long typedDatumId, Long endpointInstanceId, Integer count,
        Connection connection,
        boolean isRetry) throws SQLException {
        String endpointSql = INSERT_INTO + DB_PREFIX + TABLE_ENDPOINT_DATA + "("
            + COMPONENT_RUN_ID + COMMA + TYPED_DATUM_ID + COMMA + ENDPOINT_INSTANCE_ID + COMMA + COUNTER + ")"
            + VALUES + PLACEHOLDER_FOUR_VALUES;
        PreparedStatement stmt = connection.prepareStatement(endpointSql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, componentRunId);
        stmt.setLong(2, typedDatumId);
        stmt.setLong(3, endpointInstanceId);
        stmt.setInt(4, count);
        stmt.executeUpdate();
        Long endpointDataId = getGeneratedKey(stmt);
        stmt.close();
        return endpointDataId;
    }

    /**
     * Adds a dataset to the typed datum table.
     * 
     * @param dataType the type of the datum
     * @param value the value of the datum
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addTypedDatum(String dataType, String value, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_TYPED_DATUM + "("
            + TYPE + COMMA + STRING_PLACEHOLDER + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        String valueColumn;
        if (value.length() <= MAX_VALUE_LENGTH) {
            valueColumn = VALUE;
        } else {
            valueColumn = BIG_VALUE;
        }
        PreparedStatement stmt = connection.prepareStatement(StringUtils.format(sql, valueColumn), Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, dataType);
        stmt.setString(2, value);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Adds a set of {@link BinaryReference}s to the binary reference table.
     * 
     * @param binaryReferences the set of binary references to add
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the set of ids of the currently generated datasets
     * @throws SQLException thrown on database SQL errors
     */
    public Set<Long> addBinaryReferences(Set<BinaryReference> binaryReferences, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_BINARY_REFERENCE + "("
            + BINARY_REFERENCE_KEY + COMMA + COMPRESSION + COMMA + REVISION + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        Set<Long> ids = new HashSet<Long>();
        for (BinaryReference br : binaryReferences) {
            stmt.setString(1, br.getBinaryReferenceKey());
            stmt.setString(2, br.getCompression().toString());
            stmt.setString(3, br.getRevision());
            stmt.executeUpdate();
            ids.add(getGeneratedKey(stmt));
            stmt.clearParameters();
        }
        stmt.close();
        return ids;
    }

    /**
     * Adds a dataset to the data reference table.
     * 
     * @param dataReferenceKey the data reference key
     * @param nodeIdentifier the identifier of the node the data reference is stored on
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addDataReference(String dataReferenceKey, String nodeIdentifier, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_DATA_REFERENCE + "("
            + DATA_REFERENCE_KEY + COMMA + NODE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, dataReferenceKey);
        stmt.setString(2, nodeIdentifier);
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Adds a dataset to the binary reference table.
     * 
     * @param binaryReference the binary reference to add
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long addBinaryReference(BinaryReference binaryReference, Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + TABLE_BINARY_REFERENCE + "("
            + BINARY_REFERENCE_KEY + COMMA + COMPRESSION + COMMA + REVISION + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, binaryReference.getBinaryReferenceKey());
        stmt.setString(2, binaryReference.getCompression().toString());
        stmt.setString(3, binaryReference.getRevision());
        stmt.executeUpdate();
        Long id = getGeneratedKey(stmt);
        stmt.close();
        return id;
    }

    /**
     * Returns the {@link DataReference} object with the given {@link DataReference} id including the related {@link BinaryReference}s.
     * 
     * @param dataReferenceKey the data reference key
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the data reference object
     * @throws SQLException thrown on database SQL errors
     */
    public DataReference getDataReference(String dataReferenceKey, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + DATA_REFERENCE_ID + COMMA + NODE_ID + FROM + DB_PREFIX + TABLE_DATA_REFERENCE
            + WHERE + DATA_REFERENCE_KEY + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setString(1, dataReferenceKey);
        ResultSet rs = stmt.executeQuery();
        Long dataRefId = null;
        String dataRefNodeId = null;
        if (rs != null && rs.next()) {
            dataRefId = rs.getLong(DATA_REFERENCE_ID);
            dataRefNodeId = rs.getString(NODE_ID).trim();
            rs.close();
        }
        stmt.close();
        if (dataRefId == null) {
            return null;
        }
        sql = SELECT + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_KEY + COMMA + TABLE_BINARY_REFERENCE + DOT + COMPRESSION + COMMA
            + TABLE_BINARY_REFERENCE + DOT + REVISION
            + FROM + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + INNER_JOIN + DB_PREFIX + TABLE_BINARY_REFERENCE + ON
            + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_ID + EQUAL + REL_DATAREFERENCE_BINARYREFERENCE + DOT + BINARY_REFERENCE_ID
            + WHERE + REL_DATAREFERENCE_BINARYREFERENCE + DOT + DATA_REFERENCE_ID + EQUAL + QMARK;
        stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, dataRefId);
        rs = stmt.executeQuery();
        DataReference dataRef = null;
        Set<BinaryReference> binaryReferences = new HashSet<BinaryReference>();
        if (rs != null) {
            while (rs.next()) {
                binaryReferences.add(new BinaryReference(rs.getString(BINARY_REFERENCE_KEY).trim(), CompressionFormat.valueOf(rs
                    .getString(COMPRESSION)), rs
                    .getString(REVISION)));
            }
            dataRef = new DataReference(dataReferenceKey, NodeIdentifierFactory.fromNodeId(dataRefNodeId), binaryReferences);
            rs.close();
        }
        stmt.close();
        return dataRef;
    }

    /**
     * Returns all {@link BinaryReference} keys of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id to query
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return a map of data reference ids related to a set of binary keys
     * @throws SQLException thrown on database SQL errors
     */
    public Map<Long, Set<String>> getDataReferenceBinaryKeys(Long workflowRunId, Connection connection, boolean isRetry)
        throws SQLException {
        String sqlBinaryRefs =
            SELECT + VIEW_WORKFLOWRUN_DATAREFERENCE + DOT + DATA_REFERENCE_ID + COMMA + TABLE_BINARY_REFERENCE + DOT
                + BINARY_REFERENCE_KEY + FROM + VIEW_WORKFLOWRUN_DATAREFERENCE + COMMA + DB_PREFIX + TABLE_BINARY_REFERENCE
                + COMMA + REL_DATAREFERENCE_BINARYREFERENCE
                + WHERE + TABLE_BINARY_REFERENCE + DOT + BINARY_REFERENCE_ID
                + EQUAL + REL_DATAREFERENCE_BINARYREFERENCE + DOT + BINARY_REFERENCE_ID
                + AND + REL_DATAREFERENCE_BINARYREFERENCE + DOT + DATA_REFERENCE_ID + EQUAL + VIEW_WORKFLOWRUN_DATAREFERENCE + DOT
                + DATA_REFERENCE_ID + AND + VIEW_WORKFLOWRUN_DATAREFERENCE + DOT + WORKFLOW_RUN_ID + EQUAL + QMARK
                + ORDER_BY + DATA_REFERENCE_ID;

        Map<Long, Set<String>> keys = new HashMap<Long, Set<String>>();
        PreparedStatement stmtBinaryRefs;
        stmtBinaryRefs =
            connection.prepareStatement(sqlBinaryRefs, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmtBinaryRefs.setLong(1, workflowRunId);
        ResultSet rs = stmtBinaryRefs.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                if (keys.get(rs.getLong(DATA_REFERENCE_ID)) == null) {
                    keys.put(rs.getLong(DATA_REFERENCE_ID), new HashSet<String>());
                }
                keys.get(rs.getLong(DATA_REFERENCE_ID)).add(rs.getString(BINARY_REFERENCE_KEY).trim());
            }
            rs.close();
        }
        stmtBinaryRefs.close();
        return keys;
    }

    /**
     * Adds a id based relation between a {@link DataReference} and a {@link WorkflowRun} dataset to the corresponding relation table.
     * 
     * @param dataReferenceId the data reference id
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addDataReferenceWorkflowRunRelation(Long dataReferenceId, Long workflowRunId, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_WORKFLOWRUN_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + WORKFLOW_RUN_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Adds a id based relation between a {@link DataReference} and a {@link ComponentInstance} dataset to the corresponding relation table.
     * 
     * @param dataReferenceId the data reference id
     * @param componentInstanceId the component instance id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addDataReferenceComponentInstanceRelation(Long dataReferenceId, Long componentInstanceId, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_COMPONENTINSTANCE_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + COMPONENT_INSTANCE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, componentInstanceId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Adds a id based relation between a {@link DataReference} and a {@link ComponentRun} dataset to the corresponding relation table.
     * 
     * @param dataReferenceId the data reference id
     * @param componentRunId the component run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addDataReferenceComponentRunRelation(Long dataReferenceId, Long componentRunId, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + COMPONENT_RUN_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setLong(1, dataReferenceId);
        stmt.setLong(2, componentRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Adds a id based relation between a {@link DataReference} and a {@link BinaryReference} dataset to the corresponding relation table.
     * 
     * @param dataReferenceId the data reference id
     * @param binaryReferenceIds the binary reference id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void addDataBinaryReferenceRelations(Long dataReferenceId, Set<Long> binaryReferenceIds,
        Connection connection, boolean isRetry) throws SQLException {
        String sql = INSERT_INTO + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + "("
            + DATA_REFERENCE_ID + COMMA + BINARY_REFERENCE_ID + ")"
            + VALUES + PLACEHOLDER_TWO_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (Long id : binaryReferenceIds) {
            stmt.setLong(1, dataReferenceId);
            stmt.setLong(2, id);
            stmt.executeUpdate();
            stmt.clearParameters();
        }
        stmt.close();
    }

    /**
     * Updates a {@link WorkflowRun} dataset with an integer based to be deleted flag. The integer based types are: NOT_MARKED_TO_BE_DELETED
     * = 0; WORKFLOW_RUN_TO_BE_DELETED = 1; FILES_TO_BE_DELETED = 2;
     * 
     * @param workflowRunId the workflow run id to be updated
     * @param type the integer based type
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void markDeletion(Long workflowRunId, Integer type, Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + TO_BE_DELETED + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, type);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates all {@link ComponentRun} datasets related to a given {@link WorkflowRun} dataset with the information that the related
     * {@link DataReference}s are deleted.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void markDataReferencesDeleted(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sqlProperty = INSERT_INTO + DB_PREFIX + TABLE_WORKFLOW_RUN_PROPERTIES + "(" + WORKFLOW_RUN_ID + COMMA
            + KEY + COMMA + VALUE + ")" + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmtWorkflowRunProperty = connection.prepareStatement(sqlProperty);
        stmtWorkflowRunProperty.setLong(1, workflowRunId);
        stmtWorkflowRunProperty.setString(2, KEY_FILES_DELETED);
        stmtWorkflowRunProperty.setString(3, VALUE_FILES_DELETED_MANUALLY);
        try {
            stmtWorkflowRunProperty.executeUpdate();
        } catch (SQLException e) {
            // If found duplicate from database view (Error 23503) ignore and continue, else throw exception
            if (!e.getSQLState().equals("23503")) {
                throw e;
            }
        } finally {
            stmtWorkflowRunProperty.close();
        }

        String sqlComponentRunIds =
            SELECT + COMPONENT_RUN_ID + FROM + VIEW_WORKFLOWRUN_COMPONENTRUN + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtComponentRunIds =
            connection.prepareStatement(sqlComponentRunIds, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtComponentRunIds.setLong(1, workflowRunId);
        ResultSet rs = stmtComponentRunIds.executeQuery();
        List<Long> crIds = new ArrayList<Long>();
        if (rs != null) {
            while (rs.next()) {
                crIds.add(rs.getLong(COMPONENT_RUN_ID));
            }
            rs.close();
        }
        stmtComponentRunIds.close();
        if (crIds.size() > 0) {
            String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_RUN + SET + REFERENCES_DELETED + EQUAL + QMARK
                + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK;
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (Long id : crIds) {
                stmt.setBoolean(1, true);
                stmt.setLong(2, id);
                stmt.executeUpdate();
            }
            stmt.close();
        }
    }

    /**
     * Returns true if the {@link WorkflowRun} dataset with the given id has a final state.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return true if the worklow run final state is not null
     * @throws SQLException thrown on database SQL errors
     */
    public boolean isWorkflowFinished(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + FINAL_STATE + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            Boolean finished = rs.getString(FINAL_STATE) != null;
            rs.close();
            stmt.close();
            return finished;
        }
        stmt.close();
        return false;
    }

    /**
     * Deletes all {@link TypedDatum} datasets that are related to the {@link WorkflowRun} dataset with the given id. Includes deletion of
     * corresponding {@link EndpointData} datasets as well.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void deleteTypedDatums(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + TYPED_DATUM_ID + FROM + DB_PREFIX + VIEW_WORKFLOWRUN_TYPEDDATUM
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        Set<Long> typedDatumIds = new HashSet<Long>();
        if (rs != null) {
            while (rs.next()) {
                typedDatumIds.add(rs.getLong(TYPED_DATUM_ID));
            }
            rs.close();
        }
        stmt.close();
        String sqlEndpointData = DELETE_FROM + DB_PREFIX + TABLE_ENDPOINT_DATA + WHERE + TYPED_DATUM_ID + EQUAL + QMARK;
        String sqlTypedDatum = DELETE_FROM + DB_PREFIX + TABLE_TYPED_DATUM + WHERE + TYPED_DATUM_ID + EQUAL + QMARK;

        PreparedStatement stmtEndpointData =
            connection.prepareStatement(sqlEndpointData, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtTypedDatum =
            connection.prepareStatement(sqlTypedDatum, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        for (Long id : typedDatumIds) {
            stmtEndpointData.setLong(1, id);
            stmtTypedDatum.setLong(1, id);
            stmtEndpointData.execute();
            stmtTypedDatum.execute();
        }
        stmtEndpointData.close();
        stmtTypedDatum.close();
    }

    /**
     * Deletes all corresponding datasets and the {@link DataReference} dataset itself of a given set of data reference keys. Corresponding
     * datasets are relations to {@link WorkflowRun}s, {@link ComponentInstance}s, {@link ComponentRun}s and {@link BinaryReference}s as
     * well as the {@link BinaryReference} dataset.
     * 
     * @param dataReferenceKeys the set of data reference keys
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return true on success
     * @throws SQLException thrown on database SQL errors
     */
    public Boolean deleteDataReferences(Map<Long, Set<String>> dataReferenceKeys,
        Connection connection, boolean isRetry)
        throws SQLException {
        if (dataReferenceKeys.isEmpty()) {
            return true;
        }
        String sqlRelBinaryDataRef =
            DELETE_FROM + DB_PREFIX + REL_DATAREFERENCE_BINARYREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelCompRunDataRef =
            DELETE_FROM + DB_PREFIX + REL_COMPONENTRUN_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelCompInstanceDataRef =
            DELETE_FROM + DB_PREFIX + REL_COMPONENTINSTANCE_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlRelWorkflowRunDataRef =
            DELETE_FROM + DB_PREFIX + REL_WORKFLOWRUN_DATAREFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        String sqlBinaryRef =
            DELETE_FROM + DB_PREFIX + TABLE_BINARY_REFERENCE + WHERE + BINARY_REFERENCE_KEY + EQUAL + QMARK;
        String sqlDataRef = DELETE_FROM + DB_PREFIX + TABLE_DATA_REFERENCE + WHERE + DATA_REFERENCE_ID + EQUAL + QMARK;
        PreparedStatement stmtRelBinaryDataRef =
            connection.prepareStatement(sqlRelBinaryDataRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtRelCompRunDataRef =
            connection.prepareStatement(sqlRelCompRunDataRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtRelCompInstanceDataRef =
            connection.prepareStatement(sqlRelCompInstanceDataRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtRelWorkflowRunDataRef =
            connection.prepareStatement(sqlRelWorkflowRunDataRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtBinaryRef =
            connection.prepareStatement(sqlBinaryRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        PreparedStatement stmtDataRef = connection.prepareStatement(sqlDataRef, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        for (Long id : dataReferenceKeys.keySet()) {
            stmtRelBinaryDataRef.setLong(1, id);
            stmtRelBinaryDataRef.execute();
            stmtRelCompRunDataRef.setLong(1, id);
            stmtRelCompRunDataRef.execute();
            stmtRelCompInstanceDataRef.setLong(1, id);
            stmtRelCompInstanceDataRef.execute();
            stmtRelWorkflowRunDataRef.setLong(1, id);
            stmtRelWorkflowRunDataRef.execute();
            for (String key : dataReferenceKeys.get(id)) {
                stmtBinaryRef.setString(1, key);
                stmtBinaryRef.execute();
            }
            stmtDataRef.setLong(1, id);
            stmtDataRef.execute();
        }
        stmtRelBinaryDataRef.close();
        stmtRelCompRunDataRef.close();
        stmtRelCompInstanceDataRef.close();
        stmtRelWorkflowRunDataRef.close();
        stmtBinaryRef.close();
        stmtDataRef.close();
        return true;
    }

    /**
     * Deletes a {@link WorkflowRun} dataset and all related datasets in the database.
     * 
     * 
     * @param workflowRunId the workflow run id to be deleted
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return true on success
     * @throws SQLException thrown on database SQL errors
     */
    public Boolean deleteWorkflowRunContent(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {

        String sqlTimelineInt = DELETE_FROM + DB_PREFIX + TABLE_TIMELINE_INTERVAL + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtTimelineInt =
            connection.prepareStatement(sqlTimelineInt, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtTimelineInt.setLong(1, workflowRunId);
        stmtTimelineInt.execute();
        stmtTimelineInt.close();
        String sqlCompInstIds =
            SELECT + COMPONENT_INSTANCE_ID + FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        String sqlCompRunIds =
            SELECT + TABLE_COMPONENT_RUN + DOT + COMPONENT_RUN_ID + FROM + DB_PREFIX + TABLE_COMPONENT_RUN + COMMA + DB_PREFIX
                + TABLE_COMPONENT_INSTANCE + WHERE + TABLE_COMPONENT_RUN + DOT + COMPONENT_INSTANCE_ID + EQUAL
                + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID + AND + TABLE_COMPONENT_INSTANCE + DOT + WORKFLOW_RUN_ID
                + EQUAL + QMARK;

        String sqlcompRunProp =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_RUN_PROPERTIES + WHERE + COMPONENT_RUN_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompRunProp =
            connection.prepareStatement(StringUtils.format(sqlcompRunProp, StringUtils.format(sqlCompRunIds, sqlCompInstIds)),
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtCompRunProp.setLong(1, workflowRunId);
        stmtCompRunProp.execute();
        stmtCompRunProp.close();
        String sqlcompInstProp =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE_PROPERTIES + WHERE + COMPONENT_INSTANCE_ID + IN
                + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompInstProp =
            connection.prepareStatement(StringUtils.format(sqlcompInstProp, sqlCompInstIds), ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmtCompInstProp.setLong(1, workflowRunId);
        stmtCompInstProp.execute();
        stmtCompInstProp.close();
        String sqlEndpointInstProp =
            DELETE_FROM + DB_PREFIX + TABLE_ENDPOINT_INSTANCE_PROPERTIES + WHERE + ENDPOINT_INSTANCE_ID + IN
                + BRACKET_STRING_PLACEHOLDER;
        String sqlEndInstIds = StringUtils.format(SELECT + ENDPOINT_INSTANCE_ID + FROM + TABLE_ENDPOINT_INSTANCE
            + WHERE + COMPONENT_INSTANCE_ID + IN + BRACKET_STRING_PLACEHOLDER, sqlCompInstIds);
        PreparedStatement stmtEndpointInstProp =
            connection.prepareStatement(StringUtils.format(sqlEndpointInstProp, sqlEndInstIds), ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmtEndpointInstProp.setLong(1, workflowRunId);
        stmtEndpointInstProp.execute();
        stmtEndpointInstProp.close();
        String sqlEndpointInst =
            DELETE_FROM + DB_PREFIX + TABLE_ENDPOINT_INSTANCE + WHERE + COMPONENT_INSTANCE_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtEndpointInst =
            connection.prepareStatement(StringUtils.format(sqlEndpointInst, sqlCompInstIds), ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmtEndpointInst.setLong(1, workflowRunId);
        stmtEndpointInst.execute();
        stmtEndpointInst.close();
        String sqlCompRun =
            DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_RUN + WHERE + COMPONENT_INSTANCE_ID + IN + BRACKET_STRING_PLACEHOLDER;
        PreparedStatement stmtCompRun =
            connection.prepareStatement(StringUtils.format(sqlCompRun, sqlCompInstIds), ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmtCompRun.setLong(1, workflowRunId);
        stmtCompRun.execute();
        stmtCompRun.close();
        String sqlCompInst = DELETE_FROM + DB_PREFIX + TABLE_COMPONENT_INSTANCE + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtCompInst = connection.prepareStatement(sqlCompInst, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtCompInst.setLong(1, workflowRunId);
        stmtCompInst.execute();
        stmtCompInst.close();
        String sqlWorkflowRunProp =
            DELETE_FROM + DB_PREFIX + TABLE_WORKFLOW_RUN_PROPERTIES + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtWorkflowRunProp =
            connection.prepareStatement(sqlWorkflowRunProp, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtWorkflowRunProp.setLong(1, workflowRunId);
        stmtWorkflowRunProp.execute();
        stmtWorkflowRunProp.close();
        String sqlWorkflowRun = DELETE_FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtWorkflowRun =
            connection.prepareStatement(sqlWorkflowRun, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtWorkflowRun.setLong(1, workflowRunId);
        int affectedLines = stmtWorkflowRun.executeUpdate();
        stmtWorkflowRun.close();
        return affectedLines == 1;
    }

    /**
     * Adds a {@link DataReference} object to the database. Therefore a {@link DataReference} dataset and the {@link BinaryReference}s
     * datasets are generated and related.
     * 
     * @param dataReference the data reference object
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the currently generated data set
     * @throws SQLException thrown on database SQL errors
     */
    public Long addDataReference(final DataReference dataReference, final Connection connection, final boolean isRetry)
        throws SQLException {
        Long dataReferenceId =
            addDataReference(dataReference.getDataReferenceKey(),
                dataReference.getNodeIdentifier().getIdString(),
                connection, isRetry);
        Set<Long> binaryReferenceIds =
            addBinaryReferences(dataReference.getBinaryReferences(), connection, isRetry);
        addDataBinaryReferenceRelations(dataReferenceId, binaryReferenceIds, connection, isRetry);
        return dataReferenceId;
    }

    /**
     * Returns a properties map of property datasets of the table with the given name.
     * 
     * @param tableName the table name
     * @param relatedId the id the properties are related to
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the map of properties
     * @throws SQLException thrown on database SQL errors
     */
    public Map<String, String> getProperties(String tableName, Long relatedId, Connection connection, boolean isRetry)
        throws SQLException {
        String relatedIdColumn;
        switch (tableName) {
        case TABLE_COMPONENT_RUN_PROPERTIES:
            relatedIdColumn = COMPONENT_RUN_ID;
            break;
        case TABLE_WORKFLOW_RUN_PROPERTIES:
            relatedIdColumn = WORKFLOW_RUN_ID;
            break;
        case TABLE_COMPONENT_INSTANCE_PROPERTIES:
            relatedIdColumn = COMPONENT_INSTANCE_ID;
            break;
        case TABLE_ENDPOINT_INSTANCE_PROPERTIES:
            relatedIdColumn = ENDPOINT_INSTANCE_ID;
            break;
        default:
            relatedIdColumn = null;
        }
        String sql = StringUtils.format(SELECT + KEY + COMMA + VALUE
            + FROM + DB_PREFIX + STRING_PLACEHOLDER + WHERE + STRING_PLACEHOLDER + EQUAL + QMARK, tableName, relatedIdColumn);
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, relatedId);
        Map<String, String> results = new HashMap<String, String>();
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                results.put(rs.getString(KEY), rs.getString(VALUE));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    /**
     * Returns a list of {@link ComponentRunInterval}s of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the list of timeline intervals
     * @throws SQLException thrown on database SQL errors
     */
    public List<ComponentRunInterval> getComponentRunIntervals(Long workflowRunId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql =
            SELECT + STARTTIME + COMMA + ENDTIME + COMMA + TYPE + COMMA + COMPONENT_ID + COMMA + COMPONENT_INSTANCE_NAME
                + FROM + DB_PREFIX + VIEW_COMPONENT_TIMELINE_INTERVALS
                + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        List<ComponentRunInterval> results = new ArrayList<ComponentRunInterval>();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                results.add(new ComponentRunInterval(rs.getString(COMPONENT_ID), rs.getString(COMPONENT_INSTANCE_NAME),
                    TimelineIntervalType.valueOf(rs.getString(TYPE)), rs.getTimestamp(STARTTIME).getTime(), endtime));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    /**
     * Returns the TimeInterval of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the timeline interval
     * @throws SQLException thrown on database SQL errors
     */
    public TimelineInterval getWorkflowInterval(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + STARTTIME + COMMA + ENDTIME + FROM + DB_PREFIX + TABLE_TIMELINE_INTERVAL
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK
            + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, TimelineIntervalType.WORKFLOW_RUN.toString());
        ResultSet rs = stmt.executeQuery();
        TimelineInterval ti = null;
        if (rs != null && rs.next()) {
            // end time might be NULL, avoid NPE
            Long endtime = null;
            if (rs.getTimestamp(ENDTIME) != null) {
                endtime = rs.getTimestamp(ENDTIME).getTime();
            }
            ti = new TimelineInterval(TimelineIntervalType.WORKFLOW_RUN, rs.getTimestamp(STARTTIME).getTime(), endtime);
        }
        if (rs != null) {
            rs.close();
        }
        stmt.close();
        return ti;
    }

    /**
     * Returns the name of the {@link WorkflowRun} with the given id.
     * 
     * @param workflowRunId the workflow run id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the name of the worklfow run
     * @throws SQLException thrown on database SQL errors
     */
    public String getWorkflowRunName(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + NAME + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;

        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            String name = rs.getString(NAME);
            rs.close();
            stmt.close();
            return name;
        }
        stmt.close();
        return null;
    }

    /**
     * Updates the endtime of the {@link TimelineInterval} dataset with the given id.
     * 
     * @param timelineIntervalId the timeline interval id
     * @param endtime the endtime
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setTimelineIntervalFinished(Long timelineIntervalId, long endtime, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET
            + ENDTIME + EQUAL + QMARK
            + WHERE + TIMELINE_INTERVAL_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, timelineIntervalId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Adds a collection of {@link EndpointInstance} datasets to the database and sets the relation to {@link ComponentInstance} data set
     * with the given id.
     * 
     * @param componentInstanceId the component instance id
     * @param endpointInstances the collection of endpoint instance objects
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return a map endpoint names related to the ids of the generated datasets
     * @throws SQLException thrown on database SQL errors
     */
    public Map<String, Long> addEndpointInstances(Long componentInstanceId, Collection<EndpointInstance> endpointInstances,
        Connection connection,
        boolean isRetry) throws SQLException {
        Map<String, Long> result = new HashMap<String, Long>();
        String sql = INSERT_INTO + DB_PREFIX + TABLE_ENDPOINT_INSTANCE + "("
            + COMPONENT_INSTANCE_ID + COMMA + NAME + COMMA + TYPE + ")"
            + VALUES + PLACEHOLDER_THREE_VALUES;
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ResultSet rs;
        for (EndpointInstance ei : endpointInstances) {
            stmt.setLong(1, componentInstanceId);
            stmt.setString(2, ei.getEndpointName());
            stmt.setString(3, ei.getEndpointType().name());
            stmt.execute();
            rs = stmt.getGeneratedKeys();
            if (rs != null && rs.next()) {
                result.put(ei.getEndpointName(), rs.getLong(1));
            }
            if (rs != null) {
                rs.close();
            }
        }
        stmt.close();
        return result;
    }

    /**
     * Returns a collection of {@link ComponentRun} objects that are related to the {@link ComponentInstance} with the given id.
     * 
     * @param componentInstanceId the component instance id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the collection of {@link ComponentRun} objects
     * @throws SQLException thrown on database SQL errors
     */
    public Collection<ComponentRun> getComponentRuns(Long componentInstanceId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = SELECT + COMPONENT_RUN_ID + COMMA + COMPONENT_INSTANCE_ID + COMMA + NODE_ID + COMMA
            + COUNTER + COMMA + STARTTIME + COMMA + ENDTIME + COMMA + HISTORY_DATA_ITEM + COMMA + REFERENCES_DELETED
            + FROM + DB_PREFIX + TABLE_COMPONENT_RUN + WHERE + COMPONENT_INSTANCE_ID + EQUAL + QMARK + ORDER_BY + STARTTIME + DESCENDING;
        Collection<ComponentRun> results = new TreeSet<ComponentRun>();
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, componentInstanceId);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                results.add(new ComponentRun(rs.getLong(COMPONENT_RUN_ID), rs.getLong(COMPONENT_INSTANCE_ID), rs.getString(NODE_ID).trim(),
                    rs.getInt(COUNTER), rs.getTimestamp(STARTTIME).getTime(), endtime, rs.getString(HISTORY_DATA_ITEM),
                    rs.getBoolean(REFERENCES_DELETED), getProperties(TABLE_COMPONENT_RUN_PROPERTIES, rs.getLong(COMPONENT_RUN_ID),
                        connection, isRetry)));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    /**
     * Returns the {@link WorkflowRun} object of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id.
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the workflow run object
     * @throws SQLException thrown on database SQL errors
     */
    public WorkflowRun getWorkflowRun(Long workflowRunId, Connection connection, boolean isRetry) throws SQLException {
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + NAME + COMMA
                + TABLE_WORKFLOW_RUN + DOT + CONTROLLER_NODE_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + DATAMANAGEMENT_NODE_ID + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA + TABLE_WORKFLOW_RUN
                + DOT + FINAL_STATE + COMMA + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_FILE_REFERENCE + FROM + DB_PREFIX
                + TABLE_WORKFLOW_RUN + INNER_JOIN + DB_PREFIX + TABLE_TIMELINE_INTERVAL + ON
                + TABLE_TIMELINE_INTERVAL + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + EQUAL + QMARK + AND + TABLE_TIMELINE_INTERVAL + DOT + TYPE + EQUAL
                + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, workflowRunId);
        stmt.setString(2, TimelineIntervalType.WORKFLOW_RUN.toString());
        WorkflowRun workflowRun = null;
        ResultSet rs = stmt.executeQuery();
        if (rs != null && rs.next()) {
            // end time might be NULL, avoid NPE
            Long endtime = null;
            if (rs.getTimestamp(ENDTIME) != null) {
                endtime = rs.getTimestamp(ENDTIME).getTime();
            }
            // final state might be NULL, avoid NPE
            FinalWorkflowState finalState = null;
            if (rs.getString(FINAL_STATE) != null) {
                finalState = FinalWorkflowState.valueOf(rs.getString(FINAL_STATE));
            }
            Long wfRunId = rs.getLong(WORKFLOW_RUN_ID);
            String wfFileReference = rs.getString(WORKFLOW_FILE_REFERENCE);
            workflowRun =
                new WorkflowRun(wfRunId, rs.getString(NAME), rs.getString(CONTROLLER_NODE_ID).trim(),
                    rs.getString(DATAMANAGEMENT_NODE_ID).trim(), rs.getTimestamp(STARTTIME).getTime(),
                    endtime, finalState, null, null, getProperties(TABLE_WORKFLOW_RUN_PROPERTIES, workflowRunId, connection, isRetry),
                    wfFileReference);
            rs.close();
        }
        stmt.close();
        if (workflowRun == null) {
            return null;
        }
        Map<Long, Map<String, String>> endpointProperties = new HashMap<Long, Map<String, String>>();
        String sqlEndpointProperties =
            SELECT_ALL + FROM + DB_PREFIX + VIEW_ENDPOINT_INSTANCE_PROPERTIES + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtEndpointProperties =
            connection.prepareStatement(sqlEndpointProperties, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtEndpointProperties.setLong(1, workflowRunId);
        ResultSet rsEndpointProperties = stmtEndpointProperties.executeQuery();
        if (rsEndpointProperties != null) {
            while (rsEndpointProperties.next()) {
                Long endpointInstanceId = rsEndpointProperties.getLong(ENDPOINT_INSTANCE_ID);
                String key = rsEndpointProperties.getString(KEY);
                String value = rsEndpointProperties.getString(VALUE);
                if (endpointProperties.get(endpointInstanceId) != null) {
                    endpointProperties.get(endpointInstanceId).put(key, value);
                } else {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(key, value);
                    endpointProperties.put(endpointInstanceId, map);
                }
            }
        }

        Map<Long, Set<EndpointData>> endpointData = new HashMap<Long, Set<EndpointData>>();
        String sqlEndpointData =
            SELECT_ALL + FROM + DB_PREFIX + VIEW_ENDPOINT_DATA
                + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtEndpointData =
            connection.prepareStatement(sqlEndpointData, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmtEndpointData.setLong(1, workflowRunId);
        ResultSet rsEndpointData = stmtEndpointData.executeQuery();
        if (rsEndpointData != null) {
            while (rsEndpointData.next()) {
                String value = rsEndpointData.getString(VALUE);
                if (value == null) {
                    value = rsEndpointData.getString(BIG_VALUE);
                }
                Long id = rsEndpointData.getLong(COMPONENT_RUN_ID);
                if (endpointData.get(id) == null) {
                    endpointData.put(id, new HashSet<EndpointData>());
                }
                Long endpointInstanceId = rsEndpointData.getLong(ENDPOINT_INSTANCE_ID);
                endpointData.get(id).add(
                    new EndpointData(new EndpointInstance(rsEndpointData.getString(NAME), EndpointType
                        .valueOf(rsEndpointData
                            .getString("ENDPOINT_TYPE")), endpointProperties.get(endpointInstanceId)),
                        rsEndpointData.getInt(COUNTER), value));
            }
            rsEndpointData.close();
        }
        stmtEndpointData.close();
        String sqlComponentRuns = SELECT_ALL + FROM + DB_PREFIX + VIEW_COMPONENT_RUNS + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmtComponentRuns =
            connection.prepareStatement(sqlComponentRuns, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        stmtComponentRuns.setLong(1, workflowRunId);
        ResultSet rsComponentRuns = stmtComponentRuns.executeQuery();
        if (rsComponentRuns != null) {
            while (rsComponentRuns.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rsComponentRuns.getTimestamp(ENDTIME) != null) {
                    endtime = rsComponentRuns.getTimestamp(ENDTIME).getTime();
                }
                ComponentInstance ci =
                    new ComponentInstance(rsComponentRuns.getString(COMPONENT_ID), rsComponentRuns.getString(COMPONENT_INSTANCE_NAME),
                        rsComponentRuns.getString(FINAL_STATE));
                Long crId = rsComponentRuns.getLong(COMPONENT_RUN_ID);
                ComponentRun cr =
                    new ComponentRun(rsComponentRuns.getLong(COMPONENT_RUN_ID), rsComponentRuns.getString(NODE_ID).trim(),
                        rsComponentRuns.getInt(COUNTER), rsComponentRuns.getTimestamp(
                            STARTTIME).getTime(), endtime,
                        rsComponentRuns.getString(HISTORY_DATA_ITEM), rsComponentRuns.getBoolean(REFERENCES_DELETED),
                        getProperties(TABLE_COMPONENT_RUN_PROPERTIES, crId, connection, isRetry));

                cr.setEndpointData(endpointData.get(crId));
                workflowRun.addComponentRun(ci, cr);
            }
            rsComponentRuns.close();
        }
        stmtComponentRuns.close();
        return workflowRun;
    }

    /**
     * Returns a set of {@link WorkflowRunDescription}s of all {@link WorkflowRun} datasets in the database.
     * 
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the set of workflow run descriptions
     * @throws SQLException thrown on database SQL errors
     */
    public Set<WorkflowRunDescription> getWorkflowRunDescriptions(Connection connection, boolean isRetry) throws SQLException {
        Map<Long, String> deletionStates = getDeletionStates(connection, isRetry);
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + NAME + COMMA
                + TABLE_WORKFLOW_RUN + DOT + CONTROLLER_NODE_ID + COMMA + TABLE_WORKFLOW_RUN + DOT + DATAMANAGEMENT_NODE_ID + COMMA
                + TABLE_WORKFLOW_RUN + DOT + TO_BE_DELETED + COMMA
                + TABLE_TIMELINE_INTERVAL + DOT + STARTTIME + COMMA + TABLE_TIMELINE_INTERVAL + DOT + ENDTIME + COMMA + TABLE_WORKFLOW_RUN
                + DOT + FINAL_STATE + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + INNER_JOIN + DB_PREFIX + TABLE_TIMELINE_INTERVAL + ON
                + TABLE_TIMELINE_INTERVAL + DOT + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_TIMELINE_INTERVAL + DOT + TYPE + EQUAL + QMARK + AND + TO_BE_DELETED + NOT_EQUAL + QMARK
                + ORDER_BY + STARTTIME + DESCENDING;
        Set<WorkflowRunDescription> results = new HashSet<WorkflowRunDescription>();
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setString(1, TimelineIntervalType.WORKFLOW_RUN.toString());
        stmt.setInt(2, WORKFLOW_RUN_TO_BE_DELETED);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                // end time might be NULL, avoid NPE
                Long endtime = null;
                if (rs.getTimestamp(ENDTIME) != null) {
                    endtime = rs.getTimestamp(ENDTIME).getTime();
                }
                FinalWorkflowState finalState = null;
                if (rs.getString(FINAL_STATE) != null) {
                    finalState = FinalWorkflowState.valueOf(rs.getString(FINAL_STATE));
                }
                Long wfRunId = rs.getLong(WORKFLOW_RUN_ID);
                boolean markedForDeletion = rs.getInt(TO_BE_DELETED) != NOT_MARKED_TO_BE_DELETED;
                Boolean areFilesDeleted =
                    deletionStates.get(wfRunId) != null && deletionStates.get(wfRunId).equals(VALUE_FILES_DELETED_MANUALLY);
                results.add(new WorkflowRunDescription(wfRunId, rs.getString(NAME), rs.getString(CONTROLLER_NODE_ID)
                    .trim(), rs.getString(DATAMANAGEMENT_NODE_ID).trim(), rs.getTimestamp(STARTTIME).getTime(), endtime, finalState,
                    areFilesDeleted, markedForDeletion, getProperties(TABLE_WORKFLOW_RUN_PROPERTIES, wfRunId, connection, isRetry)));
            }
            rs.close();
        }
        stmt.close();
        return results;
    }

    private static Map<Long, String> getDeletionStates(Connection connection, boolean isRetry) throws SQLException {
        String sql =
            SELECT + WORKFLOW_RUN_ID + COMMA + VALUE + FROM + TABLE_WORKFLOW_RUN_PROPERTIES
                + WHERE + KEY + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, KEY_FILES_DELETED);
        ResultSet rs = stmt.executeQuery();
        Map<Long, String> counts = new HashMap<Long, String>();
        if (rs != null) {
            while (rs.next()) {
                counts.put(rs.getLong(WORKFLOW_RUN_ID), rs.getString(VALUE));
            }
            rs.close();
        }
        stmt.close();
        return counts;
    }

    /**
     * Updates the final state of a {@link ComponentInstance} dataset with the given {@link FinalComponentState}.
     * 
     * @param componentInstanceId the component instance id
     * @param finalState the final component state
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setComponentInstanceFinalState(Long componentInstanceId, FinalComponentState finalState, Connection connection,
        boolean isRetry) throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_INSTANCE + SET + FINAL_STATE + EQUAL + QMARK
            + WHERE + COMPONENT_INSTANCE_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, finalState.toString());
        stmt.setLong(2, componentInstanceId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates the end time of the {@link TimelineInterval} dataset with the given {@link ComponentRun} id.
     * 
     * @param componentRunId the component run id
     * @param endtime the endtime to update
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setComponentRunFinished(Long componentRunId, Long endtime, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET + ENDTIME + EQUAL + QMARK
            + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, componentRunId);
        stmt.setString(3, TimelineIntervalType.COMPONENT_RUN.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates end time of the {@link TimelineInterval} dataset with the given {@link WorkflowRun} id.
     * 
     * @param workflowRunId the workflow run id
     * @param endtime the endtime to update
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setWorklfowRunEndtime(Long workflowRunId, Long endtime, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_TIMELINE_INTERVAL + SET + ENDTIME + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK + AND + TYPE + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setTimestamp(1, new Timestamp(endtime));
        stmt.setLong(2, workflowRunId);
        stmt.setString(3, TimelineIntervalType.WORKFLOW_RUN.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates {@link FinalWorkflowState} of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id
     * @param finalState the final state
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setWorkflowRunFinalState(Long workflowRunId, FinalWorkflowState finalState, Connection connection,
        boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + FINAL_STATE + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, finalState.toString());
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates the string representation of the timeline data item of the {@link WorkflowRun} dataset with the given id.
     * 
     * @param workflowRunId the workflow run id
     * @param timelineDataItem the string representation of the timeline data item
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setOrUpdateTimelineDataItem(Long workflowRunId, String timelineDataItem, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_WORKFLOW_RUN + SET + TIMELINE_DATA_ITEM + EQUAL + QMARK
            + WHERE + WORKFLOW_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, timelineDataItem);
        stmt.setLong(2, workflowRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Returns the corresponding {@link WorkflowRun} id of the {@link ComponentInstance} dataset with the given id.
     * 
     * @param componentInstanceId the component instance id
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the id of the workflow run dataset
     * @throws SQLException thrown on database SQL errors
     */
    public Long getWorkflowRunIdByComponentInstanceId(Long componentInstanceId, Connection connection, boolean isRetry)
        throws SQLException {
        String sql =
            SELECT + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + FROM + DB_PREFIX + TABLE_WORKFLOW_RUN + INNER_JOIN + TABLE_COMPONENT_INSTANCE + ON + TABLE_COMPONENT_INSTANCE + DOT
                + WORKFLOW_RUN_ID + EQUAL + TABLE_WORKFLOW_RUN + DOT + WORKFLOW_RUN_ID
                + WHERE + TABLE_COMPONENT_INSTANCE + DOT + COMPONENT_INSTANCE_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, componentInstanceId);
        ResultSet rs = stmt.executeQuery();
        Long id = null;
        if (rs != null && rs.next()) {
            id = rs.getLong(WORKFLOW_RUN_ID);
            rs.close();
        }
        stmt.close();
        return id;
    }

    /**
     * Updates the string representation of the history data item of the {@link ComponentRun} dataset with the given id.
     * 
     * @param componentRunId the component run id
     * @param historyDataItem the string representation of the history data item
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @throws SQLException thrown on database SQL errors
     */
    public void setOrUpdateHistoryDataItem(Long componentRunId, String historyDataItem, Connection connection, boolean isRetry)
        throws SQLException {
        String sql = UPDATE + DB_PREFIX + TABLE_COMPONENT_RUN + SET + HISTORY_DATA_ITEM + EQUAL + QMARK
            + WHERE + COMPONENT_RUN_ID + EQUAL + QMARK;
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, historyDataItem);
        stmt.setLong(2, componentRunId);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Updates the {@link FinalWorkflowState} of the {@link WorkflowRun} dataset to the status corrupted if the current dataset has no
     * {@link FinalWorkflowState}.
     * 
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the count of updated datasets
     * @throws SQLException thrown on database SQL errors
     */
    public Integer cleanUpWorkflowRunFinalStates(Connection connection, boolean isRetry) throws SQLException {
        String sql = UPDATE + TABLE_WORKFLOW_RUN + SET + FINAL_STATE + EQUAL + SINGE_QOUTE + STRING_PLACEHOLDER + SINGE_QOUTE
            + WHERE + FINAL_STATE + IS_NULL;
        Statement stmt = connection.createStatement();
        int affectedLines = stmt.executeUpdate(StringUtils.format(sql, FinalWorkflowState.CORRUPTED));
        stmt.close();
        return affectedLines;
    }

    /**
     * Returns a map of {@link WorkflowRun} ids that are marked to be deleted with their corresponding integer based deletion types.
     * 
     * @param connection the connection to the meta data database
     * @param isRetry true if retrying
     * @return the map of workflow run ids and deletion types
     * @throws SQLException thrown on database SQL errors
     */
    public Map<Long, Integer> getWorkflowRunsToBeDeleted(Connection connection, boolean isRetry) throws SQLException {
        String sql = SELECT + WORKFLOW_RUN_ID + COMMA + TO_BE_DELETED + FROM + TABLE_WORKFLOW_RUN
            + WHERE + TO_BE_DELETED + NOT_EQUAL + QMARK;
        Map<Long, Integer> wfsToBeDeleted = new HashMap<Long, Integer>();
        PreparedStatement stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setInt(1, NOT_MARKED_TO_BE_DELETED);
        ResultSet rs = stmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                wfsToBeDeleted.put(rs.getLong(WORKFLOW_RUN_ID), rs.getInt(TO_BE_DELETED));
            }
            rs.close();
        }
        stmt.close();
        return wfsToBeDeleted;
    }

}
