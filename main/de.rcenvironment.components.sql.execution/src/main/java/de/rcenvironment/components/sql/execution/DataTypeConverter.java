/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.sql.execution;

import java.io.Serializable;

import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumFactory;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.SmallTableTD;
import de.rcenvironment.core.utils.common.channel.legacy.VariantArray;
import de.rcenvironment.core.utils.common.variables.legacy.TypedValue;

/**
 *  Converts data from old data type concept to new one.
 *  @author Doreen Seider
 */
@SuppressWarnings("deprecation")
//This is a legacy class which will not be adapted to the new Data Types. Thus, the deprecation warnings are suppressed here.
public final class DataTypeConverter {
    
    private DataTypeConverter() {}
    
    /**
     * @param variantArray to convert
     * @param typedDatumService {@link TypedDatumService} instance
     * @return {@link SmallTableTD}
     */
    public static SmallTableTD convert(VariantArray variantArray, TypedDatumService typedDatumService) {
        TypedDatumFactory factory = typedDatumService.getFactory();
        int[] dimension = variantArray.getDimensions();
        TypedDatum[][] tableEntries = new TypedDatum[dimension[0]][dimension[1]];
        for (int i = 0; i < dimension[0]; i++) {
            for (int j = 0; j < dimension[1]; j++) {
                tableEntries[i][j] = convert(variantArray.getValue(i, j), typedDatumService);
            }
        }
        return factory.createSmallTable(tableEntries);
    }

    /**
     * @param typedValue to convert
     * @param typedDatumService {@link TypedDatumService} instance
     * @return {@link TypedDatum}
     */
    public static TypedDatum convert(TypedValue typedValue, TypedDatumService typedDatumService) {
        TypedDatum result;
        TypedDatumFactory factory = typedDatumService.getFactory();
        switch (typedValue.getType()) {
        case Empty:
            result = factory.createEmpty();
            break;
        case Integer:
            result = factory.createInteger(typedValue.getIntegerValue());
            break;
        case Logic:
            result = factory.createBoolean(typedValue.getLogicValue());
            break;
        case Real:
            result = factory.createFloat(typedValue.getRealValue());
            break;
        case String:
            result = factory.createShortText(typedValue.getStringValue());
            break;
        default:
            throw new UnsupportedOperationException("data type can not be converted: " + typedValue.getType());
        }
        return result;
    }
    
    /**
     * @param value to convert
     * @param datatype target {@link DataType}
     * @param typedDatumService {@link TypedDatumService} instance
     * @return {@link TypedDatum}
     */
    public static TypedDatum convert(Serializable value, DataType datatype, TypedDatumService typedDatumService) {
        TypedDatum result;
        TypedDatumFactory factory = typedDatumService.getFactory();
        switch (datatype) {
        case Empty:
            result = factory.createEmpty();
            break;
        case Integer:
            result = factory.createInteger((Integer) value);
            break;
        case Boolean:
            result = factory.createBoolean((Boolean) value);
            break;
        case Float:
            result = factory.createFloat((Float) value);
            break;
        case ShortText:
            result = factory.createShortText((String) value);
            break;
        default:
            throw new UnsupportedOperationException("data type can not be converted: " + datatype.toString());
        }
        return result;
    }
}
