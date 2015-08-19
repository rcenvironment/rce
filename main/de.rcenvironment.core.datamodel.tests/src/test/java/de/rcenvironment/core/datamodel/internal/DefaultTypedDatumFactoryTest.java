/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import java.util.Calendar;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

/**
 * Test class for new data types factory.
 * 
 * @author Sascha Zur
 * @author Jan Flink
 */
public class DefaultTypedDatumFactoryTest {

    private static final String GREATER_ZERO = "greater than 0";

    private static final int MINUS_ONE = -1;

    /**
     * JUnit rule declaration for checking exceptions. Must be public.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DefaultTypedDatumFactory factory;

    /**
     * Initialization method.
     */
    @Before
    public void setUp() {
        factory = new DefaultTypedDatumFactory();
    }

    /**
     * Test for creating a {@link BooleanTD} data type.
     */
    @Test
    public void testCreateBoolean() {
        BooleanTD boolFalse = factory.createBoolean(false);
        Assert.assertFalse(boolFalse.getBooleanValue());

        BooleanTD boolTrue = factory.createBoolean(true);
        Assert.assertTrue(boolTrue.getBooleanValue());

        Assert.assertEquals(DataType.Boolean, boolFalse.getDataType());
        Assert.assertEquals(DataType.Boolean, boolTrue.getDataType());
    }

    /**
     * Test for creating a {@link IntegerTD} data type.
     */
    @Test
    public void testCreateInteger() {
        IntegerTD integer = factory.createInteger(0);
        Assert.assertEquals(0, integer.getIntValue());

        IntegerTD integer2 = factory.createInteger(1);
        Assert.assertEquals(1, integer2.getIntValue());

        IntegerTD integer3 = factory.createInteger(MINUS_ONE);
        Assert.assertEquals(MINUS_ONE, integer3.getIntValue());

        IntegerTD integer4 = factory.createInteger(Integer.MAX_VALUE);
        Assert.assertEquals(Integer.MAX_VALUE, integer4.getIntValue());

        IntegerTD integer5 = factory.createInteger(Integer.MIN_VALUE);
        Assert.assertEquals(Integer.MIN_VALUE, integer5.getIntValue());

        IntegerTD integer6 = factory.createInteger(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, integer6.getIntValue());

        IntegerTD integer7 = factory.createInteger(Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, integer7.getIntValue());

        Assert.assertEquals(DataType.Integer, integer.getDataType());
        Assert.assertEquals(DataType.Integer, integer2.getDataType());
        Assert.assertEquals(DataType.Integer, integer3.getDataType());
        Assert.assertEquals(DataType.Integer, integer4.getDataType());
        Assert.assertEquals(DataType.Integer, integer5.getDataType());
        Assert.assertEquals(DataType.Integer, integer6.getDataType());
        Assert.assertEquals(DataType.Integer, integer7.getDataType());
    }

    /**
     * Test for creating a {@link FloatTD} data type.
     */
    @Test
    public void testCreateFloat() {
        FloatTD floatValue1 = factory.createFloat(0.0);
        Assert.assertEquals(0, floatValue1.getFloatValue(), .0);

        FloatTD floatValue2 = factory.createFloat(1.0);
        Assert.assertEquals(1, floatValue2.getFloatValue(), .0);

        FloatTD floatValue3 = factory.createFloat(Double.MIN_VALUE);
        Assert.assertEquals(Double.MIN_VALUE, floatValue3.getFloatValue(), .0);

        FloatTD floatValue4 = factory.createFloat(Double.MIN_NORMAL);
        Assert.assertEquals(Double.MIN_NORMAL, floatValue4.getFloatValue(), .0);

        FloatTD floatValue5 = factory.createFloat(Double.MAX_VALUE);
        Assert.assertEquals(Double.MAX_VALUE, floatValue5.getFloatValue(), .0);

        FloatTD floatValue6 = factory.createFloat(Double.NEGATIVE_INFINITY);
        Assert.assertEquals(Double.NEGATIVE_INFINITY, floatValue6.getFloatValue(), .0);

        FloatTD floatValue7 = factory.createFloat(Double.POSITIVE_INFINITY);
        Assert.assertEquals(Double.POSITIVE_INFINITY, floatValue7.getFloatValue(), .0);

        Assert.assertEquals(DataType.Float, floatValue1.getDataType());
        Assert.assertEquals(DataType.Float, floatValue2.getDataType());
        Assert.assertEquals(DataType.Float, floatValue3.getDataType());
        Assert.assertEquals(DataType.Float, floatValue4.getDataType());
        Assert.assertEquals(DataType.Float, floatValue5.getDataType());
        Assert.assertEquals(DataType.Float, floatValue6.getDataType());
        Assert.assertEquals(DataType.Float, floatValue7.getDataType());
    }

    /**
     * Test for creating a {@link VectorTD} data type.
     * 
     * @throws IllegalArgumentException if dimension is below 0
     */
    @Test
    public void testCreateVector() throws IllegalArgumentException {
        VectorTD vector = factory.createVector(1);
        Assert.assertEquals(DataType.Vector, vector.getDataType());
        Assert.assertEquals(0.0, vector.getFloatTDOfElement(0).getFloatValue(), 0.0);
        Assert.assertEquals(1, vector.getRowDimension());

        VectorTD vector1 = factory.createVector(5);
        Assert.assertEquals(DataType.Vector, vector1.getDataType());
        Assert.assertEquals(5, vector1.getRowDimension());
        FloatTD[] testEntries =
        { factory.createFloat(1.0), factory.createFloat(2.0), factory.createFloat(3.0), factory.createFloat(4.0) };
        VectorTD vector2 = factory.createVector(testEntries);
        Assert.assertEquals(4, vector2.getRowDimension());
        Assert.assertEquals(DataType.Vector, vector2.getDataType());
        Assert.assertEquals(factory.createFloat(1.0), vector2.getFloatTDOfElement(0));
        Assert.assertEquals(factory.createFloat(3.0), vector2.getFloatTDOfElement(2));

    }

    /**
     * Test for creating a {@link SmallTableTD} data type with dimension parameters.
     * 
     * @throws IllegalArgumentException Thrown if table dimension rows and/or columns <= 0
     */
    @Test
    public void testCreateSmallTableWithDimensions() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(GREATER_ZERO);
        factory.createSmallTable(0, 0);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(GREATER_ZERO);
        factory.createSmallTable(1, 0);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(GREATER_ZERO);
        factory.createSmallTable(0, 1);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(GREATER_ZERO);
        factory.createSmallTable(MINUS_ONE, 0);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(GREATER_ZERO);
        factory.createSmallTable(0, MINUS_ONE);

        SmallTableTD smallTable = factory.createSmallTable(4, 5);
        Assert.assertEquals(4, smallTable.getColumnCount());
        Assert.assertEquals(5, smallTable.getRowCount());

        Assert.assertNull(smallTable.getTypedDatumOfCell(0, 0));
        Assert.assertNull(smallTable.getTypedDatumOfCell(2, 3));
        Assert.assertNull(smallTable.getTypedDatumOfCell(3, 4));

        Assert.assertEquals(DataType.SmallTable, smallTable.getDataType());

    }

    /**
     * Test for creating a {@link SmallTableTD} data type.
     * 
     * @throws NullPointerException Thrown if parameter tableEntries is null
     */
    @Test
    public void testCreateSmallTable() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        factory.createSmallTable(null);

        TypedDatum[][] testEntries =
        { { factory.createInteger(1), factory.createInteger(2) }, { factory.createInteger(3), factory.createInteger(4) } };

        SmallTableTD smallTable = factory.createSmallTable(testEntries);
        Assert.assertEquals(2, smallTable.getColumnCount());
        Assert.assertEquals(2, smallTable.getRowCount());

        Assert.assertEquals(DataType.SmallTable, smallTable.getDataType());
        Assert.assertEquals(DataType.Integer, smallTable.getTypedDatumOfCell(0, 0).getDataType());
        Assert.assertEquals(factory.createInteger(1), smallTable.getTypedDatumOfCell(0, 0));
        Assert.assertEquals(factory.createInteger(4), smallTable.getTypedDatumOfCell(1, 1));
    }

    /**
     * Test for creating a {@link MatrixTD} data type.
     */
    @Test
    public void testCreateMatrix() {
        MatrixTD m = factory.createMatrix(5, 5);
        Assert.assertEquals(DataType.Matrix, m.getDataType());
        Assert.assertEquals(5, m.getColumnDimension());
        Assert.assertEquals(5, m.getRowDimension());
        Assert.assertEquals(0.0, m.getFloatTDOfElement(1, 1).getFloatValue(), 0.0);
    }

    /**
     * Test for creating a {@link SmallTableTD} data type.
     */
    @Test
    public void testCreateShortText() {
        ShortTextTD text = factory.createShortText("");
        Assert.assertEquals(DataType.ShortText, text.getDataType());
        Assert.assertEquals("", text.getShortTextValue());

        ShortTextTD textHallo = factory.createShortText("hallo");
        Assert.assertEquals("hallo", textHallo.getShortTextValue());

        ShortTextTD textDigit = factory.createShortText("" + 5);
        Assert.assertEquals("5", textDigit.getShortTextValue());
    }

    /**
     * Test for creating a {@link DateTimeTD}.
     */
    @Test
    public void testCreateDateTime() {
        long now = System.currentTimeMillis();
        DateTimeTD date = factory.createDateTime(now);
        Assert.assertEquals(DataType.DateTime, date.getDataType());
        Assert.assertEquals(now, date.getDateTimeInMilliseconds());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        Assert.assertEquals(calendar.getTime(), date.getDateTime());
    }

    /**
     * Test for type {@link FileReferenceTD}.
     * 
     * @throws NullPointerException Thrown if data reference or file name is null
     */
    @Test
    public void testCreateFileReference() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        FileReferenceTD emptyRef = factory.createFileReference(null, "");

        String reference = UUID.randomUUID().toString();
        FileReferenceTD nonEmpty = factory.createFileReference(reference, "filename");
        Assert.assertEquals("filename", nonEmpty.getFileName());
        Assert.assertEquals(reference, emptyRef.getFileReference());
    }

    /**
     * Test for type {@link DirectoryReferenceTD}.
     * 
     * @throws NullPointerException Thrown if data reference or file name is null
     */
    @Test
    public void testCreateDirectoryReference() throws NullPointerException {
        thrown.expect(NullPointerException.class);
        DirectoryReferenceTD emptyRef = factory.createDirectoryReference(null, "");

        String reference = UUID.randomUUID().toString();
        DirectoryReferenceTD nonEmpty = factory.createDirectoryReference(reference, "dirname");
        Assert.assertEquals("dirname", nonEmpty.getDirectoryName());
        Assert.assertEquals(reference, emptyRef.getDirectoryReference());
    }
}
