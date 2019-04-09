/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.database.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.rcenvironment.core.component.datamanagement.api.CommonComponentHistoryDataItem;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * History data item.
 *
 * @author Oliver Seebach
 */
public class DatabaseComponentHistoryDataItem extends CommonComponentHistoryDataItem {

    private static final long serialVersionUID = -8675762321318573550L;

    private static final String STATEMENT_PATTERN = "statementPattern";

    private static final String STATEMENT_EFFECTIVE = "statementEffective";

    private static final String STATEMENT_NAME = "statementName";

    private static final String STATEMENT_ID = "statementId";

    private static final String STATEMENT_ROOT = "statementRoot";

    private List<DatabaseStatementHistoryData> databaseStatementHistoryDataList = new ArrayList<>();

    private String identifier;

    public DatabaseComponentHistoryDataItem(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String serialize(TypedDatumSerializer serializer) throws IOException {
        String data = super.serialize(serializer);
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;

        try {
            rootNode = mapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
        ArrayNode statementsArrayNode = ((ObjectNode) rootNode).putArray(STATEMENT_ROOT);
        for (DatabaseStatementHistoryData historyDatum : databaseStatementHistoryDataList) {
            ObjectNode statementObjectNode = statementsArrayNode.addObject();
            statementObjectNode.put(STATEMENT_ID, historyDatum.getStatementIndex());
            statementObjectNode.put(STATEMENT_NAME, historyDatum.getStatementName());
            statementObjectNode.put(STATEMENT_PATTERN, historyDatum.getStatementPattern());
            statementObjectNode.put(STATEMENT_EFFECTIVE, historyDatum.getStatementEffective());
        }
        return rootNode.toString();
    }

    private static void readReferenceFromString(String historyData, DatabaseComponentHistoryDataItem historyDataItem) throws IOException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(historyData);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }

        JsonNode statementsNode = ((ObjectNode) rootNode).get(STATEMENT_ROOT);

        Iterator<JsonNode> statementNodeElements = statementsNode.elements();

        while (statementNodeElements.hasNext()) {
            JsonNode node = statementNodeElements.next();

            int index = node.get(STATEMENT_ID).asInt();
            String statementName = node.get(STATEMENT_NAME).asText();
            String statementPattern = node.get(STATEMENT_PATTERN).asText();
            String statementEffective = node.get(STATEMENT_EFFECTIVE).asText();

            historyDataItem.addDatabaseStatementHistoryData(index, statementName, statementPattern, statementEffective);
        }

    }

    /**
     * Creates a history data item from a string.
     * 
     * @param historyData The history data
     * @param serializer The serializer
     * @param identifier The identifier
     * @return The created history data item
     * @throws IOException Thrown when serialization failed.
     */
    public static DatabaseComponentHistoryDataItem fromString(String historyData, TypedDatumSerializer serializer, String identifier)
        throws IOException {
        DatabaseComponentHistoryDataItem historyDataItem = new DatabaseComponentHistoryDataItem(identifier);
        CommonComponentHistoryDataItem.initializeCommonHistoryDataFromString(historyDataItem, historyData, serializer);

        readReferenceFromString(historyData, historyDataItem);
        return historyDataItem;
    }

    /**
     * Add new database statement history data.
     * 
     * @param index the index
     * @param statementName the statement name
     * @param statementPattern the statement pattern
     * @param effectiveStatement the effective statement
     */
    public void addDatabaseStatementHistoryData(int index, String statementName, String statementPattern, String effectiveStatement) {
        databaseStatementHistoryDataList.add(new DatabaseStatementHistoryData(index, statementName, statementPattern, effectiveStatement));
    }

    public List<DatabaseStatementHistoryData> getDatabaseStatementHistoryDataList() {
        return databaseStatementHistoryDataList;
    }

}
