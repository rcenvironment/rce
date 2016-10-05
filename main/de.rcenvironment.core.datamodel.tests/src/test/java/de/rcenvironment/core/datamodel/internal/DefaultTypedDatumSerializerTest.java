/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Test class for data serializer.
 * 
 * @author Jan Flink
 */
@RunWith(JUnit4.class)
public class DefaultTypedDatumSerializerTest {

    private static final int SIZE_1024 = 1024;

    private static final String COMMA_STRING = ",";

    private static final String SHORT_TEXT_TEST_VALUE = "test";

    private static final String JSON_STRING = "{\"t\":\"%s\",\"v\":%s}";

    private static final String JSON_STRING_TEXT = "{\"t\":\"%s\",\"v\":\"%s\"}";

    private static final String JSON_STRING_ARRAY = "{\"t\":\"%s\",\"v\":[%s]}";

    private static final String JSON_STRING_DIMENSION_ARRAY = "{\"t\":\"%s\",\"r\":%s,\"c\":%s,\"v\":[%s]}";

    private static DefaultTypedDatumFactory factory;

    private static DefaultTypedDatumSerializer serializer;

    /**
     * JUnit rule declaration for checking exceptions. Must be public.
     */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Initialization method.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        factory = new DefaultTypedDatumFactory();
        serializer = new DefaultTypedDatumSerializer();
    }

    /**
     * Test.
     */
    @Test
    public void testNullPointerException() {
        exception.expect(NullPointerException.class);
        serializer.serialize(null);
        TypedDatum dummy = new TypedDatum() {

            @Override
            public DataType getDataType() {
                return null;
            }
        };
        serializer.serialize(dummy);
    }

    /**
     * Test.
     */
    @Test
    public void testIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        TypedDatum dummy = new TypedDatum() {

            @Override
            public DataType getDataType() {
                return DataType.StructuredData;
            }
        };
        serializer.serialize(dummy);
        serializer.deserialize("");
        serializer.deserialize("no-json-string");
    }

    /**
     * Test.
     */
    @Test
    public void testSerialize() {

        // Serialization of BooleanTD
        BooleanTD boolFalse = factory.createBoolean(false);
        assertEquals(StringUtils.format(JSON_STRING_TEXT, DataType.Boolean.getShortName(), Boolean.FALSE.toString()),
            serializer.serialize(boolFalse));

        // Serialization of BooleanTD
        BooleanTD boolTrue = factory.createBoolean(true);
        assertEquals(StringUtils.format(JSON_STRING_TEXT, DataType.Boolean.getShortName(), Boolean.TRUE.toString()),
            serializer.serialize(boolTrue));

        // Serialization of ShortTextTD
        ShortTextTD shortText = factory.createShortText(SHORT_TEXT_TEST_VALUE);
        assertEquals(StringUtils.format(JSON_STRING_TEXT, DataType.ShortText.getShortName(), SHORT_TEXT_TEST_VALUE),
            serializer.serialize(shortText));

        // Serialization of IntegerTD
        IntegerTD integer = factory.createInteger(0);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), 0), serializer.serialize(integer));

        integer = factory.createInteger(1);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), 1), serializer.serialize(integer));

        integer = factory.createInteger(Long.MAX_VALUE);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), Long.MAX_VALUE), serializer.serialize(integer));

        long anyLong = Integer.MAX_VALUE + Integer.MAX_VALUE/2;
        integer = factory.createInteger(anyLong);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), anyLong), serializer.serialize(integer));

        integer = factory.createInteger(Long.MIN_VALUE);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), Long.MIN_VALUE), serializer.serialize(integer));

        // Serialization of FloatTD
        FloatTD floatData = factory.createFloat(0.0);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), 0.0), serializer.serialize(floatData));

        floatData = factory.createFloat(1.0);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), 1.0), serializer.serialize(floatData));

        floatData = factory.createFloat(Double.MAX_VALUE);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), Double.MAX_VALUE), serializer.serialize(floatData));

        floatData = factory.createFloat(Double.MIN_VALUE);
        assertEquals(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), Double.MIN_VALUE), serializer.serialize(floatData));

        // Serialization of DateTimeTD
        Long currentMillis = System.currentTimeMillis();
        DateTimeTD dateTime = factory.createDateTime(currentMillis);
        assertEquals(StringUtils.format(JSON_STRING, DataType.DateTime.getShortName(), String.valueOf(currentMillis)),
            serializer.serialize(dateTime));

        // Serialization of VectorTD
        FloatTD[] vectorEntries =
        { factory.createFloat(1.0), factory.createFloat(2.0), factory.createFloat(3.0) };
        VectorTD vector = factory.createVector(vectorEntries);
        assertEquals(getVectorString(vectorEntries), serializer.serialize(vector));

        FloatTD[] vectorEntries2 =
        { factory.createFloat(2.0), factory.createFloat(3.0), factory.createFloat(4.0) };
        vector = factory.createVector(vectorEntries2);
        assertEquals(getVectorString(vectorEntries2), serializer.serialize(vector));

        FloatTD[] vectorEntries3 =
        { factory.createFloat(Double.MAX_VALUE), factory.createFloat(0.0), factory.createFloat(Double.MIN_VALUE) };
        vector = factory.createVector(vectorEntries3);
        assertEquals(getVectorString(vectorEntries3), serializer.serialize(vector));

        // Serialization of MatrixTD
        FloatTD[][] matrixEntries =
        { vectorEntries, vectorEntries2, vectorEntries3 };
        MatrixTD matrix = factory.createMatrix(matrixEntries);
        assertEquals(getMatrixString(matrixEntries), serializer.serialize(matrix));

        // Serialization of SmallTableTD
        TypedDatum[][] tableEntries =
        { vectorEntries, vectorEntries2, vectorEntries3 };
        SmallTableTD smallTable = factory.createSmallTable(matrix.toArray());
        assertEquals(getSmallTableString(tableEntries), serializer.serialize(smallTable));

        TypedDatum[][] tableEntries2 =
        { { boolFalse, integer, floatData }, { dateTime, shortText, integer }, { floatData, floatData, floatData } };
        smallTable = factory.createSmallTable(3, 3);
        smallTable.setTypedDatumForCell(boolFalse, 0, 0);
        smallTable.setTypedDatumForCell(integer, 0, 1);
        smallTable.setTypedDatumForCell(floatData, 0, 2);
        smallTable.setTypedDatumForCell(dateTime, 1, 0);
        smallTable.setTypedDatumForCell(shortText, 1, 1);
        smallTable.setTypedDatumForCell(integer, 1, 2);
        smallTable.setTypedDatumForCell(floatData, 2, 0);
        smallTable.setTypedDatumForCell(floatData, 2, 1);
        smallTable.setTypedDatumForCell(floatData, 2, 2);
        assertEquals(getSmallTableString(tableEntries2), serializer.serialize(smallTable));

        // Serialization of EmptyTD
        EmptyTD empty = factory.createEmpty();
        assertEquals("{\"t\":\"" + DataType.Empty.getShortName() + "\"}", serializer.serialize(empty));

        // Serialization of FileReferenceTD
        FileReferenceTD fileReference = factory.createFileReference("example reference", "example filename");
        assertEquals(
            getFileReferenceString(),
            serializer.serialize(fileReference));
        fileReference.setLastModified(new Date(0));
        fileReference.setFileSize(SIZE_1024);
        assertEquals(getFileReferenceWithModifiedString(),
            serializer.serialize(fileReference));
    }

    /**
     * Test.
     */
    @Test
    public void testDeserialize() {

        // Deserialization of BooleanTD
        BooleanTD boolFalse = factory.createBoolean(false);
        assertEquals(boolFalse,
            serializer.deserialize(StringUtils.format(JSON_STRING_TEXT, DataType.Boolean.getShortName(), Boolean.FALSE.toString())));

        BooleanTD boolTrue = factory.createBoolean(true);
        assertEquals(boolTrue,
            serializer.deserialize(StringUtils.format(JSON_STRING_TEXT, DataType.Boolean.getShortName(), Boolean.TRUE.toString())));

        // Deserialization of ShortTextTD
        ShortTextTD shortText = factory.createShortText(SHORT_TEXT_TEST_VALUE);
        assertEquals(shortText,
            serializer.deserialize(StringUtils.format(JSON_STRING_TEXT, DataType.ShortText.getShortName(), SHORT_TEXT_TEST_VALUE)));

        // Deserialization of IntegerTD
        IntegerTD integer = factory.createInteger(5);
        assertEquals(integer, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), 5)));

        integer = factory.createInteger(Long.MAX_VALUE);
        assertEquals(integer, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), Long.MAX_VALUE)));

        long anyLong = Integer.MAX_VALUE + Integer.MAX_VALUE/2;
        integer = factory.createInteger(anyLong);
        assertEquals(integer, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), anyLong)));

        integer = factory.createInteger(Long.MIN_VALUE);
        assertEquals(integer, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Integer.getShortName(), Long.MIN_VALUE)));

        // Deserialization of FloatTD
        FloatTD floatData = factory.createFloat(5.0);
        assertEquals(floatData, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), 5.0)));

        floatData = factory.createFloat(Double.MAX_VALUE);
        assertEquals(floatData, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), Double.MAX_VALUE)));

        floatData = factory.createFloat(Double.MIN_VALUE);
        assertEquals(floatData, serializer.deserialize(StringUtils.format(JSON_STRING, DataType.Float.getShortName(), Double.MIN_VALUE)));

        // Deserialization of DateTimeTD
        Long currentMillis = System.currentTimeMillis();
        DateTimeTD dateTime = factory.createDateTime(currentMillis);
        assertEquals(dateTime,
            serializer.deserialize(StringUtils.format(JSON_STRING, DataType.DateTime.getShortName(), String.valueOf(currentMillis))));

        // Deserialization of VectorTD
        FloatTD[] vectorEntries =
        { factory.createFloat(1.0), factory.createFloat(2.0), factory.createFloat(3.0) };
        FloatTD[] vectorEntries2 =
        { factory.createFloat(2.0), factory.createFloat(3.0), factory.createFloat(4.0) };
        FloatTD[] vectorEntries3 =
        { factory.createFloat(Double.MAX_VALUE), factory.createFloat(0.0), factory.createFloat(Double.MIN_VALUE) };
        VectorTD vector = factory.createVector(vectorEntries);
        assertEquals(vector, serializer.deserialize(getVectorString(vectorEntries)));

        // Deserialization of MatrixTD
        FloatTD[][] matrixEntries =
        { vectorEntries, vectorEntries2, vectorEntries3 };
        MatrixTD matrix = factory.createMatrix(matrixEntries);
        assertEquals(matrix, serializer.deserialize(getMatrixString(matrixEntries)));

        // Deserialization of SmallTableTD
        TypedDatum[][] tableEntries =
        { vectorEntries, vectorEntries2, vectorEntries3 };
        SmallTableTD smallTable = factory.createSmallTable(matrix.toArray());
        assertEquals(smallTable, serializer.deserialize(getSmallTableString(tableEntries)));

        TypedDatum[][] tableEntries2 =
        { { boolFalse, integer, floatData }, { dateTime, shortText, integer }, { floatData, floatData, floatData } };
        smallTable = factory.createSmallTable(3, 3);
        smallTable.setTypedDatumForCell(boolFalse, 0, 0);
        smallTable.setTypedDatumForCell(integer, 0, 1);
        smallTable.setTypedDatumForCell(floatData, 0, 2);
        smallTable.setTypedDatumForCell(dateTime, 1, 0);
        smallTable.setTypedDatumForCell(shortText, 1, 1);
        smallTable.setTypedDatumForCell(integer, 1, 2);
        smallTable.setTypedDatumForCell(floatData, 2, 0);
        smallTable.setTypedDatumForCell(floatData, 2, 1);
        smallTable.setTypedDatumForCell(floatData, 2, 2);
        assertEquals(smallTable,
            serializer.deserialize(getSmallTableString(tableEntries2)));

        // Deserialization of EmptyTD
        EmptyTD empty = factory.createEmpty();
        assertEquals(empty.toString(), serializer.deserialize("{\"t\":\"" + DataType.Empty.getShortName() + "\"}").toString());

        // Deserialization of FileReferenceTD
        FileReferenceTD fileReference = factory.createFileReference("example reference",
            "example filename");
        FileReferenceTD deserializedFileReference = (FileReferenceTD) serializer
            .deserialize(getFileReferenceString());
        assertEquals(fileReference.getFileName(), deserializedFileReference.getFileName());
        assertEquals(fileReference.getFileReference(), deserializedFileReference.getFileReference());
        assertEquals(fileReference.getFileSizeInBytes(), deserializedFileReference.getFileSizeInBytes());
        assertEquals(fileReference.getLastModified(), deserializedFileReference.getLastModified());
        fileReference.setLastModified(new Date(0));
        fileReference.setFileSize(SIZE_1024);
        deserializedFileReference = (FileReferenceTD) serializer
            .deserialize(getFileReferenceWithModifiedString());
        assertEquals(fileReference.getFileName(), deserializedFileReference.getFileName());
        assertEquals(fileReference.getFileReference(), deserializedFileReference.getFileReference());
        assertEquals(fileReference.getFileSizeInBytes(), deserializedFileReference.getFileSizeInBytes());
        assertEquals(fileReference.getLastModified(), deserializedFileReference.getLastModified());

        // Deserialization of DirectoryReferenceTD
        DirectoryReferenceTD dirRef = factory.createDirectoryReference("reference", "dirname");
        dirRef.setDirectorySize(SIZE_1024);
        DirectoryReferenceTD otherDirRef = (DirectoryReferenceTD) serializer.deserialize(serializer.serialize(dirRef));
        assertEquals(dirRef.getDirectoryReference(), otherDirRef.getDirectoryReference());
        assertEquals(dirRef.getDirectoryName(), otherDirRef.getDirectoryName());
        assertEquals(dirRef.getDirectorySizeInBytes(), otherDirRef.getDirectorySizeInBytes());
    }

    private String getFileReferenceString() {
        return StringUtils.format(JSON_STRING, DataType.FileReference.getShortName(),
            "{\"fileReference\":\"example reference\",\"fileName\"" + ":\"example filename\",\"fileSize\":0}");
    }

    private String getFileReferenceWithModifiedString() {
        return StringUtils.format(JSON_STRING, DataType.FileReference.getShortName(),
            "{\"fileReference\":\"example reference\",\"fileName\"" + ":\"example filename\",\"fileSize\":1024,\"lastModified\":0}");
    }

    private String getVectorString(FloatTD[] vectorEntries) {
        String value = "";
        for (int i = 0; i < vectorEntries.length; i++) {
            value += vectorEntries[i];
            if (i < vectorEntries.length - 1) {
                value += COMMA_STRING;
            }
        }
        return StringUtils.format(JSON_STRING_ARRAY, DataType.Vector.getShortName(), value);
    }

    private String getMatrixString(FloatTD[][] matrixEntries) {
        String value = "";
        int rows = matrixEntries.length;
        int cols = 0;
        for (int i = 0; i < rows; i++) {
            if (matrixEntries[i].length > cols) {
                cols = matrixEntries[i].length;
            }
            value += "[";
            for (int j = 0; j < cols; j++) {
                value += matrixEntries[i][j];
                if (j < matrixEntries[i].length - 1) {
                    value += COMMA_STRING;
                }
            }
            value += "]";
            if (i < matrixEntries.length - 1) {
                value += COMMA_STRING;
            }
        }
        return StringUtils.format(JSON_STRING_DIMENSION_ARRAY, DataType.Matrix.getShortName(), rows, cols, value);
    }

    private String getSmallTableString(TypedDatum[][] tableEntries) {
        String value = "";
        int rows = tableEntries.length;
        int cols = 0;
        for (int i = 0; i < rows; i++) {
            if (tableEntries[i].length > cols) {
                cols = tableEntries[i].length;
            }
            value += "[";
            for (int j = 0; j < cols; j++) {
                value += serializer.serialize(tableEntries[i][j]);
                if (j < tableEntries[i].length - 1) {
                    value += COMMA_STRING;
                }
            }
            value += "]";
            if (i < tableEntries.length - 1) {
                value += COMMA_STRING;
            }
        }
        return StringUtils.format(JSON_STRING_DIMENSION_ARRAY, DataType.SmallTable.getShortName(), rows, cols, value);
    }

}
