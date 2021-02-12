/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link TypedDatumSerializer} implementation.
 * 
 * @author Jan Flink
 * @author Doreen Seider
 */
public class DefaultTypedDatumSerializer implements TypedDatumSerializer {

    private static final String UNABLE_TO_SERIALIZE_STRING = "Serialization of %s is not supported.";

    private static final String UNABLE_TO_DESERIALIZE_STRING = "Could not deserialize \"%s\"";

    private static final String FILE_NAME_STRING = "fileName";

    private static final String FILE_REFERENCE_STRING = "fileReference";

    private static final String FILE_SIZE_STRING = "fileSize";

    private static final String DIRECTORY_NAME_STRING = "directoryName";

    private static final String DIRECTORY_REFERENCE_STRING = "directoryReference";

    private static final String DIRECTORY_SIZE_STRING = "directorySize";

    private static final String LAST_MODIFIED_STRING = "lastModified";

    private static final String TYPE_STRING = "t";

    private static final String VALUE_STRING = "v";

    private static final String ROW_STRING = "r";

    private static final String COLUMN_STRING = "c";
    
    private static final String ID_STRING = "id";

    private static final Log LOGGER = LogFactory.getLog(DefaultTypedDatumSerializer.class);

    private static final ObjectMapper MAPPER = JsonUtils.getDefaultObjectMapper();

    @Override
    public TypedDatum deserialize(String input) {
        TypedDatum returnDatum = null;

        if (input.length() == 0) {
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input.toString()));
        }

        try {
            JsonNode rootNode = MAPPER.readTree(input);
            DataType dataType = DataType.byShortName(rootNode.get(TYPE_STRING).textValue());
            JsonNode valueNode = rootNode.get(VALUE_STRING);
            returnDatum = getTypedDatumFromNode(dataType, rootNode, valueNode);
        } catch (JsonParseException e) {
            LOGGER.error(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input));
        } catch (JsonProcessingException e) {
            LOGGER.error(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input));
        } catch (IOException e) {
            LOGGER.error(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, input));
        }

        return returnDatum;
    }

    private TypedDatum getTypedDatumFromNode(DataType dataType, JsonNode rootNode, JsonNode valueNode)
        throws JsonProcessingException, IOException {
        TypedDatum returnDatum;
        DefaultTypedDatumFactory factory = new DefaultTypedDatumFactory();
        switch (dataType) {
        case Boolean:
            returnDatum = factory.createBoolean(valueNode.asBoolean());
            break;
        case ShortText:
            returnDatum = factory.createShortText(valueNode.asText());
            break;
        case Integer:
            returnDatum = factory.createInteger(valueNode.asLong());
            break;
        case Float:
            returnDatum = factory.createFloat(valueNode.asDouble());
            break;
        case DateTime:
            returnDatum = factory.createDateTime(valueNode.asLong());
            break;
        case Vector:
            VectorTD vector = factory.createVector(valueNode.size());
            for (int i = 0; i < valueNode.size(); i++) {
                vector.setFloatTDForElement(factory.createFloat(valueNode.get(i).asDouble()), i);
            }
            returnDatum = vector;
            break;
        case Matrix:
            ArrayNode matrixArray = (ArrayNode) valueNode;
            MatrixTD matrix = factory.createMatrix(rootNode.get(ROW_STRING).asInt(), rootNode.get(COLUMN_STRING).asInt());
            for (int i = 0; i < matrixArray.size(); i++) {
                ArrayNode matrixRowArray = (ArrayNode) matrixArray.get(i);
                for (int j = 0; j < matrixRowArray.size(); j++) {
                    FloatTD value = factory.createFloat(matrixRowArray.get(j).asDouble());
                    matrix.setFloatTDForElement(value,
                        i, j);
                }
            }
            returnDatum = matrix;
            break;
        case SmallTable:
            SmallTableTD smallTable =
                factory.createSmallTable(rootNode.get(ROW_STRING).asInt(), rootNode.get(COLUMN_STRING).asInt());
            ArrayNode tableArray = (ArrayNode) valueNode;
            for (int i = 0; i < tableArray.size(); i++) {
                ArrayNode tableRowArray = (ArrayNode) tableArray.get(i);
                for (int j = 0; j < tableRowArray.size(); j++) {

                    // To remain compatibility to all versions below 7.1, there must this diffenerce in deserializing small table
                    if (tableRowArray.get(j) instanceof TextNode) {
                        smallTable.setTypedDatumForCell(deserialize(tableRowArray.get(j).asText()), i, j);
                    } else {
                        DataType dataTypeTableEntry = DataType.byShortName(tableRowArray.get(j).get(TYPE_STRING).asText());
                        JsonNode valueNodeTableEntry = tableRowArray.get(j).get(VALUE_STRING);
                        smallTable.setTypedDatumForCell(
                            getTypedDatumFromNode(dataTypeTableEntry, tableRowArray.get(j), valueNodeTableEntry), i, j);
                    }
                }
            }
            returnDatum = smallTable;
            break;
        case NotAValue:
            // backward-compatibility to RCE version < 8.0.0 if value is read from data management
            if (valueNode.isValueNode()) {
                String id = valueNode.textValue();
                final String failureCaseSuffix = "_flr";
                if (id.endsWith(failureCaseSuffix)) {
                    returnDatum = factory.createNotAValue(id, NotAValueTD.Cause.Failure);
                } else {
                    returnDatum = factory.createNotAValue(id, NotAValueTD.Cause.InvalidInputs);
                }
            } else {
                NotAValueTD notAValue = factory.createNotAValue(valueNode.get(ID_STRING).textValue(),
                    NotAValueTD.Cause.valueOf(valueNode.get(TYPE_STRING).textValue()));
                returnDatum = notAValue;
            }
            break;
        case Empty:
            returnDatum = factory.createEmpty();
            break;
        case FileReference:
            FileReferenceTD fileReference =
                factory.createFileReference(valueNode.get(FILE_REFERENCE_STRING).textValue(),
                    valueNode.get(FILE_NAME_STRING).textValue());
            fileReference.setFileSize(valueNode.get(FILE_SIZE_STRING).longValue());
            if (valueNode.has(LAST_MODIFIED_STRING) && !valueNode.get(LAST_MODIFIED_STRING).isNull()) {
                fileReference.setLastModified(new Date(valueNode.get(LAST_MODIFIED_STRING).asLong()));
            }
            returnDatum = fileReference;
            break;
        case DirectoryReference:
            DirectoryReferenceTD directoryReference =
                factory.createDirectoryReference(valueNode.get(DIRECTORY_REFERENCE_STRING).textValue(),
                    valueNode.get(DIRECTORY_NAME_STRING).textValue());
            directoryReference.setDirectorySize(valueNode.get(DIRECTORY_SIZE_STRING).asLong());
            returnDatum = directoryReference;
            break;
        case StructuredData:
        case BigTable:
        default:
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_DESERIALIZE_STRING, rootNode.toString()));
        }
        return returnDatum;
    }

    @Override
    public String serialize(TypedDatum input) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        if (input == null || input.getDataType() == null) {
            throw new NullPointerException();
        }
        rootNode.put(TYPE_STRING, input.getDataType().getShortName());
        switch (input.getDataType()) {
        case Boolean:
            rootNode.put(VALUE_STRING, ((BooleanTD) input).getBooleanValue());
            break;
        case ShortText:
            rootNode.put(VALUE_STRING, ((ShortTextTD) input).getShortTextValue());
            break;
        case Integer:
            IntegerTD integer = (IntegerTD) input;
            rootNode.put(VALUE_STRING, integer.getIntValue());
            break;
        case Float:
            FloatTD floatData = (FloatTD) input;
            rootNode.put(VALUE_STRING, floatData.getFloatValue());
            break;
        case DateTime:
            DateTimeTD dateTime = (DateTimeTD) input;
            rootNode.put(VALUE_STRING, dateTime.getDateTimeInMilliseconds());
            break;
        case Vector:
            VectorTD vector = (VectorTD) input;
            ArrayNode vectorArray = mapper.createArrayNode();
            for (int i = 0; i < vector.getRowDimension(); i++) {
                vectorArray.add(vector.getFloatTDOfElement(i).getFloatValue());
            }
            rootNode.set(VALUE_STRING, vectorArray);
            break;
        case Matrix:
            MatrixTD matrix = (MatrixTD) input;
            rootNode.put(ROW_STRING, matrix.getRowDimension());
            rootNode.put(COLUMN_STRING, matrix.getColumnDimension());
            ArrayNode matrixArray = mapper.createArrayNode();
            for (int i = 0; i < matrix.getRowDimension(); i++) {
                ArrayNode matrixRowArray = mapper.createArrayNode();
                for (int j = 0; j < matrix.getColumnDimension(); j++) {
                    matrixRowArray.add(matrix.getFloatTDOfElement(i, j).getFloatValue());
                }
                matrixArray.add(matrixRowArray);
            }
            rootNode.set(VALUE_STRING, matrixArray);
            break;
        case SmallTable:
            SmallTableTD smallTable = (SmallTableTD) input;
            rootNode.put(ROW_STRING, smallTable.getRowCount());
            rootNode.put(COLUMN_STRING, smallTable.getColumnCount());
            ArrayNode smallTableArray = mapper.createArrayNode();
            for (int i = 0; i < smallTable.getRowCount(); i++) {
                ArrayNode smallTableRowArray = mapper.createArrayNode();
                for (int j = 0; j < smallTable.getColumnCount(); j++) {
                    smallTableRowArray.addPOJO(serialize(smallTable.getTypedDatumOfCell(i, j)));
                }
                smallTableArray.add(smallTableRowArray);
            }
            rootNode.set(VALUE_STRING, smallTableArray);
            break;
        case FileReference:
            ObjectNode fileObjectNode = mapper.createObjectNode();
            FileReferenceTD fileReference = (FileReferenceTD) input;
            fileObjectNode.put(FILE_REFERENCE_STRING, fileReference.getFileReference());
            fileObjectNode.put(FILE_NAME_STRING, fileReference.getFileName());
            fileObjectNode.put(FILE_SIZE_STRING, fileReference.getFileSizeInBytes());
            if (fileReference.getLastModified() != null) {
                fileObjectNode.put(LAST_MODIFIED_STRING, fileReference.getLastModified().getTime());
            }
            rootNode.set(VALUE_STRING, fileObjectNode);
            break;
        case DirectoryReference:
            ObjectNode dirObjectNode = mapper.createObjectNode();
            DirectoryReferenceTD directoryReference = (DirectoryReferenceTD) input;
            dirObjectNode.put(DIRECTORY_REFERENCE_STRING, directoryReference.getDirectoryReference());
            dirObjectNode.put(DIRECTORY_NAME_STRING, directoryReference.getDirectoryName());
            dirObjectNode.put(DIRECTORY_SIZE_STRING, directoryReference.getDirectorySizeInBytes());
            rootNode.set(VALUE_STRING, dirObjectNode);
            break;
        case NotAValue:
            ObjectNode notAValueObjectNode = mapper.createObjectNode();
            NotAValueTD notAValue = (NotAValueTD) input;
            notAValueObjectNode.put(ID_STRING, notAValue.getIdentifier());
            notAValueObjectNode.put(TYPE_STRING, notAValue.getCause().name());
            rootNode.set(VALUE_STRING, notAValueObjectNode);
            break;
        case Empty:
            break;
        case BigTable:
        case StructuredData:
        default:
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_SERIALIZE_STRING, input.getDataType().getDisplayName()));
        }
        try {
            return mapper.writeValueAsString(rootNode);
        } catch (IOException e) {
            throw new IllegalArgumentException(StringUtils.format(UNABLE_TO_SERIALIZE_STRING, input.getDataType().getDisplayName()), e);
        }
    }

}
