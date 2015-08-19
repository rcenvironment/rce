/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.datamodel.internal;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.types.api.BooleanTD;
import de.rcenvironment.core.datamodel.types.api.DateTimeTD;
import de.rcenvironment.core.datamodel.types.api.EmptyTD;
import de.rcenvironment.core.datamodel.types.api.FloatTD;
import de.rcenvironment.core.datamodel.types.api.IntegerTD;
import de.rcenvironment.core.datamodel.types.api.MatrixTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.datamodel.types.api.VectorTD;

/**
 * Test class for data type converter.
 * 
 * @author Jan Flink
 * @author Sascha Zur
 */
public class DefaultTypedDatumConverterTest {

    private static DefaultTypedDatumFactory factory;

    private static DefaultTypedDatumConverter converter;

    /**
     * JUnit rule declaration for checking exceptions. Must be public.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Initialization method.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        factory = new DefaultTypedDatumFactory();
        converter = new DefaultTypedDatumConverter();
    }

    /**
     * @throws DataTypeException Thrown if {@link DataType} of input and target are not convertible
     * 
     */
    @Test
    public void testCastOrConvertTypedDatumClassOfT() throws DataTypeException {
        // BooleanTD --> IntegerTD
        BooleanTD boolFalse = factory.createBoolean(false);
        IntegerTD integer0 = converter.castOrConvert(boolFalse, IntegerTD.class);
        Assert.assertEquals(factory.createInteger(0).getIntValue(), integer0.getIntValue());

        BooleanTD boolTrue = factory.createBoolean(true);
        IntegerTD integer1 = converter.castOrConvert(boolTrue, IntegerTD.class);
        Assert.assertEquals(factory.createInteger(1).getIntValue(), integer1.getIntValue());

        FloatTD float0 = converter.castOrConvert(integer0, FloatTD.class);
        Assert.assertEquals(factory.createFloat(0.0).getFloatValue(), float0.getFloatValue());

        FloatTD float1 = converter.castOrConvert(integer1, FloatTD.class);
        Assert.assertEquals(factory.createFloat(1.0).getFloatValue(), float1.getFloatValue());

    }

    /**
     * Test for data type converter.
     * 
     * @throws DataTypeException Thrown if {@link DataType} of input and target are not convertible
     * 
     */
    @Test
    public void testCastOrConvertTypedDatumDataType() throws DataTypeException {

        // BooleanTD --> IntegerTD
        BooleanTD boolFalse = factory.createBoolean(false);
        IntegerTD integer0 = (IntegerTD) converter.castOrConvert(boolFalse, DataType.Integer);
        Assert.assertEquals(factory.createInteger(0).getIntValue(), integer0.getIntValue());

        BooleanTD boolTrue = factory.createBoolean(true);
        IntegerTD integer1 = (IntegerTD) converter.castOrConvert(boolTrue, DataType.Integer);
        Assert.assertEquals(factory.createInteger(1).getIntValue(), integer1.getIntValue());

        // IntegerTD --> FloatTD
        FloatTD float0 = (FloatTD) converter.castOrConvert(integer0, DataType.Float);
        Assert.assertEquals(factory.createFloat(0.0).getFloatValue(), float0.getFloatValue());

        FloatTD float1 = (FloatTD) converter.castOrConvert(integer1, DataType.Float);
        Assert.assertEquals(factory.createFloat(1.0).getFloatValue(), float1.getFloatValue());

        // FloatTD --> VectorTD
        VectorTD vector0 = (VectorTD) converter.castOrConvert(float0, DataType.Vector);
        Assert.assertEquals(factory.createVector((int) integer1.getIntValue()), vector0);

        VectorTD vector1 = (VectorTD) converter.castOrConvert(float1, DataType.Vector);
        VectorTD vector2 = factory.createVector((int) integer1.getIntValue());
        vector2.setFloatTDForElement(float1, 0);
        Assert.assertEquals(vector2, vector1);

        // VectorTD --> MatrixTD
        MatrixTD matrix0 = (MatrixTD) converter.castOrConvert(vector0, DataType.Matrix);
        Assert.assertEquals(factory.createMatrix(1, 1), matrix0);

        FloatTD[] vectorEntries =
        { factory.createFloat(1.0), factory.createFloat(2.0), factory.createFloat(3.0) };
        VectorTD vector3 = factory.createVector(vectorEntries);

        MatrixTD matrix1 = (MatrixTD) converter.castOrConvert(vector3, DataType.Matrix);
        Assert.assertEquals(vector3, matrix1.getColumnVector(0));

        // MatrixTD --> SmallTableTD
        SmallTableTD smallTable0 = (SmallTableTD) converter.castOrConvert(matrix0, DataType.SmallTable);
        SmallTableTD createdTable = factory.createSmallTable(1, 1);
        createdTable.setTypedDatumForCell(factory.createFloat(0.0), 0, 0);
        Assert.assertEquals(createdTable, smallTable0);

        FloatTD[][] matrixEntries =
        { vectorEntries, vectorEntries, vectorEntries };
        MatrixTD matrix2 = factory.createMatrix(matrixEntries);

        SmallTableTD smallTable1 = (SmallTableTD) converter.castOrConvert(matrix2, DataType.SmallTable);
        Assert.assertEquals(factory.createSmallTable(matrixEntries), smallTable1);

        // BooleanTD --> MatrixTD
        Assert.assertEquals(matrix0, converter.castOrConvert(boolFalse, DataType.Matrix));

        // BooleanTD --> SmallTableTD
        SmallTableTD smallTable2 = (SmallTableTD) converter.castOrConvert(boolTrue, DataType.SmallTable);
        Assert.assertEquals(1, smallTable2.getColumnCount());
        Assert.assertEquals(1, smallTable2.getRowCount());
        Assert.assertEquals(boolTrue, smallTable2.getTypedDatumOfCell(0, 0));

        // DateTimeTD --> IntegerTD
        DateTimeTD dateTime = factory.createDateTime(0);
        IntegerTD convertedDateTime = converter.castOrConvert(dateTime, IntegerTD.class);
        Assert.assertEquals(0, convertedDateTime.getIntValue());

        // DateTimeTD --> MatrixTD
        dateTime = factory.createDateTime(0);
        MatrixTD convertedToMatrix = converter.castOrConvert(dateTime, MatrixTD.class);
        Assert.assertEquals(0.0, convertedToMatrix.getFloatTDOfElement(0, 0).getFloatValue());

        // DateTimeTD --> SmallTableTD
        dateTime = factory.createDateTime(0);
        SmallTableTD convertedToST = converter.castOrConvert(dateTime, SmallTableTD.class);
        Assert.assertEquals(DataType.DateTime, convertedToST.getTypedDatumOfCell(0, 0).getDataType());

        // ShortTextTD -> SmallTableTD
        ShortTextTD shortText = factory.createShortText("ErsteZelle");
        SmallTableTD convertedSTxtToSTab = converter.castOrConvert(shortText, SmallTableTD.class);
        Assert.assertEquals("ErsteZelle", ((ShortTextTD) convertedSTxtToSTab.getTypedDatumOfCell(0, 0)).getShortTextValue());

        // FileReferenceTD -> SmallTableTD
        // commented out because EasyMock throws an Exception mocking DataReference
        // DataReference dataRefMock = EasyMock.createNiceMock(DataReference.class);
        // FileReferenceTD fileRef = factory.createFileReference(dataRefMock, "filename");
        // SmallTableTD convertedFRToSTab = converter.castOrConvert(fileRef, SmallTableTD.class);
        // Assert.assertEquals(DataType.FileReference, convertedFRToSTab.getTypedDatumOfCell(0,
        // 0).getDataType());
        // Assert.assertEquals("filename", ((FileReferenceTD)
        // convertedFRToSTab.getTypedDatumOfCell(0, 0)).getFilename());
        // Assert.assertEquals(dataRefMock, ((FileReferenceTD)
        // convertedFRToSTab.getTypedDatumOfCell(0, 0)).getFileReference());

        // MatrixTD --> Boolean
        // Expects Exception
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("Can not convert");
        converter.castOrConvert(matrix1, DataType.Boolean);

        // IntegerTD --> Boolean
        // Expects Exception
        thrown.expect(DataTypeException.class);
        thrown.expectMessage("Can not convert");
        converter.castOrConvert(integer0, DataType.Boolean);

    }

    /**
     * Test.
     */
    @Test
    public void testIsConvertibleToTypedDatumDataType() {
        Assert.assertFalse(converter.isConvertibleTo(factory.createBoolean(true), DataType.Boolean));
        Assert.assertFalse(converter.isConvertibleTo(factory.createInteger(1), DataType.Boolean));
        Assert.assertTrue(converter.isConvertibleTo(factory.createInteger(1), DataType.Float));

        Assert.assertFalse(converter.isConvertibleTo(DataType.Boolean, DataType.Boolean));
        Assert.assertFalse(converter.isConvertibleTo(DataType.Integer, DataType.Boolean));
        Assert.assertTrue(converter.isConvertibleTo(DataType.Integer, DataType.Float));

        Assert.assertFalse(converter.isConvertibleTo(factory.createBoolean(true), BooleanTD.class));
        Assert.assertFalse(converter.isConvertibleTo(factory.createInteger(1), BooleanTD.class));
        Assert.assertTrue(converter.isConvertibleTo(factory.createInteger(1), FloatTD.class));

    }

    /**
     * @throws DataTypeException Thrown if {@link DataType} of input and target are not convertible
     * 
     */
    @Test
    public void testCastOrConvertUnsafeTypedDatumClassOfT() throws DataTypeException {

        // EmptyTD --> ShortTextTD
        EmptyTD empty = factory.createEmpty();
        ShortTextTD shortText0 = converter.castOrConvertUnsafe(empty, ShortTextTD.class);
        Assert.assertEquals(factory.createShortText("").getShortTextValue(), shortText0.getShortTextValue());

        // BooleanTD --> ShortTextTD
        BooleanTD boolFalse = factory.createBoolean(false);
        shortText0 = converter.castOrConvertUnsafe(boolFalse, ShortTextTD.class);
        Assert.assertEquals(factory.createShortText("false").getShortTextValue(), shortText0.getShortTextValue());

        // IntegerTD --> ShortTextTD
        IntegerTD int0 = factory.createInteger(6);
        shortText0 = converter.castOrConvertUnsafe(int0, ShortTextTD.class);
        Assert.assertEquals(factory.createShortText("6").getShortTextValue(), shortText0.getShortTextValue());

        // FloatTD --> ShortTextTD
        FloatTD float0 = factory.createFloat(1.0);
        shortText0 = converter.castOrConvertUnsafe(float0, ShortTextTD.class);
        Assert.assertEquals(factory.createShortText("1.0").getShortTextValue(), shortText0.getShortTextValue());
    }

    /**
     * Test.
     */
    @Test
    public void testIsUnsafeConvertibleToTypedDatumDataType() {
        Assert.assertFalse(converter.isUnsafeConvertibleTo(factory.createBoolean(true), DataType.Boolean));
        Assert.assertFalse(converter.isUnsafeConvertibleTo(factory.createMatrix(1, 1), DataType.ShortText));
        Assert.assertTrue(converter.isUnsafeConvertibleTo(factory.createInteger(1), DataType.ShortText));

        Assert.assertFalse(converter.isUnsafeConvertibleTo(DataType.Boolean, DataType.Boolean));
        Assert.assertFalse(converter.isUnsafeConvertibleTo(DataType.Matrix, DataType.ShortText));
        Assert.assertTrue(converter.isUnsafeConvertibleTo(DataType.Integer, DataType.ShortText));

        Assert.assertFalse(converter.isUnsafeConvertibleTo(factory.createBoolean(true), BooleanTD.class));
        Assert.assertFalse(converter.isUnsafeConvertibleTo(factory.createMatrix(1, 1), ShortTextTD.class));
        Assert.assertTrue(converter.isUnsafeConvertibleTo(factory.createInteger(1), ShortTextTD.class));

    }
}
