/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

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
    
    private static final Log LOGGER = LogFactory.getLog(DefaultTypedDatumSerializer.class);

    @Override
    public TypedDatum deserialize(String input) {
        TypedDatum returnDatum = null;

        if (input.length() == 0) {
            throw new IllegalArgumentException(String.format(UNABLE_TO_DESERIALIZE_STRING, input.toString()));
        }

        DefaultTypedDatumFactory factory = new DefaultTypedDatumFactory();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(input);
            DataType dataType = DataType.byShortName(rootNode.get(TYPE_STRING).getTextValue());
            JsonNode valueNode = rootNode.get(VALUE_STRING);
            switch (dataType) {
            case Boolean:
                returnDatum = factory.createBoolean(Boolean.parseBoolean(valueNode.getTextValue()));
                break;
            case ShortText:
                returnDatum = factory.createShortText(valueNode.getTextValue());
                break;
            case Integer:
                returnDatum = factory.createInteger(valueNode.getIntValue());
                break;
            case Float:
                returnDatum = factory.createFloat(valueNode.getDoubleValue());
                break;
            case DateTime:
                returnDatum = factory.createDateTime(valueNode.getLongValue());
                break;
            case Vector:
                VectorTD vector = factory.createVector(valueNode.size());
                for (int i = 0; i < valueNode.size(); i++) {
                    vector.setFloatTDForElement(factory.createFloat(valueNode.get(i).getDoubleValue()), i);
                }
                returnDatum = vector;
                break;
            case Matrix:
                ArrayNode matrixArray = (ArrayNode) valueNode;
                MatrixTD matrix = factory.createMatrix(rootNode.get(ROW_STRING).getIntValue(), rootNode.get(COLUMN_STRING).getIntValue());
                for (int i = 0; i < matrixArray.size(); i++) {
                    ArrayNode matrixRowArray = (ArrayNode) matrixArray.get(i);
                    for (int j = 0; j < matrixRowArray.size(); j++) {
                        FloatTD value = factory.createFloat(matrixRowArray.get(j).getDoubleValue());
                        matrix.setFloatTDForElement(value,
                            i, j);
                    }
                }
                returnDatum = matrix;
                break;
            case SmallTable:
                SmallTableTD smallTable =
                    factory.createSmallTable(rootNode.get(ROW_STRING).getIntValue(), rootNode.get(COLUMN_STRING).getIntValue());
                ArrayNode tableArray = (ArrayNode) valueNode;
                for (int i = 0; i < tableArray.size(); i++) {
                    ArrayNode tableRowArray = (ArrayNode) tableArray.get(i);
                    for (int j = 0; j < tableRowArray.size(); j++) {
                        smallTable.setTypedDatumForCell(deserialize(tableRowArray.get(j).toString()), i, j);
                    }
                }
                returnDatum = smallTable;
                break;
            case NotAValue:
                returnDatum = factory.createNotAValue(valueNode.getTextValue());
                break;
            case Empty:
                returnDatum = factory.createEmpty();
                break;
            case FileReference:
                FileReferenceTD fileReference =
                    factory.createFileReference(valueNode.get(FILE_REFERENCE_STRING).getTextValue(),
                        valueNode.get(FILE_NAME_STRING).getTextValue());
                fileReference.setFileSize(valueNode.get(FILE_SIZE_STRING).getLongValue());
                if (valueNode.has(LAST_MODIFIED_STRING) && !valueNode.get(LAST_MODIFIED_STRING).isNull()) {
                    fileReference.setLastModified(new Date(valueNode.get(LAST_MODIFIED_STRING).getLongValue()));
                }
                returnDatum = fileReference;
                break;
            case DirectoryReference:
                DirectoryReferenceTD directoryReference =
                    factory.createDirectoryReference(valueNode.get(DIRECTORY_REFERENCE_STRING).getTextValue(),
                        valueNode.get(DIRECTORY_NAME_STRING).getTextValue());
                directoryReference.setDirectorySize(valueNode.get(DIRECTORY_SIZE_STRING).getLongValue());
                returnDatum = directoryReference;
                break;
            case StructuredData:
            case BigTable:
            default:
                throw new IllegalArgumentException(String.format(UNABLE_TO_DESERIALIZE_STRING, input));
            }

        } catch (JsonParseException e) {
            LOGGER.error(String.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(String.format(UNABLE_TO_DESERIALIZE_STRING, input));
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(String.format(UNABLE_TO_DESERIALIZE_STRING, input));
        } catch (IOException e) {
            LOGGER.error(String.format(UNABLE_TO_DESERIALIZE_STRING, input), e);
            throw new IllegalArgumentException(String.format(UNABLE_TO_DESERIALIZE_STRING, input));
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
        case ShortText:
            rootNode.put(VALUE_STRING, input.toString());
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
            rootNode.put(VALUE_STRING, vectorArray);
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
            rootNode.put(VALUE_STRING, matrixArray);
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
            rootNode.put(VALUE_STRING, smallTableArray);
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
            rootNode.put(VALUE_STRING, fileObjectNode);
            break;
        case DirectoryReference:
            ObjectNode dirObjectNode = mapper.createObjectNode();
            DirectoryReferenceTD directoryReference = (DirectoryReferenceTD) input;
            dirObjectNode.put(DIRECTORY_REFERENCE_STRING, directoryReference.getDirectoryReference());
            dirObjectNode.put(DIRECTORY_NAME_STRING, directoryReference.getDirectoryName());
            dirObjectNode.put(DIRECTORY_SIZE_STRING, directoryReference.getDirectorySizeInBytes());
            rootNode.put(VALUE_STRING, dirObjectNode);
            break;
        case NotAValue:
            rootNode.put(VALUE_STRING, ((NotAValueTD) input).getIdentifier());
            break;
        case Empty:
            break;
        case BigTable:
        case StructuredData:
        default:
            throw new IllegalArgumentException(String.format(UNABLE_TO_SERIALIZE_STRING, input.getDataType().getDisplayName()));
        }
        return rootNode.toString();
    }

}
