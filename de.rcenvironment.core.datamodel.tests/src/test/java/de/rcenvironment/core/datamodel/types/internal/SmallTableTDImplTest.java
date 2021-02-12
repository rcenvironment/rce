/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.datamodel.types.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Test cases for {@link SmallTableTDImpl}.
 * 
 * @author Doreen Seider
 */
public class SmallTableTDImplTest {

    /**
     * Tests if invalid data types for cells are rejected as expected.
     */
    @Test
    public void testHandlingOfValidAndInvalidDataTypesOfCells() {
        SmallTableTDImpl smallTable = new SmallTableTDImpl(new TypedDatum[1][1]);
        
        DataType[] validDataTypes = { DataType.Integer, DataType.ShortText, DataType.Float, DataType.Boolean, DataType.DateTime,
            DataType.Empty };
        
        DataType[] invalidDataTypes = { DataType.Vector, DataType.Matrix, DataType.SmallTable, DataType.FileReference, DataType.BigTable,
            DataType.DirectoryReference, DataType.DirectoryReference };
        
        for (DataType validDataType : validDataTypes) {
            smallTable.setTypedDatumForCell(createTypedDatumMocks(validDataType), 0, 0);
        }
        
        for (DataType invalidDataType : invalidDataTypes) {
            try {
                smallTable.setTypedDatumForCell(createTypedDatumMocks(invalidDataType), 0, 0);
                fail("IllegalArgumentException expected for data type: " + invalidDataType);
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("is not allowed"));
            }
        }
    }
    
    private TypedDatum createTypedDatumMocks(DataType dataType) {
        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        EasyMock.expect(typedDatumMock.getDataType()).andStubReturn(dataType);
        EasyMock.replay(typedDatumMock);
        return typedDatumMock;
    }

}
